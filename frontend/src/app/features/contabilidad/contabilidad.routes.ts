import { Routes } from '@angular/router';

export const contabilidadRoutes: Routes = [
  {
    path: '',
    children: [
      {
        path: '',
        loadComponent: () =>
          import('./contabilidad.component').then((m) => m.ContabilidadComponent)
      },
      {
        path: 'cuentas/nuevo',
        loadComponent: () =>
          import('./cuenta-contable-form.component').then((m) => m.CuentaContableFormComponent)
      },
      {
        path: 'asientos/nuevo',
        loadComponent: () =>
          import('./asiento-form.component').then((m) => m.AsientoFormComponent)
      }
    ]
  }
];
