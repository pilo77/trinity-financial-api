import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../environments/environment';
import {
  Account,
  AccountStatement,
  AccountStatus,
  AccountType,
  Customer,
  CustomerRequest,
  PageResponse,
  Transaction,
} from './models';

@Injectable({ providedIn: 'root' })
export class FinancialApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = environment.apiUrl;

  getCustomers(page = 0, size = 100): Observable<PageResponse<Customer>> {
    return this.http.get<PageResponse<Customer>>(`${this.baseUrl}/customers`, {
      params: { page, size },
    });
  }

  createCustomer(payload: CustomerRequest): Observable<Customer> {
    return this.http.post<Customer>(`${this.baseUrl}/customers`, payload);
  }

  updateCustomer(id: string, payload: CustomerRequest): Observable<Customer> {
    return this.http.put<Customer>(`${this.baseUrl}/customers/${id}`, payload);
  }

  deleteCustomer(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/customers/${id}`);
  }

  getAccounts(filters: {
    customerId?: string;
    accountType?: AccountType | '';
    status?: AccountStatus | '';
    page?: number;
    size?: number;
  } = {}): Observable<PageResponse<Account>> {
    let params = new HttpParams()
      .set('page', filters.page ?? 0)
      .set('size', filters.size ?? 100)
      .set('sort', 'createdAt,desc');

    if (filters.customerId) params = params.set('customerId', filters.customerId);
    if (filters.accountType) params = params.set('accountType', filters.accountType);
    if (filters.status) params = params.set('status', filters.status);

    return this.http.get<PageResponse<Account>>(`${this.baseUrl}/accounts`, { params });
  }

  createAccount(payload: {
    customerId: string;
    accountType: AccountType;
    gmfExempt: boolean;
  }): Observable<Account> {
    return this.http.post<Account>(`${this.baseUrl}/accounts`, payload);
  }

  updateAccountStatus(id: string, status: Exclude<AccountStatus, 'CANCELLED'>): Observable<Account> {
    return this.http.patch<Account>(`${this.baseUrl}/accounts/${id}/status`, { status });
  }

  cancelAccount(id: string): Observable<Account> {
    return this.http.patch<Account>(`${this.baseUrl}/accounts/${id}/cancel`, {});
  }

  deposit(payload: {
    accountNumber: string;
    amount: number;
    description: string;
  }): Observable<Transaction> {
    return this.http.post<Transaction>(`${this.baseUrl}/transactions/deposits`, payload);
  }

  withdraw(payload: {
    accountNumber: string;
    amount: number;
    description: string;
  }): Observable<Transaction> {
    return this.http.post<Transaction>(`${this.baseUrl}/transactions/withdrawals`, payload);
  }

  transfer(payload: {
    sourceAccountNumber: string;
    destinationAccountNumber: string;
    amount: number;
    description: string;
  }): Observable<Transaction> {
    return this.http.post<Transaction>(`${this.baseUrl}/transactions/transfers`, payload);
  }

  getTransactionById(id: string): Observable<Transaction> {
    return this.http.get<Transaction>(`${this.baseUrl}/transactions/${id}`);
  }

  getStatement(
    accountNumber: string,
    startDate?: string,
    endDate?: string,
  ): Observable<AccountStatement> {
    let params = new HttpParams().set('page', 0).set('size', 100).set('sort', 'createdAt,desc');
    if (startDate) params = params.set('startDate', startDate);
    if (endDate) params = params.set('endDate', endDate);

    return this.http.get<AccountStatement>(
      `${this.baseUrl}/accounts/number/${accountNumber}/statement`,
      { params },
    );
  }
}
