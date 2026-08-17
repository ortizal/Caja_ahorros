import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { DecimalPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { ApiError } from '../../core/models/auth.model';
import { PlanCuenta } from '../../core/models/contabilidad.model';
import {
  CuentaPorCobrar,
  CuentaPorPagar,
  Gasto,
  PresupuestoResumen
} from '../../core/models/tesoreria.model';
import { ContabilidadService } from '../../core/services/contabilidad.service';
import { TesoreriaService } from '../../core/services/tesoreria.service';
import { ToastService } from '../../core/services/toast.service';
import { AccionesMenuComponent } from '../../shared/components/acciones-menu/acciones-menu.component';

@Component({
  selector: 'app-tesoreria',
  imports: [ReactiveFormsModule, FormsModule, DecimalPipe, RouterLink, AccionesMenuComponent],
  templateUrl: './tesoreria.html',
  styleUrl: './tesoreria.css'
})
export class TesoreriaComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly tesoreriaService = inject(TesoreriaService);
  private readonly contabilidadService = inject(ContabilidadService);
  private readonly toast = inject(ToastService);
  private readonly auth = inject(AuthService);

  protected readonly tab = signal<string>('gastos');
  protected readonly gastos = signal<Gasto[]>([]);
  protected readonly cuentasPagar = signal<CuentaPorPagar[]>([]);
  protected readonly cuentasCobrar = signal<CuentaPorCobrar[]>([]);
  protected readonly presupuesto = signal<PresupuestoResumen | null>(null);
  protected readonly planCuentas = signal<PlanCuenta[]>([]);
  protected readonly anioSel = signal<number>(new Date().getFullYear());
  protected readonly rechazoDe = signal<number | null>(null);
  protected readonly motivo = signal('');
  protected readonly error = signal('');
  protected readonly cargando = signal(false);
  protected readonly guardando = signal(false);

  protected readonly puedeCrear = computed(() => this.auth.hasPermiso('TESORERIA:CREAR'));
  protected readonly puedeAprobar = computed(() => this.auth.hasPermiso('TESORERIA:APROBAR'));
  protected readonly puedeEditar = computed(() => this.auth.hasPermiso('TESORERIA:EDITAR'));
  protected readonly puedeAnular = computed(() => this.auth.hasPermiso('TESORERIA:ANULAR'));

  protected readonly presupuestoForm = this.fb.nonNullable.group({
    anio: [new Date().getFullYear(), [Validators.required, Validators.min(2000)]],
    concepto: ['', [Validators.required]],
    cuentaContableId: [0, [Validators.required]],
    montoPresupuestado: [0, [Validators.required, Validators.min(0.01)]]
  });

  ngOnInit(): void {
    this.cargarPlanCuentas();
    this.cargarGastos();
    this.cargarCuentasPagar();
    this.cargarCuentasCobrar();
    this.cargarPresupuesto();
  }

  cargarPlanCuentas(): void {
    this.contabilidadService.planCuentas().subscribe({
      next: (p) => this.planCuentas.set(p),
      error: () => this.planCuentas.set([])
    });
  }

  cuentaLabel(id: number): string {
    const cuenta = this.planCuentas().find((c) => c.id === id);
    return cuenta ? `${cuenta.codigo} — ${cuenta.nombre}` : '';
  }

  cargarGastos(): void {
    this.tesoreriaService.gastos().subscribe({
      next: (g) => this.gastos.set(g),
      error: (err: HttpErrorResponse) =>
        this.error.set((err.error as ApiError | undefined)?.message ?? 'No se pudieron cargar los gastos.')
    });
  }

  aprobarGasto(id: number): void {
    this.cambiarEstadoGasto('aprobar', id);
  }

  iniciarRechazo(id: number): void {
    this.rechazoDe.set(id);
    this.motivo.set('');
  }

  cancelarRechazo(): void {
    this.rechazoDe.set(null);
    this.motivo.set('');
  }

  confirmarRechazo(): void {
    const id = this.rechazoDe();
    if (id === null || !this.motivo().trim()) {
      this.error.set('Debe indicar el motivo del rechazo.');
      return;
    }
    this.rechazoDe.set(null);
    this.cambiarEstadoGasto('rechazar', id, this.motivo());
    this.motivo.set('');
  }

  pagarGasto(id: number): void {
    this.cambiarEstadoGasto('pagar', id);
  }

  anularGasto(id: number): void {
    this.cambiarEstadoGasto('anular', id);
  }

  private cambiarEstadoGasto(accion: 'aprobar' | 'rechazar' | 'pagar' | 'anular', id: number, motivoRechazo?: string): void {
    this.guardando.set(true);
    this.error.set('');
    const request = this.tesoreriaService;
    const accion$ =
      accion === 'aprobar'
        ? request.aprobarGasto(id, { aprobar: true })
        : accion === 'rechazar'
          ? request.aprobarGasto(id, { aprobar: false, motivoRechazo })
          : accion === 'pagar'
            ? request.pagarGasto(id)
            : request.anularGasto(id);
    accion$.pipe(finalize(() => this.guardando.set(false))).subscribe({
      next: (g) => {
        this.toast.success(`Gasto #${g.id} ${accion === 'aprobar' ? 'aprobado' : accion === 'rechazar' ? 'rechazado' : accion === 'pagar' ? 'pagado' : 'anulado'}.`);
        this.cargarGastos();
      },
      error: (err: HttpErrorResponse) => {
        const msg = (err.error as ApiError | undefined)?.message ?? 'No se pudo actualizar el gasto.';
        this.error.set(msg);
        this.toast.error(msg);
      }
    });
  }

  cargarCuentasPagar(): void {
    this.tesoreriaService.cuentasPorPagar().subscribe({
      next: (c) => this.cuentasPagar.set(c),
      error: (err: HttpErrorResponse) =>
        this.error.set((err.error as ApiError | undefined)?.message ?? 'No se pudieron cargar las cuentas por pagar.')
    });
  }

  pagarCuentaPagar(id: number): void {
    this.guardando.set(true);
    this.error.set('');
    this.tesoreriaService.pagarCuentaPorPagar(id).pipe(finalize(() => this.guardando.set(false))).subscribe({
      next: (c) => {
        this.toast.success(`Cuenta por pagar #${c.id} pagada.`);
        this.cargarCuentasPagar();
      },
      error: (err: HttpErrorResponse) => {
        const msg = (err.error as ApiError | undefined)?.message ?? 'No se pudo pagar la cuenta.';
        this.error.set(msg);
        this.toast.error(msg);
      }
    });
  }

  cargarCuentasCobrar(): void {
    this.tesoreriaService.cuentasPorCobrar().subscribe({
      next: (c) => this.cuentasCobrar.set(c),
      error: (err: HttpErrorResponse) =>
        this.error.set((err.error as ApiError | undefined)?.message ?? 'No se pudieron cargar las cuentas por cobrar.')
    });
  }

  cobrarCuentaCobrar(id: number): void {
    this.guardando.set(true);
    this.error.set('');
    this.tesoreriaService.cobrarCuentaPorCobrar(id).pipe(finalize(() => this.guardando.set(false))).subscribe({
      next: (c) => {
        this.toast.success(`Cuenta por cobrar #${c.id} cobrada.`);
        this.cargarCuentasCobrar();
      },
      error: (err: HttpErrorResponse) => {
        const msg = (err.error as ApiError | undefined)?.message ?? 'No se pudo cobrar la cuenta.';
        this.error.set(msg);
        this.toast.error(msg);
      }
    });
  }

  cambiarAnio(value: string): void {
    const anio = Number(value);
    if (anio > 0) {
      this.anioSel.set(anio);
      this.cargarPresupuesto();
    }
  }

  cargarPresupuesto(): void {
    this.cargando.set(true);
    this.error.set('');
    this.tesoreriaService.presupuesto(this.anioSel()).pipe(finalize(() => this.cargando.set(false))).subscribe({
      next: (p) => this.presupuesto.set(p),
      error: (err: HttpErrorResponse) =>
        this.error.set((err.error as ApiError | undefined)?.message ?? 'No se pudo cargar el presupuesto.')
    });
  }

  crearPartida(): void {
    if (this.presupuestoForm.invalid || this.guardando()) {
      return;
    }
    const raw = this.presupuestoForm.getRawValue();
    this.guardando.set(true);
    this.error.set('');
    this.tesoreriaService
      .crearPartidaPresupuesto({
        anio: Number(raw.anio),
        concepto: raw.concepto,
        cuentaContableId: Number(raw.cuentaContableId),
        montoPresupuestado: Number(raw.montoPresupuestado)
      })
      .pipe(finalize(() => this.guardando.set(false)))
      .subscribe({
        next: () => {
          this.toast.success('Partida presupuestal creada.');
          this.presupuestoForm.reset({
            anio: Number(raw.anio),
            concepto: '',
            cuentaContableId: 0,
            montoPresupuestado: 0
          });
          this.anioSel.set(Number(raw.anio));
          this.cargarPresupuesto();
        },
        error: (err: HttpErrorResponse) => {
          const msg = (err.error as ApiError | undefined)?.message ?? 'No se pudo crear la partida.';
          this.error.set(msg);
          this.toast.error(msg);
        }
      });
  }
}
