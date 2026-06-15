import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, signal } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

import { environment } from '../environments/environment';

@Component({
  selector: 'app-root',
  imports: [DatePipe, RouterLink, RouterLinkActive, RouterOutlet],
  templateUrl: './app.html',
  styleUrl: './app.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class App {
  protected readonly today = new Date();
  protected readonly environmentName = environment.production ? 'produccion' : 'desarrollo';
  protected readonly menuOpen = signal(false);
  protected readonly navigation = [
    { path: '/dashboard', label: 'Dashboard', icon: '⌂' },
    { path: '/customers', label: 'Clientes', icon: '◇' },
    { path: '/accounts', label: 'Cuentas', icon: '▣' },
    { path: '/transactions', label: 'Transacciones', icon: '↔' },
    { path: '/statements', label: 'Estados de cuenta', icon: '≡' },
    { path: '/settings', label: 'Configuracion', icon: '⚙' },
  ];

  protected toggleMenu(): void {
    this.menuOpen.update((open) => !open);
  }

  protected closeMenu(): void {
    this.menuOpen.set(false);
  }
}
