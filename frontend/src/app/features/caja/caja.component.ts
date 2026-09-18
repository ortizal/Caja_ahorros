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
  objetoParaTipo
} from '../../core/models/caja.model';
import { CajaService } from '../../core/services/caja.service';
import { ReporteService } from '../../core/services/reporte.service';
import { ToastService } from '../../core/services/toast.service';
import { AhorroService } from '../../core/services/ahorro.service';
import { CreditoService } from '../../core/services/credito.service';
import { AportacionService } from '../../core/services/aportacion.service';
import { Socio } from '../../core/models/socio.model';
import { SortState } from '../../core/models/paginado.model';
import { PaginadorComponent } from '../../shared/components/paginador/paginador.component';
import { SortableHeaderDirective } from '../../shared/components/sortable-header/sortable-header.directive';
import { SocioBuscadorComponent } from './socio-buscador.component';

interface OpcionMovimiento {
  id: number;
  label: string;
  pendiente?: number;
  saldo?: number;
  estado?: string;
}

@Component({
  selector: 'app-caja',
  imports: [ReactiveFormsModule, DecimalPipe, DatePipe, PaginadorComponent, SortableHeaderDirective, SocioBuscadorComponent],
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
  private readonly ahorroService = inject(AhorroService);
  private readonly creditoService = inject(CreditoService);
  private readonly aportacionService = inject(AportacionService);

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

  protected readonly movPage = signal(0);
  protected readonly movSize = signal(10);
  protected readonly movSort = signal<SortState | null>(null);
  protected readonly movTotalElements = signal(0);
  protected readonly movTotalPages = signal(0);

  protected readonly socioSeleccionado = signal<Socio | null>(null);
  protected readonly modalSocioAbierto = signal(false);
  protected readonly items = signal<OpcionMovimiento[]>([]);
  protected readonly cargandoOpciones = signal(false);

  protected readonly movimientoForm = this.fb.nonNullable.group({
    tipo: ['APORTACION', [Validators.required]],
    referenciaId: [null as number | null, [Validators.required]],
    monto: [0],
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
  protected readonly puedeAprobar = computed(() => this.auth.hasPermiso('CAJA:APROBAR'));
  protected readonly tieneCajaAbierta = computed(() => this.cajas().some((c) => c.estado === 'ABIERTA'));
  protected readonly cajaAbierta = computed(
    () => this.seleccionada()?.estado === 'ABIERTA' && this.cajas().some((c) => c.id === this.seleccionada()?.id)
  );

  irListado(): void {
    this.router.navigate(['/caja/listado']);
  }

  protected readonly tipoActual = computed(() => this.estadoForm().tipo ?? 'APORTACION');
  protected readonly objetoActual = computed(() => objetoParaTipo(this.tipoActual()));
  protected readonly requiereSocio = computed(() => this.objetoActual() !== null);

  protected readonly itemSeleccionado = computed(() => {
    const ref = this.estadoForm().referenciaId;
    return this.items().find((i) => i.id === ref) ?? null;
  });

  protected readonly estadoForm = signal(this.movimientoForm.getRawValue());

  protected readonly etiquetaItem = computed(() => {
    switch (this.objetoActual()) {
      case 'aportacion':
        return 'Aportación (periodo)';
      case 'cuenta':
        return 'Cuenta de Ahorro';
      case 'credito':
        return 'Crédito';
      default:
        return 'Referencia';
    }
  });

  protected readonly etiquetaBoton = computed(() => {
    switch (this.tipoActual()) {
      case 'APORTACION':
        return '💾 Pagar Aportación';
      case 'DEPOSITO':
        return '💾 Registrar Depósito';
      case 'RETIRO':
        return '💾 Registrar Retiro';
      case 'COBRO_CREDITO':
        return '💾 Registrar Cobro';
      case 'DESEMBOLSO':
        return '💾 Desembolsar Crédito';
      default:
        return '💾 Registrar';
    }
  });

  protected readonly hintMonto = computed(() => {
    const item = this.itemSeleccionado();
    if (item == null) {
      return null;
    }
    switch (this.tipoActual()) {
      case 'APORTACION':
        return item.pendiente != null ? `Pendiente a pagar: $${item.pendiente.toFixed(2)}` : null;
      case 'RETIRO':
        return item.saldo != null ? `Saldo disponible para retiro: $${item.saldo.toFixed(2)}` : null;
      case 'DEPOSITO':
        return item.saldo != null ? `Saldo actual de la cuenta: $${item.saldo.toFixed(2)}` : null;
      case 'DESEMBOLSO':
        return item.estado === 'APROBADA' ? 'Monto acreditado por el monto aprobado del crédito.' : null;
      case 'COBRO_CREDITO':
        return item.estado != null ? `Estado del crédito: ${item.estado}` : null;
      default:
        return null;
    }
  });

  protected readonly avisoItem = computed(() => {
    const item = this.itemSeleccionado();
    const tipo = this.tipoActual();
    if (item != null && tipo === 'DESEMBOLSO' && item.estado !== 'APROBADA') {
      return 'Solo se pueden desembolsar créditos en estado APROBADA.';
    }
    if (item != null && tipo === 'RETIRO' && (item.saldo ?? 0) <= 0) {
      return 'La cuenta seleccionada no tiene saldo disponible.';
    }
    if (item != null && tipo === 'APORTACION' && (item.pendiente ?? 0) <= 0) {
      return 'La aportación seleccionada no tiene monto pendiente.';
    }
    return null;
  });

  protected readonly formValido = computed(() => {
    const raw = this.estadoForm();
    if (!raw.tipo || raw.referenciaId == null) {
      return false;
    }
    if (raw.tipo === 'DESEMBOLSO') {
      return this.itemSeleccionado()?.estado === 'APROBADA';
    }
    if (raw.tipo === 'COBRO_CREDITO') {
      return (
        Number(raw.montoCapital ?? 0) + Number(raw.montoInteres ?? 0) + Number(raw.montoMora ?? 0) >
        0
      );
    }
    return Number(raw.monto) > 0;
  });

  irAbrirCaja(): void {
    if (this.tieneCajaAbierta()) {
      this.toast.warning('Ya existe una caja ABIERTA; cierre la caja actual antes de abrir otra.');
      return;
    }
    this.router.navigate(['/caja/nuevo']);
  }

  ngOnInit(): void {
    this.cobroActivo.set(this.movimientoForm.getRawValue().tipo === 'COBRO_CREDITO');
    this.movimientoForm.valueChanges.subscribe((v) => {
      this.estadoForm.set({
        tipo: v.tipo ?? 'APORTACION',
        referenciaId: v.referenciaId ?? null,
        monto: v.monto ?? 0,
        descripcion: v.descripcion ?? '',
        montoCapital: v.montoCapital ?? null,
        montoInteres: v.montoInteres ?? null,
        montoMora: v.montoMora ?? null
      });
    });
    this.movimientoForm.controls.tipo.valueChanges.subscribe((tipo) => {
      this.cobroActivo.set(tipo === 'COBRO_CREDITO');
      this.movimientoForm.controls.referenciaId.setValue(null);
      this.movimientoForm.controls.monto.setValue(0);
      this.movimientoForm.controls.montoCapital.setValue(null);
      this.movimientoForm.controls.montoInteres.setValue(null);
      this.movimientoForm.controls.montoMora.setValue(null);
      this.cargarItems();
    });
    this.movimientoForm.controls.referenciaId.valueChanges.subscribe((v) => {
      const item = this.items().find((i) => i.id === v);
      if (this.tipoActual() === 'APORTACION' && item?.pendiente != null) {
        this.movimientoForm.controls.monto.setValue(item.pendiente);
      }
    });
    this.cargarCajas();
  }

  abrirBuscadorSocio(): void {
    this.modalSocioAbierto.set(true);
  }

  cerrarBuscadorSocio(): void {
    this.modalSocioAbierto.set(false);
  }

  elegirSocio(socio: Socio): void {
    this.socioSeleccionado.set(socio);
    this.modalSocioAbierto.set(false);
    this.movimientoForm.controls.referenciaId.setValue(null);
    this.cargarItems();
  }

  quitarSocio(): void {
    this.socioSeleccionado.set(null);
    this.items.set([]);
    this.movimientoForm.controls.referenciaId.setValue(null);
  }

  cargarItems(): void {
    const socio = this.socioSeleccionado();
    if (!socio) {
      this.items.set([]);
      return;
    }
    const objeto = this.objetoActual();
    if (!objeto) {
      this.items.set([]);
      return;
    }
    this.cargandoOpciones.set(true);
    const fin = () => this.cargandoOpciones.set(false);
    if (objeto === 'aportacion') {
      this.aportacionService.aportaciones({ socioId: socio.id, size: 100 }).subscribe({
        next: (p) => {
          this.items.set(
            p.content
              .filter((a) => a.estado !== 'PAGADA')
              .map((a) => ({
                id: a.id,
                label: `${a.periodo} — Cuota $${a.montoEsperado} · Pagado $${a.montoPagado}`,
                pendiente: a.montoEsperado - a.montoPagado,
                estado: a.estado
              }))
          );
          fin();
        },
        error: () => fin()
      });
    } else if (objeto === 'cuenta') {
      this.ahorroService.cuentas(socio.id, { size: 100 }).subscribe({
        next: (p) => {
          this.items.set(
            p.content
              .filter((c) => c.estado === 'ABIERTA')
              .map((c) => ({
                id: c.id,
                label: `${c.numeroCuenta} — ${c.tipoAhorro}`,
                saldo: c.saldo,
                estado: c.estado
              }))
          );
          fin();
        },
        error: () => fin()
      });
    } else {
      this.creditoService.creditos(socio.id, { size: 100 }).subscribe({
        next: (p) => {
          this.items.set(
            p.content.map((c) => ({
              id: c.id,
              label: `#${c.id} — ${c.socioNombre ?? c.clienteNoSocioNombre ?? 'Cliente'}`,
              estado: c.estado
            }))
          );
          fin();
        },
        error: () => fin()
      });
    }
  }

  cargarCajas(): void {
    this.cargando.set(true);
    this.error.set('');
    this.cajaService.misCajas({ size: 50, sort: 'openedAt,desc' }).subscribe({
      next: (paginated) => {
        this.cajas.set(paginated.content);
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
    if (!this.formValido() || this.guardando()) {
      return;
    }
    const raw = this.movimientoForm.getRawValue();
    const tipo = raw.tipo;
    const item = this.itemSeleccionado();
    const caja = this.seleccionada();
    if (!item || !caja) {
      return;
    }
    const refId = Number(raw.referenciaId);
    const monto = Number(raw.monto);

    switch (tipo) {
      case 'APORTACION': {
        if (monto <= 0) {
          this.setError('El monto a pagar debe ser mayor a 0.');
          return;
        }
        this.ejecutar(this.aportacionService.pagar(refId, monto), 'Aportación pagada correctamente.');
        break;
      }
      case 'DEPOSITO': {
        if (monto <= 0) {
          this.setError('El monto a depositar debe ser mayor a 0.');
          return;
        }
        this.ejecutar(this.ahorroService.depositar(refId, monto), 'Depósito registrado correctamente.');
        break;
      }
      case 'RETIRO': {
        if (monto <= 0) {
          this.setError('El monto a retirar debe ser mayor a 0.');
          return;
        }
        if (item.saldo != null && monto > item.saldo) {
          this.setError('El monto a retirar supera el saldo disponible de la cuenta.');
          return;
        }
        this.ejecutar(this.ahorroService.retirar(refId, monto), 'Retiro registrado correctamente.');
        break;
      }
      case 'COBRO_CREDITO': {
        const request: {
          tipo: string;
          monto: number;
          descripcion?: string;
          referenciaTabla?: string;
          referenciaId?: number;
          montoCapital?: number;
          montoInteres?: number;
          montoMora?: number;
        } = { tipo, monto };
        request.referenciaTabla = 'credito';
        request.referenciaId = refId;
        request.montoCapital = Number(raw.montoCapital ?? 0);
        request.montoInteres = Number(raw.montoInteres ?? 0);
        request.montoMora = Number(raw.montoMora ?? 0);
        request.monto = request.montoCapital + request.montoInteres + request.montoMora;
        if (raw.descripcion) {
          request.descripcion = raw.descripcion;
        }
        this.ejecutar(this.cajaService.registrarMovimiento(caja.id, request), 'Cobro registrado correctamente.');
        break;
      }
      case 'DESEMBOLSO': {
        this.ejecutar(this.creditoService.desembolsar(refId), 'Crédito desembolsado correctamente.');
        break;
      }
      default:
        return;
    }
  }

  private ejecutar(obs: Observable<unknown>, mensaje: string): void {
    const caja = this.seleccionada();
    this.guardando.set(true);
    this.error.set('');
    obs.pipe(finalize(() => this.guardando.set(false))).subscribe({
      next: () => {
        this.toast.success(mensaje);
        this.movimientoForm.patchValue({
          referenciaId: null,
          monto: 0,
          descripcion: '',
          montoCapital: null,
          montoInteres: null,
          montoMora: null
        });
        this.cargarItems();
        if (caja) {
          this.cargarSaldo(caja.id);
          this.cargarMovimientos(caja.id);
        }
      },
      error: (err: HttpErrorResponse) => {
        const msg = (err.error as ApiError | undefined)?.message ?? 'No se pudo completar la operación.';
        this.error.set(msg);
        this.toast.error(msg);
      }
    });
  }

  private setError(msg: string): void {
    this.error.set(msg);
    this.toast.error(msg);
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