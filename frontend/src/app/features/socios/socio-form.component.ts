import { Component, OnInit, inject, signal } from '@angular/core';
import { FormArray, FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { finalize } from 'rxjs';
import { ApiError } from '../../core/models/auth.model';
import { SocioRequest } from '../../core/models/socio.model';
import { SocioService } from '../../core/services/socio.service';
import { ToastService } from '../../core/services/toast.service';

interface BeneficiarioForm {
  nombres: string;
  parentesco: string;
  porcentaje: number | null;
}

@Component({
  selector: 'app-socio-form',
  imports: [ReactiveFormsModule],
  templateUrl: './socio-form.html',
  styleUrl: './socio-form.css'
})
export class SocioFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly socioService = inject(SocioService);
  private readonly toast = inject(ToastService);

  protected readonly form = this.fb.nonNullable.group({
    identificacion: ['', [Validators.required]],
    nombres: ['', [Validators.required]],
    apellidos: ['', [Validators.required]],
    telefono: [''],
    email: [''],
    direccion: [''],
    fechaIngreso: [this.hoy(), [Validators.required]],
    estado: ['ACTIVO'],
    beneficiarios: this.fb.array<BeneficiarioForm>([])
  });
  protected readonly esEdicion = signal(false);
  protected readonly loading = signal(false);
  protected readonly guardando = signal(false);
  protected readonly error = signal('');
  private socioId: number | null = null;

  get beneficiarios(): FormArray {
    return this.form.controls.beneficiarios;
  }

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.esEdicion.set(true);
      this.socioId = Number(id);
      this.loading.set(true);
      this.socioService.obtener(this.socioId).subscribe({
        next: (socio) => {
          this.form.patchValue({
            identificacion: socio.identificacion,
            nombres: socio.nombres,
            apellidos: socio.apellidos,
            telefono: socio.telefono ?? '',
            email: socio.email ?? '',
            direccion: socio.direccion ?? '',
            fechaIngreso: socio.fechaIngreso,
            estado: socio.estado
          });
          socio.beneficiarios.forEach((b) => this.agregarBeneficiario(b.nombres, b.parentesco, b.porcentaje));
          this.loading.set(false);
        },
        error: () => {
          this.error.set('No se pudo cargar el socio.');
          this.toast.error('No se pudo cargar el socio.');
          this.loading.set(false);
        }
      });
    }
  }

  agregarBeneficiario(nombres = '', parentesco = '', porcentaje: number | null = null): void {
    this.beneficiarios.push(
      this.fb.group({
        nombres: [nombres, [Validators.required]],
        parentesco: [parentesco],
        porcentaje: [porcentaje]
      })
    );
  }

  quitarBeneficiario(index: number): void {
    this.beneficiarios.removeAt(index);
  }

  cancelar(): void {
    this.router.navigate(['/socios']);
  }

  submit(): void {
    if (this.form.invalid || this.guardando()) {
      return;
    }
    const raw = this.form.getRawValue();
    const payload: SocioRequest = {
      identificacion: raw.identificacion,
      nombres: raw.nombres,
      apellidos: raw.apellidos,
      telefono: raw.telefono || null,
      email: raw.email || null,
      direccion: raw.direccion || null,
      fechaIngreso: raw.fechaIngreso,
      estado: raw.estado,
      beneficiarios: raw.beneficiarios
        .filter((b): b is BeneficiarioForm => b !== null)
        .map((b) => ({
          nombres: b.nombres,
          parentesco: b.parentesco || undefined,
          porcentaje: b.porcentaje ?? undefined
        }))
    };

    this.guardando.set(true);
    this.error.set('');
    const request =
      this.esEdicion() && this.socioId !== null
        ? this.socioService.actualizar(this.socioId, payload)
        : this.socioService.crear(payload);

    request.pipe(finalize(() => this.guardando.set(false))).subscribe({
      next: () => {
        this.toast.success(this.esEdicion() ? 'Socio actualizado correctamente.' : 'Socio creado correctamente.');
        this.router.navigate(['/socios']);
      },
      error: (err: HttpErrorResponse) => {
        const msg = (err.error as ApiError | undefined)?.message ?? 'No se pudo guardar el socio.';
        this.error.set(msg);
        this.toast.error(msg);
      }
    });
  }

  private hoy(): string {
    return new Date().toISOString().slice(0, 10);
  }
}
