import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { ApiError } from '../../core/models/auth.model';
import { CuentaBancaria } from '../../core/models/banco.model';
import { BancoService } from '../../core/services/banco.service';
import { ToastService } from '../../core/services/toast.service';
import { AccionesMenuComponent } from '../../shared/components/acciones-menu/acciones-menu.component';
import { SortState } from '../../core/models/paginado.model';
import { PaginadorComponent } from '../../shared/components/paginador/paginador.component';
import { SortableHeaderDirective } from '../../shared/components/sortable-header/sortable-header.directive';

@Component({
  selector: 'app-cuentas-bancarias',
  imports: [RouterLink, DecimalPipe, AccionesMenuComponent, PaginadorComponent, SortableHeaderDirective],
  templateUrl: './cuentas-bancarias.html',
  styleUrl: './cuentas-bancarias.css'
})
export class CuentasBancariasComponent implements OnInit {
  private readonly bancoService = inject(BancoService);
  private readonly toast = inject(ToastService);
  private readonly auth = inject(AuthService);

  protected readonly cuentas = signal<CuentaBancaria[]>([]);
  protected readonly error = signal('');
  protected readonly cargando = signal(false);
  protected readonly puedeCrear = computed(() => this.auth.hasPermiso('BANCOS:CREAR'));

  protected readonly page = signal(0);
  protected readonly size = signal(10);
  protected readonly sort = signal<SortState | null>(null);
  protected readonly totalElements = signal(0);
  protected readonly totalPages = signal(0);

  ngOnInit(): void {
    this.cargarCuentas();
  }

  cargarCuentas(): void {
    this.cargando.set(true);
    this.error.set('');
    this.bancoService.cuentas({
      page: this.page(),
      size: this.size(),
      sort: this.sort() ? `${this.sort()!.key},${this.sort()!.dir}` : undefined
    }).subscribe({
      next: (paginated) => {
        this.cuentas.set(paginated.content);
        this.totalElements.set(paginated.totalElements);
        this.totalPages.set(paginated.totalPages);
        this.cargando.set(false);
      },
      error: (err: HttpErrorResponse) => {
        const msg = (err.error as ApiError | undefined)?.message ?? 'No se pudieron cargar las cuentas.';
        this.error.set(msg);
        this.toast.error(msg);
        this.cargando.set(false);
      }
    });
  }

  cambiarPagina(p: number): void {
    this.page.set(p);
    this.cargarCuentas();
  }

  cambiarTamano(t: number): void {
    this.size.set(t);
    this.page.set(0);
    this.cargarCuentas();
  }

  ordenar(s: SortState): void {
    this.sort.set(s);
    this.page.set(0);
    this.cargarCuentas();
  }
}
