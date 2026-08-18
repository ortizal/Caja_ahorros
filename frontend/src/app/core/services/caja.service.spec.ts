import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { environment } from '../../../environments/environment';
import { CajaService } from './caja.service';

describe('CajaService', () => {
  let service: CajaService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(CajaService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('apertura hace POST con saldoInicial', () => {
    service.apertura(500).subscribe((res) => {
      expect(res.estado).toBe('ABIERTA');
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/caja/apertura`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ saldoInicial: 500 });
    req.flush({ id: 1, cajeroId: 1, fecha: '2026-08-11', saldoInicial: 500, estado: 'ABIERTA' });
  });

  it('misCajas hace GET a /caja/mias', () => {
    service.misCajas().subscribe((res) => {
      expect(res.content.length).toBe(1);
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/caja/mias`);
    expect(req.request.method).toBe('GET');
    req.flush({ content: [{ id: 1, cajeroId: 1, fecha: '2026-08-11', saldoInicial: 500, estado: 'ABIERTA' }], page: 0, size: 10, totalElements: 1, totalPages: 1 });
  });

  it('registrarMovimiento hace POST al id correcto', () => {
    service.registrarMovimiento(7, { tipo: 'DEPOSITO', monto: 100 }).subscribe();

    const req = httpMock.expectOne(`${environment.apiUrl}/caja/7/movimientos`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ tipo: 'DEPOSITO', monto: 100 });
    req.flush({ id: 1, cajaAperturaId: 7, tipo: 'DEPOSITO', monto: 100 });
  });

  it('saldo hace GET a /caja/:id/saldo', () => {
    service.saldo(3).subscribe((res) => {
      expect(res.saldoActual).toBe(600);
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/caja/3/saldo`);
    expect(req.request.method).toBe('GET');
    req.flush({ cajaAperturaId: 3, saldoInicial: 500, totalIngresos: 100, totalEgresos: 0, saldoActual: 600 });
  });

  it('arqueo hace POST con saldoFisico', () => {
    service.arqueo(3, 590).subscribe((res) => {
      expect(res.diferencia).toBe(-10);
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/caja/3/arqueo`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ saldoFisico: 590 });
    req.flush({ id: 1, cajaAperturaId: 3, saldoSistema: 600, saldoFisico: 590, diferencia: -10 });
  });

  it('cerrar hace POST a /caja/:id/cierre', () => {
    service.cerrar(3).subscribe((res) => {
      expect(res.estado).toBe('CERRADA');
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/caja/3/cierre`);
    expect(req.request.method).toBe('POST');
    req.flush({ id: 3, cajeroId: 1, fecha: '2026-08-11', saldoInicial: 500, estado: 'CERRADA' });
  });
});
