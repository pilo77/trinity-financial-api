import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';

import { FinancialApiService } from '../../core/financial-api.service';
import { minimumAgeValidator, strictEmailValidator } from '../../core/financial-validators';
import { Customer, CustomerRequest } from '../../core/models';

@Component({
  selector: 'app-customers',
  imports: [DatePipe, ReactiveFormsModule],
  template: `
    <div class="page-header">
      <div>
        <span class="eyebrow">Personas</span>
        <h1>Clientes</h1>
        <p>Administra la informacion base de los titulares de productos financieros.</p>
      </div>
      <button class="button primary" type="button" (click)="openCreate()">Nuevo cliente</button>
    </div>

    @if (message()) { <div class="alert success">{{ message() }}</div> }
    @if (error()) { <div class="alert error">{{ error() }}</div> }

    <article class="panel">
      <div class="panel-heading">
        <div>
          <span class="eyebrow">Directorio</span>
          <h2>{{ customers().length }} clientes registrados</h2>
        </div>
        <input
          class="search"
          type="search"
          placeholder="Buscar por nombre o documento"
          [value]="query()"
          (input)="query.set($any($event.target).value)"
        />
      </div>
      @if (loading()) {
        <div class="empty-state">Consultando clientes...</div>
      } @else {
        <div class="table-wrap">
          <table>
            <thead>
              <tr><th>Cliente</th><th>Documento</th><th>Contacto</th><th>Registro</th><th></th></tr>
            </thead>
            <tbody>
              @for (customer of filteredCustomers(); track customer.id) {
                <tr>
                  <td>
                    <strong>{{ customer.firstName }} {{ customer.lastName }}</strong>
                    <small class="cell-detail">{{ customer.birthDate }}</small>
                  </td>
                  <td>{{ customer.documentType }} {{ customer.documentNumber }}</td>
                  <td>
                    {{ customer.email }}
                    <small class="cell-detail">{{ customer.phone || 'Sin telefono' }}</small>
                  </td>
                  <td>{{ customer.createdAt | date: 'dd/MM/yyyy' }}</td>
                  <td class="actions">
                    <button type="button" (click)="showDetail(customer)">Ver detalle</button>
                    <button type="button" (click)="openEdit(customer)">Editar</button>
                    <button class="danger-link" type="button" (click)="remove(customer)">Eliminar</button>
                  </td>
                </tr>
              } @empty {
                <tr><td colspan="5"><div class="empty-state">No hay clientes para mostrar.</div></td></tr>
              }
            </tbody>
          </table>
        </div>
      }
    </article>

    @if (formOpen()) {
      <div class="modal-backdrop" (click)="closeForm()">
        <section class="modal" role="dialog" aria-modal="true" (click)="$event.stopPropagation()">
          <div class="modal-heading">
            <div>
              <span class="eyebrow">{{ editingId() ? 'Actualizacion' : 'Registro' }}</span>
              <h2>{{ editingId() ? 'Editar cliente' : 'Nuevo cliente' }}</h2>
            </div>
            <button type="button" aria-label="Cerrar" (click)="closeForm()">×</button>
          </div>
          <form [formGroup]="form" (ngSubmit)="save()">
            <div class="form-grid">
              <label>Tipo de documento
                <select formControlName="documentType">
                  <option value="CC">Cedula de ciudadania</option>
                  <option value="CE">Cedula de extranjeria</option>
                  <option value="PASSPORT">Pasaporte</option>
                </select>
              </label>
              <label>Numero de documento
                <input formControlName="documentNumber" maxlength="30" />
                @if (showFieldError('documentNumber', 'required')) {
                  <small class="field-error">El numero de documento es obligatorio.</small>
                }
              </label>
              <label>Nombres
                <input formControlName="firstName" maxlength="100" />
                @if (showFieldError('firstName', 'required')) {
                  <small class="field-error">El nombre es obligatorio.</small>
                } @else if (showFieldError('firstName', 'minlength')) {
                  <small class="field-error">El nombre debe tener al menos 2 caracteres.</small>
                }
              </label>
              <label>Apellidos
                <input formControlName="lastName" maxlength="100" />
                @if (showFieldError('lastName', 'required')) {
                  <small class="field-error">El apellido es obligatorio.</small>
                } @else if (showFieldError('lastName', 'minlength')) {
                  <small class="field-error">El apellido debe tener al menos 2 caracteres.</small>
                }
              </label>
              <label>Correo electronico
                <input formControlName="email" type="email" maxlength="254" />
                @if (showFieldError('email', 'required')) {
                  <small class="field-error">El correo electronico es obligatorio.</small>
                } @else if (showFieldError('email', 'strictEmail')) {
                  <small class="field-error">Ingrese un correo valido con formato nombre@dominio.com.</small>
                }
              </label>
              <label>Telefono
                <input formControlName="phone" maxlength="30" />
              </label>
              <label>Fecha de nacimiento
                <input formControlName="birthDate" type="date" />
                @if (showFieldError('birthDate', 'required')) {
                  <small class="field-error">La fecha de nacimiento es obligatoria.</small>
                } @else if (showFieldError('birthDate', 'minimumAge')) {
                  <small class="field-error">El cliente debe tener al menos 18 anos.</small>
                }
              </label>
            </div>
            <div class="modal-actions">
              <button class="button secondary" type="button" (click)="closeForm()">Cancelar</button>
              <button class="button primary" type="submit" [disabled]="form.invalid || saving()">
                {{ saving() ? 'Guardando...' : 'Guardar cliente' }}
              </button>
            </div>
          </form>
        </section>
      </div>
    }

    @if (selectedCustomer(); as customer) {
      <div class="modal-backdrop" (click)="closeDetail()">
        <section class="modal" role="dialog" aria-modal="true" (click)="$event.stopPropagation()">
          <div class="modal-heading">
            <div>
              <span class="eyebrow">Detalle de cliente</span>
              <h2>{{ customer.firstName }} {{ customer.lastName }}</h2>
            </div>
            <button type="button" aria-label="Cerrar detalle" (click)="closeDetail()">×</button>
          </div>
          <dl class="detail-grid">
            <div><dt>ID</dt><dd class="mono">{{ customer.id }}</dd></div>
            <div><dt>Tipo de identificacion</dt><dd>{{ customer.documentType }}</dd></div>
            <div><dt>Numero de identificacion</dt><dd>{{ customer.documentNumber }}</dd></div>
            <div><dt>Nombres</dt><dd>{{ customer.firstName }}</dd></div>
            <div><dt>Apellidos</dt><dd>{{ customer.lastName }}</dd></div>
            <div><dt>Email</dt><dd>{{ customer.email }}</dd></div>
            <div><dt>Telefono</dt><dd>{{ customer.phone || 'No registrado' }}</dd></div>
            <div><dt>Fecha de nacimiento</dt><dd>{{ customer.birthDate }}</dd></div>
            <div><dt>Fecha de creacion</dt><dd>{{ customer.createdAt | date: 'medium' }}</dd></div>
            <div><dt>Fecha de modificacion</dt><dd>{{ customer.updatedAt | date: 'medium' }}</dd></div>
          </dl>
        </section>
      </div>
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Customers {
  private readonly api = inject(FinancialApiService);
  private readonly fb = inject(FormBuilder);

  protected readonly customers = signal<Customer[]>([]);
  protected readonly loading = signal(true);
  protected readonly saving = signal(false);
  protected readonly formOpen = signal(false);
  protected readonly editingId = signal<string | null>(null);
  protected readonly selectedCustomer = signal<Customer | null>(null);
  protected readonly query = signal('');
  protected readonly message = signal('');
  protected readonly error = signal('');

  protected readonly form = this.fb.nonNullable.group({
    documentType: ['CC', Validators.required],
    documentNumber: ['', [Validators.required, Validators.maxLength(30)]],
    firstName: ['', [Validators.required, Validators.minLength(2)]],
    lastName: ['', [Validators.required, Validators.minLength(2)]],
    email: ['', [Validators.required, strictEmailValidator]],
    phone: [''],
    birthDate: ['', [Validators.required, minimumAgeValidator(18)]],
  });

  constructor() {
    this.load();
  }

  protected filteredCustomers(): Customer[] {
    const term = this.query().trim().toLowerCase();
    if (!term) return this.customers();
    return this.customers().filter((customer) =>
      `${customer.firstName} ${customer.lastName} ${customer.documentNumber}`
        .toLowerCase()
        .includes(term),
    );
  }

  protected openCreate(): void {
    this.editingId.set(null);
    this.form.reset({ documentType: 'CC', documentNumber: '', firstName: '', lastName: '', email: '', phone: '', birthDate: '' });
    this.formOpen.set(true);
  }

  protected openEdit(customer: Customer): void {
    this.editingId.set(customer.id);
    this.form.setValue({
      documentType: customer.documentType,
      documentNumber: customer.documentNumber,
      firstName: customer.firstName,
      lastName: customer.lastName,
      email: customer.email,
      phone: customer.phone ?? '',
      birthDate: customer.birthDate,
    });
    this.formOpen.set(true);
  }

  protected showDetail(customer: Customer): void {
    this.selectedCustomer.set(customer);
  }

  protected closeDetail(): void {
    this.selectedCustomer.set(null);
  }

  protected closeForm(): void {
    this.formOpen.set(false);
  }

  protected save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving.set(true);
    this.error.set('');
    const payload = this.form.getRawValue() as CustomerRequest;
    const request = this.editingId()
      ? this.api.updateCustomer(this.editingId()!, payload)
      : this.api.createCustomer(payload);

    request.pipe(finalize(() => this.saving.set(false))).subscribe({
      next: () => {
        this.message.set(this.editingId() ? 'Cliente actualizado correctamente.' : 'Cliente creado correctamente.');
        this.closeForm();
        this.load();
      },
      error: (error: Error) => this.error.set(error.message),
    });
  }

  protected remove(customer: Customer): void {
    if (!confirm(`Eliminar a ${customer.firstName} ${customer.lastName}?`)) return;
    this.api.deleteCustomer(customer.id).subscribe({
      next: () => {
        this.message.set('Cliente eliminado correctamente.');
        this.load();
      },
      error: (error: Error) => this.error.set(error.message),
    });
  }

  private load(): void {
    this.loading.set(true);
    this.api
      .getCustomers()
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (response) => {
          this.customers.set(response.content);
          const selected = this.selectedCustomer();
          if (selected) {
            this.selectedCustomer.set(response.content.find((customer) => customer.id === selected.id) ?? null);
          }
        },
        error: (error: Error) => this.error.set(error.message),
      });
  }

  protected showFieldError(controlName: keyof CustomerRequest, errorName: string): boolean {
    const control = this.form.controls[controlName];
    return (control.dirty || control.touched) && control.hasError(errorName);
  }
}
