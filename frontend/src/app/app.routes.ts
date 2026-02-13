import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    redirectTo: 'dashboard',
    pathMatch: 'full',
  },
  {
    path: 'dashboard',
    loadChildren: () =>
      import('./dashboard/dashboard.routes').then((m) => m.DASHBOARD_ROUTES),
  },
  {
    path: 'onboarding',
    loadChildren: () =>
      import('./onboarding/onboarding.routes').then((m) => m.ONBOARDING_ROUTES),
  },
  {
    path: 'income',
    loadChildren: () =>
      import('./income/income.routes').then((m) => m.INCOME_ROUTES),
  },
  {
    path: 'invoicing',
    loadChildren: () =>
      import('./invoicing/invoicing.routes').then((m) => m.INVOICING_ROUTES),
  },
  {
    path: 'expenses',
    loadChildren: () =>
      import('./expense/expense.routes').then((m) => m.EXPENSE_ROUTES),
  },
  {
    path: 'tax',
    loadChildren: () =>
      import('./tax/tax.routes').then((m) => m.TAX_ROUTES),
  },
  {
    path: 'calendar',
    loadChildren: () =>
      import('./calendar/calendar.routes').then((m) => m.CALENDAR_ROUTES),
  },
  {
    path: 'documents',
    loadChildren: () =>
      import('./document/document.routes').then((m) => m.DOCUMENT_ROUTES),
  },
  {
    path: '**',
    redirectTo: 'dashboard',
  },
];
