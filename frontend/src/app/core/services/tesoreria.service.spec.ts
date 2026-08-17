import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { environment } from '../../../environments/environment';
import { TesoreriaService } from './tesoreria.service';

describe('TesoreriaService', () => {
  let service: TesoreriaService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(TesoreriaService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('gastos hace GET a /gastos', () => {
    service.gastos().subscribe((res) => {
      expect(res.length).toBe(1);
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/gastos`);
    expect(req.request.method).toBe('GET');
    req.flush([{ id: 1, concepto: 'Papelería', monto: 100 }]);
  });

  it('gastos con estado filtra por query param', () => {
    service.gastos('PENDIENTE').subscribe();

    const req = httpMock.expectOne(
      (r) => r.method === 'GET' && r.url === `${environment.apiUrl}/gastos` && r.params.get('estado') === 'PENDIENTE'
    );
    req.flush([]);
  });

  it('crearGasto hace POST con el cuerpo', () => {
    service
      .crearGasto({ concepto: 'Luz', descripcion: 'Factura', monto: 50, cuentaContableId: 21 })
      .subscribe((res) => {
        expect(res.id).toBe(3);
      });

    const req = httpMock.expectOne(`${environment.apiUrl}/gastos`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ concepto: 'Luz', descripcion: 'Factura', monto: 50, cuentaContableId: 21 });
    req.flush({ id: 3, concepto: 'Luz', monto: 50, estado: 'PENDIENTE' });
  });

  it('aprobarGasto hace POST a /gastos/:id/aprobar', () => {
    service.aprobarGasto(1, { aprobar: true }).subscribe((res) => {
      expect(res.estado).toBe('APROBADO');
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/gastos/1/aprobar`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ aprobar: true });
    req.flush({ id: 1, concepto: 'Luz', monto: 50, estado: 'APROBADO' });
  });

  it('pagarGasto hace POST a /gastos/:id/pagar', () => {
    service.pagarGasto(1).subscribe((res) => {
      expect(res.estado).toBe('PAGADO');
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/gastos/1/pagar`);
    expect(req.request.method).toBe('POST');
    req.flush({ id: 1, concepto: 'Luz', monto: 50, estado: 'PAGADO' });
  });

  it('anularGasto hace POST a /gastos/:id/anular', () => {
    service.anularGasto(1).subscribe((res) => {
      expect(res.estado).toBe('ANULADO');
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/gastos/1/anular`);
    expect(req.request.method).toBe('POST');
    req.flush({ id: 1, concepto: 'Luz', monto: 50, estado: 'ANULADO' });
  });

  it('cuentasPorPagar hace GET a /cuentas-por-pagar', () => {
    service.cuentasPorPagar().subscribe((res) => {
      expect(res.length).toBe(1);
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/cuentas-por-pagar`);
    expect(req.request.method).toBe('GET');
    req.flush([{ id: 1, proveedor: 'Proveedor S.A.', monto: 200, estado: 'PENDIENTE' }]);
  });

  it('crearCuentaPorPagar hace POST con el cuerpo', () => {
    service
      .crearCuentaPorPagar({ proveedor: 'P', concepto: 'C', monto: 200, cuentaContableId: 21, fechaVencimiento: '2026-09-30' })
      .subscribe((res) => {
        expect(res.id).toBe(2);
      });

    const req = httpMock.expectOne(`${environment.apiUrl}/cuentas-por-pagar`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ proveedor: 'P', concepto: 'C', monto: 200, cuentaContableId: 21, fechaVencimiento: '2026-09-30' });
    req.flush({ id: 2, proveedor: 'P', monto: 200, estado: 'PENDIENTE' });
  });

  it('pagarCuentaPorPagar hace POST a /cuentas-por-pagar/:id/pagar', () => {
    service.pagarCuentaPorPagar(1).subscribe((res) => {
      expect(res.estado).toBe('PAGADA');
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/cuentas-por-pagar/1/pagar`);
    expect(req.request.method).toBe('POST');
    req.flush({ id: 1, proveedor: 'P', monto: 200, estado: 'PAGADA' });
  });

  it('cuentasPorCobrar hace GET a /cuentas-por-cobrar', () => {
    service.cuentasPorCobrar().subscribe((res) => {
      expect(res.length).toBe(1);
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/cuentas-por-cobrar`);
    expect(req.request.method).toBe('GET');
    req.flush([{ id: 1, deudor: 'Juan', monto: 300, estado: 'PENDIENTE' }]);
  });

  it('crearCuentaPorCobrar hace POST con el cuerpo', () => {
    service
      .crearCuentaPorCobrar({ deudor: 'Juan', concepto: 'C', monto: 300, cuentaContableId: 18, fechaVencimiento: '2026-10-31' })
      .subscribe((res) => {
        expect(res.id).toBe(2);
      });

    const req = httpMock.expectOne(`${environment.apiUrl}/cuentas-por-cobrar`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ deudor: 'Juan', concepto: 'C', monto: 300, cuentaContableId: 18, fechaVencimiento: '2026-10-31' });
    req.flush({ id: 2, deudor: 'Juan', monto: 300, estado: 'PENDIENTE' });
  });

  it('cobrarCuentaPorCobrar hace POST a /cuentas-por-cobrar/:id/cobrar', () => {
    service.cobrarCuentaPorCobrar(1).subscribe((res) => {
      expect(res.estado).toBe('COBRADA');
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/cuentas-por-cobrar/1/cobrar`);
    expect(req.request.method).toBe('POST');
    req.flush({ id: 1, deudor: 'Juan', monto: 300, estado: 'COBRADA' });
  });

  it('presupuesto hace GET a /presupuesto', () => {
    service.presupuesto(2026).subscribe((res) => {
      expect(res.totalPresupuestado).toBe(1000);
    });

    const req = httpMock.expectOne(
      (r) => r.method === 'GET' && r.url === `${environment.apiUrl}/presupuesto` && r.params.get('anio') === '2026'
    );
    req.flush({ anio: 2026, partidas: [], totalPresupuestado: 1000, totalEjecutado: 0, porcentajeEjecucion: 0 });
  });

  it('crearPartidaPresupuesto hace POST a /presupuesto/partidas', () => {
    service
      .crearPartidaPresupuesto({ anio: 2026, concepto: 'Materiales', cuentaContableId: 21, montoPresupuestado: 500 })
      .subscribe((res) => {
        expect(res.id).toBe(1);
      });

    const req = httpMock.expectOne(`${environment.apiUrl}/presupuesto/partidas`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ anio: 2026, concepto: 'Materiales', cuentaContableId: 21, montoPresupuestado: 500 });
    req.flush({ id: 1 });
  });
});
