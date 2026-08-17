import { Component, inject } from '@angular/core';
import { ToastService, TipoToast } from '../../../core/services/toast.service';

@Component({
  selector: 'app-toast',
  standalone: true,
  imports: [],
  templateUrl: './toast.html',
  styleUrl: './toast.css'
})
export class ToastComponent {
  protected readonly toastService = inject(ToastService);

  icono(tipo: TipoToast): string {
    switch (tipo) {
      case 'success':
        return 'bi bi-check-circle-fill';
      case 'error':
        return 'bi bi-x-circle-fill';
      case 'warning':
        return 'bi bi-exclamation-triangle-fill';
      default:
        return 'bi bi-info-circle-fill';
    }
  }
}
