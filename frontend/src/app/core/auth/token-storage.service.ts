import { Injectable, signal } from '@angular/core';
import { UserSession } from '../models/auth.model';

const TOKEN_KEY = 'caja_token';
const USER_KEY = 'caja_usuario';

@Injectable({ providedIn: 'root' })
export class TokenStorageService {
  get token(): string | null {
    return localStorage.getItem(TOKEN_KEY);
  }

  set token(value: string | null) {
    if (value) {
      localStorage.setItem(TOKEN_KEY, value);
    } else {
      localStorage.removeItem(TOKEN_KEY);
    }
  }

  getUser(): UserSession | null {
    const raw = localStorage.getItem(USER_KEY);
    if (!raw) {
      return null;
    }
    try {
      return JSON.parse(raw) as UserSession;
    } catch {
      return null;
    }
  }

  saveSession(session: UserSession): void {
    localStorage.setItem(USER_KEY, JSON.stringify(session));
  }

  clear(): void {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
  }
}
