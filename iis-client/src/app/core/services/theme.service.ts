import {inject, Injectable, signal} from '@angular/core';
import {DOCUMENT} from '@angular/common';

const STORAGE_KEY = 'iis-color-scheme';

@Injectable({providedIn: 'root'})
export class ThemeService {
  private document = inject(DOCUMENT);
  private mediaQuery = window.matchMedia('(prefers-color-scheme: dark)');
  private userExplicitlyChose = false;

  readonly darkMode = signal(false);

  constructor() {
    const stored = localStorage.getItem(STORAGE_KEY);
    if (stored === 'dark' || stored === 'light') {
      this.userExplicitlyChose = true;
      this.darkMode.set(stored === 'dark');
    } else {
      this.darkMode.set(this.mediaQuery.matches);
    }

    this.applyDarkMode(this.darkMode());

    this.mediaQuery.addEventListener('change', (e) => {
      if (!this.userExplicitlyChose) {
        this.darkMode.set(e.matches);
        this.applyDarkMode(e.matches);
      }
    });
  }

  toggleDarkMode(): void {
    this.userExplicitlyChose = true;
    const newValue = !this.darkMode();
    this.darkMode.set(newValue);
    localStorage.setItem(STORAGE_KEY, newValue ? 'dark' : 'light');
    this.applyDarkMode(newValue);
  }

  private applyDarkMode(isDark: boolean): void {
    const htmlEl = this.document.documentElement;
    if (isDark) {
      htmlEl.classList.add('app-dark');
    } else {
      htmlEl.classList.remove('app-dark');
    }
  }
}
