import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { DecimalPipe, DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { ApiError } from '../../core/models/auth.model';
import {
  Capitalizacion,
  CuentaAhorro,
  MovimientoAhorro,
  ProductoAhorro
} from '../../core/models/ahorro.model';
import { AhorroService } from '../../core/services/ahorro.service';
import { ToastService } from '../../core/services/toast.service';
import { AccionesMenuComponent } from '../../shared/components/acciones-menu/acciones-menu.component';
import { SortState } from '../../core/models/paginado.model';
import { PaginadorComponent } from '../../shared/components/paginador/paginador.component';
import { SortableHeaderDirective } from '../../shared/components/sortable-header/sortable-header.directive';

@Component({
  selector: 'app-ahorros',
  imports: [ReactiveFormsModule, DecimalPipe, DatePipe, RouterLink, AccionesMenuComponent, PaginadorComponent, SortableHeaderDirective],
  templateUrl: './ahorros.html',
  styleUrl: './ahorros.css'
})
export class AhorrosComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly ahorroService = inject(AhorroService);
  private readonly auth = inject(AuthService);
  private readonly toast = inject(ToastService);

  protected readonly tab = signal<'productos' | 'cuentas' | 'capitalizar'>('productos');
  protected readonly productos = signal<ProductoAhorro[]>([]);
  protected readonly cuentas = signal<CuentaAhorro[]>([]);
  protected readonly movimientos = signal<MovimientoAhorro[]>([]);
  protected readonly cuentaSeleccionada = signal<CuentaAhorro | null>(null);
  protected readonly capitalizacion = signal<Capitalizacion | null>(null);
  protected readonly error = signal('');
  protected readonly cargando = signal(false);
  protected readonly guardando = signal(false);

  protected readonly pageProductos = signal(0);
  protected readonly sizeProductos = signal(10);
  protected readonly sortProductos = signal<SortState | null>(null);
  protected readonly totalElementsProductos = signal(0);
  protected readonly totalPagesProductos = signal(0);

  protected readonly pageCuentas = signal(0);
  protected readonly sizeCuentas = signal(10);
  protected readonly sortCuentas = signal<SortState | null>(null);
  protected readonly totalElementsCuentas = signal(0);
  protected readonly totalPagesCuentas = signal(0);

  protected readonly pageMovimientos = signal(0);
  protected readonly sizeMovimientos = signal(10);
  protected readonly sortMovimientos = signal<SortState | null>(null);
  protected readonly totalElementsMovimientos = signal(0);
  protected readonly totalPagesMovimientos = signal(0);

  protected readonly puedeVer = computedPermiso(this.auth, 'AHORROS:VER');
  protected readonly puedeCrear = computedPermiso(this.auth, 'AHORROS:CREAR');

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
    this.cargarCuentas();
  }

  private hoy(): string {
    return new Date().toISOString().slice(0, 10);
  }

  cargarProductos(): void {
    this.cargando.set(true);
    this.error.set('');
    this.ahorroService.productosPag({
      page: this.pageProductos(),
      size: this.sizeProductos(),
      sort: this.sortProductos() ? `${this.sortProductos()!.key},${this.sortProductos()!.dir}` : undefined
    }).subscribe({
      next: (p) => {
        this.productos.set(p.content);
        this.totalElementsProductos.set(p.totalElements);
        this.totalPagesProductos.set(p.totalPages);
        this.cargando.set(false);
      },
      error: () => {
        this.productos.set([]);
        this.cargando.set(false);
      }
    });
  }

  cargarCuentas(): void {
    this.cargando.set(true);
    this.error.set('');
    this.ahorroService.cuentas(undefined, {
      page: this.pageCuentas(),
      size: this.sizeCuentas(),
      sort: this.sortCuentas() ? `${this.sortCuentas()!.key},${this.sortCuentas()!.dir}` : undefined
    }).subscribe({
      next: (c) => {
        this.cuentas.set(c.content);
        this.totalElementsCuentas.set(c.totalElements);
        this.totalPagesCuentas.set(c.totalPages);
        this.cargando.set(false);
      },
      error: () => {
        this.cuentas.set([]);
        this.cargando.set(false);
      }
    });
  }

  seleccionarCuenta(cuenta: CuentaAhorro): void {
    this.cuentaSeleccionada.set(cuenta);
    this.depositoForm.patchValue({ monto: 0 });
    this.retiroForm.patchValue({ monto: 0 });
    this.pageMovimientos.set(0);
    this.cargarMovimientos(cuenta.id);
  }

  cargarMovimientos(cuentaId: number): void {
    this.ahorroService.movimientos(cuentaId, {
      page: this.pageMovimientos(),
      size: this.sizeMovimientos(),
      sort: this.sortMovimientos() ? `${this.sortMovimientos()!.key},${this.sortMovimientos()!.dir}` : undefined
    }).subscribe({
      next: (m) => {
        this.movimientos.set(m.content);
        this.totalElementsMovimientos.set(m.totalElements);
        this.totalPagesMovimientos.set(m.totalPages);
      },
      error: () => this.movimientos.set([])
    });
  }

  cambiarPaginaProductos(p: number): void {
    this.pageProductos.set(p);
    this.cargarProductos();
  }

  cambiarTamanoProductos(t: number): void {
    this.sizeProductos.set(t);
    this.pageProductos.set(0);
    this.cargarProductos();
  }

  ordenarProductos(s: SortState): void {
    this.sortProductos.set(s);
    this.pageProductos.set(0);
    this.cargarProductos();
  }

  cambiarPaginaCuentas(p: number): void {
    this.pageCuentas.set(p);
    this.cargarCuentas();
  }

  cambiarTamanoCuentas(t: number): void {
    this.sizeCuentas.set(t);
    this.pageCuentas.set(0);
    this.cargarCuentas();
  }

  ordenarCuentas(s: SortState): void {
    this.sortCuentas.set(s);
    this.pageCuentas.set(0);
    this.cargarCuentas();
  }

  cambiarPaginaMovimientos(p: number): void {
    this.pageMovimientos.set(p);
    const cuenta = this.cuentaSeleccionada();
    if (cuenta) {
      this.cargarMovimientos(cuenta.id);
    }
  }

  cambiarTamanoMovimientos(t: number): void {
    this.sizeMovimientos.set(t);
    this.pageMovimientos.set(0);
    const cuenta = this.cuentaSeleccionada();
    if (cuenta) {
      this.cargarMovimientos(cuenta.id);
    }
  }

  ordenarMovimientos(s: SortState): void {
    this.sortMovimientos.set(s);
    this.pageMovimientos.set(0);
    const cuenta = this.cuentaSeleccionada();
    if (cuenta) {
      this.cargarMovimientos(cuenta.id);
    }
  }

  depositar(): void {
    const cuenta = this.cuentaSeleccionada();
    if (!cuenta || this.depositoForm.invalid || this.guardando()) {
      return;
    }
    const raw = this.depositoForm.getRawValue();
    this.guardando.set(true);
    this.error.set('');
    this.ahorroService
      .depositar(cuenta.id, Number(raw.monto))
      .pipe(finalize(() => this.guardando.set(false)))
      .subscribe({
        next: (res) => {
          this.toast.success(`Deposito registrado en ${cuenta.numeroCuenta}.`);
          this.depositoForm.patchValue({ monto: 0 });
          this.actualizarSaldoSeleccionada(Number(res.saldoResultante));
          this.pageCuentas.set(0);
          this.cargarCuentas();
          this.pageMovimientos.set(0);
          this.cargarMovimientos(cuenta.id);
        },
        error: (err: HttpErrorResponse) => {
          const msg = (err.error as ApiError | undefined)?.message ?? 'No se pudo registrar el deposito.';
          this.error.set(msg);
          this.toast.error(msg);
        }
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
    this.ahorroService
      .retirar(cuenta.id, Number(raw.monto))
      .pipe(finalize(() => this.guardando.set(false)))
      .subscribe({
        next: (res) => {
          this.toast.success(`Retiro registrado en ${cuenta.numeroCuenta}.`);
          this.retiroForm.patchValue({ monto: 0 });
          this.actualizarSaldoSeleccionada(Number(res.saldoResultante));
          this.pageCuentas.set(0);
          this.cargarCuentas();
          this.pageMovimientos.set(0);
          this.cargarMovimientos(cuenta.id);
        },
        error: (err: HttpErrorResponse) => {
          const msg = (err.error as ApiError | undefined)?.message ?? 'No se pudo registrar el retiro.';
          this.error.set(msg);
          this.toast.error(msg);
        }
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
    this.capitalizacion.set(null);
    this.ahorroService
      .capitalizar(Number(raw.anio), Number(raw.mes))
      .pipe(finalize(() => this.guardando.set(false)))
      .subscribe({
        next: (res) => {
          this.capitalizacion.set(res);
          this.toast.success(`Intereses capitalizados: ${res.cuentasCapitalizadas} cuentas, total ${res.totalInteres}.`);
        },
        error: (err: HttpErrorResponse) => {
          const msg = (err.error as ApiError | undefined)?.message ?? 'No se pudieron capitalizar los intereses.';
          this.error.set(msg);
          this.toast.error(msg);
        }
      });
  }
}

function computedPermiso(auth: AuthService, permiso: string): ReturnType<typeof computed> {
  return computed(() => auth.hasPermiso(permiso));
}
