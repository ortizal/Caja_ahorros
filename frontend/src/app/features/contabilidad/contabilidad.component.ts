import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { DecimalPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { finalize } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { ApiError } from '../../core/models/auth.model';
import { SortState } from '../../core/models/paginado.model';
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
import { PaginadorComponent } from '../../shared/components/paginador/paginador.component';
import { SortableHeaderDirective } from '../../shared/components/sortable-header/sortable-header.directive';

@Component({
  selector: 'app-contabilidad',
  imports: [ReactiveFormsModule, DecimalPipe, RouterLink, AccionesMenuComponent, PaginadorComponent, SortableHeaderDirective],
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

  protected readonly periodoPage = signal(0);
  protected readonly periodoSize = signal(10);
  protected readonly periodoSort = signal<SortState | null>(null);
  protected readonly periodoTotalElements = signal(0);
  protected readonly periodoTotalPages = signal(0);

  protected readonly planPage = signal(0);
  protected readonly planSize = signal(10);
  protected readonly planSort = signal<SortState | null>(null);
  protected readonly planTotalElements = signal(0);
  protected readonly planTotalPages = signal(0);

  protected readonly diarioPage = signal(0);
  protected readonly diarioSize = signal(10);
  protected readonly diarioSort = signal<SortState | null>(null);
  protected readonly diarioTotalElements = signal(0);
  protected readonly diarioTotalPages = signal(0);

  protected readonly mayorPage = signal(0);
  protected readonly mayorSize = signal(10);
  protected readonly mayorSort = signal<SortState | null>(null);
  protected readonly mayorTotalElements = signal(0);
  protected readonly mayorTotalPages = signal(0);

  protected readonly balancePage = signal(0);
  protected readonly balanceSize = signal(10);
  protected readonly balanceSort = signal<SortState | null>(null);
  protected readonly balanceTotalElements = signal(0);
  protected readonly balanceTotalPages = signal(0);

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
    this.contabilidadService.periodos({
      page: this.periodoPage(),
      size: this.periodoSize(),
      sort: this.periodoSort() ? `${this.periodoSort()!.key},${this.periodoSort()!.dir}` : undefined
    }).subscribe({
      next: (p) => {
        this.periodos.set(p.content);
        this.periodoTotalElements.set(p.totalElements);
        this.periodoTotalPages.set(p.totalPages);
      },
      error: () => this.periodos.set([])
    });
  }

  cargarPlanCuentas(): void {
    this.contabilidadService.planCuentas({
      page: this.planPage(),
      size: this.planSize(),
      sort: this.planSort() ? `${this.planSort()!.key},${this.planSort()!.dir}` : undefined
    }).subscribe({
      next: (p) => {
        this.planCuentas.set(p.content);
        this.planTotalElements.set(p.totalElements);
        this.planTotalPages.set(p.totalPages);
      },
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
    this.diarioPage.set(0);
    this.diarioSort.set(null);
    this.contabilidadService.libroDiario(raw.desde, raw.hasta, {
      page: this.diarioPage(),
      size: this.diarioSize(),
      sort: this.diarioSort() ? `${this.diarioSort()!.key},${this.diarioSort()!.dir}` : undefined
    }).pipe(finalize(() => this.cargando.set(false))).subscribe({
      next: (a) => {
        this.diario.set(a.content);
        this.diarioTotalElements.set(a.totalElements);
        this.diarioTotalPages.set(a.totalPages);
      },
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
    this.mayorPage.set(0);
    this.mayorSort.set(null);
    this.contabilidadService.libroMayor(Number(raw.cuentaId), raw.desde, raw.hasta, {
      page: this.mayorPage(),
      size: this.mayorSize(),
      sort: this.mayorSort() ? `${this.mayorSort()!.key},${this.mayorSort()!.dir}` : undefined
    })
      .pipe(finalize(() => this.cargando.set(false)))
      .subscribe({
        next: (m) => {
          this.mayor.set(m.content);
          this.mayorTotalElements.set(m.totalElements);
          this.mayorTotalPages.set(m.totalPages);
        },
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
    this.balancePage.set(0);
    this.balanceSort.set(null);
    this.contabilidadService.balance(Number(raw.anio), Number(raw.mes), {
      page: this.balancePage(),
      size: this.balanceSize(),
      sort: this.balanceSort() ? `${this.balanceSort()!.key},${this.balanceSort()!.dir}` : undefined
    })
      .pipe(finalize(() => this.cargando.set(false)))
      .subscribe({
        next: (b) => {
          this.balance.set(b.content);
          this.balanceTotalElements.set(b.totalElements);
          this.balanceTotalPages.set(b.totalPages);
        },
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

  cambiarPaginaPeriodo(p: number): void {
    this.periodoPage.set(p);
    this.cargarPeriodos();
  }

  cambiarTamanoPeriodo(t: number): void {
    this.periodoSize.set(t);
    this.periodoPage.set(0);
    this.cargarPeriodos();
  }

  ordenarPeriodo(s: SortState): void {
    this.periodoSort.set(s);
    this.periodoPage.set(0);
    this.cargarPeriodos();
  }

  cambiarPaginaPlan(p: number): void {
    this.planPage.set(p);
    this.cargarPlanCuentas();
  }

  cambiarTamanoPlan(t: number): void {
    this.planSize.set(t);
    this.planPage.set(0);
    this.cargarPlanCuentas();
  }

  ordenarPlan(s: SortState): void {
    this.planSort.set(s);
    this.planPage.set(0);
    this.cargarPlanCuentas();
  }

  cambiarPaginaDiario(p: number): void {
    this.diarioPage.set(p);
    this.recargarDiario();
  }

  cambiarTamanoDiario(t: number): void {
    this.diarioSize.set(t);
    this.diarioPage.set(0);
    this.recargarDiario();
  }

  ordenarDiario(s: SortState): void {
    this.diarioSort.set(s);
    this.diarioPage.set(0);
    this.recargarDiario();
  }

  private recargarDiario(): void {
    if (this.diarioForm.invalid) {
      return;
    }
    const raw = this.diarioForm.getRawValue();
    this.cargando.set(true);
    this.error.set('');
    this.contabilidadService.libroDiario(raw.desde, raw.hasta, {
      page: this.diarioPage(),
      size: this.diarioSize(),
      sort: this.diarioSort() ? `${this.diarioSort()!.key},${this.diarioSort()!.dir}` : undefined
    }).pipe(finalize(() => this.cargando.set(false))).subscribe({
      next: (a) => {
        this.diario.set(a.content);
        this.diarioTotalElements.set(a.totalElements);
        this.diarioTotalPages.set(a.totalPages);
      },
      error: (err: HttpErrorResponse) => {
        const msg = (err.error as ApiError | undefined)?.message ?? 'No se pudo consultar el libro diario.';
        this.error.set(msg);
        this.toast.error(msg);
      }
    });
  }

  cambiarPaginaMayor(p: number): void {
    this.mayorPage.set(p);
    this.recargarMayor();
  }

  cambiarTamanoMayor(t: number): void {
    this.mayorSize.set(t);
    this.mayorPage.set(0);
    this.recargarMayor();
  }

  ordenarMayor(s: SortState): void {
    this.mayorSort.set(s);
    this.mayorPage.set(0);
    this.recargarMayor();
  }

  private recargarMayor(): void {
    if (this.mayorForm.invalid) {
      return;
    }
    const raw = this.mayorForm.getRawValue();
    this.cargando.set(true);
    this.error.set('');
    this.contabilidadService.libroMayor(Number(raw.cuentaId), raw.desde, raw.hasta, {
      page: this.mayorPage(),
      size: this.mayorSize(),
      sort: this.mayorSort() ? `${this.mayorSort()!.key},${this.mayorSort()!.dir}` : undefined
    })
      .pipe(finalize(() => this.cargando.set(false)))
      .subscribe({
        next: (m) => {
          this.mayor.set(m.content);
          this.mayorTotalElements.set(m.totalElements);
          this.mayorTotalPages.set(m.totalPages);
        },
        error: (err: HttpErrorResponse) => {
          const msg = (err.error as ApiError | undefined)?.message ?? 'No se pudo consultar el libro mayor.';
          this.error.set(msg);
          this.toast.error(msg);
        }
      });
  }

  cambiarPaginaBalance(p: number): void {
    this.balancePage.set(p);
    this.recargarBalance();
  }

  cambiarTamanoBalance(t: number): void {
    this.balanceSize.set(t);
    this.balancePage.set(0);
    this.recargarBalance();
  }

  ordenarBalance(s: SortState): void {
    this.balanceSort.set(s);
    this.balancePage.set(0);
    this.recargarBalance();
  }

  private recargarBalance(): void {
    if (this.balanceForm.invalid) {
      return;
    }
    const raw = this.balanceForm.getRawValue();
    this.cargando.set(true);
    this.error.set('');
    this.contabilidadService.balance(Number(raw.anio), Number(raw.mes), {
      page: this.balancePage(),
      size: this.balanceSize(),
      sort: this.balanceSort() ? `${this.balanceSort()!.key},${this.balanceSort()!.dir}` : undefined
    })
      .pipe(finalize(() => this.cargando.set(false)))
      .subscribe({
        next: (b) => {
          this.balance.set(b.content);
          this.balanceTotalElements.set(b.totalElements);
          this.balanceTotalPages.set(b.totalPages);
        },
        error: (err: HttpErrorResponse) => {
          const msg = (err.error as ApiError | undefined)?.message ?? 'No se pudo consultar el balance.';
          this.error.set(msg);
          this.toast.error(msg);
        }
      });
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
