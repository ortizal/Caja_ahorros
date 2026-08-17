import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { ApiError } from '../../core/models/auth.model';
import { CuentaBancaria } from '../../core/models/banco.model';
import { BancoService } from '../../core/services/banco.service';
import { ToastService } from '../../core/services/toast.service';
import { AccionesMenuComponent } from '../../shared/components/acciones-menu/acciones-menu.component';

@Component({
  selector: 'app-cuentas-bancarias',
  imports: [RouterLink, DecimalPipe, AccionesMenuComponent],
  templateUrl: './cuentas-bancarias.html',
  styleUrl: './cuentas-bancarias.css'
})
export class CuentasBancariasComponent implements OnInit {
  private readonly bancoService = inject(BancoService);
  private readonly toast = inject(ToastService);
  private readonly auth = inject(AuthService);

  protected readonly cuentas = signal<CuentaBancaria[]>([]);
  protected readonly error = signal('');
  protected readonly cargando = signal(false);
  protected readonly puedeCrear = computed(() => this.auth.hasPermiso('BANCOS:CREAR'));

  ngOnInit(): void {
    this.cargarCuentas();
  }

  cargarCuentas(): void {
    this.cargando.set(true);
    this.error.set('');
    this.bancoService.cuentas().subscribe({
      next: (cuentas) => {
        this.cuentas.set(cuentas);
        this.cargando.set(false);
      },
      error: (err: HttpErrorResponse) => {
        const msg = (err.error as ApiError | undefined)?.message ?? 'No se pudieron cargar las cuentas.';
        this.error.set(msg);
        this.toast.error(msg);
        this.cargando.set(false);
      }
    });
  }
}
