import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { DatePipe, DecimalPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Router } from '@angular/router';
import { finalize, Observable } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { ApiError } from '../../core/models/auth.model';
import {
  CajaApertura,
  CajaArqueo,
  CajaMovimiento,
  SaldoCaja,
  TIPOS_MOVIMIENTO_CAJA,
  referenciaParaTipo
} from '../../core/models/caja.model';
import { CajaService } from '../../core/services/caja.service';
import { ReporteService } from '../../core/services/reporte.service';
import { ToastService } from '../../core/services/toast.service';
import { SocioService } from '../../core/services/socio.service';
import { AhorroService } from '../../core/services/ahorro.service';
import { CreditoService } from '../../core/services/credito.service';
import { Socio } from '../../core/models/socio.model';
import { CuentaAhorro } from '../../core/models/ahorro.model';
import { Credito } from '../../core/models/credito.model';
import { AccionesMenuComponent } from '../../shared/components/acciones-menu/acciones-menu.component';
import { SortState } from '../../core/models/paginado.model';
import { PaginadorComponent } from '../../shared/components/paginador/paginador.component';
import { SortableHeaderDirective } from '../../shared/components/sortable-header/sortable-header.directive';

@Component({
  selector: 'app-caja',
  imports: [ReactiveFormsModule, DecimalPipe, DatePipe, AccionesMenuComponent, PaginadorComponent, SortableHeaderDirective],
  templateUrl: './caja.html',
  styleUrl: './caja.css'
})
export class CajaComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly cajaService = inject(CajaService);
  private readonly auth = inject(AuthService);
  private readonly reporteService = inject(ReporteService);
  private readonly toast = inject(ToastService);
  private readonly socioService = inject(SocioService);
  private readonly ahorroService = inject(AhorroService);
  private readonly creditoService = inject(CreditoService);

  protected readonly cajas = signal<CajaApertura[]>([]);
  protected readonly seleccionada = signal<CajaApertura | null>(null);
  protected readonly saldo = signal<SaldoCaja | null>(null);
  protected readonly movimientos = signal<CajaMovimiento[]>([]);
  protected readonly arqueoResultado = signal<CajaArqueo | null>(null);
  protected readonly error = signal('');
  protected readonly cargando = signal(false);
  protected readonly guardando = signal(false);
  protected readonly exportando = signal(false);
  protected readonly tipos = TIPOS_MOVIMIENTO_CAJA;
  protected readonly tabActiva = signal<'arqueo' | 'movimientos'>('arqueo');

  protected readonly page = signal(0);
  protected readonly size = signal(10);
  protected readonly sort = signal<SortState | null>(null);
  protected readonly totalElements = signal(0);
  protected readonly totalPages = signal(0);

  protected readonly movPage = signal(0);
  protected readonly movSize = signal(10);
  protected readonly movSort = signal<SortState | null>(null);
  protected readonly movTotalElements = signal(0);
  protected readonly movTotalPages = signal(0);

  protected readonly socios = signal<Socio[]>([]);
  protected readonly cuentas = signal<CuentaAhorro[]>([]);
  protected readonly creditos = signal<Credito[]>([]);
  protected readonly cargandoOpciones = signal(false);

  protected readonly movimientoForm = this.fb.nonNullable.group({
    tipo: ['APORTACION', [Validators.required]],
    referenciaId: [null as number | null, [Validators.required]],
    monto: [0, [Validators.required, Validators.min(0.01)]],
    descripcion: [''],
    montoCapital: [null as number | null],
    montoInteres: [null as number | null],
    montoMora: [null as number | null]
  });

  protected readonly arqueoForm = this.fb.nonNullable.group({
    saldoFisico: [0, [Validators.required, Validators.min(0)]],
    observacion: ['']
  });

  protected readonly cobroActivo = signal(false);
  protected readonly puedeCrear = computed(() => this.auth.hasPermiso('CAJA:CREAR'));
  protected readonly tieneCajaAbierta = computed(() => this.cajas().some((c) => c.estado === 'ABIERTA'));
  protected readonly cajaAbierta = computed(
    () => this.seleccionada()?.estado === 'ABIERTA' && this.cajas().some((c) => c.id === this.seleccionada()?.id)
  );

  irAbrirCaja(): void {
    if (this.tieneCajaAbierta()) {
      this.toast.warning('Ya existe una caja ABIERTA; cierre la caja actual antes de abrir otra.');
      return;
    }
    this.router.navigate(['/caja/nuevo']);
  }

  protected readonly tipoReferencia = computed(() => referenciaParaTipo(this.movimientoForm.controls.tipo.value));

  protected readonly opcionesPersona = computed(() => {
    const tipo = this.tipoReferencia();
    if (tipo === 'socio') {
      return this.socios().map((s) => ({ id: s.id, label: `${s.codigo} — ${s.nombres} ${s.apellidos}`, nombre: `${s.nombres} ${s.apellidos}` }));
    }
    if (tipo === 'cuenta_ahorro') {
      return this.cuentas().map((c) => ({
        id: c.id,
        label: `${c.numeroCuenta} — ${c.socioNombre ?? 'Socio'}`,
        nombre: c.socioNombre ?? null
      }));
    }
    if (tipo === 'credito') {
      return this.creditos().map((c) => ({
        id: c.id,
        label: `#${c.id} — ${c.socioNombre ?? c.clienteNoSocioNombre ?? 'Cliente'}`,
        nombre: c.socioNombre ?? c.clienteNoSocioNombre ?? null
      }));
    }
    return [] as { id: number; label: string; nombre: string | null }[];
  });

  protected readonly etiquetaPersona = computed(() => {
    switch (this.tipoReferencia()) {
      case 'socio':
        return 'Socio afectado';
      case 'cuenta_ahorro':
        return 'Cuenta de Ahorro';
      case 'credito':
        return 'Crédito / Cliente';
      default:
        return 'Persona';
    }
  });

  ngOnInit(): void {
    this.cobroActivo.set(this.movimientoForm.getRawValue().tipo === 'COBRO_CREDITO');
    this.movimientoForm.controls.tipo.valueChanges.subscribe((tipo) => {
      this.cobroActivo.set(tipo === 'COBRO_CREDITO');
      this.movimientoForm.controls.referenciaId.setValue(null);
    });
    this.cargarCajas();
    this.cargarOpcionesPersona();
  }

  cargarOpcionesPersona(): void {
    this.cargandoOpciones.set(true);
    this.socioService.listar({ size: 100 }).subscribe({
      next: (p) => {
        this.socios.set(p.content);
        this.cargarCuentas();
      },
      error: () => this.cargarCuentas()
    });
  }

  private cargarCuentas(): void {
    this.ahorroService.cuentas(undefined, { size: 100 }).subscribe({
      next: (p) => {
        this.cuentas.set(p.content);
        this.cargarCreditos();
      },
      error: () => this.cargarCreditos()
    });
  }

  private cargarCreditos(): void {
    this.creditoService.creditos(undefined, { size: 100 }).subscribe({
      next: (p) => {
        this.creditos.set(p.content);
        this.cargandoOpciones.set(false);
      },
      error: () => this.cargandoOpciones.set(false)
    });
  }

  cargarCajas(): void {
    this.cargando.set(true);
    this.error.set('');
    this.cajaService.misCajas({
      page: this.page(),
      size: this.size(),
      sort: this.sort() ? `${this.sort()!.key},${this.sort()!.dir}` : undefined
    }).subscribe({
      next: (paginated) => {
        this.cajas.set(paginated.content);
        this.totalElements.set(paginated.totalElements);
        this.totalPages.set(paginated.totalPages);
        const abierta = paginated.content.find((c) => c.estado === 'ABIERTA');
        if (abierta) {
          this.seleccionar(abierta);
        }
        this.cargando.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.error.set((err.error as ApiError | undefined)?.message ?? 'No se pudieron cargar las cajas.');
        this.cargando.set(false);
      }
    });
  }

  seleccionar(caja: CajaApertura): void {
    this.seleccionada.set(caja);
    this.arqueoResultado.set(null);
    this.cargarSaldo(caja.id);
    this.cargarMovimientos(caja.id);
  }

  cargarSaldo(id: number): void {
    this.cajaService.saldo(id).subscribe({
      next: (s) => this.saldo.set(s),
      error: () => this.saldo.set(null)
    });
  }

  cargarMovimientos(id: number): void {
    this.cajaService.movimientos(id, {
      page: this.movPage(),
      size: this.movSize(),
      sort: this.movSort() ? `${this.movSort()!.key},${this.movSort()!.dir}` : undefined
    }).subscribe({
      next: (paginated) => {
        this.movimientos.set(paginated.content);
        this.movTotalElements.set(paginated.totalElements);
        this.movTotalPages.set(paginated.totalPages);
      },
      error: () => this.movimientos.set([])
    });
  }

  cambiarPagina(p: number): void {
    this.page.set(p);
    this.cargarCajas();
  }

  cambiarTamano(t: number): void {
    this.size.set(t);
    this.page.set(0);
    this.cargarCajas();
  }

  ordenar(s: SortState): void {
    this.sort.set(s);
    this.page.set(0);
    this.cargarCajas();
  }

  cambiarMovPagina(p: number): void {
    this.movPage.set(p);
    const caja = this.seleccionada();
    if (caja) {
      this.cargarMovimientos(caja.id);
    }
  }

  cambiarMovTamano(t: number): void {
    this.movSize.set(t);
    this.movPage.set(0);
    const caja = this.seleccionada();
    if (caja) {
      this.cargarMovimientos(caja.id);
    }
  }

  ordenarMovimientos(s: SortState): void {
    this.movSort.set(s);
    this.movPage.set(0);
    const caja = this.seleccionada();
    if (caja) {
      this.cargarMovimientos(caja.id);
    }
  }

  registrarMovimiento(): void {
    if (this.movimientoForm.invalid || this.guardando()) {
      return;
    }
    const raw = this.movimientoForm.getRawValue();
    let monto = Number(raw.monto);
    const targetTipo = referenciaParaTipo(raw.tipo);
    const request: {
      tipo: string;
      monto: number;
      descripcion?: string;
      referenciaTabla?: string;
      referenciaId?: number;
      montoCapital?: number;
      montoInteres?: number;
      montoMora?: number;
    } = { tipo: raw.tipo, monto };

    if (targetTipo && raw.referenciaId != null) {
      request.referenciaTabla = targetTipo;
      request.referenciaId = Number(raw.referenciaId);
    }

    if (raw.tipo === 'COBRO_CREDITO') {
      request.montoCapital = Number(raw.montoCapital ?? 0);
      request.montoInteres = Number(raw.montoInteres ?? 0);
      request.montoMora = Number(raw.montoMora ?? 0);
      request.monto = request.montoCapital + request.montoInteres + request.montoMora;
    }
    if (raw.descripcion) {
      request.descripcion = raw.descripcion;
    }

    const caja = this.seleccionada();
    if (!caja) {
      return;
    }
    this.guardando.set(true);
    this.error.set('');
    this.cajaService.registrarMovimiento(caja.id, request).pipe(finalize(() => this.guardando.set(false))).subscribe({
      next: () => {
        this.movimientoForm.patchValue({
          tipo: 'APORTACION',
          referenciaId: null,
          monto: 0,
          descripcion: '',
          montoCapital: null,
          montoInteres: null,
          montoMora: null
        });
        this.toast.success('Movimiento registrado correctamente.');
        this.cargarSaldo(caja.id);
        this.cargarMovimientos(caja.id);
      },
      error: (err: HttpErrorResponse) => {
        const msg = (err.error as ApiError | undefined)?.message ?? 'No se pudo registrar el movimiento.';
        this.error.set(msg);
        this.toast.error(msg);
      }
    });
  }

  hacerArqueo(): void {
    if (this.arqueoForm.invalid || this.guardando()) {
      return;
    }
    const caja = this.seleccionada();
    if (!caja) {
      return;
    }
    const raw = this.arqueoForm.getRawValue();
    this.guardando.set(true);
    this.error.set('');
    this.cajaService.arqueo(caja.id, Number(raw.saldoFisico), raw.observacion || undefined)
      .pipe(finalize(() => this.guardando.set(false)))
      .subscribe({
        next: (arqueo) => {
          this.arqueoResultado.set(arqueo);
          this.toast.success('Arqueo registrado.');
        },
        error: (err: HttpErrorResponse) => {
          const msg = (err.error as ApiError | undefined)?.message ?? 'No se pudo registrar el arqueo.';
          this.error.set(msg);
          this.toast.error(msg);
        }
      });
  }

  cerrarCaja(): void {
    const caja = this.seleccionada();
    if (!caja || this.guardando()) {
      return;
    }
    this.guardando.set(true);
    this.error.set('');
    this.cajaService.cerrar(caja.id).pipe(finalize(() => this.guardando.set(false))).subscribe({
      next: () => {
        this.toast.success('Caja cerrada.');
        this.seleccionada.set(null);
        this.saldo.set(null);
        this.movimientos.set([]);
        this.arqueoResultado.set(null);
        this.cargarCajas();
      },
      error: (err: HttpErrorResponse) => {
        const msg = (err.error as ApiError | undefined)?.message ?? 'No se pudo cerrar la caja.';
        this.error.set(msg);
        this.toast.error(msg);
      }
    });
  }

  exportarExcel(): void {
    this.descargar(this.reporteService.exportarCaja('xlsx'), 'caja.xlsx');
  }

  exportarPdf(): void {
    this.descargar(this.reporteService.exportarCaja('pdf'), 'caja.pdf');
  }

  private descargar(obs: Observable<Blob>, nombre: string): void {
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
      }
    });
  }
}
