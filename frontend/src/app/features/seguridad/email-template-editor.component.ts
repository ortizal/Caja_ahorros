import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { finalize } from 'rxjs';
import { EmailConfigService } from '../../core/services/email-config.service';
import { ToastService } from '../../core/services/toast.service';
import { EmailPlantilla, MODULOS_EMAIL } from '../../core/models/email.model';

@Component({
  selector: 'app-email-template-editor',
  imports: [ReactiveFormsModule],
  templateUrl: './email-template-editor.component.html',
  styleUrl: './email-template-editor.component.css'
})
export class EmailTemplateEditorComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly emailConfigService = inject(EmailConfigService);
  private readonly toast = inject(ToastService);

  protected readonly cargando = signal(true);
  protected readonly guardando = signal(false);
  protected readonly plantillas = signal<EmailPlantilla[]>([]);
  protected readonly editando = signal(false);
  protected readonly plantillaActual = signal<EmailPlantilla | null>(null);
  protected readonly moduloFiltro = signal('');
  protected readonly preview = signal(false);
  protected readonly modulos = MODULOS_EMAIL;

  protected readonly form = this.fb.nonNullable.group({
    modulo: ['', Validators.required],
    nombre: ['', Validators.required],
    asunto: ['', Validators.required],
    cuerpoHtml: ['', Validators.required],
    variables: [''],
    activo: [true]
  });

  ngOnInit(): void {
    this.cargarPlantillas();
  }

  cargarPlantillas(): void {
    this.cargando.set(true);
    this.emailConfigService.listarPlantillas(this.moduloFiltro() || undefined).subscribe({
      next: (plantillas) => {
        this.plantillas.set(plantillas);
        this.cargando.set(false);
      },
      error: () => {
        this.plantillas.set([]);
        this.cargando.set(false);
      }
    });
  }

  nuevo(): void {
    this.form.reset({ modulo: 'general', nombre: '', asunto: '', cuerpoHtml: this.plantillaBaseHtml(), variables: '', activo: true });
    this.editando.set(true);
    this.plantillaActual.set(null);
    this.preview.set(false);
  }

  editar(p: EmailPlantilla): void {
    this.plantillaActual.set(p);
    this.form.patchValue({
      modulo: p.modulo,
      nombre: p.nombre,
      asunto: p.asunto,
      cuerpoHtml: p.cuerpoHtml,
      variables: p.variables ?? '',
      activo: p.activo
    });
    this.editando.set(true);
    this.preview.set(false);
  }

  cancelar(): void {
    this.editando.set(false);
    this.plantillaActual.set(null);
    this.preview.set(false);
  }

  guardar(): void {
    if (this.form.invalid || this.guardando()) return;
    this.guardando.set(true);
    const req = this.form.getRawValue();
    const obs = this.plantillaActual()
      ? this.emailConfigService.actualizarPlantilla(this.plantillaActual()!.id, req)
      : this.emailConfigService.crearPlantilla(req);
    obs.pipe(finalize(() => this.guardando.set(false))).subscribe({
      next: () => {
        this.toast.success(this.plantillaActual() ? 'Plantilla actualizada.' : 'Plantilla creada.');
        this.cancelar();
        this.cargarPlantillas();
      },
      error: (err: HttpErrorResponse) => {
        this.toast.error(err.error?.message ?? 'Error al guardar plantilla.');
      }
    });
  }

  toggleActivo(p: EmailPlantilla): void {
    this.emailConfigService.togglePlantilla(p.id).subscribe({
      next: () => {
        this.toast.success(`Plantilla ${p.activo ? 'desactivada' : 'activada'}.`);
        this.cargarPlantillas();
      },
      error: () => this.toast.error('Error al cambiar estado.')
    });
  }

  eliminar(p: EmailPlantilla): void {
    if (!confirm(`¿Eliminar la plantilla "${p.nombre}"?`)) return;
    this.emailConfigService.eliminarPlantilla(p.id).subscribe({
      next: () => {
        this.toast.success('Plantilla eliminada.');
        if (this.plantillaActual()?.id === p.id) this.cancelar();
        this.cargarPlantillas();
      },
      error: () => this.toast.error('Error al eliminar plantilla.')
    });
  }

  togglePreview(): void {
    this.preview.set(!this.preview());
  }

  VariablesHelper(): string {
    const raw = this.form.get('variables')?.value;
    if (!raw) return '';
    return raw.split(',').map(v => `{{${v.trim()}}}`).join(', ');
  }

  private plantillaBaseHtml(): string {
    return `<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
</head>
<body style="margin: 0; padding: 0; background-color: #f4f4f4; font-family: Arial, sans-serif;">
  <table width="100%" cellpadding="0" cellspacing="0" style="background-color: #f4f4f4; padding: 20px 0;">
    <tr>
      <td align="center">
        <table width="600" cellpadding="0" cellspacing="0" style="background-color: #ffffff; border-radius: 4px; overflow: hidden;">
          <tr>
            <td style="background-color: #1a237e; color: #ffffff; padding: 20px; text-align: center;">
              <h1 style="margin: 0; font-size: 22px;">Caja de Ahorros ALANTEK</h1>
            </td>
          </tr>
          <tr>
            <td style="padding: 24px; color: #333333;">
              <h2 style="margin-top: 0;">{{titulo}}</h2>
              <p>{{mensaje}}</p>
            </td>
          </tr>
          <tr>
            <td style="background-color: #f8f9fa; color: #999999; padding: 12px 24px; font-size: 11px; text-align: center;">
              Este es un correo automático, por favor no responder.
            </td>
          </tr>
        </table>
      </td>
    </tr>
  </table>
</body>
</html>`;
  }
}
