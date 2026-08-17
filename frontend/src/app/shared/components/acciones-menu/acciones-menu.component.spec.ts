import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { AccionesMenuComponent } from './acciones-menu.component';

@Component({
  selector: 'app-test-host',
  imports: [AccionesMenuComponent],
  template: `
    <app-acciones-menu>
      <button type="button" class="btn btn-sm btn-outline-secondary" data-testid="item-1">Ver</button>
      <button type="button" class="btn btn-sm btn-danger" data-testid="item-2">Eliminar</button>
    </app-acciones-menu>
  `
})
class TestHostComponent {}

describe('AccionesMenuComponent', () => {
  let fixture: ReturnType<typeof crearFixture>;

  function crearFixture() {
    const f = TestBed.createComponent(TestHostComponent);
    f.detectChanges();
    return f;
  }

  function abrir(): void {
    fixture.nativeElement.querySelector('[data-testid="btn-acciones-menu"]').dispatchEvent(new MouseEvent('click', { bubbles: true }));
    fixture.detectChanges();
  }

  beforeEach(() => {
    fixture = crearFixture();
  });

  afterEach(() => {
    fixture.destroy();
    document.body.innerHTML = '';
  });

  it('oculta la lista al inicio', () => {
    expect(fixture.nativeElement.querySelector('[data-testid="acciones-menu-lista"]')).toBeNull();
  });

  it('abre la lista con los botones proyectados al pulsar el boton', () => {
    abrir();
    const lista = fixture.nativeElement.querySelector('[data-testid="acciones-menu-lista"]');
    expect(lista).not.toBeNull();
    expect(lista.querySelector('[data-testid="item-1"]')).not.toBeNull();
    expect(lista.querySelector('[data-testid="item-2"]')).not.toBeNull();
  });

  it('cierra la lista al hacer click fuera', () => {
    abrir();
    document.body.dispatchEvent(new MouseEvent('click', { bubbles: true }));
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[data-testid="acciones-menu-lista"]')).toBeNull();
  });

  it('cierra la lista al presionar Escape', () => {
    abrir();
    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }));
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[data-testid="acciones-menu-lista"]')).toBeNull();
  });

  it('cierra la lista al seleccionar una accion', () => {
    abrir();
    fixture.nativeElement.querySelector('[data-testid="item-1"]').dispatchEvent(new MouseEvent('click', { bubbles: true }));
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[data-testid="acciones-menu-lista"]')).toBeNull();
  });

  it('alterna cerrado/abierto con el boton', () => {
    abrir();
    abrir();
    expect(fixture.nativeElement.querySelector('[data-testid="acciones-menu-lista"]')).toBeNull();
  });

  it('expone el testid del boton y una lista con rol menu', () => {
    const componente = fixture.debugElement.query(By.directive(AccionesMenuComponent)).componentInstance as AccionesMenuComponent;
    expect(componente).toBeTruthy();
    abrir();
    expect(fixture.nativeElement.querySelector('[data-testid="acciones-menu-lista"]').getAttribute('role')).toBe('menu');
  });
});
