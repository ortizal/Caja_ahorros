import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { environment } from '../../../environments/environment';
import { ContabilidadService } from './contabilidad.service';

describe('ContabilidadService', () => {
  let service: ContabilidadService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(ContabilidadService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('planCuentas hace GET a /plan-cuentas', () => {
    service.planCuentas().subscribe((res) => {
      expect(res.length).toBe(1);
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/plan-cuentas`);
    expect(req.request.method).toBe('GET');
    req.flush([{ id: 1, codigo: '1', nombre: 'Caja', tipo: 'ACTIVO', cuentaPadreId: null, nivel: 1, aceptaMovimiento: true }]);
  });

  it('periodos hace GET a /periodos-contables', () => {
    service.periodos().subscribe((res) => {
      expect(res[0].estado).toBe('ABIERTO');
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/periodos-contables`);
    expect(req.request.method).toBe('GET');
    req.flush([{ id: 1, anio: 2026, mes: 8, estado: 'ABIERTO', cerradoPor: null, cerradoAt: null }]);
  });

  it('cerrarPeriodo hace POST con params', () => {
    service.cerrarPeriodo(2026, 8).subscribe((res) => {
      expect(res.estado).toBe('CERRADO');
    });

    const req = httpMock.expectOne(
      (r) =>
        r.method === 'POST' &&
        r.url === `${environment.apiUrl}/periodos-contables/cerrar` &&
        r.params.get('anio') === '2026' &&
        r.params.get('mes') === '8'
    );
    req.flush({ id: 1, anio: 2026, mes: 8, estado: 'CERRADO', cerradoPor: 1, cerradoAt: '2026-08-12T00:00:00Z' });
  });

  it('libroDiario hace GET con desde y hasta', () => {
    service.libroDiario('2026-08-01', '2026-08-31').subscribe((res) => {
      expect(res.length).toBe(1);
    });

    const req = httpMock.expectOne(
      (r) =>
        r.method === 'GET' &&
        r.url === `${environment.apiUrl}/libro-diario` &&
        r.params.get('desde') === '2026-08-01' &&
        r.params.get('hasta') === '2026-08-31'
    );
    req.flush([{ id: 1, periodoId: 1, comprobanteId: null, fecha: '2026-08-01', descripcion: 'X', origen: 'MANUAL', estado: 'EJECUTADO', createdAt: '2026-08-01T00:00:00Z', createdBy: 1, detalles: [] }]);
  });

  it('registrarAsiento hace POST con cuerpo', () => {
    service
      .registrarAsiento({ fecha: '2026-08-01', descripcion: 'Ajuste', detalles: [{ cuentaId: 1, debe: 100 }] })
      .subscribe((res) => {
        expect(res.id).toBe(9);
      });

    const req = httpMock.expectOne(`${environment.apiUrl}/asientos-contables`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({
      fecha: '2026-08-01',
      descripcion: 'Ajuste',
      detalles: [{ cuentaId: 1, debe: 100 }]
    });
    req.flush({ id: 9, periodoId: 1, comprobanteId: null, fecha: '2026-08-01', descripcion: 'Ajuste', origen: 'MANUAL', estado: 'EJECUTADO', createdAt: '2026-08-01T00:00:00Z', createdBy: 1, detalles: [] });
  });

  it('balance hace GET con anio y mes', () => {
    service.balance(2026, 8).subscribe((res) => {
      expect(res.length).toBe(1);
    });

    const req = httpMock.expectOne(
      (r) =>
        r.method === 'GET' &&
        r.url === `${environment.apiUrl}/balance-comprobacion` &&
        r.params.get('anio') === '2026' &&
        r.params.get('mes') === '8'
    );
    req.flush([{ cuentaCodigo: '1', cuentaNombre: 'Caja', debe: 100, haber: 0 }]);
  });
});
