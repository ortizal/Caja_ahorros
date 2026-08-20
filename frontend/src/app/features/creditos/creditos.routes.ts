import { Routes } from '@angular/router';

export const creditosRoutes: Routes = [
  {
    path: '',
    children: [
      {
        path: '',
        loadComponent: () =>
          import('./creditos.component').then((m) => m.CreditosComponent)
      },
      {
        path: ':id/detalle',
        loadComponent: () =>
          import('./credito-detalle.component').then((m) => m.CreditoDetalleComponent)
      },
      {
        path: 'productos/nuevo',
        loadComponent: () =>
          import('./credito-producto-form.component').then((m) => m.CreditoProductoFormComponent)
      },
      {
        path: 'solicitudes/nuevo',
        loadComponent: () =>
          import('./credito-solicitud-form.component').then((m) => m.CreditoSolicitudFormComponent)
      }
    ]
  }
];
