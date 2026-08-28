import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { finalize } from 'rxjs';
import { ApiError } from '../../core/models/auth.model';
import { ProductoAhorro } from '../../core/models/ahorro.model';
import { Socio } from '../../core/models/socio.model';
import { AhorroService } from '../../core/services/ahorro.service';
import { SocioService } from '../../core/services/socio.service';
import { ToastService } from '../../core/services/toast.service';

@Component({
  selector: 'app-ahorro-apertura-form',
  imports: [ReactiveFormsModule],
  templateUrl: './ahorro-apertura-form.html',
  styleUrl: './ahorro-apertura-form.css'
})
export class AhorroAperturaFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly ahorroService = inject(AhorroService);
  private readonly socioService = inject(SocioService);
  private readonly toast = inject(ToastService);

  protected readonly form = this.fb.nonNullable.group({
    socioId: [0, [Validators.required, Validators.min(1)]],
    productoId: [0, [Validators.required, Validators.min(1)]],
    tipoAhorro: ['NORMAL', [Validators.required]]
  });
  protected readonly socios = signal<Socio[]>([]);
  protected readonly productos = signal<ProductoAhorro[]>([]);
  protected readonly guardando = signal(false);
  protected readonly error = signal('');

  ngOnInit(): void {
    this.socioService.listar({ estado: 'ACTIVO' }).subscribe({
      next: (s) => this.socios.set(s.content),
      error: () => this.socios.set([])
    });
    this.ahorroService.productos().subscribe({
      next: (p) => this.productos.set(p),
      error: () => this.productos.set([])
    });
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
      .aperturar({
        socioId: Number(raw.socioId),
        productoId: Number(raw.productoId),
        tipoAhorro: raw.tipoAhorro
      })
      .pipe(finalize(() => this.guardando.set(false)))
      .subscribe({
        next: () => {
          this.toast.success('Cuenta de ahorro aperturada correctamente.');
          this.router.navigate(['/ahorros']);
        },
        error: (err: HttpErrorResponse) => {
          const msg = (err.error as ApiError | undefined)?.message ?? 'No se pudo aperturar la cuenta.';
          this.error.set(msg);
          this.toast.error(msg);
        }
      });
  }
}
