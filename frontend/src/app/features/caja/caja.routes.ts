import { Routes } from '@angular/router';

export const cajaRoutes: Routes = [
  {
    path: '',
    children: [
      {
        path: '',
        loadComponent: () =>
          import('./caja.component').then((m) => m.CajaComponent)
      },
      {
        path: 'nuevo',
        loadComponent: () =>
          import('./caja-apertura-form.component').then((m) => m.CajaAperturaFormComponent)
      }
    ]
  }
];
