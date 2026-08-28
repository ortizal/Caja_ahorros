import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { DecimalPipe, DatePipe } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { finalize, Observable } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { ApiError } from '../../core/models/auth.model';
import { Credito, CreditoDetalle, CuotaCredito, PagoCuota } from '../../core/models/credito.model';
import { CreditoService } from '../../core/services/credito.service';
import { ToastService } from '../../core/services/toast.service';
import { PaginadorComponent } from '../../shared/components/paginador/paginador.component';

@Component({
  selector: 'app-credito-detalle',
  imports: [ReactiveFormsModule, DecimalPipe, DatePipe, PaginadorComponent],
  templateUrl: './credito-detalle.html',
  styleUrl: './credito-detalle.css'
})
export class CreditoDetalleComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly creditoService = inject(CreditoService);
  private readonly auth = inject(AuthService);
  private readonly toast = inject(ToastService);

  protected readonly creditoDetalle = signal<CreditoDetalle | null>(null);
  protected readonly cuotas = signal<CuotaCredito[]>([]);
  protected readonly pagos = signal<PagoCuota[]>([]);
  protected readonly detalleTab = signal<'resumen' | 'amortizacion' | 'pagos' | 'historial'>('resumen');
  protected readonly loading = signal(true);
  protected readonly error = signal('');
  protected readonly guardando = signal(false);
  protected readonly exportando = signal(false);

  protected readonly cuotasPag = signal({ page: 0, size: 10, totalElements: 0, totalPages: 0 });
  protected readonly pagosPag = signal({ page: 0, size: 10, totalElements: 0, totalPages: 0 });

  protected readonly puedeCrear = computed(() => this.auth.hasPermiso('CREDITOS:CREAR'));
  protected readonly puedeAprobar = computed(() => this.auth.hasPermiso('CREDITOS:APROBAR'));

  protected readonly refinanciarForm = this.fb.nonNullable.group({
    plazoMeses: [24, [Validators.required, Validators.min(1)]],
    tasaInteres: [18, [Validators.required, Validators.min(0)]]
  });

  protected readonly tipoPago = signal<'normal' | 'adelantado' | 'abono'>('normal');
  protected readonly pagoSeleccionado = signal<CuotaCredito | null>(null);
  protected readonly abonoMonto = signal(0);
  protected readonly abonoDescripcion = signal('');

  private creditoId = 0;

  ngOnInit(): void {
    this.creditoId = Number(this.route.snapshot.paramMap.get('id'));
    if (!this.creditoId) {
      this.router.navigate(['/creditos']);
      return;
    }
    this.cargarDetalle();
    this.cargarCuotas();
    this.cargarPagos();
  }

  cargarDetalle(): void {
    this.loading.set(true);
    this.creditoService.detalleCredito(this.creditoId).subscribe({
      next: (d) => {
        this.creditoDetalle.set(d);
        this.refinanciarForm.patchValue({
          plazoMeses: d.credito.plazoMeses,
          tasaInteres: Number(d.credito.tasaInteres)
        });
        this.loading.set(false);
      },
      error: () => {
        this.error.set('No se pudo cargar el detalle del credito.');
        this.loading.set(false);
      }
    });
  }

  cambiarDetalleTab(tab: 'resumen' | 'amortizacion' | 'pagos' | 'historial'): void {
    this.detalleTab.set(tab);
  }

  cargarCuotas(): void {
    this.creditoService.cuotas(this.creditoId, {
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

  cargarPagos(): void {
    this.creditoService.pagos(this.creditoId, {
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

  pagarCuota(cuota: CuotaCredito, tipo: 'normal' | 'adelantado' | 'abono'): void {
    this.pagoSeleccionado.set(cuota);
    this.tipoPago.set(tipo);
    if (tipo === 'abono') {
      this.abonoMonto.set(cuota.capital);
      this.abonoDescripcion.set('');
    }
  }

  registrarPago(): void {
    if (this.guardando()) return;
    const cuota = this.pagoSeleccionado();
    if (!cuota) return;
    const tipo = this.tipoPago();
    this.guardando.set(true);
    this.error.set('');
    let request: Record<string, unknown>;
    if (tipo === 'abono') {
      const monto = Number(this.abonoMonto());
      if (!monto || monto <= 0) {
        this.error.set('Debe indicar un monto de abono a capital valido.');
        this.toast.error(this.error());
        this.guardando.set(false);
        return;
      }
      request = { cuotaId: cuota.id, montoAbonoCapital: monto, tipo: 'ABONO', descripcion: this.abonoDescripcion() || undefined };
    } else if (tipo === 'adelantado') {
      request = { cuotaId: cuota.id, tipo: 'ADELANTADO' };
    } else {
      request = { cuotaId: cuota.id, tipo: 'CUOTA' };
    }
    this.creditoService
      .pagarCuota(this.creditoId, request as any)
      .pipe(finalize(() => this.guardando.set(false)))
      .subscribe({
        next: () => {
          const label = tipo === 'abono' ? 'Abono a capital registrado' : (tipo === 'adelantado' ? `Cuota ${cuota.numeroCuota} pagada por adelantado` : `Cuota ${cuota.numeroCuota} pagada`);
          this.toast.success(label + '.');
          this.pagoSeleccionado.set(null);
          this.cargarDetalle();
          this.cargarCuotas();
          this.cargarPagos();
        },
        error: (err: HttpErrorResponse) => {
          const msg = (err.error as ApiError | undefined)?.message ?? 'No se pudo registrar el pago.';
          this.error.set(msg);
          this.toast.error(msg);
        }
      });
  }

  cerrarPago(): void {
    this.pagoSeleccionado.set(null);
  }

  private nombreCliente(): string {
    const det = this.creditoDetalle();
    if (!det) return '';
    if (det.credito.socioNombre) return det.credito.socioNombre;
    return det.credito.clienteNoSocioNombre ?? '';
  }

  refinanciar(): void {
    if (this.refinanciarForm.invalid || this.guardando()) return;
    const raw = this.refinanciarForm.getRawValue();
    this.guardando.set(true);
    this.error.set('');
    this.creditoService
      .refinanciar(this.creditoId, { plazoMeses: Number(raw.plazoMeses), tasaInteres: Number(raw.tasaInteres) })
      .pipe(finalize(() => this.guardando.set(false)))
      .subscribe({
        next: () => {
          this.toast.success(`Credito refinanciado.`);
          this.cargarDetalle();
          this.cargarCuotas();
        },
        error: (err: HttpErrorResponse) => {
          const msg = (err.error as ApiError | undefined)?.message ?? 'No se pudo refinanciar el credito.';
          this.error.set(msg);
          this.toast.error(msg);
        }
      });
  }

  descargarContratoPdf(): void {
    this.descargarReporte(this.creditoService.contratoCredito(this.creditoId, 'pdf'), `contrato-credito-${this.creditoId}.pdf`);
  }

  descargarContratoExcel(): void {
    this.descargarReporte(this.creditoService.contratoCredito(this.creditoId, 'xlsx'), `contrato-credito-${this.creditoId}.xlsx`);
  }

  cambiarPaginaCuotas(p: number): void {
    this.cuotasPag.update(s => ({ ...s, page: p }));
    this.cargarCuotas();
  }

  cambiarTamanoCuotas(t: number): void {
    this.cuotasPag.update(s => ({ ...s, size: t, page: 0 }));
    this.cargarCuotas();
  }

  cambiarPaginaPagos(p: number): void {
    this.pagosPag.update(s => ({ ...s, page: p }));
    this.cargarPagos();
  }

  cambiarTamanoPagos(t: number): void {
    this.pagosPag.update(s => ({ ...s, size: t, page: 0 }));
    this.cargarPagos();
  }

  volver(): void {
    this.router.navigate(['/creditos']);
  }

  private descargarReporte(obs: Observable<Blob>, nombre: string): void {
    if (this.exportando()) return;
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
}
