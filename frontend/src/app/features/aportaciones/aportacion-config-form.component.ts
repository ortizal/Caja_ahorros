import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { finalize } from 'rxjs';
import { ApiError } from '../../core/models/auth.model';
import { AportacionService } from '../../core/services/aportacion.service';
import { ToastService } from '../../core/services/toast.service';

@Component({
  selector: 'app-aportacion-config-form',
  imports: [ReactiveFormsModule],
  templateUrl: './aportacion-config-form.html',
  styleUrl: './aportacion-config-form.css'
})
export class AportacionConfigFormComponent {
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly aportacionService = inject(AportacionService);
  private readonly toast = inject(ToastService);

  protected readonly form = this.fb.nonNullable.group({
    tipo: ['OBLIGATORIA', [Validators.required]],
    modoCalculo: ['FIJO', [Validators.required]],
    valor: [25, [Validators.required, Validators.min(0.01)]],
    periodicidad: ['MENSUAL', [Validators.required]],
    vigenteDesde: [this.hoy(), [Validators.required]]
  });
  protected readonly guardando = signal(false);
  protected readonly error = signal('');

  private hoy(): string {
    return new Date().toISOString().slice(0, 10);
  }

  cancelar(): void {
    this.router.navigate(['/aportaciones']);
  }

  submit(): void {
    if (this.form.invalid || this.guardando()) {
      return;
    }
    const raw = this.form.getRawValue();
    this.guardando.set(true);
    this.error.set('');
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
          this.toast.success('Configuración de aportaciones creada correctamente.');
          this.router.navigate(['/aportaciones']);
        },
        error: (err: HttpErrorResponse) => {
          const msg = (err.error as ApiError | undefined)?.message ?? 'No se pudo crear la configuración.';
          this.error.set(msg);
          this.toast.error(msg);
        }
      });
  }
}
