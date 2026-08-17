import { Component, input } from '@angular/core';
import { DatePipe } from '@angular/common';
import { Usuario } from '../../core/models/seguridad.model';

@Component({
  selector: 'app-usuario-detalle',
  imports: [DatePipe],
  templateUrl: './usuario-detalle.html',
  styleUrl: './usuario-detalle.css'
})
export class UsuarioDetalleComponent {
  readonly usuario = input.required<Usuario>();
}
