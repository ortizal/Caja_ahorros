import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormArray, FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { DecimalPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { finalize } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { ApiError } from '../../core/models/auth.model';
import {
  Asiento,
  BalanceLinea,
  MayorLinea,
  PeriodoContable,
  PlanCuenta,
  TIPOS_CUENTA
} from '../../core/models/contabilidad.model';
import { ContabilidadService } from '../../core/services/contabilidad.service';

@Component({
  selector: 'app-contabilidad',
  imports: [ReactiveFormsModule, DecimalPipe],
  templateUrl: './contabilidad.html',
  styleUrl: './contabilidad.css'
})
export class ContabilidadComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly contabilidadService = inject(ContabilidadService);
  private readonly auth = inject(AuthService);

  protected readonly tab = signal<string>('periodos');
  protected readonly periodos = signal<PeriodoContable[]>([]);
  protected readonly planCuentas = signal<PlanCuenta[]>([]);
  protected readonly diario = signal<Asiento[]>([]);
  protected readonly mayor = signal<MayorLinea[]>([]);
  protected readonly balance = signal<BalanceLinea[]>([]);
  protected readonly error = signal('');
  protected readonly ok = signal('');
  protected readonly cargando = signal(false);
  protected readonly guardando = signal(false);
  protected readonly tiposCuenta = TIPOS_CUENTA;

  protected readonly puedeCrear = computedPermiso(this.auth, 'CONTABILIDAD:CREAR');
  protected readonly puedeAprobar = computedPermiso(this.auth, 'CONTABILIDAD:APROBAR');
  protected readonly esAdmin = computedRole(this.auth, 'ADMIN');

  protected readonly cuentaForm = this.fb.nonNullable.group({
    codigo: ['', [Validators.required]],
    nombre: ['', [Validators.required]],
    tipo: ['ACTIVO', [Validators.required]],
    nivel: [1, [Validators.required, Validators.min(1)]],
    aceptaMovimiento: [true]
  });

  protected readonly diarioForm = this.fb.nonNullable.group({
    desde: [this.inicioMes(), [Validators.required]],
    hasta: [this.hoy(), [Validators.required]]
  });

  protected readonly mayorForm = this.fb.nonNullable.group({
    cuentaId: [0, [Validators.required]],
    desde: [this.inicioMes(), [Validators.required]],
    hasta: [this.hoy(), [Validators.required]]
  });

  protected readonly balanceForm = this.fb.nonNullable.group({
    anio: [Number(this.hoy().slice(0, 4)), [Validators.required]],
    mes: [Number(this.hoy().slice(5, 7)), [Validators.required, Validators.min(1), Validators.max(12)]]
  });

  protected readonly asientoForm = this.fb.nonNullable.group({
    fecha: [this.hoy(), [Validators.required]],
    descripcion: ['', [Validators.required]],
    detalles: this.fb.array([])
  });

  ngOnInit(): void {
    this.cargarPeriodos();
    this.cargarPlanCuentas();
    this.agregarDetalle();
    this.agregarDetalle();
  }

  private hoy(): string {
    return new Date().toISOString().slice(0, 10);
  }

  private inicioMes(): string {
    const hoy = new Date();
    return `${hoy.getFullYear()}-${String(hoy.getMonth() + 1).padStart(2, '0')}-01`;
  }

  get detalles(): FormArray {
    return this.asientoForm.get('detalles') as FormArray;
  }

  agregarDetalle(): void {
    this.detalles.push(
      this.fb.nonNullable.group({
        cuentaId: [0, [Validators.required]],
        debe: [0, [Validators.min(0)]],
        haber: [0, [Validators.min(0)]]
      })
    );
  }

  quitarDetalle(index: number): void {
    this.detalles.removeAt(index);
  }

  cargarPeriodos(): void {
    this.contabilidadService.periodos().subscribe({
      next: (p) => this.periodos.set(p),
      error: () => this.periodos.set([])
    });
  }

  cargarPlanCuentas(): void {
    this.contabilidadService.planCuentas().subscribe({
      next: (p) => this.planCuentas.set(p),
      error: () => this.planCuentas.set([])
    });
  }

  crearCuenta(): void {
    if (this.cuentaForm.invalid || this.guardando()) {
      return;
    }
    const raw = this.cuentaForm.getRawValue();
    this.guardando.set(true);
    this.error.set('');
    this.ok.set('');
    this.contabilidadService
      .crearCuenta({
        codigo: raw.codigo,
        nombre: raw.nombre,
        tipo: raw.tipo,
        nivel: Number(raw.nivel),
        aceptaMovimiento: raw.aceptaMovimiento
      })
      .pipe(finalize(() => this.guardando.set(false)))
      .subscribe({
        next: () => {
          this.ok.set('Cuenta contable creada.');
          this.cuentaForm.reset({ codigo: '', nombre: '', tipo: 'ACTIVO', nivel: 1, aceptaMovimiento: true });
          this.cargarPlanCuentas();
        },
        error: (err: HttpErrorResponse) =>
          this.error.set((err.error as ApiError | undefined)?.message ?? 'No se pudo crear la cuenta.')
      });
  }

  consultarDiario(): void {
    if (this.diarioForm.invalid) {
      return;
    }
    const raw = this.diarioForm.getRawValue();
    this.cargando.set(true);
    this.error.set('');
    this.contabilidadService.libroDiario(raw.desde, raw.hasta).pipe(finalize(() => this.cargando.set(false))).subscribe({
      next: (a) => this.diario.set(a),
      error: (err: HttpErrorResponse) =>
        this.error.set((err.error as ApiError | undefined)?.message ?? 'No se pudo consultar el libro diario.')
    });
  }

  consultarMayor(): void {
    if (this.mayorForm.invalid) {
      return;
    }
    const raw = this.mayorForm.getRawValue();
    this.cargando.set(true);
    this.error.set('');
    this.contabilidadService.libroMayor(Number(raw.cuentaId), raw.desde, raw.hasta)
      .pipe(finalize(() => this.cargando.set(false)))
      .subscribe({
        next: (m) => this.mayor.set(m),
        error: (err: HttpErrorResponse) =>
          this.error.set((err.error as ApiError | undefined)?.message ?? 'No se pudo consultar el libro mayor.')
      });
  }

  consultarBalance(): void {
    if (this.balanceForm.invalid) {
      return;
    }
    const raw = this.balanceForm.getRawValue();
    this.cargando.set(true);
    this.error.set('');
    this.contabilidadService.balance(Number(raw.anio), Number(raw.mes))
      .pipe(finalize(() => this.cargando.set(false)))
      .subscribe({
        next: (b) => this.balance.set(b),
        error: (err: HttpErrorResponse) =>
          this.error.set((err.error as ApiError | undefined)?.message ?? 'No se pudo consultar el balance.')
      });
  }

  totalBalance(tipo: 'debe' | 'haber'): number {
    return this.balance().reduce((acc, l) => acc + (tipo === 'debe' ? l.debe : l.haber), 0);
  }

  registrarAsiento(): void {
    if (this.asientoForm.invalid || this.guardando()) {
      return;
    }
    const raw = this.asientoForm.getRawValue();
    const detalles = (raw.detalles as { cuentaId: number; debe: number; haber: number }[])
      .map((d) => ({
        cuentaId: Number(d.cuentaId),
        debe: Number(d.debe) > 0 ? Number(d.debe) : undefined,
        haber: Number(d.haber) > 0 ? Number(d.haber) : undefined
      }))
      .filter((d) => d.debe !== undefined || d.haber !== undefined);

    const totalDebe = detalles.reduce((acc, d) => acc + (d.debe ?? 0), 0);
    const totalHaber = detalles.reduce((acc, d) => acc + (d.haber ?? 0), 0);
    if (detalles.length === 0 || totalDebe <= 0 || totalDebe !== totalHaber) {
      this.error.set('El asiento debe estar cuadradado (total debe = total haber).');
      return;
    }

    this.guardando.set(true);
    this.error.set('');
    this.ok.set('');
    this.contabilidadService
      .registrarAsiento({ fecha: raw.fecha, descripcion: raw.descripcion, detalles })
      .pipe(finalize(() => this.guardando.set(false)))
      .subscribe({
        next: () => {
          this.ok.set('Asiento registrado correctamente.');
          this.asientoForm.patchValue({ descripcion: '' });
          while (this.detalles.length > 0) {
            this.detalles.removeAt(0);
          }
          this.agregarDetalle();
          this.agregarDetalle();
        },
        error: (err: HttpErrorResponse) =>
          this.error.set((err.error as ApiError | undefined)?.message ?? 'No se pudo registrar el asiento.')
      });
  }

  cerrarPeriodo(periodo: PeriodoContable): void {
    this.guardando.set(true);
    this.error.set('');
    this.contabilidadService.cerrarPeriodo(periodo.anio, periodo.mes)
      .pipe(finalize(() => this.guardando.set(false)))
      .subscribe({
        next: () => {
          this.ok.set('Periodo cerrado.');
          this.cargarPeriodos();
        },
        error: (err: HttpErrorResponse) =>
          this.error.set((err.error as ApiError | undefined)?.message ?? 'No se pudo cerrar el periodo.')
      });
  }

  reabrirPeriodo(periodo: PeriodoContable): void {
    this.guardando.set(true);
    this.error.set('');
    this.contabilidadService.reabrirPeriodo(periodo.anio, periodo.mes)
      .pipe(finalize(() => this.guardando.set(false)))
      .subscribe({
        next: () => {
          this.ok.set('Periodo reabierto.');
          this.cargarPeriodos();
        },
        error: (err: HttpErrorResponse) =>
          this.error.set((err.error as ApiError | undefined)?.message ?? 'No se pudo reabrir el periodo.')
      });
  }
}

function computedPermiso(auth: AuthService, permiso: string): ReturnType<typeof computed> {
  return computed(() => auth.hasPermiso(permiso));
}

function computedRole(auth: AuthService, rol: string): ReturnType<typeof computed> {
  return computed(() => auth.currentUser()?.roles.includes(rol) ?? false);
}
