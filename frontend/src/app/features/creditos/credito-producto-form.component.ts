import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { finalize } from 'rxjs';
import { ApiError } from '../../core/models/auth.model';
import { CreditoService } from '../../core/services/credito.service';
import { ToastService } from '../../core/services/toast.service';

@Component({
  selector: 'app-credito-producto-form',
  imports: [ReactiveFormsModule],
  templateUrl: './credito-producto-form.html',
  styleUrl: './credito-producto-form.css'
})
export class CreditoProductoFormComponent {
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly creditoService = inject(CreditoService);
  private readonly toast = inject(ToastService);

  protected readonly form = this.fb.nonNullable.group({
    nombre: ['', [Validators.required]],
    tasaInteres: [18, [Validators.required, Validators.min(0)]],
    tasaMora: [1, [Validators.min(0)]],
    sistemaAmortizacion: ['FRANCES', [Validators.required]],
    plazoMaxMeses: [36, [Validators.required, Validators.min(1)]],
    montoMin: [0, [Validators.min(0)]],
    montoMax: [5000, [Validators.min(0)]],
    permiteNoSocio: [false],
    requiereGarante: [false],
    vigenteDesde: [this.hoy(), [Validators.required]]
  });
  protected readonly guardando = signal(false);
  protected readonly error = signal('');

  private hoy(): string {
    return new Date().toISOString().slice(0, 10);
  }

  cancelar(): void {
    this.router.navigate(['/creditos']);
  }

  submit(): void {
    if (this.form.invalid || this.guardando()) {
      return;
    }
    const raw = this.form.getRawValue();
    this.guardando.set(true);
    this.error.set('');
    this.creditoService
      .crearProducto({
        nombre: raw.nombre,
        tasaInteres: Number(raw.tasaInteres),
        tasaMora: Number(raw.tasaMora),
        sistemaAmortizacion: raw.sistemaAmortizacion,
        plazoMaxMeses: Number(raw.plazoMaxMeses),
        montoMin: Number(raw.montoMin),
        montoMax: Number(raw.montoMax),
        permiteNoSocio: Boolean(raw.permiteNoSocio),
        requiereGarante: Boolean(raw.requiereGarante),
        vigenteDesde: raw.vigenteDesde
      })
      .pipe(finalize(() => this.guardando.set(false)))
      .subscribe({
        next: () => {
          this.toast.success('Producto de credito creado.');
          this.router.navigate(['/creditos']);
        },
        error: (err: HttpErrorResponse) => {
          const msg = (err.error as ApiError | undefined)?.message ?? 'No se pudo crear el producto.';
          this.error.set(msg);
          this.toast.error(msg);
        }
      });
  }
}
