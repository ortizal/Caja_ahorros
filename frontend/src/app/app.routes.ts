import { Routes } from '@angular/router';
import { authGuard, permisoGuard } from './core/auth/auth.guard';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./features/login/login.component').then((m) => m.LoginComponent)
  },
  {
    path: 'portal',
    canActivate: [authGuard, permisoGuard('PORTAL:VER')],
    loadComponent: () => import('./features/portal/portal.component').then((m) => m.PortalComponent)
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
        loadChildren: () => import('./features/caja/caja.routes').then((m) => m.cajaRoutes)
      },
      {
        path: 'bancos',
        canActivate: [permisoGuard('BANCOS:VER')],
        loadChildren: () => import('./features/bancos/bancos.routes').then((m) => m.bancosRoutes)
      },
      {
        path: 'contabilidad',
        canActivate: [permisoGuard('CONTABILIDAD:VER')],
        loadChildren: () => import('./features/contabilidad/contabilidad.routes').then((m) => m.contabilidadRoutes)
      },
      {
        path: 'aportaciones',
        canActivate: [permisoGuard('APORTACIONES:VER')],
        loadChildren: () => import('./features/aportaciones/aportaciones.routes').then((m) => m.aportacionesRoutes)
      },
      {
        path: 'ahorros',
        canActivate: [permisoGuard('AHORROS:VER')],
        loadChildren: () => import('./features/ahorros/ahorros.routes').then((m) => m.ahorrosRoutes)
      },
      {
        path: 'creditos',
        canActivate: [permisoGuard('CREDITOS:VER')],
        loadChildren: () => import('./features/creditos/creditos.routes').then((m) => m.creditosRoutes)
      },
      {
        path: 'tesoreria',
        canActivate: [permisoGuard('TESORERIA:VER')],
        loadChildren: () => import('./features/tesoreria/tesoreria.routes').then((m) => m.tesoreriaRoutes)
      },
      {
        path: 'seguridad',
        canActivate: [permisoGuard('SEGURIDAD:VER')],
        loadChildren: () => import('./features/seguridad/seguridad.routes').then((m) => m.seguridadRoutes)
      }
    ]
  },
  { path: '**', redirectTo: 'dashboard' }
];
