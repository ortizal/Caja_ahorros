import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DecimalPipe } from '@angular/common';
import { BalanceService } from '../../core/services/balance.service';
import { BalanceComprobacionResponse } from '../../core/models/balance.model';

@Component({
  selector: 'app-balance-comprobacion',
  imports: [FormsModule, DecimalPipe],
  templateUrl: './balance-comprobacion.html',
  styleUrl: './balance-comprobacion.css'
})
export class BalanceComprobacionComponent {
  private readonly balanceService = inject(BalanceService);

  public readonly anio = signal(new Date().getFullYear());
  public readonly mes = signal(new Date().getMonth() + 1);
  public readonly datos = signal<BalanceComprobacionResponse | null>(null);
  public readonly cargando = signal(false);
  public readonly error = signal('');

  cargar(): void {
    this.cargando.set(true);
    this.error.set('');
    this.balanceService.balanceComprobacion(this.anio(), this.mes()).subscribe({
      next: (res) => {
        this.datos.set(res);
        this.cargando.set(false);
      },
      error: () => {
        this.error.set('No se pudo cargar el balance de comprobación.');
        this.cargando.set(false);
      }
    });
  }

  exportar(formato: 'pdf' | 'xlsx'): void {
    this.balanceService.exportComprobacion(this.anio(), this.mes(), formato);
  }
}