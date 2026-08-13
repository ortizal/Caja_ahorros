import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { AuthService } from '../../core/auth/auth.service';
import { ApiError } from '../../core/models/auth.model';
import { ESTADOS_SOCIO, Socio } from '../../core/models/socio.model';
import { SocioService } from '../../core/services/socio.service';

@Component({
  selector: 'app-socio-detail',
  imports: [RouterLink],
  templateUrl: './socio-detail.html',
  styleUrl: './socio-detail.css'
})
export class SocioDetailComponent implements OnInit {
  protected readonly socio = signal<Socio | null>(null);
  protected readonly error = signal('');
  protected readonly estados = ESTADOS_SOCIO;
  protected readonly cambiandoEstado = signal(false);

  private readonly auth = inject(AuthService);

  constructor(
    private readonly route: ActivatedRoute,
    private readonly socioService: SocioService
  ) {}

  puedeEditar(): boolean {
    return this.auth.hasPermiso('SOCIOS:EDITAR');
  }

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.socioService.obtener(id).subscribe({
      next: (s) => this.socio.set(s),
      error: (err: HttpErrorResponse) =>
        this.error.set((err.error as ApiError | undefined)?.message ?? 'No se pudo cargar el socio.')
    });
  }

  cambiarEstado(estado: string): void {
    const socio = this.socio();
    if (!socio || this.cambiandoEstado() || socio.estado === estado) {
      return;
    }
    this.cambiandoEstado.set(true);
    this.socioService.cambiarEstado(socio.id, estado).subscribe({
      next: () => {
        this.cambiandoEstado.set(false);
        this.cargar();
      },
      error: (err: HttpErrorResponse) => {
        this.cambiandoEstado.set(false);
        this.error.set((err.error as ApiError | undefined)?.message ?? 'No se pudo cambiar el estado.');
      }
    });
  }
}
