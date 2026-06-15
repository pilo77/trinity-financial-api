export type AccountType = 'SAVINGS' | 'CHECKING';
export type AccountStatus = 'ACTIVE' | 'INACTIVE' | 'CANCELLED';
export type TransactionType = 'DEPOSIT' | 'WITHDRAWAL' | 'TRANSFER';
export type MovementType = 'DEBIT' | 'CREDIT';

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

export interface Customer {
  id: string;
  documentType: string;
  documentNumber: string;
  firstName: string;
  lastName: string;
  email: string;
  phone?: string;
  birthDate: string;
  createdAt: string;
  updatedAt: string;
}

export interface CustomerRequest {
  documentType: string;
  documentNumber: string;
  firstName: string;
  lastName: string;
  email: string;
  phone: string;
  birthDate: string;
}

export interface Account {
  id: string;
  accountNumber: string;
  accountType: AccountType;
  status: AccountStatus;
  balance: number;
  availableBalance: number;
  gmfExempt: boolean;
  customerId: string;
  createdAt: string;
  updatedAt: string;
}

export interface AccountMovement {
  id: string;
  accountNumber?: string;
  movementType: MovementType;
  amount: number;
  balanceAfterMovement?: number;
  transactionId?: string;
  createdAt: string;
  description?: string;
}

export interface Transaction {
  id: string;
  transactionType: TransactionType;
  amount: number;
  sourceAccountNumber?: string;
  destinationAccountNumber?: string;
  status: 'COMPLETED' | 'REJECTED';
  description?: string;
  transactionDate: string;
  createdAt: string;
  movements: AccountMovement[];
}

export interface PageInfo {
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
  hasNext: boolean;
  hasPrevious: boolean;
}

export interface AccountStatement {
  accountNumber: string;
  accountType: AccountType;
  status: AccountStatus;
  balance: number;
  availableBalance: number;
  statementDate: string;
  movements: AccountMovement[];
  page: PageInfo;
}

export interface ProblemDetail {
  title?: string;
  detail?: string;
  status?: number;
  code?: string;
  fieldErrors?: Array<{ field?: string; message?: string }>;
}
