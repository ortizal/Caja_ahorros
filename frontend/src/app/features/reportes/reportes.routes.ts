import { Routes } from '@angular/router';

export const reportesRoutes: Routes = [
  {
    path: '',
    children: [
      {
        path: '',
        loadComponent: () =>
          import('./reporte-list.component').then((m) => m.ReporteListComponent)
      },
      {
        path: 'nuevo',
        loadComponent: () =>
          import('./reporte-form.component').then((m) => m.ReporteFormComponent)
      },
      {
        path: ':id/editar',
        loadComponent: () =>
          import('./reporte-form.component').then((m) => m.ReporteFormComponent)
      }
    ]
  }
];
