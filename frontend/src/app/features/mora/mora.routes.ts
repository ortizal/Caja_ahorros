import { Routes } from '@angular/router';

export const moraRoutes: Routes = [
  {
    path: '',
    children: [
      {
        path: '',
        loadComponent: () =>
          import('./mora-list.component').then((m) => m.MoraListComponent)
      },
      {
        path: 'cartera-vencida',
        loadComponent: () =>
          import('./cartera-vencida.component').then((m) => m.CarteraVencidaComponent)
      },
      {
        path: ':id',
        loadComponent: () =>
          import('./mora-detalle.component').then((m) => m.MoraDetalleComponent)
      }
    ]
  }
];
