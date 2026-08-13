import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { environment } from '../../../environments/environment';
import { AuthService } from './auth.service';
import { TokenStorageService } from './token-storage.service';

describe('AuthService', () => {
  let service: AuthService;
  let storage: TokenStorageService;
  let httpMock: HttpTestingController;
  const routerSpy = { navigate: vi.fn() };

  beforeEach(() => {
    localStorage.clear();

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: Router, useValue: routerSpy }
      ]
    });

    service = TestBed.inject(AuthService);
    storage = TestBed.inject(TokenStorageService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  it('login guarda token y sesión', () => {
    service
      .login('admin', 'admin123')
      .subscribe((res) => {
        expect(res.token).toBe('jwt-fake');
        expect(service.isAuthenticated()).toBeTruthy();
        expect(service.hasPermiso('SOCIOS:VER')).toBeTruthy();
      });

    const req = httpMock.expectOne(`${environment.apiUrl}/auth/login`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ username: 'admin', password: 'admin123' });
    req.flush({
      token: 'jwt-fake',
      usuarioId: 1,
      username: 'admin',
      nombreCompleto: 'Administrador',
      roles: ['ADMIN'],
      permisos: ['SOCIOS:VER', 'SEGURIDAD:VER']
    });

    expect(storage.token).toBe('jwt-fake');
    expect(service.currentUser()?.username).toBe('admin');
  });

  it('login con error no marca sesión', () => {
    let failed = false;
    service.login('admin', 'incorrecta').subscribe({
      error: () => (failed = true)
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/auth/login`);
    req.flush({ message: 'Credenciales inválidas' }, { status: 401, statusText: 'Unauthorized' });

    expect(failed).toBeTruthy();
    expect(service.isAuthenticated()).toBeFalsy();
  });

  it('logout limpia sesión y navega a login', () => {
    storage.token = 'jwt-fake';
    storage.saveSession({
      usuarioId: 1,
      username: 'admin',
      nombreCompleto: 'Administrador',
      roles: ['ADMIN'],
      permisos: []
    });
    service = crearServicioNuevo();
    expect(service.isAuthenticated()).toBeTruthy();

    service.logout();

    const req = httpMock.expectOne(`${environment.apiUrl}/auth/logout`);
    req.flush(null);
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/login']);
    expect(service.isAuthenticated()).toBeFalsy();
    expect(storage.token).toBeNull();
  });

  it('clearSession limpia sin llamar logout', () => {
    storage.saveSession({
      usuarioId: 1,
      username: 'admin',
      nombreCompleto: 'Administrador',
      roles: ['ADMIN'],
      permisos: []
    });
    service = crearServicioNuevo();

    service.clearSession();

    expect(routerSpy.navigate).toHaveBeenCalledWith(['/login']);
    expect(service.isAuthenticated()).toBeFalsy();
  });

  function crearServicioNuevo(): AuthService {
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: Router, useValue: routerSpy }
      ]
    });
    storage = TestBed.inject(TokenStorageService);
    httpMock = TestBed.inject(HttpTestingController);
    return TestBed.inject(AuthService);
  }
});
