import { Component, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { DecimalPipe } from '@angular/common';
import { AuthService } from '../../core/auth/auth.service';
import { ReporteService } from '../../core/services/reporte.service';
import { DashboardResumen } from '../../core/models/reporte.model';

@Component({
  selector: 'app-dashboard',
  imports: [RouterLink, DecimalPipe],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css'
})
export class DashboardComponent implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly reporte = inject(ReporteService);

  readonly user = this.auth.currentUser;

  resumen = signal<DashboardResumen | null>(null);
  error = signal<string>('');

  ngOnInit(): void {
    this.reporte.resumen().subscribe({
      next: (data) => this.resumen.set(data),
      error: () => this.error.set('No se pudo cargar el resumen del dashboard.')
    });
  }

  has(permiso: string): boolean {
    return this.auth.hasPermiso(permiso);
  }

  formato(value: number | undefined): string {
    if (value === undefined || value === null || Number.isNaN(value)) {
      return '0';
    }
    return new Intl.NumberFormat('es-EC', { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(value);
  }
}
