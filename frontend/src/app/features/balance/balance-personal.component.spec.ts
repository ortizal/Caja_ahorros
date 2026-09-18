import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { environment } from '../../../environments/environment';
import { BalancePersonalComponent } from './balance-personal.component';

describe('BalancePersonalComponent', () => {
  let component: BalancePersonalComponent;
  let fixture: ComponentFixture<BalancePersonalComponent>;
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [BalancePersonalComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()]
    }).compileComponents();

    fixture = TestBed.createComponent(BalancePersonalComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('crea el componente', () => {
    expect(component).toBeTruthy();
  });

  it('no carga si socioId es 0', () => {
    component.cargar();
    expect(component.error()).toContain('Ingrese el ID');
    httpMock.expectNone(`${environment.apiUrl}/balance/personal`);
  });

  it('carga datos con socioId válido', () => {
    component.socioId.set(1);
    component.cargar();
    const req = httpMock.expectOne(`${environment.apiUrl}/balance/personal?socioId=1`);
    expect(req.request.method).toBe('GET');
    req.flush({ socioId: 1, codigo: 'SOC-001', nombreCompleto: 'Juan Perez', posicionNeta: 3000 });
    expect(component.datos()).toBeTruthy();
    expect(component.datos()!.socioId).toBe(1);
  });

  it('muestra error al fallar', () => {
    component.socioId.set(999);
    component.cargar();
    const req = httpMock.expectOne(`${environment.apiUrl}/balance/personal?socioId=999`);
    req.flush('Error', { status: 404, statusText: 'Not Found' });
    expect(component.error()).toContain('No se pudo cargar');
  });
});