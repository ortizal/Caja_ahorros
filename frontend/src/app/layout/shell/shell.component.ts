import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { Notificacion } from '../../core/models/notificacion.model';
import { NotificacionService } from '../../core/services/notificacion.service';
import { AppHeaderComponent } from '../../shared/components/app-header/app-header.component';
import { AppSidebarComponent, MenuItem } from '../../shared/components/app-sidebar/app-sidebar.component';
import { AppFooterComponent } from '../../shared/components/app-footer/app-footer.component';

const MENU: MenuItem[] = [
  { ruta: '/dashboard', etiqueta: 'Dashboard', permiso: '' },
  { ruta: '/socios', etiqueta: 'Socios', permiso: 'SOCIOS:VER' },
  { ruta: '/caja', etiqueta: 'Caja', permiso: 'CAJA:VER' },
  { ruta: '/bancos', etiqueta: 'Bancos', permiso: 'BANCOS:VER' },
  { ruta: '/contabilidad', etiqueta: 'Contabilidad', permiso: 'CONTABILIDAD:VER' },
  { ruta: '/aportaciones', etiqueta: 'Aportaciones', permiso: 'APORTACIONES:VER' },
  { ruta: '/ahorros', etiqueta: 'Ahorros', permiso: 'AHORROS:VER' },
  { ruta: '/creditos', etiqueta: 'Creditos', permiso: 'CREDITOS:VER' },
  { ruta: '/tesoreria', etiqueta: 'Tesorería', permiso: 'TESORERIA:VER' },
  { ruta: '/seguridad', etiqueta: 'Seguridad', permiso: 'SEGURIDAD:VER' }
];

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [RouterOutlet, AppHeaderComponent, AppSidebarComponent, AppFooterComponent],
  templateUrl: './shell.html',
  styleUrl: './shell.css'
})
export class ShellComponent implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly notificacionService = inject(NotificacionService);

  protected readonly menu = signal(MENU);
  protected readonly user = this.auth.currentUser;
  protected readonly lista = signal<Notificacion[]>([]);
  protected readonly noLeidas = signal(0);
  protected readonly panelAbierto = signal(false);
  protected readonly sidebarCollapsed = signal(false);
  protected readonly mobileMenuOpen = signal(false);

  ngOnInit(): void {
    this.cargar();
  }

  visibleMenu(): MenuItem[] {
    return this.menu().filter((m) => !m.permiso || this.auth.hasPermiso(m.permiso));
  }

  cargar(): void {
    this.notificacionService.listar().subscribe({
      next: (lista) => {
        this.lista.set(lista);
        this.noLeidas.set(lista.filter((n) => !n.leida).length);
      },
      error: () => undefined
    });
  }

  togglePanel(): void {
    this.panelAbierto.set(!this.panelAbierto());
    if (this.panelAbierto()) {
      this.cargar();
    }
  }

  toggleSidebar(): void {
    if (window.innerWidth <= 768) {
      this.mobileMenuOpen.set(!this.mobileMenuOpen());
    } else {
      this.sidebarCollapsed.set(!this.sidebarCollapsed());
    }
  }

  closeMobile(): void {
    this.mobileMenuOpen.set(false);
  }

  marcarUna(id: number): void {
    this.notificacionService.marcarLeida(id).subscribe({
      next: () => this.cargar(),
      error: () => undefined
    });
  }

  marcarTodas(): void {
    this.notificacionService.marcarTodasLeidas().subscribe({
      next: () => this.cargar(),
      error: () => undefined
    });
  }

  logout(): void {
    this.auth.logout();
  }
}

