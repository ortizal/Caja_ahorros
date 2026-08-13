import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { DecimalPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { ApiError } from '../../core/models/auth.model';
import {
  BancoMovimiento,
  Conciliacion,
  CuentaBancaria,
  TIPOS_MOVIMIENTO_BANCO
} from '../../core/models/banco.model';
import { BancoService } from '../../core/services/banco.service';

@Component({
  selector: 'app-cuenta-bancaria-detalle',
  imports: [ReactiveFormsModule, RouterLink, DecimalPipe],
  templateUrl: './cuenta-bancaria-detalle.html',
  styleUrl: './cuenta-bancaria-detalle.css'
})
export class CuentaBancariaDetalleComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly bancoService = inject(BancoService);
  private readonly route = inject(ActivatedRoute);

  protected readonly cuenta = signal<CuentaBancaria | null>(null);
  protected readonly movimientos = signal<BancoMovimiento[]>([]);
  protected readonly conciliacion = signal<Conciliacion | null>(null);
  protected readonly error = signal('');
  protected readonly ok = signal('');
  protected readonly cargando = signal(false);
  protected readonly guardando = signal(false);
  protected readonly tipos = TIPOS_MOVIMIENTO_BANCO;

  protected readonly movimientoForm = this.fb.nonNullable.group({
    tipo: ['DEPOSITO', [Validators.required]],
    monto: [0, [Validators.required, Validators.min(0.01)]],
    fecha: [this.hoy(), [Validators.required]]
  });

  protected readonly conciliacionForm = this.fb.nonNullable.group({
    periodo: [this.periodoActual(), [Validators.required]],
    saldoBancario: [0, [Validators.required, Validators.min(0)]]
  });

  private cuentaId = 0;

  ngOnInit(): void {
    this.cuentaId = Number(this.route.snapshot.paramMap.get('id'));
    this.cargarTodo();
  }

  private hoy(): string {
    return new Date().toISOString().slice(0, 10);
  }

  private periodoActual(): string {
    return this.hoy().slice(0, 7);
  }

  cargarTodo(): void {
    this.cargando.set(true);
    this.error.set('');
    this.bancoService.cuentas().subscribe({
      next: (cuentas) => {
        const cuenta = cuentas.find((c) => c.id === this.cuentaId) ?? null;
        this.cuenta.set(cuenta);
        if (cuenta) {
          this.cargarMovimientos();
        }
        this.cargando.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.error.set((err.error as ApiError | undefined)?.message ?? 'No se pudo cargar la cuenta.');
        this.cargando.set(false);
      }
    });
  }

  cargarMovimientos(): void {
    this.bancoService.movimientos(this.cuentaId).subscribe({
      next: (m) => this.movimientos.set(m),
      error: () => this.movimientos.set([])
    });
  }

  registrarMovimiento(): void {
    if (this.movimientoForm.invalid || this.guardando()) {
      return;
    }
    const raw = this.movimientoForm.getRawValue();
    this.guardando.set(true);
    this.error.set('');
    this.ok.set('');
    this.bancoService
      .registrarMovimiento(this.cuentaId, {
        tipo: raw.tipo,
        monto: Number(raw.monto),
        fecha: raw.fecha
      })
      .pipe(finalize(() => this.guardando.set(false)))
      .subscribe({
        next: () => {
          this.movimientoForm.patchValue({ tipo: 'DEPOSITO', monto: 0, fecha: this.hoy() });
          this.ok.set('Movimiento bancario registrado.');
          this.cargarTodo();
        },
        error: (err: HttpErrorResponse) =>
          this.error.set((err.error as ApiError | undefined)?.message ?? 'No se pudo registrar el movimiento.')
      });
  }

  conciliar(): void {
    if (this.conciliacionForm.invalid || this.guardando()) {
      return;
    }
    const raw = this.conciliacionForm.getRawValue();
    this.guardando.set(true);
    this.error.set('');
    this.ok.set('');
    this.bancoService
      .conciliar(this.cuentaId, raw.periodo, Number(raw.saldoBancario))
      .pipe(finalize(() => this.guardando.set(false)))
      .subscribe({
        next: (conciliacion) => {
          this.conciliacion.set(conciliacion);
          this.ok.set('Conciliación generada.');
          this.cargarMovimientos();
        },
        error: (err: HttpErrorResponse) =>
          this.error.set((err.error as ApiError | undefined)?.message ?? 'No se pudo conciliar la cuenta.')
      });
  }
}
