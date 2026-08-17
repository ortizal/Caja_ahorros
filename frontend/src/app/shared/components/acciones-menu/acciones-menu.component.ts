import { AfterViewInit, Component, DestroyRef, ElementRef, inject, signal } from '@angular/core';

@Component({
  selector: 'app-acciones-menu',
  standalone: true,
  imports: [],
  templateUrl: './acciones-menu.html',
  styleUrl: './acciones-menu.css'
})
export class AccionesMenuComponent implements AfterViewInit {
  protected readonly abierto = signal(false);
  protected readonly posicion = signal<{ top: number; left: number }>({ top: 0, left: 0 });

  private readonly elementRef = inject<ElementRef<HTMLElement>>(ElementRef);
  private readonly destroyRef = inject(DestroyRef);
  private boton!: HTMLButtonElement;
  private destruido = false;

  ngAfterViewInit(): void {
    this.boton = this.elementRef.nativeElement.querySelector<HTMLButtonElement>('button')!;
    this.suscribir(true);
    this.destroyRef.onDestroy(() => {
      this.destruido = true;
      this.suscribir(false);
    });
  }

  toggle(event: MouseEvent): void {
    event.stopPropagation();
    if (this.abierto()) {
      this.cerrar();
      return;
    }
    const rect = this.boton.getBoundingClientRect();
    this.abierto.set(true);
    this.posicionar(rect);
  }

  cerrar(): void {
    this.abierto.set(false);
  }

  private posicionar(rect: DOMRect): void {
    window.setTimeout(() => {
      if (this.destruido) {
        return;
      }
      const lista = this.elementRef.nativeElement.querySelector<HTMLElement>('.acciones-menu-lista');
      if (!lista) {
        return;
      }
      const alto = lista.offsetHeight;
      const sobraAbajo = rect.bottom + 4 + alto > window.innerHeight;
      const top = sobraAbajo ? Math.max(4, rect.top - alto - 4) : rect.bottom + 4;
      this.posicion.set({ top, left: rect.right });
    }, 0);
  }

  private suscribir(activar: boolean): void {
    if (activar) {
      document.addEventListener('click', this.onClickFuera);
      document.addEventListener('keydown', this.onKeydown);
    } else {
      document.removeEventListener('click', this.onClickFuera);
      document.removeEventListener('keydown', this.onKeydown);
    }
  }

  private readonly onClickFuera = (event: MouseEvent): void => {
    if (!this.elementRef.nativeElement.contains(event.target as Node)) {
      this.cerrar();
    }
  };

  private readonly onKeydown = (event: KeyboardEvent): void => {
    if (event.key === 'Escape') {
      this.cerrar();
    }
  };
}
