import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { finalize } from 'rxjs';
import { EmailConfigService } from '../../core/services/email-config.service';
import { ToastService } from '../../core/services/toast.service';
import { EmailConfiguracion } from '../../core/models/email.model';

@Component({
  selector: 'app-email-config',
  imports: [ReactiveFormsModule],
  templateUrl: './email-config.component.html',
  styleUrl: './email-config.component.css'
})
export class EmailConfigComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly emailConfigService = inject(EmailConfigService);
  private readonly toast = inject(ToastService);

  protected readonly cargando = signal(true);
  protected readonly guardando = signal(false);
  protected readonly testEmail = signal('');
  protected readonly enviandoTest = signal(false);

  protected readonly config = signal<EmailConfiguracion | null>(null);

  protected readonly form = this.fb.nonNullable.group({
    metodo: ['SMTP', Validators.required],
    smtpHost: [''],
    smtpPort: [587],
    smtpUsername: [''],
    smtpPassword: [''],
    smtpUseTls: [true],
    smtpUseSsl: [false],
    apiUrl: [''],
    apiKey: [''],
    apiProvider: [''],
    fromEmail: ['', Validators.email],
    fromName: [''],
    activo: [true]
  });

  protected readonly showSmtp = signal(true);

  ngOnInit(): void {
    this.cargar();
    this.form.get('metodo')!.valueChanges.subscribe(m => {
      this.showSmtp.set(m !== 'API');
    });
  }

  cargar(): void {
    this.cargando.set(true);
    this.emailConfigService.obtenerConfiguracion().subscribe({
      next: (cfg) => {
        if (cfg) {
          this.config.set(cfg);
          this.form.patchValue({
            metodo: cfg.metodo,
            smtpHost: cfg.smtpHost ?? '',
            smtpPort: cfg.smtpPort ?? 587,
            smtpUsername: cfg.smtpUsername ?? '',
            smtpUseTls: cfg.smtpUseTls,
            smtpUseSsl: cfg.smtpUseSsl,
            apiUrl: cfg.apiUrl ?? '',
            apiProvider: cfg.apiProvider ?? '',
            fromEmail: cfg.fromEmail ?? '',
            fromName: cfg.fromName ?? '',
            activo: cfg.activo
          });
          this.showSmtp.set(cfg.metodo !== 'API');
        }
        this.cargando.set(false);
      },
      error: () => this.cargando.set(false)
    });
  }

  guardar(): void {
    if (this.form.invalid || this.guardando()) return;
    this.guardando.set(true);
    this.emailConfigService.guardarConfiguracion(this.form.getRawValue()).subscribe({
      next: (cfg) => {
        this.config.set(cfg);
        this.toast.success('Configuración de email guardada.');
        this.guardando.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.toast.error(err.error?.message ?? 'Error al guardar configuración.');
        this.guardando.set(false);
      }
    });
  }

  enviarTest(): void {
    const to = this.testEmail();
    if (!to || this.enviandoTest()) return;
    this.enviandoTest.set(true);
    this.emailConfigService.testEmail(to).subscribe({
      next: (res) => {
        this.toast.success(res.message);
        this.enviandoTest.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.toast.error(err.error?.message ?? 'Error al enviar correo de prueba.');
        this.enviandoTest.set(false);
      }
    });
  }
}
