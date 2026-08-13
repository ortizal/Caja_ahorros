import { Routes } from '@angular/router';
import { authGuard, permisoGuard } from './core/auth/auth.guard';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./features/login/login.component').then((m) => m.LoginComponent)
  },
  {
    path: '',
    loadComponent: () => import('./layout/shell/shell.component').then((m) => m.ShellComponent),
    canActivate: [authGuard],
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      {
        path: 'dashboard',
        loadComponent: () => import('./features/dashboard/dashboard.component').then((m) => m.DashboardComponent)
      },
      {
        path: 'socios',
        canActivate: [permisoGuard('SOCIOS:VER')],
        loadChildren: () => import('./features/socios/socios.routes').then((m) => m.sociosRoutes)
      },
      {
        path: 'caja',
        canActivate: [permisoGuard('CAJA:VER')],
        loadComponent: () => import('./features/caja/caja.component').then((m) => m.CajaComponent)
      },
      {
        path: 'bancos',
        canActivate: [permisoGuard('BANCOS:VER')],
        loadChildren: () => import('./features/bancos/bancos.routes').then((m) => m.bancosRoutes)
      },
      {
        path: 'contabilidad',
        canActivate: [permisoGuard('CONTABILIDAD:VER')],
        loadComponent: () => import('./features/contabilidad/contabilidad.component').then((m) => m.ContabilidadComponent)
      },
      {
        path: 'aportaciones',
        canActivate: [permisoGuard('APORTACIONES:VER')],
        loadComponent: () => import('./features/aportaciones/aportaciones.component').then((m) => m.AportacionesComponent)
      },
      {
        path: 'ahorros',
        canActivate: [permisoGuard('AHORROS:VER')],
        loadComponent: () => import('./features/ahorros/ahorros.component').then((m) => m.AhorrosComponent)
      },
      {
        path: 'creditos',
        canActivate: [permisoGuard('CREDITOS:VER')],
        loadComponent: () => import('./features/creditos/creditos.component').then((m) => m.CreditosComponent)
      },
      {
        path: 'seguridad',
        canActivate: [permisoGuard('SEGURIDAD:VER')],
        loadComponent: () => import('./features/seguridad/seguridad.component').then((m) => m.SeguridadComponent)
      }
    ]
  },
  { path: '**', redirectTo: 'dashboard' }
];
