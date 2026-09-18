import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DecimalPipe } from '@angular/common';
import { BalanceService } from '../../core/services/balance.service';
import { BalancePersonalResponse } from '../../core/models/balance.model';

@Component({
  selector: 'app-balance-personal',
  imports: [FormsModule, DecimalPipe],
  templateUrl: './balance-personal.html',
  styleUrl: './balance-personal.css'
})
export class BalancePersonalComponent {
  private readonly balanceService = inject(BalanceService);

  public readonly socioId = signal(0);
  public readonly datos = signal<BalancePersonalResponse | null>(null);
  public readonly cargando = signal(false);
  public readonly error = signal('');

  cargar(): void {
    if (!this.socioId()) {
      this.error.set('Ingrese el ID del socio.');
      return;
    }
    this.cargando.set(true);
    this.error.set('');
    this.balanceService.balancePersonal(this.socioId()).subscribe({
      next: (res) => {
        this.datos.set(res);
        this.cargando.set(false);
      },
      error: () => {
        this.error.set('No se pudo cargar el balance del socio.');
        this.cargando.set(false);
      }
    });
  }

  exportar(formato: 'pdf' | 'xlsx'): void {
    if (this.datos()) {
      this.balanceService.exportPersonal(this.socioId(), formato);
    }
  }
}