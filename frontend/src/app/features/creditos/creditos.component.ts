import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { DecimalPipe, DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import { finalize, Observable } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { ApiError } from '../../core/models/auth.model';
import {
  Credito,
  CuotaCredito,
  MoraProcesada,
  PagoCuota,
  ProductoCredito,
  SimulacionCredito,
  SolicitudCredito
} from '../../core/models/credito.model';
import { CarteraItem, Morosidad } from '../../core/models/reporte.model';
import { Socio } from '../../core/models/socio.model';
import { CreditoService } from '../../core/services/credito.service';
import { ReporteService } from '../../core/services/reporte.service';
import { SocioService } from '../../core/services/socio.service';
import { ToastService } from '../../core/services/toast.service';
import { SortState } from '../../core/models/paginado.model';
import { AccionesMenuComponent } from '../../shared/components/acciones-menu/acciones-menu.component';
import { PaginadorComponent } from '../../shared/components/paginador/paginador.component';
import { SortableHeaderDirective } from '../../shared/components/sortable-header/sortable-header.directive';

@Component({
  selector: 'app-creditos',
  imports: [ReactiveFormsModule, DecimalPipe, DatePipe, RouterLink, AccionesMenuComponent, PaginadorComponent, SortableHeaderDirective],
  templateUrl: './creditos.html',
  styleUrl: './creditos.css'
})
export class CreditosComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly creditoService = inject(CreditoService);
  private readonly reporteService = inject(ReporteService);
  private readonly socioService = inject(SocioService);
  private readonly auth = inject(AuthService);
  private readonly toast = inject(ToastService);

  protected readonly tab = signal<'productos' | 'solicitudes' | 'creditos' | 'simulador' | 'cartera'>('productos');
  protected readonly productos = signal<ProductoCredito[]>([]);
  protected readonly socios = signal<Socio[]>([]);
  protected readonly solicitudes = signal<SolicitudCredito[]>([]);
  protected readonly creditos = signal<Credito[]>([]);
  protected readonly creditoSeleccionado = signal<Credito | null>(null);
  protected readonly cuotas = signal<CuotaCredito[]>([]);
  protected readonly pagos = signal<PagoCuota[]>([]);
  protected readonly simulacion = signal<SimulacionCredito | null>(null);
  protected readonly simPag = signal({ page: 0, size: 10 });

  protected readonly simCuotasPag = computed(() => {
    const s = this.simulacion();
    const p = this.simPag();
    if (!s) return [];
    const start = p.page * p.size;
    return s.cuotas.slice(start, start + p.size);
  });

  protected readonly simTotalPages = computed(() => {
    const s = this.simulacion();
    if (!s) return 0;
    return Math.ceil(s.cuotas.length / this.simPag().size);
  });

  protected readonly moraResultado = signal<MoraProcesada | null>(null);
  protected readonly cartera = signal<CarteraItem[]>([]);
  protected readonly morosidad = signal<Morosidad | null>(null);
  protected readonly carteraFiltro = signal<'TODOS' | 'PENDIENTE' | 'VENCIDA'>('TODOS');
  protected readonly carteraFiltros = ['TODOS', 'PENDIENTE', 'VENCIDA'] as const;
  protected readonly exportando = signal(false);
  protected readonly rechazoId = signal<number | null>(null);
  protected readonly error = signal('');
  protected readonly guardando = signal(false);

  protected readonly productosPag = signal({ page: 0, size: 10, totalElements: 0, totalPages: 0 });
  protected readonly productosSort = signal<SortState | null>(null);
  protected readonly solicitudesPag = signal({ page: 0, size: 10, totalElements: 0, totalPages: 0 });
  protected readonly solicitudesSort = signal<SortState | null>(null);
  protected readonly creditosPag = signal({ page: 0, size: 10, totalElements: 0, totalPages: 0 });
  protected readonly creditosSort = signal<SortState | null>(null);
  protected readonly cuotasPag = signal({ page: 0, size: 10, totalElements: 0, totalPages: 0 });
  protected readonly pagosPag = signal({ page: 0, size: 10, totalElements: 0, totalPages: 0 });

  protected readonly puedeCrear = computedPermiso(this.auth, 'CREDITOS:CREAR');
  protected readonly puedeEditar = computedPermiso(this.auth, 'CREDITOS:EDITAR');
  protected readonly puedeAprobar = computedPermiso(this.auth, 'CREDITOS:APROBAR');

  protected readonly rechazoForm = this.fb.nonNullable.group({
    motivoRechazo: ['', [Validators.required]]
  });

  protected readonly refinanciarForm = this.fb.nonNullable.group({
    plazoMeses: [24, [Validators.required, Validators.min(1)]],
    tasaInteres: [18, [Validators.required, Validators.min(0)]]
  });

  protected readonly simuladorForm = this.fb.nonNullable.group({
    monto: [1000, [Validators.required, Validators.min(0.01)]],
    plazoMeses: [12, [Validators.required, Validators.min(1)]],
    tasaInteres: [18, [Validators.required, Validators.min(0)]],
    sistemaAmortizacion: ['FRANCES', [Validators.required]]
  });

  ngOnInit(): void {
    this.cargarProductos();
    this.cargarSocios();
    this.cargarSolicitudes();
    this.cargarCreditos();
    this.cargarCartera();
    this.cargarMorosidad();
  }

  cargarProductos(): void {
    this.creditoService.productosPag({
      page: this.productosPag().page,
      size: this.productosPag().size,
      sort: this.productosSort() ? `${this.productosSort()!.key},${this.productosSort()!.dir}` : undefined
    }).subscribe({
      next: (p) => {
        this.productos.set(p.content);
        this.productosPag.set({ page: p.page, size: p.size, totalElements: p.totalElements, totalPages: p.totalPages });
      },
      error: () => this.productos.set([])
    });
  }

  cargarSocios(): void {
    this.socioService.listar({ estado: 'ACTIVO' }).subscribe({
      next: (s) => this.socios.set(s.content),
      error: () => this.socios.set([])
    });
  }

  cargarSolicitudes(): void {
    this.creditoService.solicitudes(undefined, {
      page: this.solicitudesPag().page,
      size: this.solicitudesPag().size,
      sort: this.solicitudesSort() ? `${this.solicitudesSort()!.key},${this.solicitudesSort()!.dir}` : undefined
    }).subscribe({
      next: (s) => {
        this.solicitudes.set(s.content);
        this.solicitudesPag.set({ page: s.page, size: s.size, totalElements: s.totalElements, totalPages: s.totalPages });
      },
      error: () => this.solicitudes.set([])
    });
  }

  cargarCreditos(): void {
    this.creditoService.creditos(undefined, {
      page: this.creditosPag().page,
      size: this.creditosPag().size,
      sort: this.creditosSort() ? `${this.creditosSort()!.key},${this.creditosSort()!.dir}` : undefined
    }).subscribe({
      next: (c) => {
        this.creditos.set(c.content);
        this.creditosPag.set({ page: c.page, size: c.size, totalElements: c.totalElements, totalPages: c.totalPages });
      },
      error: () => this.creditos.set([])
    });
  }

  evaluar(id: number): void {
    if (this.guardando()) {
      return;
    }
    this.guardando.set(true);
    this.error.set('');
    this.creditoService
      .evaluarSolicitud(id)
      .pipe(finalize(() => this.guardando.set(false)))
      .subscribe({
        next: () => {
          this.toast.success(`Solicitud ${id} enviada a evaluacion.`);
          this.cargarSolicitudes();
        },
        error: (err: HttpErrorResponse) => {
          const msg = (err.error as ApiError | undefined)?.message ?? 'No se pudo evaluar la solicitud.';
          this.error.set(msg);
          this.toast.error(msg);
        }
      });
  }

  aprobar(id: number): void {
    if (this.guardando()) {
      return;
    }
    this.guardando.set(true);
    this.error.set('');
    this.creditoService
      .aprobarSolicitud(id, { aprobar: true })
      .pipe(finalize(() => this.guardando.set(false)))
      .subscribe({
        next: () => {
          this.toast.success(`Solicitud ${id} aprobada.`);
          this.cargarSolicitudes();
          this.cargarCreditos();
        },
        error: (err: HttpErrorResponse) => {
          const msg = (err.error as ApiError | undefined)?.message ?? 'No se pudo aprobar la solicitud.';
          this.error.set(msg);
          this.toast.error(msg);
        }
      });
  }

  abrirRechazo(id: number): void {
    this.rechazoId.set(id);
    this.rechazoForm.reset({ motivoRechazo: '' });
  }

  cancelarRechazo(): void {
    this.rechazoId.set(null);
  }

  rechazar(id: number): void {
    if (this.rechazoForm.invalid || this.guardando()) {
      return;
    }
    const raw = this.rechazoForm.getRawValue();
    this.guardando.set(true);
    this.error.set('');
    this.creditoService
      .aprobarSolicitud(id, { aprobar: false, motivoRechazo: raw.motivoRechazo })
      .pipe(finalize(() => this.guardando.set(false)))
      .subscribe({
        next: () => {
          this.toast.success(`Solicitud ${id} rechazada.`);
          this.rechazoId.set(null);
          this.cargarSolicitudes();
        },
        error: (err: HttpErrorResponse) => {
          const msg = (err.error as ApiError | undefined)?.message ?? 'No se pudo rechazar la solicitud.';
          this.error.set(msg);
          this.toast.error(msg);
        }
      });
  }

  seleccionarCredito(credito: Credito): void {
    this.creditoSeleccionado.set(credito);
    this.refinanciarForm.patchValue({ plazoMeses: credito.plazoMeses, tasaInteres: Number(credito.tasaInteres) });
    this.cargarCuotas(credito.id);
    this.cargarPagos(credito.id);
  }

  cargarCuotas(creditoId: number): void {
    this.creditoService.cuotas(creditoId, {
      page: this.cuotasPag().page,
      size: this.cuotasPag().size
    }).subscribe({
      next: (c) => {
        this.cuotas.set(c.content);
        this.cuotasPag.set({ page: c.page, size: c.size, totalElements: c.totalElements, totalPages: c.totalPages });
      },
      error: () => this.cuotas.set([])
    });
  }

  cargarPagos(creditoId: number): void {
    this.creditoService.pagos(creditoId, {
      page: this.pagosPag().page,
      size: this.pagosPag().size
    }).subscribe({
      next: (p) => {
        this.pagos.set(p.content);
        this.pagosPag.set({ page: p.page, size: p.size, totalElements: p.totalElements, totalPages: p.totalPages });
      },
      error: () => this.pagos.set([])
    });
  }

  desembolsar(id: number): void {
    if (this.guardando()) {
      return;
    }
    this.guardando.set(true);
    this.error.set('');
    this.creditoService
      .desembolsar(id)
      .pipe(finalize(() => this.guardando.set(false)))
      .subscribe({
        next: (res) => {
          this.toast.success(`Credito ${id} desembolsado.`);
          this.cargarCreditos();
          if (this.creditoSeleccionado()?.id === id) {
            this.creditoSeleccionado.set(res);
            this.cargarCuotas(id);
          }
        },
        error: (err: HttpErrorResponse) => {
          const msg = (err.error as ApiError | undefined)?.message ?? 'No se pudo desembolsar el credito.';
          this.error.set(msg);
          this.toast.error(msg);
        }
      });
  }

  pagarCuota(credito: Credito, cuota: CuotaCredito): void {
    if (this.guardando()) {
      return;
    }
    this.guardando.set(true);
    this.error.set('');
    this.creditoService
      .pagarCuota(credito.id, { cuotaId: cuota.id })
      .pipe(finalize(() => this.guardando.set(false)))
      .subscribe({
        next: () => {
          this.toast.success(`Cuota ${cuota.numeroCuota} pagada.`);
          this.cargarCreditos();
          this.cargarCuotas(credito.id);
          this.cargarPagos(credito.id);
          if (this.creditoSeleccionado()?.id === credito.id) {
            this.creditoService.obtenerCredito(credito.id).subscribe({
              next: (c) => this.creditoSeleccionado.set(c)
            });
          }
        },
        error: (err: HttpErrorResponse) => {
          const msg = (err.error as ApiError | undefined)?.message ?? 'No se pudo registrar el pago.';
          this.error.set(msg);
          this.toast.error(msg);
        }
      });
  }

  refinanciar(): void {
    const credito = this.creditoSeleccionado();
    if (!credito || this.refinanciarForm.invalid || this.guardando()) {
      return;
    }
    const raw = this.refinanciarForm.getRawValue();
    this.guardando.set(true);
    this.error.set('');
    this.creditoService
      .refinanciar(credito.id, { plazoMeses: Number(raw.plazoMeses), tasaInteres: Number(raw.tasaInteres) })
      .pipe(finalize(() => this.guardando.set(false)))
      .subscribe({
        next: (res) => {
          this.toast.success(`Credito ${credito.id} refinanciado.`);
          this.creditoSeleccionado.set(res);
          this.cargarCreditos();
          this.cargarCuotas(credito.id);
        },
        error: (err: HttpErrorResponse) => {
          const msg = (err.error as ApiError | undefined)?.message ?? 'No se pudo refinanciar el credito.';
          this.error.set(msg);
          this.toast.error(msg);
        }
      });
  }

  procesarVencidas(): void {
    if (this.guardando()) {
      return;
    }
    this.guardando.set(true);
    this.error.set('');
    this.moraResultado.set(null);
    this.creditoService
      .procesarVencidas()
      .pipe(finalize(() => this.guardando.set(false)))
      .subscribe({
        next: (res) => {
          this.moraResultado.set(res);
          this.toast.success(`Mora procesada: ${res.cuotasMarcadas} cuotas, ${res.creditosEnMora} creditos.`);
          this.cargarCreditos();
          const credito = this.creditoSeleccionado();
          if (credito) {
            this.cargarCuotas(credito.id);
          }
        },
        error: (err: HttpErrorResponse) => {
          const msg = (err.error as ApiError | undefined)?.message ?? 'No se pudo procesar la mora.';
          this.error.set(msg);
          this.toast.error(msg);
        }
      });
  }

  cargarCartera(): void {
    const estado = this.carteraFiltro() === 'TODOS' ? undefined : this.carteraFiltro();
    this.reporteService.cartera(estado).subscribe({
      next: (c) => this.cartera.set(c),
      error: () => this.cartera.set([])
    });
  }

  cargarMorosidad(): void {
    this.reporteService.morosidad().subscribe({
      next: (m) => this.morosidad.set(m),
      error: () => this.morosidad.set(null)
    });
  }

  filtrarCartera(estado: 'TODOS' | 'PENDIENTE' | 'VENCIDA'): void {
    this.carteraFiltro.set(estado);
    this.cargarCartera();
  }

  exportarCartera(): void {
    this.descargarReporte(this.reporteService.exportarCartera(), 'cartera.csv');
  }

  exportarCarteraExcel(): void {
    this.descargarReporte(this.reporteService.exportarCarteraExcel(), 'cartera.xlsx');
  }

  exportarCarteraPdf(): void {
    this.descargarReporte(this.reporteService.exportarCarteraPdf(), 'cartera.pdf');
  }

  cambiarPaginaProductos(p: number): void {
    this.productosPag.update(s => ({ ...s, page: p }));
    this.cargarProductos();
  }

  cambiarTamanoProductos(t: number): void {
    this.productosPag.update(s => ({ ...s, size: t, page: 0 }));
    this.cargarProductos();
  }

  ordenarProductos(s: SortState): void {
    this.productosSort.set(s);
    this.productosPag.update(p => ({ ...p, page: 0 }));
    this.cargarProductos();
  }

  cambiarPaginaSolicitudes(p: number): void {
    this.solicitudesPag.update(s => ({ ...s, page: p }));
    this.cargarSolicitudes();
  }

  cambiarTamanoSolicitudes(t: number): void {
    this.solicitudesPag.update(s => ({ ...s, size: t, page: 0 }));
    this.cargarSolicitudes();
  }

  ordenarSolicitudes(s: SortState): void {
    this.solicitudesSort.set(s);
    this.solicitudesPag.update(p => ({ ...p, page: 0 }));
    this.cargarSolicitudes();
  }

  cambiarPaginaCreditos(p: number): void {
    this.creditosPag.update(s => ({ ...s, page: p }));
    this.cargarCreditos();
  }

  cambiarTamanoCreditos(t: number): void {
    this.creditosPag.update(s => ({ ...s, size: t, page: 0 }));
    this.cargarCreditos();
  }

  ordenarCreditos(s: SortState): void {
    this.creditosSort.set(s);
    this.creditosPag.update(p => ({ ...p, page: 0 }));
    this.cargarCreditos();
  }

  cambiarPaginaCuotas(p: number): void {
    this.cuotasPag.update(s => ({ ...s, page: p }));
    const credito = this.creditoSeleccionado();
    if (credito) {
      this.cargarCuotas(credito.id);
    }
  }

  cambiarTamanoCuotas(t: number): void {
    this.cuotasPag.update(s => ({ ...s, size: t, page: 0 }));
    const credito = this.creditoSeleccionado();
    if (credito) {
      this.cargarCuotas(credito.id);
    }
  }

  cambiarPaginaPagos(p: number): void {
    this.pagosPag.update(s => ({ ...s, page: p }));
    const credito = this.creditoSeleccionado();
    if (credito) {
      this.cargarPagos(credito.id);
    }
  }

  cambiarTamanoPagos(t: number): void {
    this.pagosPag.update(s => ({ ...s, size: t, page: 0 }));
    const credito = this.creditoSeleccionado();
    if (credito) {
      this.cargarPagos(credito.id);
    }
  }

  private descargarReporte(obs: Observable<Blob>, nombre: string): void {
    if (this.exportando()) {
      return;
    }
    this.exportando.set(true);
    this.error.set('');
    obs.subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = nombre;
        a.click();
        URL.revokeObjectURL(url);
        this.exportando.set(false);
      },
      error: () => {
        this.exportando.set(false);
        this.error.set('No se pudo exportar el reporte.');
        this.toast.error('No se pudo exportar el reporte.');
      }
    });
  }

  simularPagina(p: number): void {
    this.simPag.update(s => ({ ...s, page: p }));
  }

  simularTamano(t: number): void {
    this.simPag.update(s => ({ ...s, size: t, page: 0 }));
  }

  imprimirSimulacion(): void {
    const s = this.simulacion();
    if (!s) return;
    const form = this.simuladorForm.getRawValue();
    const rows = s.cuotas.map(c =>
      `<tr><td>#${c.numero}</td><td>${c.fechaVencimiento}</td><td>$${c.capital.toFixed(2)}</td><td>$${c.interes.toFixed(2)}</td><td><strong>$${c.cuota.toFixed(2)}</strong></td><td>$${c.saldo.toFixed(2)}</td></tr>`
    ).join('');
    const html = `<!DOCTYPE html><html><head><title>Simulación de Crédito</title>
<style>body{font-family:Arial,sans-serif;padding:20px}table{width:100%;border-collapse:collapse;margin-top:16px}th,td{border:1px solid #ddd;padding:6px 8px;text-align:right;font-size:12px}th{background:#1e293b;color:#fff;text-align:center}td:first-child,td:nth-child(2){text-align:center}.summary{background:#f0f9ff;padding:12px;border-radius:6px;margin-bottom:16px;font-size:14px}.summary strong{color:#1d4ed8}@media print{th{background:#1e293b!important;-webkit-print-color-adjust:exact;print-color-adjust:exact}}</style></head><body>
<h2>Simulación de Crédito — ${s.sistemaAmortizacion}</h2>
<div class="summary">Monto: <strong>$${form.monto}</strong> · Plazo: <strong>${form.plazoMeses} meses</strong> · Tasa: <strong>${form.tasaInteres}%</strong><br>
Cuota mensual: <strong>$${s.cuotaMensual.toFixed(2)}</strong> · Interés total: <strong>$${s.totalInteres.toFixed(2)}</strong> · Total a pagar: <strong>$${s.totalPagar.toFixed(2)}</strong></div>
<table><thead><tr><th>Cuota</th><th>Vencimiento</th><th>Capital</th><th>Interés</th><th>Cuota Total</th><th>Saldo Capital</th></tr></thead><tbody>${rows}</tbody></table>
</body></html>`;
    const w = window.open('', '_blank');
    if (w) { w.document.write(html); w.document.close(); w.print(); }
  }

  exportarSimulacionPdf(): void {
    const form = this.simuladorForm.getRawValue();
    this.descargarReporte(
      this.reporteService.simulacionReporte(Number(form.monto), Number(form.tasaInteres), Number(form.plazoMeses), form.sistemaAmortizacion, 'pdf'),
      'simulacion.pdf'
    );
  }

  exportarSimulacionExcel(): void {
    const form = this.simuladorForm.getRawValue();
    this.descargarReporte(
      this.reporteService.simulacionReporte(Number(form.monto), Number(form.tasaInteres), Number(form.plazoMeses), form.sistemaAmortizacion, 'xlsx'),
      'simulacion.xlsx'
    );
  }

  simular(): void {
    if (this.simuladorForm.invalid || this.guardando()) {
      return;
    }
    const raw = this.simuladorForm.getRawValue();
    this.guardando.set(true);
    this.error.set('');
    this.simulacion.set(null);
    this.creditoService
      .simular({
        monto: Number(raw.monto),
        plazoMeses: Number(raw.plazoMeses),
        tasaInteres: Number(raw.tasaInteres),
        sistemaAmortizacion: raw.sistemaAmortizacion
      })
      .pipe(finalize(() => this.guardando.set(false)))
      .subscribe({
        next: (res) => this.simulacion.set(res),
        error: (err: HttpErrorResponse) => {
          const msg = (err.error as ApiError | undefined)?.message ?? 'No se pudo simular el credito.';
          this.error.set(msg);
          this.toast.error(msg);
        }
      });
  }
}

function computedPermiso(auth: AuthService, permiso: string): ReturnType<typeof computed> {
  return computed(() => auth.hasPermiso(permiso));
}
