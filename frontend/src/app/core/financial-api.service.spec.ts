import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../environments/environment';
import { FinancialApiService } from './financial-api.service';

describe('FinancialApiService', () => {
  let service: FinancialApiService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [FinancialApiService, provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(FinancialApiService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('requests customers with pagination', () => {
    service.getCustomers(1, 25).subscribe();

    const request = http.expectOne(
      (candidate) =>
        candidate.url === `${environment.apiUrl}/customers` &&
        candidate.params.get('page') === '1' &&
        candidate.params.get('size') === '25',
    );
    expect(request.request.method).toBe('GET');
    request.flush({ content: [], page: 1, size: 25, totalElements: 0, totalPages: 0, last: true });
  });

  it('creates an account through the accounts endpoint', () => {
    const payload = {
      customerId: 'customer-id',
      accountType: 'SAVINGS' as const,
      gmfExempt: false,
    };
    service.createAccount(payload).subscribe();

    const request = http.expectOne(`${environment.apiUrl}/accounts`);
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(payload);
    request.flush({});
  });

  it('requests a transaction by UUID', () => {
    const id = '11111111-1111-4111-8111-111111111111';
    service.getTransactionById(id).subscribe();

    const request = http.expectOne(`${environment.apiUrl}/transactions/${id}`);
    expect(request.request.method).toBe('GET');
    request.flush({});
  });

  it('preserves statement filters and pagination parameters', () => {
    service.getStatement('1000000001', '2026-06-01T00:00', '2026-06-15T23:59').subscribe();

    const request = http.expectOne(
      (candidate) =>
        candidate.url === `${environment.apiUrl}/accounts/number/1000000001/statement` &&
        candidate.params.get('startDate') === '2026-06-01T00:00' &&
        candidate.params.get('endDate') === '2026-06-15T23:59' &&
        candidate.params.get('page') === '0' &&
        candidate.params.get('size') === '100' &&
        candidate.params.get('sort') === 'createdAt,desc',
    );
    expect(request.request.method).toBe('GET');
    request.flush({});
  });
});
