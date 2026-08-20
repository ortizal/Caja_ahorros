import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { UpperCasePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { Reporte, ENTIDADES_MAP } from '../../core/models/reporte.model';
import { ReporteAdminService } from '../../core/services/reporte-admin.service';
import { ToastService } from '../../core/services/toast.service';
import { AuthService } from '../../core/auth/auth.service';
import { SortState } from '../../core/models/paginado.model';
import { PaginadorComponent } from '../../shared/components/paginador/paginador.component';
import { SortableHeaderDirective } from '../../shared/components/sortable-header/sortable-header.directive';

@Component({
  selector: 'app-reporte-list',
  imports: [RouterLink, UpperCasePipe, PaginadorComponent, SortableHeaderDirective],
  templateUrl: './reporte-list.html',
  styleUrl: './reporte-list.css'
})
export class ReporteListComponent implements OnInit {
  private readonly reporteService = inject(ReporteAdminService);
  private readonly toast = inject(ToastService);
  private readonly auth = inject(AuthService);

  protected readonly reportes = signal<Reporte[]>([]);
  protected readonly cargando = signal(false);
  protected readonly error = signal('');
  protected readonly buscar = signal('');

  protected readonly page = signal(0);
  protected readonly size = signal(10);
  protected readonly sort = signal<SortState | null>(null);
  protected readonly totalElements = signal(0);
  protected readonly totalPages = signal(0);

  protected readonly puedeCrear = computed(() => this.auth.hasPermiso('SEGURIDAD:CREAR'));
  protected readonly puedeEditar = computed(() => this.auth.hasPermiso('SEGURIDAD:EDITAR'));

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.cargando.set(true);
    this.error.set('');
    this.reporteService.listar(
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
    this.reporteService.toggleActivo(reporte.id).subscribe({
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
    this.reporteService.eliminar(reporte.id).subscribe({
      next: () => {
        this.toast.success('Reporte desactivado');
        this.cargar();
      },
      error: () => this.toast.error('Error al desactivar reporte')
    });
  }

  entidadLabel(entidad: string): string {
    return ENTIDADES_MAP[entidad] || entidad;
  }
}
