import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { finalize } from 'rxjs';
import { ApiError } from '../../core/models/auth.model';
import { PlanCuenta } from '../../core/models/contabilidad.model';
import { ContabilidadService } from '../../core/services/contabilidad.service';
import { TesoreriaService } from '../../core/services/tesoreria.service';
import { ToastService } from '../../core/services/toast.service';

@Component({
  selector: 'app-cuenta-pagar-form',
  imports: [ReactiveFormsModule],
  templateUrl: './cuenta-pagar-form.html',
  styleUrl: './cuenta-pagar-form.css'
})
export class CuentaPagarFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly tesoreriaService = inject(TesoreriaService);
  private readonly contabilidadService = inject(ContabilidadService);
  private readonly toast = inject(ToastService);

  protected readonly form = this.fb.nonNullable.group({
    proveedor: ['', [Validators.required]],
    concepto: ['', [Validators.required]],
    monto: [0, [Validators.required, Validators.min(0.01)]],
    cuentaContableId: [0, [Validators.required]],
    fechaVencimiento: ['', [Validators.required]]
  });
  protected readonly planCuentas = signal<PlanCuenta[]>([]);
  protected readonly guardando = signal(false);
  protected readonly error = signal('');

  ngOnInit(): void {
    this.contabilidadService.planCuentas().subscribe({
      next: (p) => this.planCuentas.set(p),
      error: () => this.planCuentas.set([])
    });
  }

  cancelar(): void {
    this.router.navigate(['/tesoreria']);
  }

  submit(): void {
    if (this.form.invalid || this.guardando()) {
      return;
    }
    const raw = this.form.getRawValue();
    this.guardando.set(true);
    this.error.set('');
    this.tesoreriaService
      .crearCuentaPorPagar({
        proveedor: raw.proveedor,
        concepto: raw.concepto,
        monto: Number(raw.monto),
        cuentaContableId: Number(raw.cuentaContableId),
        fechaVencimiento: raw.fechaVencimiento
      })
      .pipe(finalize(() => this.guardando.set(false)))
      .subscribe({
        next: () => {
          this.toast.success('Cuenta por pagar registrada.');
          this.router.navigate(['/tesoreria']);
        },
        error: (err: HttpErrorResponse) => {
          const msg = (err.error as ApiError | undefined)?.message ?? 'No se pudo registrar la cuenta por pagar.';
          this.error.set(msg);
          this.toast.error(msg);
        }
      });
  }
}
