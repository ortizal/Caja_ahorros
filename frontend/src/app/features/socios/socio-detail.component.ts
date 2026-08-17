import { Component, inject, input, output, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { AuthService } from '../../core/auth/auth.service';
import { ApiError } from '../../core/models/auth.model';
import { ESTADOS_SOCIO, Socio } from '../../core/models/socio.model';
import { SocioService } from '../../core/services/socio.service';
import { ToastService } from '../../core/services/toast.service';

@Component({
  selector: 'app-socio-detalle',
  imports: [],
  templateUrl: './socio-detail.html',
  styleUrl: './socio-detail.css'
})
export class SocioDetalleComponent {
  readonly socio = input.required<Socio>();
  readonly estadoActualizado = output<Socio>();

  protected readonly estados = ESTADOS_SOCIO;
  protected readonly error = signal('');
  protected readonly cambiandoEstado = signal(false);

  private readonly auth = inject(AuthService);
  private readonly socioService = inject(SocioService);
  private readonly toast = inject(ToastService);

  puedeEditar(): boolean {
    return this.auth.hasPermiso('SOCIOS:EDITAR');
  }

  cambiarEstado(estado: string): void {
    const socio = this.socio();
    if (!socio || this.cambiandoEstado() || socio.estado === estado) {
      return;
    }
    this.cambiandoEstado.set(true);
    this.error.set('');
    this.socioService.cambiarEstado(socio.id, estado).subscribe({
      next: () => {
        this.socioService.obtener(socio.id).subscribe({
          next: (actualizado) => {
            this.cambiandoEstado.set(false);
            this.estadoActualizado.emit(actualizado);
            this.toast.success(`Estado del socio actualizado a ${estado}.`);
          },
          error: () => this.cambiandoEstado.set(false)
        });
      },
      error: (err: HttpErrorResponse) => {
        this.cambiandoEstado.set(false);
        const msg = (err.error as ApiError | undefined)?.message ?? 'No se pudo cambiar el estado.';
        this.error.set(msg);
        this.toast.error(msg);
      }
    });
  }
}
