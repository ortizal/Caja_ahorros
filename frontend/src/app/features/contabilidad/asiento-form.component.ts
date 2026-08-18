import { Component, OnInit, inject, signal } from '@angular/core';
import { FormArray, FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { finalize } from 'rxjs';
import { ApiError } from '../../core/models/auth.model';
import { PlanCuenta } from '../../core/models/contabilidad.model';
import { ContabilidadService } from '../../core/services/contabilidad.service';
import { ToastService } from '../../core/services/toast.service';

@Component({
  selector: 'app-asiento-form',
  imports: [ReactiveFormsModule],
  templateUrl: './asiento-form.html',
  styleUrl: './asiento-form.css'
})
export class AsientoFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly contabilidadService = inject(ContabilidadService);
  private readonly toast = inject(ToastService);

  protected readonly form = this.fb.nonNullable.group({
    fecha: [this.hoy(), [Validators.required]],
    descripcion: ['', [Validators.required]],
    detalles: this.fb.array([])
  });
  protected readonly planCuentas = signal<PlanCuenta[]>([]);
  protected readonly guardando = signal(false);
  protected readonly error = signal('');

  ngOnInit(): void {
    this.contabilidadService.planCuentas({ size: 500 }).subscribe({
      next: (p) => this.planCuentas.set(p.content),
      error: (err: HttpErrorResponse) => {
        const msg = (err.error as ApiError | undefined)?.message ?? 'No se pudo cargar el plan de cuentas.';
        this.error.set(msg);
        this.toast.error(msg);
      }
    });
    this.agregarDetalle();
    this.agregarDetalle();
  }

  private hoy(): string {
    return new Date().toISOString().slice(0, 10);
  }

  get detalles(): FormArray {
    return this.form.get('detalles') as FormArray;
  }

  agregarDetalle(): void {
    this.detalles.push(
      this.fb.nonNullable.group({
        cuentaId: [0, [Validators.required]],
        debe: [0, [Validators.min(0)]],
        haber: [0, [Validators.min(0)]]
      })
    );
  }

  quitarDetalle(index: number): void {
    this.detalles.removeAt(index);
  }

  cancelar(): void {
    this.router.navigate(['/contabilidad']);
  }

  submit(): void {
    if (this.form.invalid || this.guardando()) {
      return;
    }
    const raw = this.form.getRawValue();
    const detalles = (raw.detalles as { cuentaId: number; debe: number; haber: number }[])
      .map((d) => ({
        cuentaId: Number(d.cuentaId),
        debe: Number(d.debe) > 0 ? Number(d.debe) : undefined,
        haber: Number(d.haber) > 0 ? Number(d.haber) : undefined
      }))
      .filter((d) => d.debe !== undefined || d.haber !== undefined);

    const totalDebe = detalles.reduce((acc, d) => acc + (d.debe ?? 0), 0);
    const totalHaber = detalles.reduce((acc, d) => acc + (d.haber ?? 0), 0);
    if (detalles.length === 0 || totalDebe <= 0 || totalDebe !== totalHaber) {
      const msg = 'El asiento debe estar cuadradado (total debe = total haber).';
      this.error.set(msg);
      this.toast.error(msg);
      return;
    }

    this.guardando.set(true);
    this.error.set('');
    this.contabilidadService
      .registrarAsiento({ fecha: raw.fecha, descripcion: raw.descripcion, detalles })
      .pipe(finalize(() => this.guardando.set(false)))
      .subscribe({
        next: () => {
          this.toast.success('Asiento registrado correctamente.');
          this.router.navigate(['/contabilidad']);
        },
        error: (err: HttpErrorResponse) => {
          const msg = (err.error as ApiError | undefined)?.message ?? 'No se pudo registrar el asiento.';
          this.error.set(msg);
          this.toast.error(msg);
        }
      });
  }
}
