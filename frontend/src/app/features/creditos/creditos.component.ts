import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { DecimalPipe, DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { finalize } from 'rxjs';
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

@Component({
  selector: 'app-creditos',
  imports: [ReactiveFormsModule, DecimalPipe, DatePipe],
  templateUrl: './creditos.html',
  styleUrl: './creditos.css'
})
export class CreditosComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly creditoService = inject(CreditoService);
  private readonly reporteService = inject(ReporteService);
  private readonly socioService = inject(SocioService);
  private readonly auth = inject(AuthService);

  protected readonly tab = signal<'productos' | 'solicitudes' | 'creditos' | 'simulador' | 'cartera'>('productos');
  protected readonly productos = signal<ProductoCredito[]>([]);
  protected readonly socios = signal<Socio[]>([]);
  protected readonly solicitudes = signal<SolicitudCredito[]>([]);
  protected readonly creditos = signal<Credito[]>([]);
  protected readonly creditoSeleccionado = signal<Credito | null>(null);
  protected readonly cuotas = signal<CuotaCredito[]>([]);
  protected readonly pagos = signal<PagoCuota[]>([]);
  protected readonly simulacion = signal<SimulacionCredito | null>(null);
  protected readonly moraResultado = signal<MoraProcesada | null>(null);
  protected readonly cartera = signal<CarteraItem[]>([]);
  protected readonly morosidad = signal<Morosidad | null>(null);
  protected readonly carteraFiltro = signal<'TODOS' | 'PENDIENTE' | 'VENCIDA'>('TODOS');
  protected readonly carteraFiltros = ['TODOS', 'PENDIENTE', 'VENCIDA'] as const;
  protected readonly exportando = signal(false);
  protected readonly rechazoId = signal<number | null>(null);
  protected readonly error = signal('');
  protected readonly ok = signal('');
  protected readonly guardando = signal(false);

  protected readonly puedeCrear = computedPermiso(this.auth, 'CREDITOS:CREAR');
  protected readonly puedeEditar = computedPermiso(this.auth, 'CREDITOS:EDITAR');
  protected readonly puedeAprobar = computedPermiso(this.auth, 'CREDITOS:APROBAR');

  protected readonly productoForm = this.fb.nonNullable.group({
    nombre: ['', [Validators.required]],
    tasaInteres: [18, [Validators.required, Validators.min(0)]],
    tasaMora: [1, [Validators.min(0)]],
    sistemaAmortizacion: ['FRANCES', [Validators.required]],
    plazoMaxMeses: [36, [Validators.required, Validators.min(1)]],
    montoMin: [0, [Validators.min(0)]],
    montoMax: [5000, [Validators.min(0)]],
    requiereGarante: [false],
    vigenteDesde: [this.hoy(), [Validators.required]]
  });

  protected readonly solicitudForm = this.fb.nonNullable.group({
    socioId: [0, [Validators.required, Validators.min(1)]],
    productoId: [0, [Validators.required, Validators.min(1)]],
    montoSolicitado: [500, [Validators.required, Validators.min(0.01)]],
    plazoMeses: [12, [Validators.required, Validators.min(1)]],
    destino: ['']
  });

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

  private hoy(): string {
    return new Date().toISOString().slice(0, 10);
  }

  cargarProductos(): void {
    this.creditoService.productos().subscribe({
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

  cargarSolicitudes(): void {
    this.creditoService.solicitudes().subscribe({
      next: (s) => this.solicitudes.set(s),
      error: () => this.solicitudes.set([])
    });
  }

  cargarCreditos(): void {
    this.creditoService.creditos().subscribe({
      next: (c) => this.creditos.set(c),
      error: () => this.creditos.set([])
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
    this.creditoService
      .crearProducto({
        nombre: raw.nombre,
        tasaInteres: Number(raw.tasaInteres),
        tasaMora: Number(raw.tasaMora),
        sistemaAmortizacion: raw.sistemaAmortizacion,
        plazoMaxMeses: Number(raw.plazoMaxMeses),
        montoMin: Number(raw.montoMin),
        montoMax: Number(raw.montoMax),
        requiereGarante: Boolean(raw.requiereGarante),
        vigenteDesde: raw.vigenteDesde
      })
      .pipe(finalize(() => this.guardando.set(false)))
      .subscribe({
        next: () => {
          this.ok.set('Producto de credito creado.');
          this.productoForm.reset({
            nombre: '',
            tasaInteres: 18,
            tasaMora: 1,
            sistemaAmortizacion: 'FRANCES',
            plazoMaxMeses: 36,
            montoMin: 0,
            montoMax: 5000,
            requiereGarante: false,
            vigenteDesde: this.hoy()
          });
          this.cargarProductos();
        },
        error: (err: HttpErrorResponse) =>
          this.error.set((err.error as ApiError | undefined)?.message ?? 'No se pudo crear el producto.')
      });
  }

  crearSolicitud(): void {
    if (this.solicitudForm.invalid || this.guardando()) {
      return;
    }
    const raw = this.solicitudForm.getRawValue();
    this.guardando.set(true);
    this.error.set('');
    this.ok.set('');
    this.creditoService
      .crearSolicitud({
        socioId: Number(raw.socioId),
        productoId: Number(raw.productoId),
        montoSolicitado: Number(raw.montoSolicitado),
        plazoMeses: Number(raw.plazoMeses),
        destino: raw.destino || undefined
      })
      .pipe(finalize(() => this.guardando.set(false)))
      .subscribe({
        next: () => {
          this.ok.set('Solicitud de credito registrada.');
          this.solicitudForm.reset({ socioId: 0, productoId: 0, montoSolicitado: 500, plazoMeses: 12, destino: '' });
          this.cargarSolicitudes();
        },
        error: (err: HttpErrorResponse) =>
          this.error.set((err.error as ApiError | undefined)?.message ?? 'No se pudo registrar la solicitud.')
      });
  }

  evaluar(id: number): void {
    if (this.guardando()) {
      return;
    }
    this.guardando.set(true);
    this.error.set('');
    this.ok.set('');
    this.creditoService
      .evaluarSolicitud(id)
      .pipe(finalize(() => this.guardando.set(false)))
      .subscribe({
        next: () => {
          this.ok.set(`Solicitud ${id} enviada a evaluacion.`);
          this.cargarSolicitudes();
        },
        error: (err: HttpErrorResponse) =>
          this.error.set((err.error as ApiError | undefined)?.message ?? 'No se pudo evaluar la solicitud.')
      });
  }

  aprobar(id: number): void {
    if (this.guardando()) {
      return;
    }
    this.guardando.set(true);
    this.error.set('');
    this.ok.set('');
    this.creditoService
      .aprobarSolicitud(id, { aprobar: true })
      .pipe(finalize(() => this.guardando.set(false)))
      .subscribe({
        next: () => {
          this.ok.set(`Solicitud ${id} aprobada.`);
          this.cargarSolicitudes();
          this.cargarCreditos();
        },
        error: (err: HttpErrorResponse) =>
          this.error.set((err.error as ApiError | undefined)?.message ?? 'No se pudo aprobar la solicitud.')
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
    this.ok.set('');
    this.creditoService
      .aprobarSolicitud(id, { aprobar: false, motivoRechazo: raw.motivoRechazo })
      .pipe(finalize(() => this.guardando.set(false)))
      .subscribe({
        next: () => {
          this.ok.set(`Solicitud ${id} rechazada.`);
          this.rechazoId.set(null);
          this.cargarSolicitudes();
        },
        error: (err: HttpErrorResponse) =>
          this.error.set((err.error as ApiError | undefined)?.message ?? 'No se pudo rechazar la solicitud.')
      });
  }

  seleccionarCredito(credito: Credito): void {
    this.creditoSeleccionado.set(credito);
    this.refinanciarForm.patchValue({ plazoMeses: credito.plazoMeses, tasaInteres: Number(credito.tasaInteres) });
    this.cargarCuotas(credito.id);
    this.cargarPagos(credito.id);
  }

  cargarCuotas(creditoId: number): void {
    this.creditoService.cuotas(creditoId).subscribe({
      next: (c) => this.cuotas.set(c),
      error: () => this.cuotas.set([])
    });
  }

  cargarPagos(creditoId: number): void {
    this.creditoService.pagos(creditoId).subscribe({
      next: (p) => this.pagos.set(p),
      error: () => this.pagos.set([])
    });
  }

  desembolsar(id: number): void {
    if (this.guardando()) {
      return;
    }
    this.guardando.set(true);
    this.error.set('');
    this.ok.set('');
    this.creditoService
      .desembolsar(id)
      .pipe(finalize(() => this.guardando.set(false)))
      .subscribe({
        next: (res) => {
          this.ok.set(`Credito ${id} desembolsado.`);
          this.cargarCreditos();
          if (this.creditoSeleccionado()?.id === id) {
            this.creditoSeleccionado.set(res);
            this.cargarCuotas(id);
          }
        },
        error: (err: HttpErrorResponse) =>
          this.error.set((err.error as ApiError | undefined)?.message ?? 'No se pudo desembolsar el credito.')
      });
  }

  pagarCuota(credito: Credito, cuota: CuotaCredito): void {
    if (this.guardando()) {
      return;
    }
    this.guardando.set(true);
    this.error.set('');
    this.ok.set('');
    this.creditoService
      .pagarCuota(credito.id, { cuotaId: cuota.id })
      .pipe(finalize(() => this.guardando.set(false)))
      .subscribe({
        next: () => {
          this.ok.set(`Cuota ${cuota.numeroCuota} pagada.`);
          this.cargarCreditos();
          this.cargarCuotas(credito.id);
          this.cargarPagos(credito.id);
          if (this.creditoSeleccionado()?.id === credito.id) {
            this.creditoService.obtenerCredito(credito.id).subscribe({
              next: (c) => this.creditoSeleccionado.set(c)
            });
          }
        },
        error: (err: HttpErrorResponse) =>
          this.error.set((err.error as ApiError | undefined)?.message ?? 'No se pudo registrar el pago.')
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
    this.ok.set('');
    this.creditoService
      .refinanciar(credito.id, { plazoMeses: Number(raw.plazoMeses), tasaInteres: Number(raw.tasaInteres) })
      .pipe(finalize(() => this.guardando.set(false)))
      .subscribe({
        next: (res) => {
          this.ok.set(`Credito ${credito.id} refinanciado.`);
          this.creditoSeleccionado.set(res);
          this.cargarCreditos();
          this.cargarCuotas(credito.id);
        },
        error: (err: HttpErrorResponse) =>
          this.error.set((err.error as ApiError | undefined)?.message ?? 'No se pudo refinanciar el credito.')
      });
  }

  procesarVencidas(): void {
    if (this.guardando()) {
      return;
    }
    this.guardando.set(true);
    this.error.set('');
    this.ok.set('');
    this.moraResultado.set(null);
    this.creditoService
      .procesarVencidas()
      .pipe(finalize(() => this.guardando.set(false)))
      .subscribe({
        next: (res) => {
          this.moraResultado.set(res);
          this.ok.set(`Mora procesada: ${res.cuotasMarcadas} cuotas, ${res.creditosEnMora} creditos.`);
          this.cargarCreditos();
          const credito = this.creditoSeleccionado();
          if (credito) {
            this.cargarCuotas(credito.id);
          }
        },
        error: (err: HttpErrorResponse) =>
          this.error.set((err.error as ApiError | undefined)?.message ?? 'No se pudo procesar la mora.')
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
    if (this.exportando()) {
      return;
    }
    this.exportando.set(true);
    this.error.set('');
    this.reporteService.exportarCartera().subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = 'cartera.csv';
        a.click();
        URL.revokeObjectURL(url);
        this.exportando.set(false);
      },
      error: () => {
        this.exportando.set(false);
        this.error.set('No se pudo exportar la cartera.');
      }
    });
  }

  simular(): void {
    if (this.simuladorForm.invalid || this.guardando()) {
      return;
    }
    const raw = this.simuladorForm.getRawValue();
    this.guardando.set(true);
    this.error.set('');
    this.ok.set('');
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
        error: (err: HttpErrorResponse) =>
          this.error.set((err.error as ApiError | undefined)?.message ?? 'No se pudo simular el credito.')
      });
  }
}

function computedPermiso(auth: AuthService, permiso: string): ReturnType<typeof computed> {
  return computed(() => auth.hasPermiso(permiso));
}
