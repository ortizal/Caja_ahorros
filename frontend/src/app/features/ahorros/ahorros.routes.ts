import { Routes } from '@angular/router';

export const ahorrosRoutes: Routes = [
  {
    path: '',
    children: [
      {
        path: '',
        loadComponent: () =>
          import('./ahorros.component').then((m) => m.AhorrosComponent)
      },
      {
        path: 'productos/nuevo',
        loadComponent: () =>
          import('./ahorro-producto-form.component').then((m) => m.AhorroProductoFormComponent)
      },
      {
        path: 'cuentas/nueva',
        loadComponent: () =>
          import('./ahorro-apertura-form.component').then((m) => m.AhorroAperturaFormComponent)
      }
    ]
  }
];
