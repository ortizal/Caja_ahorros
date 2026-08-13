import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { AuthService } from '../../core/auth/auth.service';
import { Socio } from '../../core/models/socio.model';
import { SocioService } from '../../core/services/socio.service';
import { ApiError } from '../../core/models/auth.model';

@Component({
  selector: 'app-socio-list',
  imports: [RouterLink],
  templateUrl: './socio-list.html',
  styleUrl: './socio-list.css'
})
export class SocioListComponent implements OnInit {
  private readonly socioService = inject(SocioService);
  private readonly auth = inject(AuthService);

  protected readonly socios = signal<Socio[]>([]);
  protected readonly loading = signal(true);
  protected readonly error = signal('');

  protected puedeCrear(): boolean {
    return this.auth.hasPermiso('SOCIOS:CREAR');
  }

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.loading.set(true);
    this.error.set('');
    this.socioService.listar().subscribe({
      next: (data) => {
        this.socios.set(data);
        this.loading.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.error.set((err.error as ApiError | undefined)?.message ?? 'No se pudieron cargar los socios.');
        this.loading.set(false);
      }
    });
  }
}
