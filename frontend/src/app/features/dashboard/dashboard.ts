import { CurrencyPipe, DatePipe, DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { finalize, forkJoin } from 'rxjs';

import { Account, Customer } from '../../core/models';
import { FinancialApiService } from '../../core/financial-api.service';

@Component({
  selector: 'app-dashboard',
  imports: [CurrencyPipe, DatePipe, DecimalPipe, RouterLink],
  template: `
    <div class="page-header">
      <div>
        <span class="eyebrow">Resumen ejecutivo</span>
        <h1>Dashboard financiero</h1>
        <p>Una vista operativa de clientes, productos y saldos administrados.</p>
      </div>
      <a class="button primary" routerLink="/transactions">Nueva transaccion</a>
    </div>

    @if (error()) {
      <div class="alert error">{{ error() }}</div>
    }

    <div class="metrics-grid">
      <article class="metric-card featured">
        <span>Saldo total administrado</span>
        <strong>{{ totalBalance() | currency: 'COP' : 'symbol-narrow' : '1.0-0' }}</strong>
        <small>Balance consolidado de cuentas</small>
      </article>
      <article class="metric-card">
        <span>Clientes activos</span>
        <strong>{{ customers().length | number }}</strong>
        <small>Personas registradas</small>
      </article>
      <article class="metric-card">
        <span>Productos financieros</span>
        <strong>{{ accounts().length | number }}</strong>
        <small>{{ activeAccounts() }} cuentas activas</small>
      </article>
      <article class="metric-card">
        <span>Saldo disponible</span>
        <strong>{{ availableBalance() | currency: 'COP' : 'symbol-narrow' : '1.0-0' }}</strong>
        <small>Disponible para operaciones</small>
      </article>
    </div>

    <div class="content-grid">
      <article class="panel">
        <div class="panel-heading">
          <div>
            <span class="eyebrow">Portafolio</span>
            <h2>Cuentas recientes</h2>
          </div>
          <a routerLink="/accounts">Ver todas</a>
        </div>
        @if (loading()) {
          <div class="empty-state">Consultando cuentas...</div>
        } @else if (accounts().length === 0) {
          <div class="empty-state">Aun no hay cuentas registradas.</div>
        } @else {
          <div class="table-wrap">
            <table>
              <thead>
                <tr><th>Cuenta</th><th>Tipo</th><th>Estado</th><th>Saldo</th></tr>
              </thead>
              <tbody>
                @for (account of accounts().slice(0, 6); track account.id) {
                  <tr>
                    <td><strong>{{ account.accountNumber }}</strong></td>
                    <td>{{ account.accountType === 'SAVINGS' ? 'Ahorros' : 'Corriente' }}</td>
                    <td><span class="status" [class]="account.status.toLowerCase()">{{ account.status }}</span></td>
                    <td>{{ account.balance | currency: 'COP' : 'symbol-narrow' : '1.0-0' }}</td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
        }
      </article>

      <article class="panel">
        <div class="panel-heading">
          <div>
            <span class="eyebrow">Crecimiento</span>
            <h2>Clientes recientes</h2>
          </div>
          <a routerLink="/customers">Gestionar</a>
        </div>
        <div class="customer-list">
          @for (customer of customers().slice(0, 5); track customer.id) {
            <div class="customer-row">
              <span class="avatar">{{ initials(customer) }}</span>
              <div>
                <strong>{{ customer.firstName }} {{ customer.lastName }}</strong>
                <small>{{ customer.email }}</small>
              </div>
              <time>{{ customer.createdAt | date: 'dd MMM' }}</time>
            </div>
          } @empty {
            <div class="empty-state">Aun no hay clientes registrados.</div>
          }
        </div>
      </article>
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Dashboard {
  private readonly api = inject(FinancialApiService);

  protected readonly customers = signal<Customer[]>([]);
  protected readonly accounts = signal<Account[]>([]);
  protected readonly loading = signal(true);
  protected readonly error = signal('');

  constructor() {
    forkJoin({
      customers: this.api.getCustomers(),
      accounts: this.api.getAccounts(),
    })
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: ({ customers, accounts }) => {
          this.customers.set(customers.content);
          this.accounts.set(accounts.content);
        },
        error: (error: Error) => this.error.set(error.message),
      });
  }

  protected totalBalance(): number {
    return this.accounts().reduce((total, account) => total + Number(account.balance), 0);
  }

  protected availableBalance(): number {
    return this.accounts().reduce(
      (total, account) => total + Number(account.availableBalance),
      0,
    );
  }

  protected activeAccounts(): number {
    return this.accounts().filter((account) => account.status === 'ACTIVE').length;
  }

  protected initials(customer: Customer): string {
    return `${customer.firstName.charAt(0)}${customer.lastName.charAt(0)}`.toUpperCase();
  }
}
