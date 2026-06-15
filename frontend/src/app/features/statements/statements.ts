import { CurrencyPipe, DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';

import { FinancialApiService } from '../../core/financial-api.service';
import { Account, AccountStatement } from '../../core/models';

@Component({
  selector: 'app-statements',
  imports: [CurrencyPipe, DatePipe, ReactiveFormsModule],
  template: `
    <div class="page-header">
      <div>
        <span class="eyebrow">Consulta</span>
        <h1>Estados de cuenta</h1>
        <p>Consulta saldos y movimientos por cuenta y rango de fechas.</p>
      </div>
    </div>

    @if (error()) { <div class="alert error">{{ error() }}</div> }

    <article class="panel statement-search">
      <form [formGroup]="form" (ngSubmit)="search()">
        <label>Cuenta
          <select formControlName="accountNumber">
            <option value="">Selecciona una cuenta</option>
            @for (account of accounts(); track account.id) {
              <option [value]="account.accountNumber">{{ account.accountNumber }}</option>
            }
          </select>
        </label>
        <label>Desde
          <input type="datetime-local" formControlName="startDate" />
        </label>
        <label>Hasta
          <input type="datetime-local" formControlName="endDate" />
        </label>
        <button class="button primary" type="submit" [disabled]="form.invalid || loading()">
          {{ loading() ? 'Consultando...' : 'Generar estado' }}
        </button>
      </form>
    </article>

    @if (statement(); as result) {
      <div class="statement-summary">
        <article class="metric-card featured">
          <span>Saldo actual</span>
          <strong>{{ result.balance | currency: 'COP' : 'symbol-narrow' : '1.0-0' }}</strong>
          <small>Cuenta {{ result.accountNumber }}</small>
        </article>
        <article class="metric-card">
          <span>Saldo disponible</span>
          <strong>{{ result.availableBalance | currency: 'COP' : 'symbol-narrow' : '1.0-0' }}</strong>
          <small>{{ result.accountType === 'SAVINGS' ? 'Cuenta de ahorros' : 'Cuenta corriente' }}</small>
        </article>
        <article class="metric-card">
          <span>Movimientos</span>
          <strong>{{ result.page.totalElements }}</strong>
          <small>Generado {{ result.statementDate | date: 'short' }}</small>
        </article>
      </div>

      <article class="panel">
        <div class="panel-heading">
          <div><span class="eyebrow">Detalle</span><h2>Movimientos de la cuenta</h2></div>
          <span class="status" [class]="result.status.toLowerCase()">{{ result.status }}</span>
        </div>
        <div class="table-wrap">
          <table>
            <thead><tr><th>Fecha</th><th>Descripcion</th><th>Referencia</th><th>Tipo</th><th>Monto</th></tr></thead>
            <tbody>
              @for (movement of result.movements; track movement.id) {
                <tr>
                  <td>{{ movement.createdAt | date: 'dd/MM/yyyy HH:mm' }}</td>
                  <td>{{ movement.description || 'Movimiento financiero' }}</td>
                  <td><span class="mono">{{ movement.transactionId }}</span></td>
                  <td><span class="movement" [class.credit]="movement.movementType === 'CREDIT'">{{ movement.movementType }}</span></td>
                  <td [class.positive]="movement.movementType === 'CREDIT'" [class.negative]="movement.movementType === 'DEBIT'">
                    {{ movement.movementType === 'CREDIT' ? '+' : '-' }}{{ movement.amount | currency: 'COP' : 'symbol-narrow' : '1.0-2' }}
                  </td>
                </tr>
              } @empty {
                <tr><td colspan="5"><div class="empty-state">La cuenta no tiene movimientos en el periodo.</div></td></tr>
              }
            </tbody>
          </table>
        </div>
      </article>
    } @else {
      <div class="empty-state standalone">Selecciona una cuenta para consultar su estado.</div>
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Statements {
  private readonly api = inject(FinancialApiService);
  private readonly fb = inject(FormBuilder);

  protected readonly accounts = signal<Account[]>([]);
  protected readonly statement = signal<AccountStatement | null>(null);
  protected readonly loading = signal(false);
  protected readonly error = signal('');
  protected readonly form = this.fb.nonNullable.group({
    accountNumber: ['', Validators.required],
    startDate: [''],
    endDate: [''],
  });

  constructor() {
    this.api.getAccounts().subscribe({
      next: (response) => this.accounts.set(response.content),
      error: (error: Error) => this.error.set(error.message),
    });
  }

  protected search(): void {
    if (this.form.invalid) return;
    const value = this.form.getRawValue();
    this.loading.set(true);
    this.error.set('');
    this.api
      .getStatement(
        value.accountNumber,
        value.startDate || undefined,
        value.endDate || undefined,
      )
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (statement) => this.statement.set(statement),
        error: (error: Error) => this.error.set(error.message),
      });
  }
}
