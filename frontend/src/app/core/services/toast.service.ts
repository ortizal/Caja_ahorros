import { Injectable, signal } from '@angular/core';

export type TipoToast = 'success' | 'error' | 'warning' | 'info';

export interface Toast {
  id: number;
  tipo: TipoToast;
  mensaje: string;
}

@Injectable({ providedIn: 'root' })
export class ToastService {
  private readonly lista = signal<Toast[]>([]);
  private nextId = 1;
  private readonly duracionMs = 5000;

  readonly toasts = this.lista.asReadonly();

  success(mensaje: string): void {
    this.mostrar('success', mensaje);
  }

  error(mensaje: string): void {
    this.mostrar('error', mensaje);
  }

  warning(mensaje: string): void {
    this.mostrar('warning', mensaje);
  }

  info(mensaje: string): void {
    this.mostrar('info', mensaje);
  }

  quitar(id: number): void {
    this.lista.update((actuales) => actuales.filter((t) => t.id !== id));
  }

  private mostrar(tipo: TipoToast, mensaje: string): void {
    const id = this.nextId++;
    this.lista.update((actuales) => [...actuales, { id, tipo, mensaje }]);
    setTimeout(() => this.quitar(id), this.duracionMs);
  }
}
