import { Routes } from '@angular/router';

export const tesoreriaRoutes: Routes = [
  {
    path: '',
    children: [
      {
        path: '',
        loadComponent: () =>
          import('./tesoreria.component').then((m) => m.TesoreriaComponent)
      },
      {
        path: 'gastos/nuevo',
        loadComponent: () =>
          import('./gasto-form.component').then((m) => m.GastoFormComponent)
      },
      {
        path: 'cuentas-pagar/nuevo',
        loadComponent: () =>
          import('./cuenta-pagar-form.component').then((m) => m.CuentaPagarFormComponent)
      },
      {
        path: 'cuentas-cobrar/nuevo',
        loadComponent: () =>
          import('./cuenta-cobrar-form.component').then((m) => m.CuentaCobrarFormComponent)
      }
    ]
  }
];
