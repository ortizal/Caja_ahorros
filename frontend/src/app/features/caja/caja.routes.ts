import { Routes } from '@angular/router';
import { permisoGuard } from '../../core/auth/auth.guard';

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
      },
      {
        path: 'listado',
        canActivate: [permisoGuard('CAJA:APROBAR')],
        loadComponent: () =>
          import('./caja-listado.component').then((m) => m.CajaListadoComponent)
      }
    ]
  }
];