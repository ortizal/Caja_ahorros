import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { finalize } from 'rxjs';
import { ApiError } from '../../core/models/auth.model';
import { Rol, UsuarioRequest } from '../../core/models/seguridad.model';
import { SeguridadService } from '../../core/services/seguridad.service';
import { ToastService } from '../../core/services/toast.service';

@Component({
  selector: 'app-usuario-form',
  imports: [ReactiveFormsModule],
  templateUrl: './usuario-form.html',
  styleUrl: './usuario-form.css'
})
export class UsuarioFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly seguridadService = inject(SeguridadService);
  private readonly toast = inject(ToastService);

  protected readonly form = this.fb.nonNullable.group({
    username: ['', [Validators.required]],
    password: [''],
    nombreCompleto: ['', [Validators.required]],
    email: ['']
  });
  protected readonly roles = signal<Rol[]>([]);
  protected readonly rolSeleccionados = signal<number[]>([]);
  protected readonly esEdicion = signal(false);
  protected readonly loading = signal(false);
  protected readonly guardando = signal(false);
  protected readonly error = signal('');
  private usuarioId: number | null = null;

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.esEdicion.set(true);
      this.usuarioId = Number(id);
    }
    this.seguridadService.roles().subscribe({
      next: (roles) => {
        this.roles.set(roles);
        if (this.esEdicion()) {
          this.cargarUsuario();
        }
      },
      error: () => {
        this.error.set('No se pudieron cargar los roles.');
        this.toast.error('No se pudieron cargar los roles.');
      }
    });
  }

  private cargarUsuario(): void {
    if (this.usuarioId === null) {
      return;
    }
    this.loading.set(true);
    this.seguridadService.usuarios().subscribe({
      next: (lista) => {
        const usuario = lista.find((u) => u.id === this.usuarioId);
        if (!usuario) {
          this.error.set('No se encontró el usuario.');
          this.loading.set(false);
          return;
        }
        this.form.patchValue({
          username: usuario.username,
          password: '',
          nombreCompleto: usuario.nombreCompleto,
          email: usuario.email ?? ''
        });
        this.rolSeleccionados.set(
          this.roles().filter((rol) => usuario.roles.includes(rol.nombre)).map((rol) => rol.id)
        );
        this.loading.set(false);
      },
      error: () => {
        this.error.set('No se pudo cargar el usuario.');
        this.toast.error('No se pudo cargar el usuario.');
        this.loading.set(false);
      }
    });
  }

  toggleRol(id: number): void {
    this.rolSeleccionados.update((actual) =>
      actual.includes(id) ? actual.filter((x) => x !== id) : [...actual, id]
    );
  }

  cancelar(): void {
    this.router.navigate(['/seguridad']);
  }

  submit(): void {
    if (this.form.invalid || this.guardando()) {
      return;
    }
    const raw = this.form.getRawValue();
    const request: UsuarioRequest = {
      username: raw.username,
      password: raw.password || undefined,
      nombreCompleto: raw.nombreCompleto,
      email: raw.email || undefined,
      rolIds: this.rolSeleccionados()
    };

    this.guardando.set(true);
    this.error.set('');
    const llamada =
      this.usuarioId !== null
        ? this.seguridadService.actualizarUsuario(this.usuarioId, request)
        : this.seguridadService.crearUsuario(request);

    llamada.pipe(finalize(() => this.guardando.set(false))).subscribe({
      next: () => {
        this.toast.success(this.esEdicion() ? 'Usuario actualizado correctamente.' : 'Usuario guardado correctamente.');
        this.router.navigate(['/seguridad']);
      },
      error: (err: HttpErrorResponse) => {
        const msg = (err.error as ApiError | undefined)?.message ?? 'No se pudo guardar el usuario.';
        this.error.set(msg);
        this.toast.error(msg);
      }
    });
  }
}
