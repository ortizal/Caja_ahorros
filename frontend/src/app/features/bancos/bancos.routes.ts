import { Routes } from '@angular/router';

export const bancosRoutes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./cuentas-bancarias.component').then((m) => m.CuentasBancariasComponent)
  },
  {
    path: 'nuevo',
    loadComponent: () =>
      import('./cuenta-bancaria-form.component').then((m) => m.CuentaBancariaFormComponent)
  },
  {
    path: ':id',
    loadComponent: () =>
      import('./cuenta-bancaria-detalle.component').then((m) => m.CuentaBancariaDetalleComponent)
  }
];
