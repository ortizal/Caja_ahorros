import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { Socio } from '../../core/models/socio.model';
import { SocioService } from '../../core/services/socio.service';
import { ReporteService } from '../../core/services/reporte.service';
import { ApiError } from '../../core/models/auth.model';
import { ToastService } from '../../core/services/toast.service';
import { ModalComponent, ModalFooterDirective } from '../../shared/components/modal/modal.component';
import { AccionesMenuComponent } from '../../shared/components/acciones-menu/acciones-menu.component';
import { SocioDetalleComponent } from './socio-detail.component';

@Component({
  selector: 'app-socio-list',
  imports: [RouterLink, ModalComponent, ModalFooterDirective, SocioDetalleComponent, AccionesMenuComponent],
  templateUrl: './socio-list.html',
  styleUrl: './socio-list.css'
})
export class SocioListComponent implements OnInit {
  private readonly socioService = inject(SocioService);
  private readonly auth = inject(AuthService);
  private readonly reporteService = inject(ReporteService);
  private readonly toast = inject(ToastService);

  protected readonly socios = signal<Socio[]>([]);
  protected readonly loading = signal(true);
  protected readonly error = signal('');
  protected readonly exportando = signal(false);
  protected readonly detalle = signal<Socio | null>(null);
  protected readonly infoAbierto = signal(false);

  protected puedeCrear(): boolean {
    return this.auth.hasPermiso('SOCIOS:CREAR');
  }

  protected puedeEditar(): boolean {
    return this.auth.hasPermiso('SOCIOS:EDITAR');
  }

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.loading.set(true);
    this.error.set('');
    this.socioService.listar().subscribe({
      next: (data) => {
        this.socios.set(data);
        this.loading.set(false);
      },
      error: (err: HttpErrorResponse) => {
        const msg = (err.error as ApiError | undefined)?.message ?? 'No se pudieron cargar los socios.';
        this.error.set(msg);
        this.toast.error(msg);
        this.loading.set(false);
      }
    });
  }

  ver(socio: Socio): void {
    this.detalle.set(socio);
    this.infoAbierto.set(true);
  }

  cerrarInfo(): void {
    this.infoAbierto.set(false);
    this.detalle.set(null);
  }

  onEstadoActualizado(actualizado: Socio): void {
    this.socios.update((lista) => lista.map((s) => (s.id === actualizado.id ? actualizado : s)));
    this.detalle.set(actualizado);
  }

  exportarExcel(): void {
    this.descargar(this.reporteService.exportarSocios('xlsx'), 'socios.xlsx');
  }

  exportarPdf(): void {
    this.descargar(this.reporteService.exportarSocios('pdf'), 'socios.pdf');
  }

  private descargar(obs: Observable<Blob>, nombre: string): void {
    if (this.exportando()) {
      return;
    }
    this.exportando.set(true);
    this.error.set('');
    obs.subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = nombre;
        a.click();
        URL.revokeObjectURL(url);
        this.exportando.set(false);
        this.toast.success(`Reporte ${nombre} descargado.`);
      },
      error: () => {
        this.exportando.set(false);
        const msg = 'No se pudo exportar el reporte.';
        this.error.set(msg);
        this.toast.error(msg);
      }
    });
  }
}
