import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { environment } from '../../../environments/environment';
import { BancoService } from './banco.service';

describe('BancoService', () => {
  let service: BancoService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(BancoService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('cuentas hace GET a /cuentas-bancarias', () => {
    service.cuentas().subscribe((res) => {
      expect(res.content.length).toBe(1);
      expect(res.totalElements).toBe(1);
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/cuentas-bancarias`);
    expect(req.request.method).toBe('GET');
    req.flush({ content: [{ id: 1, banco: 'Banco Nacional', numeroCuenta: '001', tipo: 'CORRIENTE', saldoContable: 1000 }], page: 0, size: 10, totalElements: 1, totalPages: 1 });
  });

  it('crearCuenta hace POST con el cuerpo', () => {
    service
      .crearCuenta({ banco: 'Banco Nacional', numeroCuenta: '002', tipo: 'AHORROS', saldoContable: 0 })
      .subscribe((res) => {
        expect(res.id).toBe(2);
      });

    const req = httpMock.expectOne(`${environment.apiUrl}/cuentas-bancarias`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ banco: 'Banco Nacional', numeroCuenta: '002', tipo: 'AHORROS', saldoContable: 0 });
    req.flush({ id: 2, banco: 'Banco Nacional', numeroCuenta: '002', tipo: 'AHORROS', saldoContable: 0 });
  });

  it('movimientos hace GET a /cuentas-bancarias/:id/movimientos', () => {
    service.movimientos(1).subscribe((res) => {
      expect(res.content.length).toBe(1);
      expect(res.totalElements).toBe(1);
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/cuentas-bancarias/1/movimientos`);
    expect(req.request.method).toBe('GET');
    req.flush({ content: [{ id: 1, cuentaBancariaId: 1, tipo: 'DEPOSITO', monto: 100, fecha: '2026-08-11', conciliado: false, saldoContable: 100 }], page: 0, size: 10, totalElements: 1, totalPages: 1 });
  });

  it('conciliar hace POST con params cuentaId', () => {
    service.conciliar(1, '2026-08', 900).subscribe((res) => {
      expect(res.diferencia).toBe(-10);
    });

    const req = httpMock.expectOne(
      (r) =>
        r.method === 'POST' &&
        r.url === `${environment.apiUrl}/conciliacion-bancaria` &&
        r.params.get('cuentaId') === '1'
    );
    expect(req.request.body).toEqual({ periodo: '2026-08', saldoBancario: 900 });
    req.flush({ id: 1, cuentaBancariaId: 1, periodo: '2026-08', saldoContable: 910, saldoBancario: 900, diferencia: -10 });
  });
});
