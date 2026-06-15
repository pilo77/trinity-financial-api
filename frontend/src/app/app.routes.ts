import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
  {
    path: 'dashboard',
    loadComponent: () => import('./features/dashboard/dashboard').then((m) => m.Dashboard),
    title: 'Dashboard | Trinity Financial',
  },
  {
    path: 'customers',
    loadComponent: () => import('./features/customers/customers').then((m) => m.Customers),
    title: 'Clientes | Trinity Financial',
  },
  {
    path: 'accounts',
    loadComponent: () => import('./features/accounts/accounts').then((m) => m.Accounts),
    title: 'Cuentas | Trinity Financial',
  },
  {
    path: 'transactions',
    loadComponent: () =>
      import('./features/transactions/transactions').then((m) => m.Transactions),
    title: 'Transacciones | Trinity Financial',
  },
  {
    path: 'statements',
    loadComponent: () => import('./features/statements/statements').then((m) => m.Statements),
    title: 'Estados de cuenta | Trinity Financial',
  },
  {
    path: 'settings',
    loadComponent: () => import('./features/settings/settings').then((m) => m.Settings),
    title: 'Configuracion | Trinity Financial',
  },
  { path: '**', redirectTo: 'dashboard' },
];
