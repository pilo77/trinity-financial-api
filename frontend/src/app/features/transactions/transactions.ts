import { CurrencyPipe, DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';

import { ApiError } from '../../core/api-error.interceptor';
import { FinancialApiService } from '../../core/financial-api.service';
import {
  distinctAccountsValidator,
  positiveAmountValidator,
} from '../../core/financial-validators';
import { Account, Transaction, TransactionType } from '../../core/models';

@Component({
  selector: 'app-transactions',
  imports: [CurrencyPipe, DatePipe, ReactiveFormsModule],
  template: `
    <div class="page-header">
      <div>
        <span class="eyebrow">Operaciones</span>
        <h1>Transacciones</h1>
        <p>Ejecuta consignaciones, retiros y transferencias con validacion del backend.</p>
      </div>
    </div>

    @if (message()) { <div class="alert success">{{ message() }}</div> }
    @if (error()) { <div class="alert error">{{ error() }}</div> }

    <div class="transaction-layout">
      <article class="panel">
        <div class="operation-tabs">
          <button [class.active]="operation() === 'DEPOSIT'" type="button" (click)="selectOperation('DEPOSIT')">Consignar</button>
          <button [class.active]="operation() === 'WITHDRAWAL'" type="button" (click)="selectOperation('WITHDRAWAL')">Retirar</button>
          <button [class.active]="operation() === 'TRANSFER'" type="button" (click)="selectOperation('TRANSFER')">Transferir</button>
        </div>

        <form [formGroup]="form" (ngSubmit)="submit()">
          <div class="form-stack">
            <label>{{ operation() === 'TRANSFER' ? 'Cuenta origen' : 'Numero de cuenta' }}
              <select formControlName="sourceAccountNumber">
                <option value="">Selecciona una cuenta activa</option>
                @for (account of activeAccounts(); track account.id) {
                  <option [value]="account.accountNumber">
                    {{ account.accountNumber }} · {{ account.balance | currency: 'COP' : 'symbol-narrow' : '1.0-0' }}
                  </option>
                }
              </select>
            </label>

            @if (operation() === 'TRANSFER') {
              <label>Cuenta destino
                <select formControlName="destinationAccountNumber">
                  <option value="">Selecciona la cuenta destino</option>
                  @for (account of activeAccounts(); track account.id) {
                    <option [value]="account.accountNumber">{{ account.accountNumber }}</option>
                  }
                </select>
              </label>
              @if (form.hasError('sameAccount')) {
                <small class="field-error">La cuenta destino debe ser diferente de la cuenta origen.</small>
              }
            }

            <label>Monto
              <div class="money-input">
                <span>$</span>
                <input formControlName="amount" type="number" min="0.01" step="0.01" placeholder="0.00" />
              </div>
              @if ((form.controls.amount.dirty || form.controls.amount.touched) && form.controls.amount.invalid) {
                <small class="field-error">El monto debe ser mayor que cero.</small>
              }
            </label>
            <label>Descripcion
              <textarea formControlName="description" maxlength="255" rows="3" placeholder="Concepto de la operacion"></textarea>
            </label>
          </div>
          <button class="button primary wide" type="submit" [disabled]="form.invalid || saving()">
            {{ saving() ? 'Procesando...' : actionLabel() }}
          </button>
        </form>
      </article>

      <aside>
        <article class="panel operation-summary">
          <span class="eyebrow">Confirmacion</span>
          <h2>Resumen de operacion</h2>
          <dl>
            <div><dt>Tipo</dt><dd>{{ actionLabel() }}</dd></div>
            <div><dt>Cuenta</dt><dd>{{ form.controls.sourceAccountNumber.value || 'Pendiente' }}</dd></div>
            <div><dt>Monto</dt><dd>{{ form.controls.amount.value | currency: 'COP' : 'symbol-narrow' : '1.0-2' }}</dd></div>
          </dl>
        </article>

        @if (lastTransaction(); as transaction) {
          <article class="panel receipt">
            <span class="status active">COMPLETED</span>
            <h2>Operacion aprobada</h2>
            <p>Referencia {{ transaction.id }}</p>
            <strong>{{ transaction.amount | currency: 'COP' : 'symbol-narrow' : '1.0-2' }}</strong>
            <time>{{ transaction.transactionDate | date: 'medium' }}</time>
          </article>
        }
      </aside>
    </div>

    <article class="panel transaction-search">
      <div>
        <span class="eyebrow">Trazabilidad</span>
        <h2>Consultar transaccion por ID</h2>
        <p class="muted">Busca una operacion registrada usando su identificador UUID.</p>
      </div>
      <form [formGroup]="searchForm" (ngSubmit)="searchTransaction()">
        <label>ID de transaccion
          <input formControlName="transactionId" placeholder="00000000-0000-0000-0000-000000000000" />
          @if ((searchForm.controls.transactionId.dirty || searchForm.controls.transactionId.touched) && searchForm.controls.transactionId.invalid) {
            <small class="field-error">Ingresa un UUID valido.</small>
          }
        </label>
        <button class="button primary" type="submit" [disabled]="searchForm.invalid || searching()">
          {{ searching() ? 'Buscando...' : 'Buscar transaccion' }}
        </button>
      </form>
      @if (searchError()) { <div class="alert error">{{ searchError() }}</div> }
      @if (searchedTransaction(); as transaction) {
        <div class="transaction-result">
          <span class="status active">{{ transaction.status }}</span>
          <strong>{{ transaction.transactionType }}</strong>
          <span class="mono">{{ transaction.id }}</span>
          <span>{{ transaction.amount | currency: 'COP' : 'symbol-narrow' : '1.0-2' }}</span>
          <time>{{ transaction.transactionDate | date: 'medium' }}</time>
        </div>
      }
    </article>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Transactions {
  private readonly api = inject(FinancialApiService);
  private readonly fb = inject(FormBuilder);

  protected readonly accounts = signal<Account[]>([]);
  protected readonly operation = signal<TransactionType>('DEPOSIT');
  protected readonly saving = signal(false);
  protected readonly message = signal('');
  protected readonly error = signal('');
  protected readonly lastTransaction = signal<Transaction | null>(null);
  protected readonly searching = signal(false);
  protected readonly searchError = signal('');
  protected readonly searchedTransaction = signal<Transaction | null>(null);
  protected readonly form = this.fb.nonNullable.group(
    {
      sourceAccountNumber: ['', [Validators.required, Validators.pattern(/^\d{10}$/)]],
      destinationAccountNumber: [''],
      amount: [0, [Validators.required, positiveAmountValidator]],
      description: ['', Validators.maxLength(255)],
    },
    { validators: distinctAccountsValidator },
  );
  protected readonly searchForm = this.fb.nonNullable.group({
    transactionId: [
      '',
      [
        Validators.required,
        Validators.pattern(
          /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i,
        ),
      ],
    ],
  });

  constructor() {
    this.api.getAccounts({ status: 'ACTIVE' }).subscribe({
      next: (response) => this.accounts.set(response.content),
      error: (error: Error) => this.error.set(error.message),
    });
  }

  protected activeAccounts(): Account[] {
    return this.accounts().filter((account) => account.status === 'ACTIVE');
  }

  protected selectOperation(operation: TransactionType): void {
    this.operation.set(operation);
    const destination = this.form.controls.destinationAccountNumber;
    if (operation === 'TRANSFER') {
      destination.addValidators([Validators.required, Validators.pattern(/^\d{10}$/)]);
    } else {
      destination.clearValidators();
      destination.setValue('');
    }
    destination.updateValueAndValidity();
    this.error.set('');
    this.message.set('');
  }

  protected actionLabel(): string {
    return {
      DEPOSIT: 'Realizar consignacion',
      WITHDRAWAL: 'Realizar retiro',
      TRANSFER: 'Realizar transferencia',
    }[this.operation()];
  }

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const value = this.form.getRawValue();
    this.saving.set(true);
    this.error.set('');
    this.message.set('');

    const request =
      this.operation() === 'DEPOSIT'
        ? this.api.deposit({
            accountNumber: value.sourceAccountNumber,
            amount: value.amount,
            description: value.description,
          })
        : this.operation() === 'WITHDRAWAL'
          ? this.api.withdraw({
              accountNumber: value.sourceAccountNumber,
              amount: value.amount,
              description: value.description,
            })
          : this.api.transfer({
              sourceAccountNumber: value.sourceAccountNumber,
              destinationAccountNumber: value.destinationAccountNumber,
              amount: value.amount,
              description: value.description,
            });

    request.pipe(finalize(() => this.saving.set(false))).subscribe({
      next: (transaction) => {
        this.lastTransaction.set(transaction);
        this.message.set('Transaccion completada correctamente.');
        this.form.patchValue({ amount: 0, description: '' });
        this.api.getAccounts({ status: 'ACTIVE' }).subscribe((response) => this.accounts.set(response.content));
      },
      error: (error: Error) => this.error.set(error.message),
    });
  }

  protected searchTransaction(): void {
    if (this.searchForm.invalid) {
      this.searchForm.markAllAsTouched();
      return;
    }

    this.searching.set(true);
    this.searchError.set('');
    this.searchedTransaction.set(null);
    this.api
      .getTransactionById(this.searchForm.controls.transactionId.value)
      .pipe(finalize(() => this.searching.set(false)))
      .subscribe({
        next: (transaction) => this.searchedTransaction.set(transaction),
        error: (error: Error) =>
          this.searchError.set(
            error instanceof ApiError && error.status === 404
              ? 'No se encontro una transaccion con ese identificador.'
              : error.message,
          ),
      });
  }
}
