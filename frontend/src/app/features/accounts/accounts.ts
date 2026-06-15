import { CurrencyPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize, forkJoin } from 'rxjs';

import { FinancialApiService } from '../../core/financial-api.service';
import { Account, AccountStatus, AccountType, Customer } from '../../core/models';

@Component({
  selector: 'app-accounts',
  imports: [CurrencyPipe, ReactiveFormsModule],
  template: `
    <div class="page-header">
      <div>
        <span class="eyebrow">Productos</span>
        <h1>Cuentas financieras</h1>
        <p>Controla cuentas de ahorro, corrientes, saldos y estados operativos.</p>
      </div>
      <button class="button primary" type="button" (click)="formOpen.set(true)">Nueva cuenta</button>
    </div>

    @if (message()) { <div class="alert success">{{ message() }}</div> }
    @if (error()) { <div class="alert error">{{ error() }}</div> }

    <div class="filter-bar">
      <select [value]="typeFilter()" (change)="typeFilter.set($any($event.target).value)">
        <option value="">Todos los tipos</option>
        <option value="SAVINGS">Ahorros</option>
        <option value="CHECKING">Corriente</option>
      </select>
      <select [value]="statusFilter()" (change)="statusFilter.set($any($event.target).value)">
        <option value="">Todos los estados</option>
        <option value="ACTIVE">Activas</option>
        <option value="INACTIVE">Inactivas</option>
        <option value="CANCELLED">Canceladas</option>
      </select>
    </div>

    <article class="panel">
      @if (loading()) {
        <div class="empty-state">Consultando productos...</div>
      } @else {
        <div class="table-wrap">
          <table>
            <thead><tr><th>Cuenta</th><th>Titular</th><th>Estado</th><th>Saldo</th><th>Disponible</th><th></th></tr></thead>
            <tbody>
              @for (account of filteredAccounts(); track account.id) {
                <tr>
                  <td>
                    <strong>{{ account.accountNumber }}</strong>
                    <small class="cell-detail">{{ account.accountType === 'SAVINGS' ? 'Cuenta de ahorros' : 'Cuenta corriente' }}</small>
                  </td>
                  <td>{{ customerName(account.customerId) }}</td>
                  <td><span class="status" [class]="account.status.toLowerCase()">{{ account.status }}</span></td>
                  <td>{{ account.balance | currency: 'COP' : 'symbol-narrow' : '1.0-0' }}</td>
                  <td>{{ account.availableBalance | currency: 'COP' : 'symbol-narrow' : '1.0-0' }}</td>
                  <td class="actions">
                    @if (account.status !== 'CANCELLED') {
                      <button type="button" (click)="toggleStatus(account)">
                        {{ account.status === 'ACTIVE' ? 'Inactivar' : 'Activar' }}
                      </button>
                      <button class="danger-link" type="button" (click)="cancel(account)">Cancelar</button>
                    }
                  </td>
                </tr>
              } @empty {
                <tr><td colspan="6"><div class="empty-state">No hay cuentas para mostrar.</div></td></tr>
              }
            </tbody>
          </table>
        </div>
      }
    </article>

    @if (formOpen()) {
      <div class="modal-backdrop" (click)="formOpen.set(false)">
        <section class="modal compact" role="dialog" aria-modal="true" (click)="$event.stopPropagation()">
          <div class="modal-heading">
            <div><span class="eyebrow">Producto nuevo</span><h2>Abrir cuenta</h2></div>
            <button type="button" (click)="formOpen.set(false)">×</button>
          </div>
          <form [formGroup]="form" (ngSubmit)="create()">
            <div class="form-stack">
              <label>Cliente
                <select formControlName="customerId">
                  <option value="">Selecciona un titular</option>
                  @for (customer of customers(); track customer.id) {
                    <option [value]="customer.id">{{ customer.firstName }} {{ customer.lastName }} · {{ customer.documentNumber }}</option>
                  }
                </select>
              </label>
              <label>Tipo de cuenta
                <select formControlName="accountType">
                  <option value="SAVINGS">Cuenta de ahorros</option>
                  <option value="CHECKING">Cuenta corriente</option>
                </select>
              </label>
              <label class="check">
                <input type="checkbox" formControlName="gmfExempt" />
                Marcar como exenta de GMF
              </label>
            </div>
            <div class="modal-actions">
              <button class="button secondary" type="button" (click)="formOpen.set(false)">Cancelar</button>
              <button class="button primary" type="submit" [disabled]="form.invalid || saving()">
                {{ saving() ? 'Creando...' : 'Crear cuenta' }}
              </button>
            </div>
          </form>
        </section>
      </div>
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Accounts {
  private readonly api = inject(FinancialApiService);
  private readonly fb = inject(FormBuilder);

  protected readonly accounts = signal<Account[]>([]);
  protected readonly customers = signal<Customer[]>([]);
  protected readonly loading = signal(true);
  protected readonly saving = signal(false);
  protected readonly formOpen = signal(false);
  protected readonly typeFilter = signal<AccountType | ''>('');
  protected readonly statusFilter = signal<AccountStatus | ''>('');
  protected readonly message = signal('');
  protected readonly error = signal('');
  protected readonly form = this.fb.nonNullable.group({
    customerId: ['', Validators.required],
    accountType: ['SAVINGS' as AccountType, Validators.required],
    gmfExempt: [false],
  });

  constructor() {
    this.load();
  }

  protected filteredAccounts(): Account[] {
    return this.accounts().filter(
      (account) =>
        (!this.typeFilter() || account.accountType === this.typeFilter()) &&
        (!this.statusFilter() || account.status === this.statusFilter()),
    );
  }

  protected customerName(id: string): string {
    const customer = this.customers().find((item) => item.id === id);
    return customer ? `${customer.firstName} ${customer.lastName}` : 'Cliente no disponible';
  }

  protected create(): void {
    if (this.form.invalid) return;
    this.saving.set(true);
    this.api
      .createAccount(this.form.getRawValue())
      .pipe(finalize(() => this.saving.set(false)))
      .subscribe({
        next: () => {
          this.message.set('Cuenta creada correctamente.');
          this.formOpen.set(false);
          this.form.reset({ customerId: '', accountType: 'SAVINGS', gmfExempt: false });
          this.load();
        },
        error: (error: Error) => this.error.set(error.message),
      });
  }

  protected toggleStatus(account: Account): void {
    const status = account.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
    this.api.updateAccountStatus(account.id, status).subscribe({
      next: () => {
        this.message.set(`Cuenta ${status === 'ACTIVE' ? 'activada' : 'inactivada'} correctamente.`);
        this.load();
      },
      error: (error: Error) => this.error.set(error.message),
    });
  }

  protected cancel(account: Account): void {
    if (!confirm(`Cancelar la cuenta ${account.accountNumber}? Debe tener saldo cero.`)) return;
    this.api.cancelAccount(account.id).subscribe({
      next: () => {
        this.message.set('Cuenta cancelada correctamente.');
        this.load();
      },
      error: (error: Error) => this.error.set(error.message),
    });
  }

  private load(): void {
    this.loading.set(true);
    forkJoin({ accounts: this.api.getAccounts(), customers: this.api.getCustomers() })
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: ({ accounts, customers }) => {
          this.accounts.set(accounts.content);
          this.customers.set(customers.content);
        },
        error: (error: Error) => this.error.set(error.message),
      });
  }
}
