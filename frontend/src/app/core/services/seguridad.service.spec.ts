import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { environment } from '../../../environments/environment';
import { SeguridadService } from './seguridad.service';

describe('SeguridadService', () => {
  let service: SeguridadService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(SeguridadService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('usuarios hace GET a /usuarios', () => {
    service.usuarios().subscribe((res) => {
      expect(res[0].username).toBe('admin');
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/usuarios`);
    expect(req.request.method).toBe('GET');
    req.flush([{ id: 1, username: 'admin', nombreCompleto: 'Administrador', email: null, estado: 'ACTIVO', roles: ['ADMIN'], ultimoAcceso: null, createdAt: '2026-08-01T00:00:00Z' }]);
  });

  it('crearUsuario hace POST con cuerpo', () => {
    service
      .crearUsuario({ username: 'nuevo', password: 'clave123', nombreCompleto: 'Nuevo Usuario', rolIds: [2] })
      .subscribe((res) => {
        expect(res.username).toBe('nuevo');
      });

    const req = httpMock.expectOne(`${environment.apiUrl}/usuarios`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ username: 'nuevo', password: 'clave123', nombreCompleto: 'Nuevo Usuario', rolIds: [2] });
    req.flush({ id: 2, username: 'nuevo', nombreCompleto: 'Nuevo Usuario', email: null, estado: 'ACTIVO', roles: [], ultimoAcceso: null, createdAt: '2026-08-01T00:00:00Z' });
  });

  it('cambiarEstado hace PUT con param estado', () => {
    service.cambiarEstado(2, 'INACTIVO').subscribe();

    const req = httpMock.expectOne(
      (r) =>
        r.method === 'PUT' &&
        r.url === `${environment.apiUrl}/usuarios/2/estado` &&
        r.params.get('estado') === 'INACTIVO'
    );
    req.flush(null, { status: 204, statusText: 'No Content' });
  });

  it('permisos hace GET a /permisos', () => {
    service.permisos().subscribe((res) => {
      expect(res[0].modulo).toBe('BANCOS');
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/permisos`);
    expect(req.request.method).toBe('GET');
    req.flush([{ id: 1, modulo: 'BANCOS', accion: 'ANULAR' }]);
  });

  it('asignarPermisos hace POST con el array de ids', () => {
    service.asignarPermisos(3, [1, 2, 3]).subscribe((res) => {
      expect(res.id).toBe(3);
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/roles/3/permisos`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual([1, 2, 3]);
    req.flush({ id: 3, nombre: 'CONTADOR', descripcion: '', permisos: [] });
  });

  it('auditoria hace GET con filtros opcionales', () => {
    service.auditoria('socios', '2026-08-01T00:00:00Z', '2026-08-31T23:59:59Z').subscribe((res) => {
      expect(res.length).toBe(1);
    });

    const req = httpMock.expectOne(
      (r) =>
        r.method === 'GET' &&
        r.url === `${environment.apiUrl}/auditoria` &&
        r.params.get('tabla') === 'socios' &&
        r.params.get('desde') === '2026-08-01T00:00:00Z'
    );
    req.flush([{ id: 1, usuarioId: 1, tablaAfectada: 'socios', registroId: 5, accion: 'CREAR', valorAnterior: null, valorNuevo: '{}', ip: '127.0.0.1', createdAt: '2026-08-01T10:00:00Z' }]);
  });
});
