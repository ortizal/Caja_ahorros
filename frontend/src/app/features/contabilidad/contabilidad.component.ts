import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { DecimalPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { finalize } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { ApiError } from '../../core/models/auth.model';
import {
  Asiento,
  BalanceLinea,
  MayorLinea,
  PeriodoContable,
  PlanCuenta
} from '../../core/models/contabilidad.model';
import { ContabilidadService } from '../../core/services/contabilidad.service';
import { ToastService } from '../../core/services/toast.service';
import { AccionesMenuComponent } from '../../shared/components/acciones-menu/acciones-menu.component';

@Component({
  selector: 'app-contabilidad',
  imports: [ReactiveFormsModule, DecimalPipe, RouterLink, AccionesMenuComponent],
  templateUrl: './contabilidad.html',
  styleUrl: './contabilidad.css'
})
export class ContabilidadComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly contabilidadService = inject(ContabilidadService);
  private readonly auth = inject(AuthService);
  private readonly toast = inject(ToastService);

  protected readonly tab = signal<string>('periodos');
  protected readonly periodos = signal<PeriodoContable[]>([]);
  protected readonly planCuentas = signal<PlanCuenta[]>([]);
  protected readonly diario = signal<Asiento[]>([]);
  protected readonly mayor = signal<MayorLinea[]>([]);
  protected readonly balance = signal<BalanceLinea[]>([]);
  protected readonly error = signal('');
  protected readonly cargando = signal(false);
  protected readonly guardando = signal(false);

  protected readonly puedeCrear = computedPermiso(this.auth, 'CONTABILIDAD:CREAR');
  protected readonly puedeAprobar = computedPermiso(this.auth, 'CONTABILIDAD:APROBAR');
  protected readonly esAdmin = computedRole(this.auth, 'ADMIN');

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

  ngOnInit(): void {
    this.cargarPeriodos();
    this.cargarPlanCuentas();
  }

  private hoy(): string {
    return new Date().toISOString().slice(0, 10);
  }

  private inicioMes(): string {
    const hoy = new Date();
    return `${hoy.getFullYear()}-${String(hoy.getMonth() + 1).padStart(2, '0')}-01`;
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

  consultarDiario(): void {
    if (this.diarioForm.invalid) {
      return;
    }
    const raw = this.diarioForm.getRawValue();
    this.cargando.set(true);
    this.error.set('');
    this.contabilidadService.libroDiario(raw.desde, raw.hasta).pipe(finalize(() => this.cargando.set(false))).subscribe({
      next: (a) => this.diario.set(a),
      error: (err: HttpErrorResponse) => {
        const msg = (err.error as ApiError | undefined)?.message ?? 'No se pudo consultar el libro diario.';
        this.error.set(msg);
        this.toast.error(msg);
      }
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
        error: (err: HttpErrorResponse) => {
          const msg = (err.error as ApiError | undefined)?.message ?? 'No se pudo consultar el libro mayor.';
          this.error.set(msg);
          this.toast.error(msg);
        }
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
        error: (err: HttpErrorResponse) => {
          const msg = (err.error as ApiError | undefined)?.message ?? 'No se pudo consultar el balance.';
          this.error.set(msg);
          this.toast.error(msg);
        }
      });
  }

  totalBalance(tipo: 'debe' | 'haber'): number {
    return this.balance().reduce((acc, l) => acc + (tipo === 'debe' ? l.debe : l.haber), 0);
  }

  cerrarPeriodo(periodo: PeriodoContable): void {
    this.guardando.set(true);
    this.error.set('');
    this.contabilidadService.cerrarPeriodo(periodo.anio, periodo.mes)
      .pipe(finalize(() => this.guardando.set(false)))
      .subscribe({
        next: () => {
          this.toast.success('Periodo cerrado.');
          this.cargarPeriodos();
        },
        error: (err: HttpErrorResponse) => {
          const msg = (err.error as ApiError | undefined)?.message ?? 'No se pudo cerrar el periodo.';
          this.error.set(msg);
          this.toast.error(msg);
        }
      });
  }

  reabrirPeriodo(periodo: PeriodoContable): void {
    this.guardando.set(true);
    this.error.set('');
    this.contabilidadService.reabrirPeriodo(periodo.anio, periodo.mes)
      .pipe(finalize(() => this.guardando.set(false)))
      .subscribe({
        next: () => {
          this.toast.success('Periodo reabierto.');
          this.cargarPeriodos();
        },
        error: (err: HttpErrorResponse) => {
          const msg = (err.error as ApiError | undefined)?.message ?? 'No se pudo reabrir el periodo.';
          this.error.set(msg);
          this.toast.error(msg);
        }
      });
  }
}

function computedPermiso(auth: AuthService, permiso: string): ReturnType<typeof computed> {
  return computed(() => auth.hasPermiso(permiso));
}

function computedRole(auth: AuthService, rol: string): ReturnType<typeof computed> {
  return computed(() => auth.currentUser()?.roles.includes(rol) ?? false);
}
