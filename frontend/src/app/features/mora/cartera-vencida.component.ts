import { Component, OnInit, inject, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { CarteraItem, Morosidad } from '../../core/models/reporte.model';
import { ReporteService } from '../../core/services/reporte.service';
import { ToastService } from '../../core/services/toast.service';

@Component({
  selector: 'app-cartera-vencida',
  imports: [DecimalPipe],
  templateUrl: './cartera-vencida.html',
  styleUrl: './cartera-vencida.css'
})
export class CarteraVencidaComponent implements OnInit {
  private readonly reporteService = inject(ReporteService);
  private readonly toast = inject(ToastService);

  protected readonly cartera = signal<CarteraItem[]>([]);
  protected readonly morosidad = signal<Morosidad | null>(null);
  protected readonly cargando = signal(false);
  protected readonly error = signal('');
  protected readonly filtroEstado = signal<string>('TODOS');
  protected readonly buscar = signal('');
  protected readonly exportando = signal(false);

  readonly filtros = ['TODOS', 'PENDIENTE', 'VENCIDA'] as const;

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.cargando.set(true);
    this.error.set('');

    const estado = this.filtroEstado() === 'TODOS' ? undefined : this.filtroEstado();
    this.reporteService.cartera(estado).subscribe({
      next: (data) => {
        this.cartera.set(data);
        this.cargando.set(false);
      },
      error: () => {
        this.error.set('Error al cargar cartera vencida');
        this.cargando.set(false);
      }
    });

    this.reporteService.morosidad().subscribe({
      next: (m) => this.morosidad.set(m),
      error: () => undefined
    });
  }

  filtrar(estado: string): void {
    this.filtroEstado.set(estado);
    this.cargar();
  }

  itemsFiltrados(): CarteraItem[] {
    const term = this.buscar().toLowerCase();
    if (!term) return this.cartera();
    return this.cartera().filter(item =>
      item.socioCodigo?.toLowerCase().includes(term) ||
      item.socioNombre?.toLowerCase().includes(term) ||
      item.nombreProducto?.toLowerCase().includes(term)
    );
  }

  onBuscar(event: Event): void {
    this.buscar.set((event.target as HTMLInputElement).value);
  }

  exportarCsv(): void {
    this.exportando.set(true);
    this.reporteService.exportarCartera().subscribe({
      next: (blob) => this.descargar(blob, 'cartera.csv'),
      error: () => { this.toast.error('Error al exportar CSV'); this.exportando.set(false); }
    });
  }

  exportarExcel(): void {
    this.exportando.set(true);
    this.reporteService.exportarCarteraVencida('xlsx').subscribe({
      next: (blob) => this.descargar(blob, 'cartera-vencida.xlsx'),
      error: () => { this.toast.error('Error al exportar Excel'); this.exportando.set(false); }
    });
  }

  exportarPdf(): void {
    this.exportando.set(true);
    this.reporteService.exportarCarteraVencida('pdf').subscribe({
      next: (blob) => this.descargar(blob, 'cartera-vencida.pdf'),
      error: () => { this.toast.error('Error al exportar PDF'); this.exportando.set(false); }
    });
  }

  private descargar(blob: Blob, nombre: string): void {
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = nombre;
    a.click();
    URL.revokeObjectURL(url);
    this.exportando.set(false);
    this.toast.success('Archivo descargado');
  }

  badgeEstado(estado: string): string {
    return estado === 'VENCIDA' ? 'badge badge-danger' : 'badge badge-secondary';
  }
}
