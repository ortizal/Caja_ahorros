import { Routes } from '@angular/router';

export const balanceRoutes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./balance.component').then((m) => m.BalanceComponent)
  }
];