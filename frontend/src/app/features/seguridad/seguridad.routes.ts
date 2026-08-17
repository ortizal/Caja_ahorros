import { Routes } from '@angular/router';

export const seguridadRoutes: Routes = [
  {
    path: '',
    children: [
      {
        path: '',
        loadComponent: () =>
          import('./seguridad.component').then((m) => m.SeguridadComponent)
      },
      {
        path: 'usuarios/nuevo',
        loadComponent: () =>
          import('./usuario-form.component').then((m) => m.UsuarioFormComponent)
      },
      {
        path: 'usuarios/:id/editar',
        loadComponent: () =>
          import('./usuario-form.component').then((m) => m.UsuarioFormComponent)
      }
    ]
  }
];
