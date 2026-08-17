import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { finalize } from 'rxjs';
import { ApiError } from '../../core/models/auth.model';
import { CajaService } from '../../core/services/caja.service';
import { ToastService } from '../../core/services/toast.service';

@Component({
  selector: 'app-caja-apertura-form',
  imports: [ReactiveFormsModule],
  templateUrl: './caja-apertura-form.html',
  styleUrl: './caja-apertura-form.css'
})
export class CajaAperturaFormComponent {
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly cajaService = inject(CajaService);
  private readonly toast = inject(ToastService);

  protected readonly form = this.fb.nonNullable.group({
    saldoInicial: [0, [Validators.required, Validators.min(0)]]
  });
  protected readonly guardando = signal(false);
  protected readonly error = signal('');

  cancelar(): void {
    this.router.navigate(['/caja']);
  }

  submit(): void {
    if (this.form.invalid || this.guardando()) {
      return;
    }
    this.guardando.set(true);
    this.error.set('');
    this.cajaService
      .apertura(this.form.getRawValue().saldoInicial)
      .pipe(finalize(() => this.guardando.set(false)))
      .subscribe({
        next: (caja) => {
          this.toast.success(`Caja #${caja.id} abierta correctamente.`);
          this.router.navigate(['/caja']);
        },
        error: (err: HttpErrorResponse) => {
          const msg = (err.error as ApiError | undefined)?.message ?? 'No se pudo abrir la caja.';
          this.error.set(msg);
          this.toast.error(msg);
        }
      });
  }
}
