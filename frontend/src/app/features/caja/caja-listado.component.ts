import { Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Router } from '@angular/router';
import { ApiError } from '../../core/models/auth.model';
import { CajaApertura } from '../../core/models/caja.model';
import { AuthService } from '../../core/auth/auth.service';
import { CajaService } from '../../core/services/caja.service';
import { ToastService } from '../../core/services/toast.service';
import { SortState } from '../../core/models/paginado.model';
import { PaginadorComponent } from '../../shared/components/paginador/paginador.component';
import { SortableHeaderDirective } from '../../shared/components/sortable-header/sortable-header.directive';

const ESTADOS = ['', 'ABIERTA', 'CERRADA'] as const;

@Component({
  selector: 'app-caja-listado',
  imports: [DatePipe, DecimalPipe, PaginadorComponent, SortableHeaderDirective],
  templateUrl: './caja-listado.html',
  styleUrl: './caja-listado.css'
})
export class CajaListadoComponent implements OnInit {
  private readonly cajaService = inject(CajaService);
  private readonly auth = inject(AuthService);
  private readonly toast = inject(ToastService);
  private readonly router = inject(Router);

  protected readonly estados = ESTADOS;
  protected readonly estado = signal('');
  protected readonly cajas = signal<CajaApertura[]>([]);
  protected readonly cargando = signal(false);
  protected readonly error = signal('');

  protected readonly page = signal(0);
  protected readonly size = signal(10);
  protected readonly sort = signal<SortState | null>(null);
  protected readonly totalElements = signal(0);
  protected readonly totalPages = signal(0);

  protected readonly puedeCrear = this.auth.hasPermiso;

  ngOnInit(): void {
    this.cargar();
  }

  filtrarPorEstados(estado: string): void {
    this.estado.set(estado);
    this.page.set(0);
    this.cargar();
  }

  cargar(): void {
    this.cargando.set(true);
    this.error.set('');
    this.cajaService.listado(this.estado() || undefined, {
      page: this.page(),
      size: this.size(),
      sort: this.sort() ? `${this.sort()!.key},${this.sort()!.dir}` : undefined
    }).subscribe({
      next: (paginated) => {
        this.cajas.set(paginated.content);
        this.totalElements.set(paginated.totalElements);
        this.totalPages.set(paginated.totalPages);
        this.cargando.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.error.set((err.error as ApiError | undefined)?.message ?? 'No se pudieron cargar las cajas.');
        this.cargando.set(false);
      }
    });
  }

  cambiarPagina(p: number): void {
    this.page.set(p);
    this.cargar();
  }

  cambiarTamano(t: number): void {
    this.size.set(t);
    this.page.set(0);
    this.cargar();
  }

  ordenar(s: SortState): void {
    this.sort.set(s);
    this.page.set(0);
    this.cargar();
  }

  irOperaciones(): void {
    this.router.navigate(['/caja']);
  }

  abrirCaja(): void {
    if (this.cajas().some((c) => c.estado === 'ABIERTA')) {
      this.toast.warning('Ya existe una caja ABIERTA; cierre la caja actual antes de abrir otra.');
      return;
    }
    this.router.navigate(['/caja/nuevo']);
  }
}