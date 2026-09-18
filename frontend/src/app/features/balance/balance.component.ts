import { Component, inject, signal } from '@angular/core';
import { BalanceGeneralComponent } from './balance-general.component';
import { BalanceComprobacionComponent } from './balance-comprobacion.component';
import { BalanceSocialComponent } from './balance-social.component';
import { BalancePersonalComponent } from './balance-personal.component';

@Component({
  selector: 'app-balance',
  imports: [
    BalanceGeneralComponent,
    BalanceComprobacionComponent,
    BalanceSocialComponent,
    BalancePersonalComponent
  ],
  templateUrl: './balance.html',
  styleUrl: './balance.css'
})
export class BalanceComponent {
  public readonly tabActiva = signal<'general' | 'comprobacion' | 'social' | 'personal'>('general');
}