import { Component, inject, viewChild } from '@angular/core';
import { BreakpointObserver, Breakpoints } from '@angular/cdk/layout';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatSidenavModule, MatSidenav } from '@angular/material/sidenav';
import { MatListModule } from '@angular/material/list';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { toSignal } from '@angular/core/rxjs-interop';
import { map } from 'rxjs';

interface NavItem {
  label: string;
  route: string;
  icon: string;
}

@Component({
  selector: 'app-root',
  imports: [
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    MatToolbarModule,
    MatSidenavModule,
    MatListModule,
    MatIconModule,
    MatButtonModule,
  ],
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App {
  private readonly breakpointObserver = inject(BreakpointObserver);
  readonly sidenavRef = viewChild<MatSidenav>('sidenavRef');

  readonly isMobile = toSignal(
    this.breakpointObserver
      .observe([Breakpoints.Handset, Breakpoints.TabletPortrait])
      .pipe(map((result) => result.matches)),
    { initialValue: false }
  );

  readonly navItems: NavItem[] = [
    { label: 'Dashboard', route: '/dashboard', icon: 'dashboard' },
    { label: 'Einnahmen', route: '/income', icon: 'trending_up' },
    { label: 'Ausgaben', route: '/expenses', icon: 'trending_down' },
    { label: 'Rechnungen', route: '/invoicing', icon: 'receipt_long' },
    { label: 'Steuer', route: '/tax', icon: 'calculate' },
    { label: 'Kalender', route: '/calendar', icon: 'calendar_month' },
    { label: 'Dokumente', route: '/documents', icon: 'folder' },
  ];

  onNavClick(): void {
    if (this.isMobile()) {
      this.sidenavRef()?.close();
    }
  }
}
