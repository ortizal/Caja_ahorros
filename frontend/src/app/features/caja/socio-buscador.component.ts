import { Component, effect, inject, input, output, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { finalize } from 'rxjs';
import { ApiError } from '../../core/models/auth.model';
import { Socio } from '../../core/models/socio.model';
import { SocioService } from '../../core/services/socio.service';
import { ModalComponent, ModalFooterDirective } from '../../shared/components/modal/modal.component';

@Component({
  selector: 'app-socio-buscador',
  imports: [ModalComponent, ModalFooterDirective],
  templateUrl: './socio-buscador.html',
  styleUrl: './socio-buscador.css'
})
export class SocioBuscadorComponent {
  readonly abierto = input(false);
  readonly seleccionar = output<Socio>();
  readonly cerrar = output<void>();

  private readonly socioService = inject(SocioService);

  protected readonly q = signal('');
  protected readonly resultados = signal<Socio[]>([]);
  protected readonly buscando = signal(false);
  protected readonly error = signal('');

  constructor() {
    effect(() => {
      if (this.abierto()) {
        this.q.set('');
        this.resultados.set([]);
        this.error.set('');
      }
    });
  }

  buscar(): void {
    const termino = this.q().trim();
    if (!termino || this.buscando()) {
      return;
    }
    this.buscando.set(true);
    this.error.set('');
    this.socioService.listar({ q: termino, size: 25 }).pipe(finalize(() => this.buscando.set(false))).subscribe({
      next: (p) => this.resultados.set(p.content),
      error: (err: HttpErrorResponse) => {
        this.resultados.set([]);
        this.error.set((err.error as ApiError | undefined)?.message ?? 'No se pudo buscar el socio.');
      }
    });
  }

  elegir(socio: Socio): void {
    this.seleccionar.emit(socio);
  }
}