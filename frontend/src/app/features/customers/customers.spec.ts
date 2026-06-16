import { FormGroup } from '@angular/forms';
import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';

import { FinancialApiService } from '../../core/financial-api.service';
import { Customer, PageResponse } from '../../core/models';
import { Customers } from './customers';

const customer: Customer = {
  id: '11111111-1111-4111-8111-111111111111',
  documentType: 'CC',
  documentNumber: '1234567890',
  firstName: 'Ana',
  lastName: 'Perez',
  email: 'ana.perez@example.com',
  phone: '3001234567',
  birthDate: '1990-01-15',
  createdAt: '2026-06-15T10:00:00',
  updatedAt: '2026-06-15T10:00:00',
};

const customersResponse: PageResponse<Customer> = {
  content: [customer],
  page: 0,
  size: 100,
  totalElements: 1,
  totalPages: 1,
  last: true,
};

describe('Customers component', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Customers],
      providers: [
        {
          provide: FinancialApiService,
          useValue: {
            getCustomers: () => of(customersResponse),
          },
        },
      ],
    }).compileComponents();
  });

  it('rejects an invalid customer email in the component form', () => {
    const fixture = TestBed.createComponent(Customers);
    fixture.detectChanges();

    const component = fixture.componentInstance as unknown as { form: FormGroup };
    const email = component.form.controls['email'];
    email.setValue('invalid-email');
    email.markAsTouched();

    expect(email.hasError('strictEmail')).toBe(true);
    expect(component.form.invalid).toBe(true);
  });

  it('renders the selected customer detail dialog', () => {
    const fixture = TestBed.createComponent(Customers);
    fixture.detectChanges();

    const buttons = Array.from(
      fixture.nativeElement.querySelectorAll('button'),
    ) as HTMLButtonElement[];
    const detailButton = buttons.find((button) => button.textContent?.trim() === 'Ver detalle');

    expect(detailButton).toBeDefined();

    detailButton!.click();
    fixture.detectChanges();

    const dialog = fixture.nativeElement.querySelector('[role="dialog"]') as HTMLElement;
    expect(dialog.textContent).toContain('Detalle de cliente');
    expect(dialog.textContent).toContain('Ana Perez');
    expect(dialog.textContent).toContain('ana.perez@example.com');
  });
});
