import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { BalanceComponent } from './balance.component';

describe('BalanceComponent', () => {
  let component: BalanceComponent;
  let fixture: ComponentFixture<BalanceComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [BalanceComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()]
    }).compileComponents();

    fixture = TestBed.createComponent(BalanceComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('crea el componente', () => {
    expect(component).toBeTruthy();
  });

  it('inicia con la pestaña general', () => {
    expect(component.tabActiva()).toBe('general');
  });

  it('cambia a pestaña comprobación', () => {
    component.tabActiva.set('comprobacion');
    expect(component.tabActiva()).toBe('comprobacion');
  });

  it('cambia a pestaña social', () => {
    component.tabActiva.set('social');
    expect(component.tabActiva()).toBe('social');
  });

  it('cambia a pestaña personal', () => {
    component.tabActiva.set('personal');
    expect(component.tabActiva()).toBe('personal');
  });

  it('renderiza las 4 pestañas', () => {
    const tabs = fixture.nativeElement.querySelectorAll('[data-testid="balance-tabs"] button');
    expect(tabs.length).toBe(4);
  });
});