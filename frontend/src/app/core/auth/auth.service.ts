import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { LoginResponse, UserSession } from '../models/auth.model';
import { TokenStorageService } from './token-storage.service';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly user = signal<UserSession | null>(null);

  readonly currentUser = this.user.asReadonly();

  constructor(
    private http: HttpClient,
    private storage: TokenStorageService,
    private router: Router
  ) {
    this.user.set(this.storage.getUser());
  }

  isAuthenticated(): boolean {
    return this.user() !== null;
  }

  hasPermiso(permiso: string): boolean {
    return this.user()?.permisos.includes(permiso) ?? false;
  }

  hasRol(...roles: string[]): boolean {
    const userRoles = this.user()?.roles ?? [];
    return roles.some((r) => userRoles.includes(r));
  }

  login(username: string, password: string): Observable<LoginResponse> {
    return this.http
      .post<LoginResponse>(`${environment.apiUrl}/auth/login`, { username, password })
      .pipe(
        tap((res) => {
          this.storage.token = res.token;
          const session: UserSession = {
            usuarioId: res.usuarioId,
            username: res.username,
            nombreCompleto: res.nombreCompleto,
            roles: res.roles ?? [],
            permisos: res.permisos ?? []
          };
          this.storage.saveSession(session);
          this.user.set(session);
        })
      );
  }

  refresh(): Observable<LoginResponse> {
    const token = this.storage.token;
    return this.http
      .post<LoginResponse>(`${environment.apiUrl}/auth/refresh`, null, {
        headers: token ? { Authorization: `Bearer ${token}` } : {}
      })
      .pipe(
        tap((res) => {
          this.storage.token = res.token;
          const session: UserSession = {
            usuarioId: res.usuarioId,
            username: res.username,
            nombreCompleto: res.nombreCompleto,
            roles: res.roles ?? [],
            permisos: res.permisos ?? []
          };
          this.storage.saveSession(session);
          this.user.set(session);
        })
      );
  }

  logout(): void {
    const token = this.storage.token;
    if (token) {
      this.http
        .post<void>(`${environment.apiUrl}/auth/logout`, null)
        .subscribe({ error: () => undefined });
    }
    this.clearSession();
  }

  clearSession(): void {
    this.storage.clear();
    this.user.set(null);
    this.router.navigate(['/login']);
  }
}
