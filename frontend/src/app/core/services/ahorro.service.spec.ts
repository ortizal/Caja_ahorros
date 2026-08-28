import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { environment } from '../../../environments/environment';
import { AhorroService } from './ahorro.service';

describe('AhorroService', () => {
  let service: AhorroService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(AhorroService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('productos hace GET a /productos-ahorro', () => {
    service.productos().subscribe((res) => {
      expect(res.length).toBe(1);
    });

    const req = httpMock.expectOne((r) => r.method === 'GET' && r.url === `${environment.apiUrl}/productos-ahorro`);
    expect(req.request.method).toBe('GET');
    req.flush({ content: [{ id: 1, nombre: 'A LA VISTA', tasaInteres: 2.5, periodicidadCapitalizacion: 'MENSUAL', saldoMinimo: 0, vigenteDesde: '2026-01-01', activo: true }], page: 0, size: 100, totalElements: 1, totalPages: 1 });
  });

  it('crearProducto hace POST con cuerpo', () => {
    service
      .crearProducto({ nombre: 'PROGRAMADO', tasaInteres: 3.5, periodicidadCapitalizacion: 'MENSUAL', saldoMinimo: 50, vigenteDesde: '2026-01-01' })
      .subscribe((res) => {
        expect(res.id).toBe(2);
      });

    const req = httpMock.expectOne(`${environment.apiUrl}/productos-ahorro`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({
      nombre: 'PROGRAMADO',
      tasaInteres: 3.5,
      periodicidadCapitalizacion: 'MENSUAL',
      saldoMinimo: 50,
      vigenteDesde: '2026-01-01'
    });
    req.flush({ id: 2, nombre: 'PROGRAMADO', tasaInteres: 3.5, periodicidadCapitalizacion: 'MENSUAL', saldoMinimo: 50, vigenteDesde: '2026-01-01', activo: true });
  });

  it('cuentas hace GET a /cuentas-ahorro', () => {
    service.cuentas().subscribe((res) => {
      expect(res.content.length).toBe(1);
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/cuentas-ahorro`);
    expect(req.request.method).toBe('GET');
    req.flush({ content: [{ id: 1, socioId: 2, productoId: 1, numeroCuenta: 'AH-0000001', saldo: 0, estado: 'ACTIVA', fechaApertura: '2026-08-12' }], page: 0, size: 10, totalElements: 1, totalPages: 1 });
  });

  it('aperturar hace POST con socio y producto', () => {
    service.aperturar({ socioId: 2, productoId: 1 }).subscribe((res) => {
      expect(res.numeroCuenta).toBe('AH-0000002');
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/cuentas-ahorro`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ socioId: 2, productoId: 1 });
    req.flush({ id: 2, socioId: 2, productoId: 1, numeroCuenta: 'AH-0000002', tipoAhorro: 'NORMAL', saldo: 0, estado: 'ACTIVA', fechaApertura: '2026-08-12' });
  });

  it('aperturar con tipoAhorro DECIMO13 lo envia en el cuerpo', () => {
    service.aperturar({ socioId: 2, productoId: 1, tipoAhorro: 'DECIMO13' }).subscribe((res) => {
      expect(res.tipoAhorro).toBe('DECIMO13');
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/cuentas-ahorro`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ socioId: 2, productoId: 1, tipoAhorro: 'DECIMO13' });
    req.flush({ id: 3, socioId: 2, productoId: 1, numeroCuenta: 'AH-0000003', tipoAhorro: 'DECIMO13', saldo: 0, estado: 'ACTIVA', fechaApertura: '2026-08-12' });
  });

  it('depositar hace POST a /depositos', () => {
    service.depositar(2, 100).subscribe((res) => {
      expect(res.saldoResultante).toBe(100);
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/cuentas-ahorro/2/depositos`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ monto: 100 });
    req.flush({ id: 1, cuentaId: 2, tipo: 'DEPOSITO', monto: 100, saldoResultante: 100, estado: 'EJECUTADO', createdAt: '2026-08-12T00:00:00Z' });
  });

  it('retirar hace POST a /retiros', () => {
    service.retirar(2, 30).subscribe((res) => {
      expect(res.saldoResultante).toBe(70);
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/cuentas-ahorro/2/retiros`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ monto: 30 });
    req.flush({ id: 2, cuentaId: 2, tipo: 'RETIRO', monto: 30, saldoResultante: 70, estado: 'EJECUTADO', createdAt: '2026-08-12T00:00:00Z' });
  });

  it('capitalizar hace POST con params', () => {
    service.capitalizar(2030, 12).subscribe((res) => {
      expect(res.cuentasCapitalizadas).toBe(1);
    });

    const req = httpMock.expectOne(
      (r) =>
        r.method === 'POST' &&
        r.url === `${environment.apiUrl}/ahorros/capitalizar` &&
        r.params.get('anio') === '2030' &&
        r.params.get('mes') === '12'
    );
    req.flush({ anio: 2030, mes: 12, cuentasCapitalizadas: 1, totalInteres: 0.21 });
  });
});
