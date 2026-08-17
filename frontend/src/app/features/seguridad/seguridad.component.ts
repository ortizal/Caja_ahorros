import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { finalize } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { ApiError } from '../../core/models/auth.model';
import { Auditoria, Permiso, Rol, Usuario } from '../../core/models/seguridad.model';
import { SeguridadService } from '../../core/services/seguridad.service';
import { ToastService } from '../../core/services/toast.service';
import { ModalComponent, ModalFooterDirective } from '../../shared/components/modal/modal.component';
import { AccionesMenuComponent } from '../../shared/components/acciones-menu/acciones-menu.component';
import { UsuarioDetalleComponent } from './usuario-detalle.component';

@Component({
  selector: 'app-seguridad',
  imports: [ReactiveFormsModule, DatePipe, RouterLink, AccionesMenuComponent, ModalComponent, ModalFooterDirective, UsuarioDetalleComponent],
  templateUrl: './seguridad.html',
  styleUrl: './seguridad.css'
})
export class SeguridadComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly seguridadService = inject(SeguridadService);
  private readonly toast = inject(ToastService);
  private readonly auth = inject(AuthService);

  protected readonly puedeCrear = computed(() => this.auth.hasPermiso('SEGURIDAD:CREAR'));
  protected readonly puedeEditar = computed(() => this.auth.hasPermiso('SEGURIDAD:EDITAR'));

  protected readonly tab = signal<string>('usuarios');
  protected readonly usuarios = signal<Usuario[]>([]);
  protected readonly roles = signal<Rol[]>([]);
  protected readonly permisos = signal<Permiso[]>([]);
  protected readonly auditoria = signal<Auditoria[]>([]);
  protected readonly error = signal('');
  protected readonly cargando = signal(false);
  protected readonly guardando = signal(false);

  protected readonly detalle = signal<Usuario | null>(null);
  protected readonly infoAbierto = signal(false);

  protected readonly rolEditando = signal<Rol | null>(null);
  protected readonly rolPermisosAbierto = signal(false);
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
      error: () => {
        this.usuarios.set([]);
        this.error.set('No se pudieron cargar los usuarios.');
      }
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

  ver(usuario: Usuario): void {
    this.detalle.set(usuario);
    this.infoAbierto.set(true);
  }

  cerrarInfo(): void {
    this.infoAbierto.set(false);
    this.detalle.set(null);
  }

  permisoSeleccionadosParaRol(rol: Rol): void {
    this.rolEditando.set(rol);
    this.permisoSeleccionados.set(rol.permisos.map((p) => p.id));
    this.rolPermisosAbierto.set(true);
  }

  cerrarPermisos(): void {
    this.rolPermisosAbierto.set(false);
    this.rolEditando.set(null);
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
    this.seguridadService.asignarPermisos(rol.id, this.permisoSeleccionados())
      .pipe(finalize(() => this.guardando.set(false)))
      .subscribe({
        next: (actualizado) => {
          this.toast.success(`Permisos de ${actualizado.nombre} actualizados.`);
          this.cargarRoles();
          this.cerrarPermisos();
        },
        error: (err: HttpErrorResponse) => {
          const msg = (err.error as ApiError | undefined)?.message ?? 'No se pudieron asignar los permisos.';
          this.error.set(msg);
          this.toast.error(msg);
        }
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
          this.toast.success(`Usuario ${nuevoEstado.toLowerCase()}.`);
          this.cargarUsuarios();
        },
        error: (err: HttpErrorResponse) => {
          const msg = (err.error as ApiError | undefined)?.message ?? 'No se pudo cambiar el estado.';
          this.error.set(msg);
          this.toast.error(msg);
        }
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
        error: (err: HttpErrorResponse) => {
          const msg = (err.error as ApiError | undefined)?.message ?? 'No se pudo consultar la auditoría.';
          this.error.set(msg);
          this.toast.error(msg);
        }
      });
  }
}
