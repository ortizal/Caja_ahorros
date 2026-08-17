import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { DatePipe, DecimalPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import { finalize, Observable } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { ApiError } from '../../core/models/auth.model';
import {
  CajaApertura,
  CajaArqueo,
  CajaMovimiento,
  SaldoCaja,
  TIPOS_MOVIMIENTO_CAJA
} from '../../core/models/caja.model';
import { CajaService } from '../../core/services/caja.service';
import { ReporteService } from '../../core/services/reporte.service';
import { ToastService } from '../../core/services/toast.service';
import { AccionesMenuComponent } from '../../shared/components/acciones-menu/acciones-menu.component';

@Component({
  selector: 'app-caja',
  imports: [ReactiveFormsModule, DecimalPipe, DatePipe, RouterLink, AccionesMenuComponent],
  templateUrl: './caja.html',
  styleUrl: './caja.css'
})
export class CajaComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly cajaService = inject(CajaService);
  private readonly auth = inject(AuthService);
  private readonly reporteService = inject(ReporteService);
  private readonly toast = inject(ToastService);

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

  protected readonly movimientoForm = this.fb.nonNullable.group({
    tipo: ['APORTACION', [Validators.required]],
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
  protected readonly cajaAbierta = computed(
    () => this.seleccionada()?.estado === 'ABIERTA' && this.cajas().some((c) => c.id === this.seleccionada()?.id)
  );

  ngOnInit(): void {
    this.cobroActivo.set(this.movimientoForm.getRawValue().tipo === 'COBRO_CREDITO');
    this.movimientoForm.controls.tipo.valueChanges.subscribe((tipo) =>
      this.cobroActivo.set(tipo === 'COBRO_CREDITO')
    );
    this.cargarCajas();
  }

  cargarCajas(): void {
    this.cargando.set(true);
    this.error.set('');
    this.cajaService.misCajas().subscribe({
      next: (cajas) => {
        this.cajas.set(cajas);
        const abierta = cajas.find((c) => c.estado === 'ABIERTA');
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
    this.cajaService.movimientos(id).subscribe({
      next: (m) => this.movimientos.set(m),
      error: () => this.movimientos.set([])
    });
  }

  registrarMovimiento(): void {
    if (this.movimientoForm.invalid || this.guardando()) {
      return;
    }
    const raw = this.movimientoForm.getRawValue();
    let monto = Number(raw.monto);
    const request: {
      tipo: string;
      monto: number;
      descripcion?: string;
      montoCapital?: number;
      montoInteres?: number;
      montoMora?: number;
    } = { tipo: raw.tipo, monto };

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
