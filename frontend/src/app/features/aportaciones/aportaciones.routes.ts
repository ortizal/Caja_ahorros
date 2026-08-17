import { Routes } from '@angular/router';

export const aportacionesRoutes: Routes = [
  {
    path: '',
    children: [
      {
        path: '',
        loadComponent: () =>
          import('./aportaciones.component').then((m) => m.AportacionesComponent)
      },
      {
        path: 'nuevo',
        loadComponent: () =>
          import('./aportacion-config-form.component').then((m) => m.AportacionConfigFormComponent)
      }
    ]
  }
];
