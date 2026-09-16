import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { finalize } from 'rxjs';
import { ApiError } from '../../core/models/auth.model';
import { Cajero } from '../../core/models/caja.model';
import { AuthService } from '../../core/auth/auth.service';
import { CajaService } from '../../core/services/caja.service';
import { ToastService } from '../../core/services/toast.service';

@Component({
  selector: 'app-caja-apertura-form',
  imports: [ReactiveFormsModule],
  templateUrl: './caja-apertura-form.html',
  styleUrl: './caja-apertura-form.css'
})
export class CajaAperturaFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly cajaService = inject(CajaService);
  private readonly auth = inject(AuthService);
  private readonly toast = inject(ToastService);

  protected readonly form = this.fb.nonNullable.group({
    saldoInicial: [0, [Validators.required, Validators.min(0)]],
    cajeroId: [null as number | null]
  });
  protected readonly guardando = signal(false);
  protected readonly error = signal('');
  protected readonly cajeros = signal<Cajero[]>([]);
  protected readonly cargandoCajeros = signal(false);
  protected readonly puedeAsignar = computed(() => this.auth.hasPermiso('CAJA:APROBAR'));

  ngOnInit(): void {
    if (this.puedeAsignar()) {
      this.cargandoCajeros.set(true);
      this.cajaService.cajeros().subscribe({
        next: (lista) => this.cajeros.set(lista),
        error: () => undefined,
        complete: () => this.cargandoCajeros.set(false)
      });
    }
  }

  cancelar(): void {
    this.router.navigate(['/caja']);
  }

  submit(): void {
    if (this.form.invalid || this.guardando()) {
      return;
    }
    const raw = this.form.getRawValue();
    this.guardando.set(true);
    this.error.set('');
    this.cajaService
      .apertura(raw.saldoInicial, undefined, raw.cajeroId ?? undefined)
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