import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { environment } from '../../../environments/environment';
import { BalanceGeneralComponent } from './balance-general.component';

describe('BalanceGeneralComponent', () => {
  let component: BalanceGeneralComponent;
  let fixture: ComponentFixture<BalanceGeneralComponent>;
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [BalanceGeneralComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()]
    }).compileComponents();

    fixture = TestBed.createComponent(BalanceGeneralComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('crea el componente', () => {
    fixture.detectChanges();
    const req = httpMock.expectOne(`${environment.apiUrl}/balance/situacion`);
    req.flush({ totalActivo: 0, totalPasivo: 0, totalPatrimonio: 0, activo: [], pasivo: [], patrimonio: [] });
    expect(component).toBeTruthy();
  });

  it('carga datos al iniciar', () => {
    fixture.detectChanges();
    const req = httpMock.expectOne(`${environment.apiUrl}/balance/situacion`);
    expect(req.request.method).toBe('GET');
    req.flush({ totalActivo: 1000, totalPasivo: 500, totalPatrimonio: 500, activo: [], pasivo: [], patrimonio: [] });
    expect(component.datos()).toBeTruthy();
  });

  it('muestra error al fallar la carga', () => {
    fixture.detectChanges();
    const req = httpMock.expectOne(`${environment.apiUrl}/balance/situacion`);
    req.flush('Error', { status: 500, statusText: 'Server Error' });
    expect(component.error()).toContain('No se pudo cargar');
  });
});