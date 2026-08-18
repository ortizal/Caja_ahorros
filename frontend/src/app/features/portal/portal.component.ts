import { Component, OnInit, inject, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { AuthService } from '../../core/auth/auth.service';
import { PortalService } from '../../core/services/portal.service';
import { NotificacionService } from '../../core/services/notificacion.service';
import { Notificacion } from '../../core/models/notificacion.model';
import { PortalAhorro, PortalAportacion, PortalCredito, PortalResumen } from '../../core/models/portal.model';
import { PaginadorComponent } from '../../shared/components/paginador/paginador.component';

@Component({
  selector: 'app-portal',
  imports: [DecimalPipe, PaginadorComponent],
  templateUrl: './portal.html',
  styleUrl: './portal.css'
})
export class PortalComponent implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly portal = inject(PortalService);
  private readonly notificacionesService = inject(NotificacionService);

  readonly user = this.auth.currentUser;

  resumen = signal<PortalResumen | null>(null);
  ahorro = signal<PortalAhorro[]>([]);
  ahorroPage = signal(0);
  ahorroSize = signal(10);
  ahorroTotalElements = signal(0);
  ahorroTotalPages = signal(0);
  aportaciones = signal<PortalAportacion[]>([]);
  aportacionesPage = signal(0);
  aportacionesSize = signal(10);
  aportacionesTotalElements = signal(0);
  aportacionesTotalPages = signal(0);
  creditos = signal<PortalCredito[]>([]);
  creditosPage = signal(0);
  creditosSize = signal(10);
  creditosTotalElements = signal(0);
  creditosTotalPages = signal(0);
  notificaciones = signal<Notificacion[]>([]);
  notifPage = signal(0);
  notifSize = signal(10);
  notifTotalElements = signal(0);
  notifTotalPages = signal(0);
  error = signal('');

  ngOnInit(): void {
    this.cargarResumen();
    this.cargarAhorro();
    this.cargarAportaciones();
    this.cargarCreditos();
    this.cargarNotificaciones();
  }

  cargarResumen(): void {
    this.portal.resumen().subscribe({
      next: (data) => this.resumen.set(data),
      error: () => this.error.set('No se pudo cargar el resumen.')
    });
  }

  cargarAhorro(): void {
    this.portal.ahorro({ page: this.ahorroPage(), size: this.ahorroSize() }).subscribe({
      next: (paginated) => {
        this.ahorro.set(paginated.content);
        this.ahorroTotalElements.set(paginated.totalElements);
        this.ahorroTotalPages.set(paginated.totalPages);
      },
      error: () => undefined
    });
  }

  cargarAportaciones(): void {
    this.portal.aportaciones({ page: this.aportacionesPage(), size: this.aportacionesSize() }).subscribe({
      next: (paginated) => {
        this.aportaciones.set(paginated.content);
        this.aportacionesTotalElements.set(paginated.totalElements);
        this.aportacionesTotalPages.set(paginated.totalPages);
      },
      error: () => undefined
    });
  }

  cargarCreditos(): void {
    this.portal.creditos({ page: this.creditosPage(), size: this.creditosSize() }).subscribe({
      next: (paginated) => {
        this.creditos.set(paginated.content);
        this.creditosTotalElements.set(paginated.totalElements);
        this.creditosTotalPages.set(paginated.totalPages);
      },
      error: () => undefined
    });
  }

  cargarNotificaciones(): void {
    this.notificacionesService.listar({
      page: this.notifPage(),
      size: this.notifSize()
    }).subscribe({
      next: (paginated) => {
        this.notificaciones.set(paginated.content);
        this.notifTotalElements.set(paginated.totalElements);
        this.notifTotalPages.set(paginated.totalPages);
      },
      error: () => undefined
    });
  }

  cambiarPaginaAhorro(p: number): void {
    this.ahorroPage.set(p);
    this.cargarAhorro();
  }

  cambiarTamanoAhorro(t: number): void {
    this.ahorroSize.set(t);
    this.ahorroPage.set(0);
    this.cargarAhorro();
  }

  cambiarPaginaAportaciones(p: number): void {
    this.aportacionesPage.set(p);
    this.cargarAportaciones();
  }

  cambiarTamanoAportaciones(t: number): void {
    this.aportacionesSize.set(t);
    this.aportacionesPage.set(0);
    this.cargarAportaciones();
  }

  cambiarPaginaCreditos(p: number): void {
    this.creditosPage.set(p);
    this.cargarCreditos();
  }

  cambiarTamanoCreditos(t: number): void {
    this.creditosSize.set(t);
    this.creditosPage.set(0);
    this.cargarCreditos();
  }

  cambiarPaginaNotif(p: number): void {
    this.notifPage.set(p);
    this.cargarNotificaciones();
  }

  cambiarTamanoNotif(t: number): void {
    this.notifSize.set(t);
    this.notifPage.set(0);
    this.cargarNotificaciones();
  }

  marcarLeida(id: number): void {
    this.notificacionesService.marcarLeida(id).subscribe({
      next: () => this.cargarNotificaciones(),
      error: () => undefined
    });
  }

  marcarTodas(): void {
    this.notificacionesService.marcarTodasLeidas().subscribe({
      next: () => this.cargarNotificaciones(),
      error: () => undefined
    });
  }

  logout(): void {
    this.auth.logout();
  }

  formato(value: number | undefined): string {
    if (value === undefined || value === null || Number.isNaN(value)) {
      return '0.00';
    }
    return new Intl.NumberFormat('es-EC', { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(value);
  }
}
