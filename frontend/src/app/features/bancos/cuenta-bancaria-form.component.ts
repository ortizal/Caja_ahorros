import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { finalize } from 'rxjs';
import { ApiError } from '../../core/models/auth.model';
import { BancoService } from '../../core/services/banco.service';
import { ToastService } from '../../core/services/toast.service';

@Component({
  selector: 'app-cuenta-bancaria-form',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './cuenta-bancaria-form.html',
  styleUrl: './cuenta-bancaria-form.css'
})
export class CuentaBancariaFormComponent {
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly bancoService = inject(BancoService);
  private readonly toast = inject(ToastService);

  protected readonly form = this.fb.nonNullable.group({
    banco: ['', [Validators.required]],
    numeroCuenta: ['', [Validators.required]],
    tipo: ['CORRIENTE', [Validators.required]],
    saldoContable: [0, [Validators.min(0)]]
  });
  protected readonly guardando = signal(false);
  protected readonly error = signal('');

  cancelar(): void {
    this.router.navigate(['/bancos']);
  }

  submit(): void {
    if (this.form.invalid || this.guardando()) {
      return;
    }
    const raw = this.form.getRawValue();
    this.guardando.set(true);
    this.error.set('');
    this.bancoService
      .crearCuenta({
        banco: raw.banco,
        numeroCuenta: raw.numeroCuenta,
        tipo: raw.tipo,
        saldoContable: Number(raw.saldoContable)
      })
      .pipe(finalize(() => this.guardando.set(false)))
      .subscribe({
        next: (cuenta) => {
          this.toast.success(`Cuenta "${cuenta.numeroCuenta}" creada correctamente.`);
          this.router.navigate(['/bancos']);
        },
        error: (err: HttpErrorResponse) => {
          const msg = (err.error as ApiError | undefined)?.message ?? 'No se pudo crear la cuenta.';
          this.error.set(msg);
          this.toast.error(msg);
        }
      });
  }
}
