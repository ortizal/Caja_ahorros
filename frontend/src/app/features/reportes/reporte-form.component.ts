import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { finalize } from 'rxjs';
import { Reporte, ENTIDADES } from '../../core/models/reporte.model';
import { ReporteAdminService } from '../../core/services/reporte-admin.service';
import { ToastService } from '../../core/services/toast.service';

@Component({
  selector: 'app-reporte-form',
  imports: [ReactiveFormsModule],
  templateUrl: './reporte-form.html',
  styleUrl: './reporte-form.css'
})
export class ReporteFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly reporteService = inject(ReporteAdminService);
  private readonly toast = inject(ToastService);

  protected readonly form = this.fb.nonNullable.group({
    nombre: ['', [Validators.required, Validators.pattern(/^[a-z0-9_-]+$/)]],
    descripcion: [''],
    titulo: ['', [Validators.required]],
    entidad: ['GENERAL', [Validators.required]],
    formatoDefault: ['pdf', [Validators.required]],
    orientacion: ['portrait', [Validators.required]],
    jrxml: ['', [Validators.required]]
  });

  protected readonly entidades = ENTIDADES;
  protected readonly esEdicion = signal(false);
  protected readonly loading = signal(false);
  protected readonly guardando = signal(false);
  protected readonly error = signal('');
  protected readonly compilacionOk = signal<boolean | null>(null);
  protected readonly fileName = signal('');
  private reporteId: number | null = null;

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.esEdicion.set(true);
      this.reporteId = Number(id);
      this.loading.set(true);
      this.reporteService.obtener(this.reporteId).subscribe({
        next: (reporte) => {
          this.form.patchValue({
            nombre: reporte.nombre,
            descripcion: reporte.descripcion || '',
            titulo: reporte.titulo,
            entidad: reporte.entidad,
            formatoDefault: reporte.formatoDefault,
            orientacion: reporte.orientacion,
            jrxml: reporte.jrxml
          });
          this.loading.set(false);
        },
        error: () => {
          this.error.set('Error al cargar el reporte');
          this.loading.set(false);
        }
      });
    }
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;
    this.fileName.set(file.name);
    const reader = new FileReader();
    reader.onload = () => {
      this.form.patchValue({ jrxml: reader.result as string });
    };
    reader.readAsText(file);
    input.value = '';
  }

  onSubmit(): void {
    if (this.form.invalid) return;
    this.guardando.set(true);
    this.error.set('');

    const valores = this.form.getRawValue();
    const reporte: Reporte = {
      nombre: valores.nombre,
      descripcion: valores.descripcion || undefined,
      titulo: valores.titulo,
      entidad: valores.entidad,
      formatoDefault: valores.formatoDefault,
      orientacion: valores.orientacion,
      jrxml: valores.jrxml,
      activo: true
    };

    const request$ = this.esEdicion() && this.reporteId
      ? this.reporteService.actualizar(this.reporteId, reporte)
      : this.reporteService.crear(reporte);

    request$.pipe(finalize(() => this.guardando.set(false))).subscribe({
      next: () => {
        this.toast.success(this.esEdicion() ? 'Reporte actualizado' : 'Reporte creado');
        this.router.navigate(['/reportes']);
      },
      error: (err) => {
        const msg = err.error?.message || err.error?.error || 'Error al guardar reporte';
        this.error.set(msg);
        this.toast.error(msg);
      }
    });
  }

  cancelar(): void {
    this.router.navigate(['/reportes']);
  }
}
