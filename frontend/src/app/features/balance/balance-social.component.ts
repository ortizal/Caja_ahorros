import { Component, OnInit, inject, signal } from '@angular/core';
import { DecimalPipe, PercentPipe } from '@angular/common';
import { BalanceService } from '../../core/services/balance.service';
import { BalanceSocialResponse } from '../../core/models/balance.model';

@Component({
  selector: 'app-balance-social',
  imports: [DecimalPipe, PercentPipe],
  templateUrl: './balance-social.html',
  styleUrl: './balance-social.css'
})
export class BalanceSocialComponent implements OnInit {
  private readonly balanceService = inject(BalanceService);

  public readonly datos = signal<BalanceSocialResponse | null>(null);
  public readonly cargando = signal(false);
  public readonly error = signal('');

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.cargando.set(true);
    this.error.set('');
    this.balanceService.balanceSocial().subscribe({
      next: (res) => {
        this.datos.set(res);
        this.cargando.set(false);
      },
      error: () => {
        this.error.set('No se pudo cargar el balance social.');
        this.cargando.set(false);
      }
    });
  }

  exportar(formato: 'pdf' | 'xlsx'): void {
    this.balanceService.exportSocial(formato);
  }
}