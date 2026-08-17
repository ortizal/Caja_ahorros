import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { finalize } from 'rxjs';
import { ApiError } from '../../core/models/auth.model';
import { AhorroService } from '../../core/services/ahorro.service';
import { ToastService } from '../../core/services/toast.service';

@Component({
  selector: 'app-ahorro-producto-form',
  imports: [ReactiveFormsModule],
  templateUrl: './ahorro-producto-form.html',
  styleUrl: './ahorro-producto-form.css'
})
export class AhorroProductoFormComponent {
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly ahorroService = inject(AhorroService);
  private readonly toast = inject(ToastService);

  protected readonly form = this.fb.nonNullable.group({
    nombre: ['', [Validators.required]],
    tasaInteres: [2.5, [Validators.required, Validators.min(0)]],
    periodicidadCapitalizacion: ['MENSUAL', [Validators.required]],
    saldoMinimo: [0, [Validators.min(0)]],
    limiteRetirosMes: [1, [Validators.min(1)]],
    vigenteDesde: [this.hoy(), [Validators.required]]
  });
  protected readonly guardando = signal(false);
  protected readonly error = signal('');

  private hoy(): string {
    return new Date().toISOString().slice(0, 10);
  }

  cancelar(): void {
    this.router.navigate(['/ahorros']);
  }

  submit(): void {
    if (this.form.invalid || this.guardando()) {
      return;
    }
    const raw = this.form.getRawValue();
    this.guardando.set(true);
    this.error.set('');
    this.ahorroService
      .crearProducto({
        nombre: raw.nombre,
        tasaInteres: Number(raw.tasaInteres),
        periodicidadCapitalizacion: raw.periodicidadCapitalizacion,
        saldoMinimo: Number(raw.saldoMinimo),
        limiteRetirosMes: Number(raw.limiteRetirosMes),
        vigenteDesde: raw.vigenteDesde
      })
      .pipe(finalize(() => this.guardando.set(false)))
      .subscribe({
        next: () => {
          this.toast.success('Producto de ahorro creado correctamente.');
          this.router.navigate(['/ahorros']);
        },
        error: (err: HttpErrorResponse) => {
          const msg = (err.error as ApiError | undefined)?.message ?? 'No se pudo crear el producto.';
          this.error.set(msg);
          this.toast.error(msg);
        }
      });
  }
}
