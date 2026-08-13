import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { DecimalPipe, DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { finalize } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { ApiError } from '../../core/models/auth.model';
import {
  Capitalizacion,
  CuentaAhorro,
  MovimientoAhorro,
  ProductoAhorro
} from '../../core/models/ahorro.model';
import { Socio } from '../../core/models/socio.model';
import { AhorroService } from '../../core/services/ahorro.service';
import { SocioService } from '../../core/services/socio.service';

@Component({
  selector: 'app-ahorros',
  imports: [ReactiveFormsModule, DecimalPipe, DatePipe],
  templateUrl: './ahorros.html',
  styleUrl: './ahorros.css'
})
export class AhorrosComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly ahorroService = inject(AhorroService);
  private readonly socioService = inject(SocioService);
  private readonly auth = inject(AuthService);

  protected readonly tab = signal<'productos' | 'cuentas' | 'capitalizar'>('productos');
  protected readonly productos = signal<ProductoAhorro[]>([]);
  protected readonly cuentas = signal<CuentaAhorro[]>([]);
  protected readonly socios = signal<Socio[]>([]);
  protected readonly movimientos = signal<MovimientoAhorro[]>([]);
  protected readonly cuentaSeleccionada = signal<CuentaAhorro | null>(null);
  protected readonly capitalizacion = signal<Capitalizacion | null>(null);
  protected readonly error = signal('');
  protected readonly ok = signal('');
  protected readonly cargando = signal(false);
  protected readonly guardando = signal(false);

  protected readonly puedeVer = computedPermiso(this.auth, 'AHORROS:VER');
  protected readonly puedeCrear = computedPermiso(this.auth, 'AHORROS:CREAR');

  protected readonly productoForm = this.fb.nonNullable.group({
    nombre: ['', [Validators.required]],
    tasaInteres: [2.5, [Validators.required, Validators.min(0)]],
    periodicidadCapitalizacion: ['MENSUAL', [Validators.required]],
    saldoMinimo: [0, [Validators.min(0)]],
    limiteRetirosMes: [1, [Validators.min(1)]],
    vigenteDesde: [this.hoy(), [Validators.required]]
  });

  protected readonly aperturaForm = this.fb.nonNullable.group({
    socioId: [0, [Validators.required, Validators.min(1)]],
    productoId: [0, [Validators.required, Validators.min(1)]]
  });

  protected readonly depositoForm = this.fb.nonNullable.group({
    monto: [0, [Validators.required, Validators.min(0.01)]]
  });

  protected readonly retiroForm = this.fb.nonNullable.group({
    monto: [0, [Validators.required, Validators.min(0.01)]]
  });

  protected readonly capitalizarForm = this.fb.nonNullable.group({
    anio: [Number(this.hoy().slice(0, 4)), [Validators.required, Validators.min(2000)]],
    mes: [Number(this.hoy().slice(5, 7)), [Validators.required, Validators.min(1), Validators.max(12)]]
  });

  ngOnInit(): void {
    this.cargarProductos();
    this.cargarSocios();
    this.cargarCuentas();
  }

  private hoy(): string {
    return new Date().toISOString().slice(0, 10);
  }

  cargarProductos(): void {
    this.ahorroService.productos().subscribe({
      next: (p) => this.productos.set(p),
      error: () => this.productos.set([])
    });
  }

  cargarSocios(): void {
    this.socioService.listar('ACTIVO').subscribe({
      next: (s) => this.socios.set(s),
      error: () => this.socios.set([])
    });
  }

  cargarCuentas(): void {
    this.ahorroService.cuentas().subscribe({
      next: (c) => this.cuentas.set(c),
      error: () => this.cuentas.set([])
    });
  }

  crearProducto(): void {
    if (this.productoForm.invalid || this.guardando()) {
      return;
    }
    const raw = this.productoForm.getRawValue();
    this.guardando.set(true);
    this.error.set('');
    this.ok.set('');
    this.ahorroService
      .crearProducto({
        nombre: raw.nombre,
        tasaInteres: Number(raw.tasaInteres),
        periodicidadCapitalizacion: raw.periodicidadCapitalizacion,
        saldoMinimo: Number(raw.saldoMinimo),
        limiteRetirosMes: Number(raw.limiteRetirosMes),
        vigenteDesde: raw.vigenteDesde
      })
      .pipe(finalize(() => this.guardando.set(false)))
      .subscribe({
        next: () => {
          this.ok.set('Producto de ahorro creado.');
          this.productoForm.reset({
            nombre: '',
            tasaInteres: 2.5,
            periodicidadCapitalizacion: 'MENSUAL',
            saldoMinimo: 0,
            limiteRetirosMes: 1,
            vigenteDesde: this.hoy()
          });
          this.cargarProductos();
        },
        error: (err: HttpErrorResponse) =>
          this.error.set((err.error as ApiError | undefined)?.message ?? 'No se pudo crear el producto.')
      });
  }

  aperturar(): void {
    if (this.aperturaForm.invalid || this.guardando()) {
      return;
    }
    const raw = this.aperturaForm.getRawValue();
    this.guardando.set(true);
    this.error.set('');
    this.ok.set('');
    this.ahorroService
      .aperturar({ socioId: Number(raw.socioId), productoId: Number(raw.productoId) })
      .pipe(finalize(() => this.guardando.set(false)))
      .subscribe({
        next: () => {
          this.ok.set('Cuenta de ahorro aperturada.');
          this.aperturaForm.reset({ socioId: 0, productoId: 0 });
          this.cargarCuentas();
        },
        error: (err: HttpErrorResponse) =>
          this.error.set((err.error as ApiError | undefined)?.message ?? 'No se pudo aperturar la cuenta.')
      });
  }

  seleccionarCuenta(cuenta: CuentaAhorro): void {
    this.cuentaSeleccionada.set(cuenta);
    this.depositoForm.patchValue({ monto: 0 });
    this.retiroForm.patchValue({ monto: 0 });
    this.cargarMovimientos(cuenta.id);
  }

  cargarMovimientos(cuentaId: number): void {
    this.ahorroService.movimientos(cuentaId).subscribe({
      next: (m) => this.movimientos.set(m),
      error: () => this.movimientos.set([])
    });
  }

  depositar(): void {
    const cuenta = this.cuentaSeleccionada();
    if (!cuenta || this.depositoForm.invalid || this.guardando()) {
      return;
    }
    const raw = this.depositoForm.getRawValue();
    this.guardando.set(true);
    this.error.set('');
    this.ok.set('');
    this.ahorroService
      .depositar(cuenta.id, Number(raw.monto))
      .pipe(finalize(() => this.guardando.set(false)))
      .subscribe({
        next: (res) => {
          this.ok.set(`Deposito registrado en ${cuenta.numeroCuenta}.`);
          this.depositoForm.patchValue({ monto: 0 });
          this.actualizarSaldoSeleccionada(Number(res.saldoResultante));
          this.cargarCuentas();
          this.cargarMovimientos(cuenta.id);
        },
        error: (err: HttpErrorResponse) =>
          this.error.set((err.error as ApiError | undefined)?.message ?? 'No se pudo registrar el deposito.')
      });
  }

  retirar(): void {
    const cuenta = this.cuentaSeleccionada();
    if (!cuenta || this.retiroForm.invalid || this.guardando()) {
      return;
    }
    const raw = this.retiroForm.getRawValue();
    this.guardando.set(true);
    this.error.set('');
    this.ok.set('');
    this.ahorroService
      .retirar(cuenta.id, Number(raw.monto))
      .pipe(finalize(() => this.guardando.set(false)))
      .subscribe({
        next: (res) => {
          this.ok.set(`Retiro registrado en ${cuenta.numeroCuenta}.`);
          this.retiroForm.patchValue({ monto: 0 });
          this.actualizarSaldoSeleccionada(Number(res.saldoResultante));
          this.cargarCuentas();
          this.cargarMovimientos(cuenta.id);
        },
        error: (err: HttpErrorResponse) =>
          this.error.set((err.error as ApiError | undefined)?.message ?? 'No se pudo registrar el retiro.')
      });
  }

  actualizarSaldoSeleccionada(saldo: number): void {
    const cuenta = this.cuentaSeleccionada();
    if (cuenta) {
      this.cuentaSeleccionada.set({ ...cuenta, saldo });
    }
  }

  capitalizar(): void {
    if (this.capitalizarForm.invalid || this.guardando()) {
      return;
    }
    const raw = this.capitalizarForm.getRawValue();
    this.guardando.set(true);
    this.error.set('');
    this.ok.set('');
    this.capitalizacion.set(null);
    this.ahorroService
      .capitalizar(Number(raw.anio), Number(raw.mes))
      .pipe(finalize(() => this.guardando.set(false)))
      .subscribe({
        next: (res) => {
          this.capitalizacion.set(res);
          this.ok.set(`Intereses capitalizados: ${res.cuentasCapitalizadas} cuentas, total ${res.totalInteres}.`);
        },
        error: (err: HttpErrorResponse) =>
          this.error.set((err.error as ApiError | undefined)?.message ?? 'No se pudieron capitalizar los intereses.')
      });
  }
}

function computedPermiso(auth: AuthService, permiso: string): ReturnType<typeof computed> {
  return computed(() => auth.hasPermiso(permiso));
}
