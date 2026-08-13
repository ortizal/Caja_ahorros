import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { DecimalPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { ApiError } from '../../core/models/auth.model';
import { CuentaBancaria } from '../../core/models/banco.model';
import { BancoService } from '../../core/services/banco.service';

@Component({
  selector: 'app-cuentas-bancarias',
  imports: [ReactiveFormsModule, RouterLink, DecimalPipe],
  templateUrl: './cuentas-bancarias.html',
  styleUrl: './cuentas-bancarias.css'
})
export class CuentasBancariasComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly bancoService = inject(BancoService);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly cuentas = signal<CuentaBancaria[]>([]);
  protected readonly error = signal('');
  protected readonly ok = signal('');
  protected readonly cargando = signal(false);
  protected readonly guardando = signal(false);
  protected readonly puedeCrear = signal(this.auth.hasPermiso('BANCOS:CREAR'));

  protected readonly form = this.fb.nonNullable.group({
    banco: ['', [Validators.required]],
    numeroCuenta: ['', [Validators.required]],
    tipo: ['CORRIENTE', [Validators.required]],
    saldoContable: [0, [Validators.min(0)]]
  });

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
        this.error.set((err.error as ApiError | undefined)?.message ?? 'No se pudieron cargar las cuentas.');
        this.cargando.set(false);
      }
    });
  }

  crearCuenta(): void {
    if (this.form.invalid || this.guardando()) {
      return;
    }
    const raw = this.form.getRawValue();
    this.guardando.set(true);
    this.error.set('');
    this.ok.set('');
    this.bancoService
      .crearCuenta({
        banco: raw.banco,
        numeroCuenta: raw.numeroCuenta,
        tipo: raw.tipo,
        saldoContable: Number(raw.saldoContable)
      })
      .pipe(finalize(() => this.guardando.set(false)))
      .subscribe({
        next: (cuenta) => {
          this.form.reset({ banco: '', numeroCuenta: '', tipo: 'CORRIENTE', saldoContable: 0 });
          this.ok.set(`Cuenta "${cuenta.numeroCuenta}" creada correctamente.`);
          this.cargarCuentas();
        },
        error: (err: HttpErrorResponse) =>
          this.error.set((err.error as ApiError | undefined)?.message ?? 'No se pudo crear la cuenta.')
      });
  }
}
