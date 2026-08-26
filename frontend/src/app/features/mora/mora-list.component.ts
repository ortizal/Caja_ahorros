import { Component, OnInit, inject, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { Router } from '@angular/router';
import { MoraCliente } from '../../core/models/mora.model';
import { MoraService } from '../../core/services/mora.service';
import { ReporteService } from '../../core/services/reporte.service';
import { ToastService } from '../../core/services/toast.service';

@Component({
  selector: 'app-mora-list',
  imports: [DecimalPipe],
  templateUrl: './mora-list.html',
  styleUrl: './mora-list.css'
})
export class MoraListComponent implements OnInit {
  private readonly moraService = inject(MoraService);
  private readonly reporteService = inject(ReporteService);
  private readonly toast = inject(ToastService);
  private readonly router = inject(Router);

  protected readonly clientes = signal<MoraCliente[]>([]);
  protected readonly cargando = signal(false);
  protected readonly error = signal('');
  protected readonly buscar = signal('');
  protected readonly exportando = signal(false);

  protected readonly totalCreditosEnMora = signal(0);
  protected readonly totalCuotasVencidas = signal(0);
  protected readonly totalMora = signal(0);

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.cargando.set(true);
    this.error.set('');
    this.moraService.listarClientesConMora().subscribe({
      next: (data) => {
        this.clientes.set(data);
        this.totalCreditosEnMora.set(data.reduce((s, c) => s + c.creditosEnMora, 0));
        this.totalCuotasVencidas.set(data.reduce((s, c) => s + c.cuotasVencidas, 0));
        this.totalMora.set(data.reduce((s, c) => s + c.moraTotal, 0));
        this.cargando.set(false);
      },
      error: () => {
        this.error.set('Error al cargar clientes con mora');
        this.cargando.set(false);
      }
    });
  }

  verDetalle(socioId: number): void {
    this.router.navigate(['/mora', socioId]);
  }

  clientesFiltrados(): MoraCliente[] {
    const term = this.buscar().toLowerCase();
    if (!term) return this.clientes();
    return this.clientes().filter(c =>
      c.socioCodigo?.toLowerCase().includes(term) ||
      c.socioNombre?.toLowerCase().includes(term) ||
      c.socioIdentificacion?.toLowerCase().includes(term)
    );
  }

  onBuscar(event: Event): void {
    this.buscar.set((event.target as HTMLInputElement).value);
  }

  badgeDias(dias: number): string {
    if (dias > 90) return 'badge badge-danger';
    if (dias > 30) return 'badge badge-warning';
    return 'badge badge-secondary';
  }

  exportarMora(formato: 'pdf' | 'xlsx'): void {
    if (this.exportando()) return;
    this.exportando.set(true);
    this.reporteService.exportarMora(formato).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `mora.${formato}`;
        a.click();
        URL.revokeObjectURL(url);
        this.exportando.set(false);
        this.toast.success(`Reporte mora.${formato} descargado.`);
      },
      error: () => {
        this.exportando.set(false);
        this.toast.error('Error al exportar reporte de mora.');
      }
    });
  }
}
