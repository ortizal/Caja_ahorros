import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { UpperCasePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { Reporte, ENTIDADES_MAP } from '../../core/models/reporte.model';
import { ReporteAdminService } from '../../core/services/reporte-admin.service';
import { ReporteService } from '../../core/services/reporte.service';
import { ToastService } from '../../core/services/toast.service';
import { AuthService } from '../../core/auth/auth.service';
import { PaginadorComponent } from '../../shared/components/paginador/paginador.component';
import { AccionesMenuComponent } from '../../shared/components/acciones-menu/acciones-menu.component';
import { ModalComponent } from '../../shared/components/modal/modal.component';

@Component({
  selector: 'app-reporte-list',
  imports: [RouterLink, UpperCasePipe, PaginadorComponent, AccionesMenuComponent, ModalComponent],
  templateUrl: './reporte-list.html',
  styleUrl: './reporte-list.css'
})
export class ReporteListComponent implements OnInit {
  private readonly reporteAdminService = inject(ReporteAdminService);
  private readonly reporteService = inject(ReporteService);
  private readonly toast = inject(ToastService);
  private readonly auth = inject(AuthService);

  protected readonly reportes = signal<Reporte[]>([]);
  protected readonly cargando = signal(false);
  protected readonly error = signal('');
  protected readonly buscar = signal('');

  protected readonly page = signal(0);
  protected readonly size = signal(10);
  protected readonly totalElements = signal(0);
  protected readonly totalPages = signal(0);

  protected readonly puedeCrear = computed(() => this.auth.hasPermiso('SEGURIDAD:CREAR'));
  protected readonly puedeEditar = computed(() => this.auth.hasPermiso('SEGURIDAD:EDITAR'));

  protected readonly previewAbierto = signal(false);
  protected readonly previewUrl = signal('');
  protected readonly previewNombre = signal('');
  protected readonly previewCargando = signal(false);

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.cargando.set(true);
    this.error.set('');
    this.reporteAdminService.listar(
      { page: this.page(), size: this.size() },
      this.buscar() || undefined
    ).subscribe({
      next: (res) => {
        this.reportes.set(res.content);
        this.totalElements.set(res.totalElements);
        this.totalPages.set(res.totalPages);
        this.cargando.set(false);
      },
      error: () => {
        this.error.set('Error al cargar reportes');
        this.cargando.set(false);
      }
    });
  }

  onBuscar(event: Event): void {
    const valor = (event.target as HTMLInputElement).value;
    this.buscar.set(valor);
    this.page.set(0);
    this.cargar();
  }

  onPageChange(pagina: number): void {
    this.page.set(pagina);
    this.cargar();
  }

  onSizeChange(tamano: number): void {
    this.size.set(tamano);
    this.page.set(0);
    this.cargar();
  }

  toggleActivo(reporte: Reporte): void {
    if (!reporte.id) return;
    this.reporteAdminService.toggleActivo(reporte.id).subscribe({
      next: () => {
        this.toast.success(`Reporte ${reporte.activo ? 'desactivado' : 'activado'} correctamente`);
        this.cargar();
      },
      error: () => this.toast.error('Error al cambiar estado del reporte')
    });
  }

  eliminar(reporte: Reporte): void {
    if (!reporte.id) return;
    if (!confirm(`¿Desactivar el reporte "${reporte.nombre}"?`)) return;
    this.reporteAdminService.eliminar(reporte.id).subscribe({
      next: () => {
        this.toast.success('Reporte desactivado');
        this.cargar();
      },
      error: () => this.toast.error('Error al desactivar reporte')
    });
  }

  vistaPrevia(reporte: Reporte): void {
    if (reporte.formatoDefault === 'xlsx') {
      this.descargarEjemplo(reporte);
      return;
    }
    this.previewCargando.set(true);
    this.previewNombre.set(reporte.titulo);
    this.previewAbierto.set(true);
    this.previewUrl.set('');

    this.reporteService.descargarReporte(reporte.nombre, 'pdf').subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        this.previewUrl.set(url);
        this.previewCargando.set(false);
      },
      error: () => {
        this.toast.error('No se pudo generar la vista previa');
        this.previewAbierto.set(false);
        this.previewCargando.set(false);
      }
    });
  }

  descargarEjemplo(reporte: Reporte): void {
    this.reporteService.descargarReporte(reporte.nombre, 'xlsx').subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `${reporte.nombre}.xlsx`;
        a.click();
        URL.revokeObjectURL(url);
        this.toast.success('Archivo descargado');
      },
      error: () => this.toast.error('No se pudo descargar el archivo')
    });
  }

  cerrarPreview(): void {
    if (this.previewUrl()) {
      URL.revokeObjectURL(this.previewUrl());
    }
    this.previewAbierto.set(false);
    this.previewUrl.set('');
  }

  entidadLabel(entidad: string): string {
    return ENTIDADES_MAP[entidad] || entidad;
  }
}
