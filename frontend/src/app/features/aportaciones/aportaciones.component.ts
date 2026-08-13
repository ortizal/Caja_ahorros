import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { DecimalPipe, DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { finalize } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { ApiError } from '../../core/models/auth.model';
import { Aportacion, AportacionConfig, AportacionPago } from '../../core/models/aportacion.model';
import { Socio } from '../../core/models/socio.model';
import { AportacionService } from '../../core/services/aportacion.service';
import { SocioService } from '../../core/services/socio.service';

@Component({
  selector: 'app-aportaciones',
  imports: [ReactiveFormsModule, DecimalPipe, DatePipe],
  templateUrl: './aportaciones.html',
  styleUrl: './aportaciones.css'
})
export class AportacionesComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly aportacionService = inject(AportacionService);
  private readonly socioService = inject(SocioService);
  private readonly auth = inject(AuthService);

  protected readonly tab = signal<'config' | 'aportaciones'>('config');
  protected readonly configs = signal<AportacionConfig[]>([]);
  protected readonly aportaciones = signal<Aportacion[]>([]);
  protected readonly pagos = signal<AportacionPago[]>([]);
  protected readonly socios = signal<Socio[]>([]);
  protected readonly pagoTarget = signal<Aportacion | null>(null);
  protected readonly filtroPeriodo = signal('');
  protected readonly error = signal('');
  protected readonly ok = signal('');
  protected readonly cargando = signal(false);
  protected readonly guardando = signal(false);

  protected readonly puedeVer = computedPermiso(this.auth, 'APORTACIONES:VER');
  protected readonly puedeCrear = computedPermiso(this.auth, 'APORTACIONES:CREAR');

  protected readonly configForm = this.fb.nonNullable.group({
    tipo: ['OBLIGATORIA', [Validators.required]],
    modoCalculo: ['FIJO', [Validators.required]],
    valor: [25, [Validators.required, Validators.min(0.01)]],
    periodicidad: ['MENSUAL', [Validators.required]],
    vigenteDesde: [this.hoy(), [Validators.required]]
  });

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

  private hoy(): string {
    return new Date().toISOString().slice(0, 10);
  }

  private periodoActual(): string {
    const ahora = new Date();
    return `${ahora.getFullYear()}-${String(ahora.getMonth() + 1).padStart(2, '0')}`;
  }

  cargarConfigs(): void {
    this.aportacionService.configs().subscribe({
      next: (c) => this.configs.set(c),
      error: () => this.configs.set([])
    });
  }

  cargarSocios(): void {
    this.socioService.listar('ACTIVO').subscribe({
      next: (s) => this.socios.set(s),
      error: () => this.socios.set([])
    });
  }

  cargarAportaciones(): void {
    this.aportacionService.aportaciones(this.filtroPeriodo() || undefined).subscribe({
      next: (a) => this.aportaciones.set(a),
      error: () => this.aportaciones.set([])
    });
  }

  aplicarFiltro(event: Event): void {
    this.filtroPeriodo.set((event.target as HTMLInputElement).value);
    this.cargarAportaciones();
  }

  crearConfig(): void {
    if (this.configForm.invalid || this.guardando()) {
      return;
    }
    const raw = this.configForm.getRawValue();
    this.guardando.set(true);
    this.error.set('');
    this.ok.set('');
    this.aportacionService
      .crearConfig({
        tipo: raw.tipo,
        modoCalculo: raw.modoCalculo,
        valor: Number(raw.valor),
        periodicidad: raw.periodicidad,
        vigenteDesde: raw.vigenteDesde
      })
      .pipe(finalize(() => this.guardando.set(false)))
      .subscribe({
        next: () => {
          this.ok.set('Configuracion de aportaciones creada.');
          this.configForm.reset({ tipo: 'OBLIGATORIA', modoCalculo: 'FIJO', valor: 25, periodicidad: 'MENSUAL', vigenteDesde: this.hoy() });
          this.cargarConfigs();
        },
        error: (err: HttpErrorResponse) =>
          this.error.set((err.error as ApiError | undefined)?.message ?? 'No se pudo crear la configuracion.')
      });
  }

  generarPeriodo(): void {
    if (this.generarForm.invalid || this.guardando()) {
      return;
    }
    const raw = this.generarForm.getRawValue();
    this.guardando.set(true);
    this.error.set('');
    this.ok.set('');
    this.aportacionService
      .generarPeriodo(raw.periodo)
      .pipe(finalize(() => this.guardando.set(false)))
      .subscribe({
        next: (res) => {
          this.ok.set(`Aportaciones generadas para ${res.periodo}: ${res.generadas}.`);
          this.cargarAportaciones();
        },
        error: (err: HttpErrorResponse) =>
          this.error.set((err.error as ApiError | undefined)?.message ?? 'No se pudieron generar las aportaciones.')
      });
  }

  seleccionar(a: Aportacion): void {
    this.pagoTarget.set(a);
    this.pagoForm.patchValue({ monto: Number(a.montoEsperado) - Number(a.montoPagado) });
    this.cargarPagos(a.id);
  }

  cargarPagos(aportacionId: number): void {
    this.aportacionService.pagos(aportacionId).subscribe({
      next: (p) => this.pagos.set(p),
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
    this.ok.set('');
    this.aportacionService
      .pagar(target.id, Number(raw.monto))
      .pipe(finalize(() => this.guardando.set(false)))
      .subscribe({
        next: () => {
          this.ok.set(`Pago registrado para ${target.socioNombre}.`);
          this.pagoTarget.set(null);
          this.cargarAportaciones();
        },
        error: (err: HttpErrorResponse) =>
          this.error.set((err.error as ApiError | undefined)?.message ?? 'No se pudo registrar el pago.')
      });
  }
}

function computedPermiso(auth: AuthService, permiso: string): ReturnType<typeof computed> {
  return computed(() => auth.hasPermiso(permiso));
}
