import { Routes } from '@angular/router';

export const sociosRoutes: Routes = [
  {
    path: '',
    children: [
      {
        path: '',
        loadComponent: () =>
          import('./socio-list.component').then((m) => m.SocioListComponent)
      },
      {
        path: 'nuevo',
        loadComponent: () =>
          import('./socio-form.component').then((m) => m.SocioFormComponent)
      },
      {
        path: ':id/editar',
        loadComponent: () =>
          import('./socio-form.component').then((m) => m.SocioFormComponent)
      },
      {
        path: ':id',
        loadComponent: () =>
          import('./socio-detail.component').then((m) => m.SocioDetailComponent)
      }
    ]
  }
];
