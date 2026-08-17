import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Notificacion } from '../../../core/models/notificacion.model';
import { UserSession } from '../../../core/models/auth.model';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [CommonModule],
  template: `
    <header class="app-header">
      <div class="header-left">
        <button
          type="button"
          class="btn-toggle-menu"
          aria-label="Toggle Sidebar"
          (click)="onToggleSidebar()"
        >
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <line x1="3" y1="12" x2="21" y2="12"></line>
            <line x1="3" y1="6" x2="21" y2="6"></line>
            <line x1="3" y1="18" x2="21" y2="18"></line>
          </svg>
        </button>

        <div class="header-brand">
          <span class="brand-logo-icon">🏦</span>
          <span class="brand-title">Caja de Ahorros</span>
        </div>
      </div>

      <div class="header-right">
        <!-- Notificaciones -->
        <div class="notif" [class.open]="panelAbierto">
          <button
            type="button"
            class="btn-icon-topbar notif-btn"
            data-testid="btn-notificaciones"
            aria-label="Notificaciones"
            (click)="onTogglePanel()"
          >
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"></path>
              <path d="M13.73 21a2 2 0 0 1-3.46 0"></path>
            </svg>
            @if (noLeidas > 0) {
              <span class="badge">{{ noLeidas }}</span>
            }
          </button>

          @if (panelAbierto) {
            <div class="notif-panel" data-testid="notif-panel">
              <div class="notif-head">
                <div class="notif-head-title">
                  <strong>Notificaciones</strong>
                  @if (noLeidas > 0) {
                    <span class="notif-count-pill">{{ noLeidas }} nuevas</span>
                  }
                </div>
                @if (noLeidas > 0) {
                  <button type="button" class="btn-link" (click)="onMarcarTodas()">Marcar todas</button>
                }
              </div>
              <div class="notif-list">
                @for (n of lista; track n.id) {
                  <button
                    type="button"
                    class="notif-item"
                    [class.unread]="!n.leida"
                    (click)="onMarcarUna(n.id)"
                  >
                    <span class="notif-msg">{{ n.mensaje }}</span>
                    <span class="notif-date">{{ n.createdAt }}</span>
                  </button>
                } @empty {
                  <div class="notif-empty">No hay notificaciones</div>
                }
              </div>
            </div>
          }
        </div>

        <!-- User Profile Pill -->
        @if (user; as u) {
          <div class="user-pill">
            <div class="user-avatar">
              {{ (u.nombreCompleto || u.username).substring(0, 2).toUpperCase() }}
            </div>
            <div class="user-details">
              <span class="user-name">{{ u.nombreCompleto || u.username }}</span>
              <span class="user-roles">{{ u.roles.join(', ') }}</span>
            </div>
          </div>
        }

        <!-- Logout Action -->
        <button
          type="button"
          class="btn btn-outline btn-sm btn-logout"
          (click)="onLogout()"
        >
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"></path>
            <polyline points="16 17 21 12 16 7"></polyline>
            <line x1="21" y1="12" x2="9" y2="12"></line>
          </svg>
          <span>Salir</span>
        </button>
      </div>
    </header>
  `,
  styles: [`
    :host {
      display: block;
      width: 100%;
      z-index: 50;
    }

    .app-header {
      height: 56px;
      background: #ffffff;
      border-bottom: 1px solid var(--color-border);
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 0 16px;
      box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
    }

    .header-left {
      display: flex;
      align-items: center;
      gap: 12px;
    }

    .btn-toggle-menu {
      background: transparent;
      border: 1px solid var(--color-border);
      border-radius: var(--radius-sm);
      width: 34px;
      height: 34px;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      color: var(--color-text);
      cursor: pointer;
      transition: all var(--transition-fast);
    }

    .btn-toggle-menu:hover {
      background: #f8fafc;
      border-color: var(--color-border-hover);
      color: var(--color-primary);
    }

    .header-brand {
      display: flex;
      align-items: center;
      gap: 8px;
    }

    .brand-logo-icon {
      font-size: 20px;
    }

    .brand-title {
      font-size: var(--font-lg);
      font-weight: 700;
      color: var(--color-dark-bg);
      letter-spacing: -0.01em;
    }

    .header-right {
      display: flex;
      align-items: center;
      gap: 14px;
    }

    .notif {
      position: relative;
    }

    .btn-icon-topbar {
      position: relative;
      background: #ffffff;
      border: 1px solid var(--color-border);
      border-radius: var(--radius-sm);
      width: 36px;
      height: 36px;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      color: var(--color-muted);
      cursor: pointer;
      transition: all var(--transition-fast);
    }

    .btn-icon-topbar:hover {
      background: #f8fafc;
      color: var(--color-text);
      border-color: var(--color-border-hover);
    }

    .notif-btn .badge {
      position: absolute;
      top: -5px;
      right: -5px;
      background: var(--color-danger);
      color: #ffffff;
      font-size: 10px;
      font-weight: 700;
      border-radius: var(--radius-full);
      min-width: 18px;
      height: 18px;
      line-height: 18px;
      text-align: center;
      padding: 0 4px;
      box-shadow: 0 0 0 2px #ffffff;
    }

    .notif-panel {
      position: absolute;
      right: 0;
      top: calc(100% + 8px);
      width: 340px;
      background: #ffffff;
      border: 1px solid var(--color-border);
      border-radius: var(--radius-md);
      box-shadow: var(--shadow-lg);
      z-index: 100;
      overflow: hidden;
      animation: modalFadeIn 150ms ease-out;
    }

    .notif-head {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 10px 14px;
      background: #f8fafc;
      border-bottom: 1px solid var(--color-border);
    }

    .notif-head-title {
      display: flex;
      align-items: center;
      gap: 6px;
      font-size: var(--font-md);
    }

    .notif-count-pill {
      background: var(--color-primary-light);
      color: var(--color-primary);
      font-size: var(--font-xs);
      font-weight: 600;
      padding: 2px 6px;
      border-radius: var(--radius-full);
    }

    .notif-list {
      max-height: 320px;
      overflow-y: auto;
      display: flex;
      flex-direction: column;
    }

    .notif-item {
      display: flex;
      flex-direction: column;
      gap: 2px;
      text-align: left;
      padding: 10px 14px;
      border: none;
      border-bottom: 1px solid var(--color-border);
      background: #ffffff;
      cursor: pointer;
      transition: background var(--transition-fast);
    }

    .notif-item:hover {
      background: #f8fafc;
    }

    .notif-item.unread {
      background: var(--color-primary-light);
      border-left: 3px solid var(--color-primary);
    }

    .notif-msg {
      font-size: var(--font-md);
      color: var(--color-text);
      line-height: 1.35;
    }

    .notif-date {
      font-size: var(--font-xs);
      color: var(--color-muted);
    }

    .notif-empty {
      padding: 20px;
      font-size: var(--font-md);
      color: var(--color-muted);
      text-align: center;
    }

    .user-pill {
      display: flex;
      align-items: center;
      gap: 10px;
      padding: 4px 10px 4px 4px;
      background: #f8fafc;
      border: 1px solid var(--color-border);
      border-radius: var(--radius-full);
    }

    .user-avatar {
      width: 30px;
      height: 30px;
      border-radius: var(--radius-full);
      background: var(--color-dark-bg);
      color: #ffffff;
      font-size: 11px;
      font-weight: 700;
      display: flex;
      align-items: center;
      justify-content: center;
    }

    .user-details {
      display: flex;
      flex-direction: column;
      line-height: 1.2;
    }

    .user-name {
      font-size: var(--font-md);
      font-weight: 600;
      color: var(--color-text);
    }

    .user-roles {
      font-size: 10px;
      color: var(--color-muted);
      text-transform: uppercase;
      letter-spacing: 0.03em;
    }

    .btn-logout {
      height: 34px;
    }

    @media (max-width: 640px) {
      .user-roles, .brand-title {
        display: none;
      }
    }
  `]
})
export class AppHeaderComponent {
  @Input() user: UserSession | null = null;
  @Input() lista: Notificacion[] = [];
  @Input() noLeidas = 0;
  @Input() panelAbierto = false;

  @Output() toggleSidebar = new EventEmitter<void>();
  @Output() togglePanel = new EventEmitter<void>();
  @Output() marcarUna = new EventEmitter<number>();
  @Output() marcarTodas = new EventEmitter<void>();
  @Output() logout = new EventEmitter<void>();

  onToggleSidebar(): void {
    this.toggleSidebar.emit();
  }

  onTogglePanel(): void {
    this.togglePanel.emit();
  }

  onMarcarUna(id: number): void {
    this.marcarUna.emit(id);
  }

  onMarcarTodas(): void {
    this.marcarTodas.emit();
  }

  onLogout(): void {
    this.logout.emit();
  }
}
