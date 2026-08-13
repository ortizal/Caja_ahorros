import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { finalize } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { ApiError } from '../../core/models/auth.model';
import { Auditoria, Permiso, Rol, Usuario } from '../../core/models/seguridad.model';
import { SeguridadService } from '../../core/services/seguridad.service';

@Component({
  selector: 'app-seguridad',
  imports: [ReactiveFormsModule, DatePipe],
  templateUrl: './seguridad.html',
  styleUrl: './seguridad.css'
})
export class SeguridadComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly seguridadService = inject(SeguridadService);
  private readonly auth = inject(AuthService);

  protected readonly puedeCrear = computed(() => this.auth.hasPermiso('SEGURIDAD:CREAR'));
  protected readonly puedeEditar = computed(() => this.auth.hasPermiso('SEGURIDAD:EDITAR'));

  protected readonly tab = signal<string>('usuarios');
  protected readonly usuarios = signal<Usuario[]>([]);
  protected readonly roles = signal<Rol[]>([]);
  protected readonly permisos = signal<Permiso[]>([]);
  protected readonly auditoria = signal<Auditoria[]>([]);
  protected readonly error = signal('');
  protected readonly ok = signal('');
  protected readonly cargando = signal(false);
  protected readonly guardando = signal(false);

  protected readonly editandoId = signal<number | null>(null);
  protected readonly usuarioForm = this.fb.nonNullable.group({
    username: ['', [Validators.required]],
    password: [''],
    nombreCompleto: ['', [Validators.required]],
    email: ['']
  });
  protected readonly rolSeleccionados = signal<number[]>([]);

  protected readonly rolEditando = signal<Rol | null>(null);
  protected readonly permisoSeleccionados = signal<number[]>([]);
  protected readonly modulos = signal<string[]>([]);

  protected readonly auditoriaForm = this.fb.nonNullable.group({
    tabla: [''],
    desde: [''],
    hasta: ['']
  });

  ngOnInit(): void {
    this.cargarUsuarios();
    this.cargarRoles();
    this.cargarPermisos();
  }

  cargarUsuarios(): void {
    this.seguridadService.usuarios().subscribe({
      next: (u) => this.usuarios.set(u),
      error: () => this.usuarios.set([])
    });
  }

  cargarRoles(): void {
    this.seguridadService.roles().subscribe({
      next: (r) => this.roles.set(r),
      error: () => this.roles.set([])
    });
  }

  cargarPermisos(): void {
    this.seguridadService.permisos().subscribe({
      next: (p) => {
        this.permisos.set(p);
        this.modulos.set([...new Set(p.map((x) => x.modulo))].sort());
      },
      error: () => this.permisos.set([])
    });
  }

  permisoSeleccionadosParaRol(rol: Rol): void {
    this.rolEditando.set(rol);
    this.permisoSeleccionados.set(rol.permisos.map((p) => p.id));
  }

  togglePermiso(id: number): void {
    this.permisoSeleccionados.update((actual) =>
      actual.includes(id) ? actual.filter((x) => x !== id) : [...actual, id]
    );
  }

  guardarPermisos(): void {
    const rol = this.rolEditando();
    if (!rol || this.guardando()) {
      return;
    }
    this.guardando.set(true);
    this.error.set('');
    this.ok.set('');
    this.seguridadService.asignarPermisos(rol.id, this.permisoSeleccionados())
      .pipe(finalize(() => this.guardando.set(false)))
      .subscribe({
        next: (actualizado) => {
          this.ok.set(`Permisos de ${actualizado.nombre} actualizados.`);
          this.cargarRoles();
        },
        error: (err: HttpErrorResponse) =>
          this.error.set((err.error as ApiError | undefined)?.message ?? 'No se pudieron asignar los permisos.')
      });
  }

  nuevoUsuario(): void {
    this.editandoId.set(null);
    this.usuarioForm.reset({ username: '', password: '', nombreCompleto: '', email: '' });
    this.rolSeleccionados.set([]);
  }

  editarUsuario(usuario: Usuario): void {
    this.editandoId.set(usuario.id);
    this.usuarioForm.patchValue({
      username: usuario.username,
      password: '',
      nombreCompleto: usuario.nombreCompleto,
      email: usuario.email ?? ''
    });
    const ids = this.roles()
      .filter((r) => usuario.roles.includes(r.nombre))
      .map((r) => r.id);
    this.rolSeleccionados.set(ids);
  }

  toggleRol(id: number): void {
    this.rolSeleccionados.update((actual) =>
      actual.includes(id) ? actual.filter((x) => x !== id) : [...actual, id]
    );
  }

  guardarUsuario(): void {
    if (this.usuarioForm.invalid || this.guardando()) {
      return;
    }
    const raw = this.usuarioForm.getRawValue();
    const request = {
      username: raw.username,
      password: raw.password || undefined,
      nombreCompleto: raw.nombreCompleto,
      email: raw.email || undefined,
      rolIds: this.rolSeleccionados()
    };

    this.guardando.set(true);
    this.error.set('');
    this.ok.set('');
    const id = this.editandoId();
    const llamada = id
      ? this.seguridadService.actualizarUsuario(id, request)
      : this.seguridadService.crearUsuario(request);
    llamada.pipe(finalize(() => this.guardando.set(false))).subscribe({
      next: () => {
        this.ok.set('Usuario guardado correctamente.');
        this.nuevoUsuario();
        this.cargarUsuarios();
      },
      error: (err: HttpErrorResponse) =>
        this.error.set((err.error as ApiError | undefined)?.message ?? 'No se pudo guardar el usuario.')
    });
  }

  cambiarEstado(usuario: Usuario): void {
    const nuevoEstado = usuario.estado === 'ACTIVO' ? 'INACTIVO' : 'ACTIVO';
    this.guardando.set(true);
    this.error.set('');
    this.seguridadService.cambiarEstado(usuario.id, nuevoEstado)
      .pipe(finalize(() => this.guardando.set(false)))
      .subscribe({
        next: () => {
          this.ok.set(`Usuario ${nuevoEstado.toLowerCase()}.`);
          this.cargarUsuarios();
        },
        error: (err: HttpErrorResponse) =>
          this.error.set((err.error as ApiError | undefined)?.message ?? 'No se pudo cambiar el estado.')
      });
  }

  consultarAuditoria(): void {
    const raw = this.auditoriaForm.getRawValue();
    this.cargando.set(true);
    this.error.set('');
    this.seguridadService
      .auditoria(raw.tabla || undefined, raw.desde ? `${raw.desde}T00:00:00Z` : undefined, raw.hasta ? `${raw.hasta}T23:59:59Z` : undefined)
      .pipe(finalize(() => this.cargando.set(false)))
      .subscribe({
        next: (a) => this.auditoria.set(a),
        error: (err: HttpErrorResponse) =>
          this.error.set((err.error as ApiError | undefined)?.message ?? 'No se pudo consultar la auditoría.')
      });
  }
}
