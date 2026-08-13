import { Component, inject, signal } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';

interface MenuItem {
  ruta: string;
  etiqueta: string;
  permiso: string;
}

const MENU: MenuItem[] = [
  { ruta: '/dashboard', etiqueta: 'Dashboard', permiso: '' },
  { ruta: '/socios', etiqueta: 'Socios', permiso: 'SOCIOS:VER' },
  { ruta: '/caja', etiqueta: 'Caja', permiso: 'CAJA:VER' },
  { ruta: '/bancos', etiqueta: 'Bancos', permiso: 'BANCOS:VER' },
  { ruta: '/contabilidad', etiqueta: 'Contabilidad', permiso: 'CONTABILIDAD:VER' },
  { ruta: '/aportaciones', etiqueta: 'Aportaciones', permiso: 'APORTACIONES:VER' },
  { ruta: '/ahorros', etiqueta: 'Ahorros', permiso: 'AHORROS:VER' },
  { ruta: '/creditos', etiqueta: 'Creditos', permiso: 'CREDITOS:VER' },
  { ruta: '/seguridad', etiqueta: 'Seguridad', permiso: 'SEGURIDAD:VER' }
];

@Component({
  selector: 'app-shell',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './shell.html',
  styleUrl: './shell.css'
})
export class ShellComponent {
  private readonly auth = inject(AuthService);

  protected readonly menu = signal(MENU);
  protected readonly user = this.auth.currentUser;

  visibleMenu(): MenuItem[] {
    return this.menu().filter((m) => !m.permiso || this.auth.hasPermiso(m.permiso));
  }

  logout(): void {
    this.auth.logout();
  }
}
