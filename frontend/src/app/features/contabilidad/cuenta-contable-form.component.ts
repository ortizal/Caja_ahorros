import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { finalize } from 'rxjs';
import { ApiError } from '../../core/models/auth.model';
import { TIPOS_CUENTA } from '../../core/models/contabilidad.model';
import { ContabilidadService } from '../../core/services/contabilidad.service';
import { ToastService } from '../../core/services/toast.service';

@Component({
  selector: 'app-cuenta-contable-form',
  imports: [ReactiveFormsModule],
  templateUrl: './cuenta-contable-form.html',
  styleUrl: './cuenta-contable-form.css'
})
export class CuentaContableFormComponent {
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly contabilidadService = inject(ContabilidadService);
  private readonly toast = inject(ToastService);

  protected readonly form = this.fb.nonNullable.group({
    codigo: ['', [Validators.required]],
    nombre: ['', [Validators.required]],
    tipo: ['ACTIVO', [Validators.required]],
    nivel: [1, [Validators.required, Validators.min(1)]],
    aceptaMovimiento: [true]
  });
  protected readonly tiposCuenta = TIPOS_CUENTA;
  protected readonly guardando = signal(false);
  protected readonly error = signal('');

  cancelar(): void {
    this.router.navigate(['/contabilidad']);
  }

  submit(): void {
    if (this.form.invalid || this.guardando()) {
      return;
    }
    const raw = this.form.getRawValue();
    this.guardando.set(true);
    this.error.set('');
    this.contabilidadService
      .crearCuenta({
        codigo: raw.codigo,
        nombre: raw.nombre,
        tipo: raw.tipo,
        nivel: Number(raw.nivel),
        aceptaMovimiento: raw.aceptaMovimiento
      })
      .pipe(finalize(() => this.guardando.set(false)))
      .subscribe({
        next: () => {
          this.toast.success('Cuenta contable creada correctamente.');
          this.router.navigate(['/contabilidad']);
        },
        error: (err: HttpErrorResponse) => {
          const msg = (err.error as ApiError | undefined)?.message ?? 'No se pudo crear la cuenta.';
          this.error.set(msg);
          this.toast.error(msg);
        }
      });
  }
}
