import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { DecimalPipe, DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { finalize } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { ApiError } from '../../core/models/auth.model';
import { Aportacion, AportacionConfig, AportacionPago } from '../../core/models/aportacion.model';
import { Socio } from '../../core/models/socio.model';
import { AportacionService } from '../../core/services/aportacion.service';
import { SocioService } from '../../core/services/socio.service';
import { ToastService } from '../../core/services/toast.service';
import { AccionesMenuComponent } from '../../shared/components/acciones-menu/acciones-menu.component';
import { SortState } from '../../core/models/paginado.model';
import { PaginadorComponent } from '../../shared/components/paginador/paginador.component';
import { SortableHeaderDirective } from '../../shared/components/sortable-header/sortable-header.directive';

@Component({
  selector: 'app-aportaciones',
  imports: [ReactiveFormsModule, DecimalPipe, DatePipe, RouterLink, AccionesMenuComponent, PaginadorComponent, SortableHeaderDirective],
  templateUrl: './aportaciones.html',
  styleUrl: './aportaciones.css'
})
export class AportacionesComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly aportacionService = inject(AportacionService);
  private readonly socioService = inject(SocioService);
  private readonly auth = inject(AuthService);
  private readonly toast = inject(ToastService);

  protected readonly tab = signal<'config' | 'aportaciones'>('config');
  protected readonly configs = signal<AportacionConfig[]>([]);
  protected readonly aportaciones = signal<Aportacion[]>([]);
  protected readonly pagos = signal<AportacionPago[]>([]);
  protected readonly socios = signal<Socio[]>([]);
  protected readonly pagoTarget = signal<Aportacion | null>(null);
  protected readonly filtroPeriodo = signal('');
  protected readonly error = signal('');
  protected readonly cargando = signal(false);
  protected readonly guardando = signal(false);

  protected readonly page = signal(0);
  protected readonly size = signal(10);
  protected readonly sort = signal<SortState | null>(null);
  protected readonly totalElements = signal(0);
  protected readonly totalPages = signal(0);

  protected readonly pageConfigs = signal(0);
  protected readonly sizeConfigs = signal(10);
  protected readonly totalElementsConfigs = signal(0);
  protected readonly totalPagesConfigs = signal(0);

  protected readonly pagePagos = signal(0);
  protected readonly sizePagos = signal(10);
  protected readonly totalElementsPagos = signal(0);
  protected readonly totalPagesPagos = signal(0);

  protected readonly puedeVer = computed(() => this.auth.hasPermiso('APORTACIONES:VER'));
  protected readonly puedeCrear = computed(() => this.auth.hasPermiso('APORTACIONES:CREAR'));

  protected readonly generarForm = this.fb.nonNullable.group({
    periodo: [this.periodoActual(), [Validators.required, Validators.pattern(/^\d{4}-\d{2}$/)]]
  });

  protected readonly pagoForm = this.fb.nonNullable.group({
    monto: [0, [Validators.required, Validators.min(0.01)]]
  });

  ngOnInit(): void {
    this.cargarConfigs();
    this.cargarSocios();
    this.cargarAportaciones();
  }

  private periodoActual(): string {
    const ahora = new Date();
    return `${ahora.getFullYear()}-${String(ahora.getMonth() + 1).padStart(2, '0')}`;
  }

  cargarConfigs(): void {
    this.aportacionService.configs({
      page: this.pageConfigs(),
      size: this.sizeConfigs()
    }).subscribe({
      next: (paginated) => {
        this.configs.set(paginated.content);
        this.totalElementsConfigs.set(paginated.totalElements);
        this.totalPagesConfigs.set(paginated.totalPages);
      },
      error: () => this.configs.set([])
    });
  }

  cargarSocios(): void {
    this.socioService.listar({ estado: 'ACTIVO' }).subscribe({
      next: (s) => this.socios.set(s.content),
      error: () => this.socios.set([])
    });
  }

  cargarAportaciones(): void {
    this.cargando.set(true);
    this.aportacionService.aportaciones({
      page: this.page(),
      size: this.size(),
      sort: this.sort() ? `${this.sort()!.key},${this.sort()!.dir}` : undefined,
      periodo: this.filtroPeriodo() || undefined
    }).subscribe({
      next: (paginated) => {
        this.aportaciones.set(paginated.content);
        this.totalElements.set(paginated.totalElements);
        this.totalPages.set(paginated.totalPages);
        this.cargando.set(false);
      },
      error: () => {
        this.aportaciones.set([]);
        this.cargando.set(false);
      }
    });
  }

  aplicarFiltro(event: Event): void {
    this.filtroPeriodo.set((event.target as HTMLInputElement).value);
    this.page.set(0);
    this.cargarAportaciones();
  }

  cambiarPagina(p: number): void {
    this.page.set(p);
    this.cargarAportaciones();
  }

  cambiarTamano(t: number): void {
    this.size.set(t);
    this.page.set(0);
    this.cargarAportaciones();
  }

  ordenar(s: SortState): void {
    this.sort.set(s);
    this.page.set(0);
    this.cargarAportaciones();
  }

  cambiarPaginaConfigs(p: number): void {
    this.pageConfigs.set(p);
    this.cargarConfigs();
  }

  cambiarTamanoConfigs(t: number): void {
    this.sizeConfigs.set(t);
    this.pageConfigs.set(0);
    this.cargarConfigs();
  }

  cambiarPaginaPagos(p: number): void {
    this.pagePagos.set(p);
    const target = this.pagoTarget();
    if (target) {
      this.cargarPagos(target.id);
    }
  }

  cambiarTamanoPagos(t: number): void {
    this.sizePagos.set(t);
    this.pagePagos.set(0);
    const target = this.pagoTarget();
    if (target) {
      this.cargarPagos(target.id);
    }
  }

  generarPeriodo(): void {
    if (this.generarForm.invalid || this.guardando()) {
      return;
    }
    const raw = this.generarForm.getRawValue();
    this.guardando.set(true);
    this.error.set('');
    this.aportacionService
      .generarPeriodo(raw.periodo)
      .pipe(finalize(() => this.guardando.set(false)))
      .subscribe({
        next: (res) => {
          this.toast.success(`Aportaciones generadas para ${res.periodo}: ${res.generadas}.`);
          this.cargarAportaciones();
        },
        error: (err: HttpErrorResponse) => {
          const msg = (err.error as ApiError | undefined)?.message ?? 'No se pudieron generar las aportaciones.';
          this.error.set(msg);
          this.toast.error(msg);
        }
      });
  }

  seleccionar(a: Aportacion): void {
    this.pagoTarget.set(a);
    this.pagoForm.patchValue({ monto: Number(a.montoEsperado) - Number(a.montoPagado) });
    this.pagePagos.set(0);
    this.cargarPagos(a.id);
  }

  cargarPagos(aportacionId: number): void {
    this.aportacionService.pagos(aportacionId, {
      page: this.pagePagos(),
      size: this.sizePagos()
    }).subscribe({
      next: (paginated) => {
        this.pagos.set(paginated.content);
        this.totalElementsPagos.set(paginated.totalElements);
        this.totalPagesPagos.set(paginated.totalPages);
      },
      error: () => this.pagos.set([])
    });
  }

  pagar(): void {
    const target = this.pagoTarget();
    if (!target || this.pagoForm.invalid || this.guardando()) {
      return;
    }
    const raw = this.pagoForm.getRawValue();
    this.guardando.set(true);
    this.error.set('');
    this.aportacionService
      .pagar(target.id, Number(raw.monto))
      .pipe(finalize(() => this.guardando.set(false)))
      .subscribe({
        next: () => {
          this.toast.success(`Pago registrado para ${target.socioNombre}.`);
          this.pagoTarget.set(null);
          this.cargarAportaciones();
        },
        error: (err: HttpErrorResponse) => {
          const msg = (err.error as ApiError | undefined)?.message ?? 'No se pudo registrar el pago.';
          this.error.set(msg);
          this.toast.error(msg);
        }
      });
  }
}
