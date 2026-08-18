import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { finalize } from 'rxjs';
import { ApiError } from '../../core/models/auth.model';
import { ProductoCredito } from '../../core/models/credito.model';
import { Socio } from '../../core/models/socio.model';
import { CreditoService } from '../../core/services/credito.service';
import { SocioService } from '../../core/services/socio.service';
import { ToastService } from '../../core/services/toast.service';

@Component({
  selector: 'app-credito-solicitud-form',
  imports: [ReactiveFormsModule],
  templateUrl: './credito-solicitud-form.html',
  styleUrl: './credito-solicitud-form.css'
})
export class CreditoSolicitudFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly creditoService = inject(CreditoService);
  private readonly socioService = inject(SocioService);
  private readonly toast = inject(ToastService);

  protected readonly form = this.fb.nonNullable.group({
    socioId: [0, [Validators.required, Validators.min(1)]],
    productoId: [0, [Validators.required, Validators.min(1)]],
    montoSolicitado: [500, [Validators.required, Validators.min(0.01)]],
    plazoMeses: [12, [Validators.required, Validators.min(1)]],
    destino: ['']
  });
  protected readonly socios = signal<Socio[]>([]);
  protected readonly productos = signal<ProductoCredito[]>([]);
  protected readonly guardando = signal(false);
  protected readonly error = signal('');

  ngOnInit(): void {
    this.socioService.listar({ estado: 'ACTIVO' }).subscribe({
      next: (s) => this.socios.set(s.content),
      error: () => this.socios.set([])
    });
    this.creditoService.productos().subscribe({
      next: (p) => this.productos.set(p),
      error: () => this.productos.set([])
    });
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
      .crearSolicitud({
        socioId: Number(raw.socioId),
        productoId: Number(raw.productoId),
        montoSolicitado: Number(raw.montoSolicitado),
        plazoMeses: Number(raw.plazoMeses),
        destino: raw.destino || undefined
      })
      .pipe(finalize(() => this.guardando.set(false)))
      .subscribe({
        next: () => {
          this.toast.success('Solicitud de credito registrada.');
          this.router.navigate(['/creditos']);
        },
        error: (err: HttpErrorResponse) => {
          const msg = (err.error as ApiError | undefined)?.message ?? 'No se pudo registrar la solicitud.';
          this.error.set(msg);
          this.toast.error(msg);
        }
      });
  }
}
