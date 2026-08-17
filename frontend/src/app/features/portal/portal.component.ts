import { Component, OnInit, inject, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { AuthService } from '../../core/auth/auth.service';
import { PortalService } from '../../core/services/portal.service';
import { NotificacionService } from '../../core/services/notificacion.service';
import { Notificacion } from '../../core/models/notificacion.model';
import { PortalAhorro, PortalAportacion, PortalCredito, PortalResumen } from '../../core/models/portal.model';

@Component({
  selector: 'app-portal',
  imports: [DecimalPipe],
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
  aportaciones = signal<PortalAportacion[]>([]);
  creditos = signal<PortalCredito[]>([]);
  notificaciones = signal<Notificacion[]>([]);
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
    this.portal.ahorro().subscribe({ next: (data) => this.ahorro.set(data), error: () => undefined });
  }

  cargarAportaciones(): void {
    this.portal.aportaciones().subscribe({ next: (data) => this.aportaciones.set(data), error: () => undefined });
  }

  cargarCreditos(): void {
    this.portal.creditos().subscribe({ next: (data) => this.creditos.set(data), error: () => undefined });
  }

  cargarNotificaciones(): void {
    this.notificacionesService.listar().subscribe({
      next: (data) => this.notificaciones.set(data),
      error: () => undefined
    });
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
