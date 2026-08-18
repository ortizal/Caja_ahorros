import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { DecimalPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { ApiError } from '../../core/models/auth.model';
import {
  BancoMovimiento,
  Conciliacion,
  CuentaBancaria,
  TIPOS_MOVIMIENTO_BANCO
} from '../../core/models/banco.model';
import { BancoService } from '../../core/services/banco.service';
import { ToastService } from '../../core/services/toast.service';
import { SortState } from '../../core/models/paginado.model';
import { PaginadorComponent } from '../../shared/components/paginador/paginador.component';
import { SortableHeaderDirective } from '../../shared/components/sortable-header/sortable-header.directive';

@Component({
  selector: 'app-cuenta-bancaria-detalle',
  imports: [ReactiveFormsModule, RouterLink, DecimalPipe, PaginadorComponent, SortableHeaderDirective],
  templateUrl: './cuenta-bancaria-detalle.html',
  styleUrl: './cuenta-bancaria-detalle.css'
})
export class CuentaBancariaDetalleComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly bancoService = inject(BancoService);
  private readonly toast = inject(ToastService);
  private readonly auth = inject(AuthService);
  private readonly route = inject(ActivatedRoute);

  protected readonly cuenta = signal<CuentaBancaria | null>(null);
  protected readonly movimientos = signal<BancoMovimiento[]>([]);
  protected readonly conciliacion = signal<Conciliacion | null>(null);
  protected readonly error = signal('');
  protected readonly cargando = signal(false);
  protected readonly guardando = signal(false);
  protected readonly tipos = TIPOS_MOVIMIENTO_BANCO;
  protected readonly puedeCrear = computed(() => this.auth.hasPermiso('BANCOS:CREAR'));

  protected readonly page = signal(0);
  protected readonly size = signal(10);
  protected readonly sort = signal<SortState | null>(null);
  protected readonly totalElements = signal(0);
  protected readonly totalPages = signal(0);

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
    this.bancoService.cuentas({ page: 0, size: 1000 }).subscribe({
      next: (paginated) => {
        const cuenta = paginated.content.find((c) => c.id === this.cuentaId) ?? null;
        this.cuenta.set(cuenta);
        if (cuenta) {
          this.cargarMovimientos();
        }
        this.cargando.set(false);
      },
      error: (err: HttpErrorResponse) => {
        const msg = (err.error as ApiError | undefined)?.message ?? 'No se pudo cargar la cuenta.';
        this.error.set(msg);
        this.toast.error(msg);
        this.cargando.set(false);
      }
    });
  }

  cargarMovimientos(): void {
    this.bancoService.movimientos(this.cuentaId, {
      page: this.page(),
      size: this.size(),
      sort: this.sort() ? `${this.sort()!.key},${this.sort()!.dir}` : undefined
    }).subscribe({
      next: (paginated) => {
        this.movimientos.set(paginated.content);
        this.totalElements.set(paginated.totalElements);
        this.totalPages.set(paginated.totalPages);
      },
      error: () => this.movimientos.set([])
    });
  }

  cambiarPagina(p: number): void {
    this.page.set(p);
    this.cargarMovimientos();
  }

  cambiarTamano(t: number): void {
    this.size.set(t);
    this.page.set(0);
    this.cargarMovimientos();
  }

  ordenar(s: SortState): void {
    this.sort.set(s);
    this.page.set(0);
    this.cargarMovimientos();
  }

  registrarMovimiento(): void {
    if (this.movimientoForm.invalid || this.guardando()) {
      return;
    }
    const raw = this.movimientoForm.getRawValue();
    this.guardando.set(true);
    this.error.set('');
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
          this.toast.success('Movimiento bancario registrado.');
          this.cargarTodo();
        },
        error: (err: HttpErrorResponse) => {
          const msg = (err.error as ApiError | undefined)?.message ?? 'No se pudo registrar el movimiento.';
          this.error.set(msg);
          this.toast.error(msg);
        }
      });
  }

  conciliar(): void {
    if (this.conciliacionForm.invalid || this.guardando()) {
      return;
    }
    const raw = this.conciliacionForm.getRawValue();
    this.guardando.set(true);
    this.error.set('');
    this.bancoService
      .conciliar(this.cuentaId, raw.periodo, Number(raw.saldoBancario))
      .pipe(finalize(() => this.guardando.set(false)))
      .subscribe({
        next: (conciliacion) => {
          this.conciliacion.set(conciliacion);
          this.toast.success('Conciliación generada.');
          this.cargarMovimientos();
        },
        error: (err: HttpErrorResponse) => {
          const msg = (err.error as ApiError | undefined)?.message ?? 'No se pudo conciliar la cuenta.';
          this.error.set(msg);
          this.toast.error(msg);
        }
      });
  }
}
