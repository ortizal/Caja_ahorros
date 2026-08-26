import { Component, OnInit, inject, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { MoraClienteDetalle } from '../../core/models/mora.model';
import { MoraService } from '../../core/services/mora.service';

@Component({
  selector: 'app-mora-detalle',
  imports: [DecimalPipe],
  templateUrl: './mora-detalle.html',
  styleUrl: './mora-detalle.css'
})
export class MoraDetalleComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly moraService = inject(MoraService);

  protected readonly detalle = signal<MoraClienteDetalle | null>(null);
  protected readonly cargando = signal(false);
  protected readonly error = signal('');
  protected readonly creditoExpandido = signal<number | null>(null);

  ngOnInit(): void {
    const socioId = Number(this.route.snapshot.paramMap.get('id'));
    if (!socioId) {
      this.router.navigate(['/mora']);
      return;
    }
    this.cargar(socioId);
  }

  cargar(socioId: number): void {
    this.cargando.set(true);
    this.error.set('');
    this.moraService.detalleCliente(socioId).subscribe({
      next: (data) => {
        this.detalle.set(data);
        this.cargando.set(false);
      },
      error: () => {
        this.error.set('Error al cargar detalle del socio');
        this.cargando.set(false);
      }
    });
  }

  toggleCredito(creditoId: number): void {
    this.creditoExpandido.set(this.creditoExpandido() === creditoId ? null : creditoId);
  }

  volver(): void {
    this.router.navigate(['/mora']);
  }

  verCredito(creditoId: number): void {
    this.router.navigate(['/creditos', creditoId, 'detalle']);
  }

  badgeEstado(estado: string): string {
    switch (estado) {
      case 'VENCIDA': return 'badge badge-danger';
      case 'PENDIENTE': return 'badge badge-warning';
      case 'PAGADA': return 'badge badge-success';
      default: return 'badge badge-secondary';
    }
  }

  badgeDias(dias: number): string {
    if (dias > 90) return 'badge badge-danger';
    if (dias > 30) return 'badge badge-warning';
    return 'badge badge-secondary';
  }
}
