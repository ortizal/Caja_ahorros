import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { environment } from '../../../environments/environment';
import { NotificacionService } from './notificacion.service';

describe('NotificacionService', () => {
  let service: NotificacionService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(NotificacionService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('listar hace GET a /notificaciones', () => {
    service.listar().subscribe((res) => {
      expect(res.content.length).toBe(1);
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/notificaciones`);
    expect(req.request.method).toBe('GET');
    req.flush({ content: [{ id: 1, tipo: 'MORA', mensaje: 'Cuota vencida', leida: false }], page: 0, size: 10, totalElements: 1, totalPages: 1 });
  });

  it('contarNoLeidas hace GET a /notificaciones/no-leidas', () => {
    service.contarNoLeidas().subscribe((res) => {
      expect(res).toBe(2);
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/notificaciones/no-leidas`);
    expect(req.request.method).toBe('GET');
    req.flush(2);
  });

  it('marcarLeida hace POST a /notificaciones/:id/leida', () => {
    service.marcarLeida(3).subscribe((res) => {
      expect(res.leida).toBe(true);
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/notificaciones/3/leida`);
    expect(req.request.method).toBe('POST');
    req.flush({ id: 3, tipo: 'MORA', mensaje: 'Cuota vencida', leida: true });
  });

  it('marcarTodasLeidas hace POST a /notificaciones/leidas', () => {
    service.marcarTodasLeidas().subscribe(() => {
      expect(true).toBe(true);
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/notificaciones/leidas`);
    expect(req.request.method).toBe('POST');
    req.flush(null);
  });

  it('generar hace POST a /notificaciones/generar', () => {
    service.generar().subscribe(() => {
      expect(true).toBe(true);
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/notificaciones/generar`);
    expect(req.request.method).toBe('POST');
    req.flush(null);
  });
});
