import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { environment } from '../../../environments/environment';
import { AportacionService } from './aportacion.service';

describe('AportacionService', () => {
  let service: AportacionService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(AportacionService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('configs hace GET a /aportaciones/config', () => {
    service.configs().subscribe((res) => {
      expect(res.length).toBe(1);
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/aportaciones/config`);
    expect(req.request.method).toBe('GET');
    req.flush([{ id: 1, tipo: 'OBLIGATORIA', modoCalculo: 'FIJO', valor: 25, periodicidad: 'MENSUAL', vigenteDesde: '2026-01-01' }]);
  });

  it('crearConfig hace POST con cuerpo', () => {
    service
      .crearConfig({ tipo: 'EXTRAORDINARIA', modoCalculo: 'FIJO', valor: 10, periodicidad: 'MENSUAL', vigenteDesde: '2026-01-01' })
      .subscribe((res) => {
        expect(res.id).toBe(3);
      });

    const req = httpMock.expectOne(`${environment.apiUrl}/aportaciones/config`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({
      tipo: 'EXTRAORDINARIA',
      modoCalculo: 'FIJO',
      valor: 10,
      periodicidad: 'MENSUAL',
      vigenteDesde: '2026-01-01'
    });
    req.flush({ id: 3, tipo: 'EXTRAORDINARIA', modoCalculo: 'FIJO', valor: 10, periodicidad: 'MENSUAL', vigenteDesde: '2026-01-01' });
  });

  it('generarPeriodo hace POST con param periodo', () => {
    service.generarPeriodo('2026-09').subscribe((res) => {
      expect(res.generadas).toBe(1);
    });

    const req = httpMock.expectOne(
      (r) =>
        r.method === 'POST' &&
        r.url === `${environment.apiUrl}/aportaciones/generar` &&
        r.params.get('periodo') === '2026-09'
    );
    req.flush({ generadas: 1, periodo: '2026-09' });
  });

  it('aportaciones hace GET con filtro de periodo', () => {
    service.aportaciones('2026-09').subscribe((res) => {
      expect(res.length).toBe(1);
    });

    const req = httpMock.expectOne(
      (r) =>
        r.method === 'GET' &&
        r.url === `${environment.apiUrl}/aportaciones` &&
        r.params.get('periodo') === '2026-09'
    );
    req.flush([{ id: 1, socioId: 2, configId: 1, periodo: '2026-09', montoEsperado: 25, montoPagado: 0, mora: 0, estado: 'PENDIENTE' }]);
  });

  it('pagar hace POST al detalle de pagos', () => {
    service.pagar(1, 25).subscribe((res) => {
      expect(res.monto).toBe(25);
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/aportaciones/1/pagos`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ monto: 25 });
    req.flush({ id: 1, aportacionId: 1, monto: 25, cajaMovimientoId: 10, comprobanteNumero: 'CMP-00000010', pagadoAt: '2026-08-12T00:00:00Z' });
  });
});
