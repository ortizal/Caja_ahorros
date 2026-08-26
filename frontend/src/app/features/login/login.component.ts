import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { finalize } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { HttpErrorResponse } from '@angular/common/http';
import { ApiError } from '../../core/models/auth.model';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule],
  templateUrl: './login.html',
  styleUrl: './login.css'
})
export class LoginComponent {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly form = this.fb.nonNullable.group({
    username: ['', [Validators.required]],
    password: ['', [Validators.required]]
  });
  protected readonly loading = signal(false);
  protected readonly error = signal('');
  protected readonly showPassword = signal(false);

  submit(): void {
    if (this.form.invalid || this.loading()) {
      return;
    }
    this.error.set('');
    this.loading.set(true);
    this.auth
      .login(this.form.getRawValue().username, this.form.getRawValue().password)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: () => this.router.navigate([this.auth.hasRol('SOCIO') ? '/portal' : '/dashboard']),
        error: (err: HttpErrorResponse) => {
          const body = err.error as ApiError | undefined;
          this.error.set(body?.message ?? 'No se pudo iniciar sesión. Verifique sus credenciales.');
        }
      });
  }
}
