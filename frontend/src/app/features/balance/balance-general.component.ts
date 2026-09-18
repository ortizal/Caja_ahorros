import { Component, OnInit, inject, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { BalanceService } from '../../core/services/balance.service';
import { BalanceSituacionResponse } from '../../core/models/balance.model';

@Component({
  selector: 'app-balance-general',
  imports: [DecimalPipe],
  templateUrl: './balance-general.html',
  styleUrl: './balance-general.css'
})
export class BalanceGeneralComponent implements OnInit {
  private readonly balanceService = inject(BalanceService);

  public readonly datos = signal<BalanceSituacionResponse | null>(null);
  public readonly cargando = signal(false);
  public readonly error = signal('');

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.cargando.set(true);
    this.error.set('');
    this.balanceService.balanceSituacion().subscribe({
      next: (res) => {
        this.datos.set(res);
        this.cargando.set(false);
      },
      error: () => {
        this.error.set('No se pudo cargar el balance general.');
        this.cargando.set(false);
      }
    });
  }

  exportar(formato: 'pdf' | 'xlsx'): void {
    this.balanceService.exportSituacion(formato);
  }
}