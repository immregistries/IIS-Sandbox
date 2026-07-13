import {Component, computed, inject, OnInit, signal} from '@angular/core';
import {Button} from 'primeng/button';
import {Tag} from 'primeng/tag';
import {Tooltip} from 'primeng/tooltip';
import {AuthService} from '../../../core/services/auth.service';
import {TenantContextService} from '../../../core/services/tenant-context.service';
import {ThemeService} from '../../../core/services/theme.service';
import {TenantApiService} from '../../../features/tenant/services/tenant-api.service';
import {getActiveFlavors, ProcessingFlavor} from '../../../features/tenant/models/flavor.model';

@Component({
  selector: 'app-topbar',
  standalone: true,
  imports: [Button, Tag, Tooltip],
  template: `
    <header class="topbar">
      <div class="topbar-left">
        <p-tag value="FHIR R4" severity="info" />
        @if (tenantContext.hasTenant()) {
          <span class="tenant-badge">{{ tenantContext.tenantName() }}</span>
          @for (flavor of activeFlavorsWithDesc(); track flavor.key) {
            <p-tag [value]="flavor.key" severity="success" [rounded]="true" [pTooltip]="flavor.behaviorDescription" tooltipPosition="bottom" />
          }
        }
      </div>
      <div class="topbar-right">
        <p-button
          [icon]="themeService.darkMode() ? 'pi pi-sun' : 'pi pi-moon'"
          [rounded]="true"
          [text]="true"
          severity="secondary"
          (onClick)="themeService.toggleDarkMode()"
        />
        <span class="username">{{ authService.username() }}</span>
        <p-button label="Logout" icon="pi pi-sign-out" severity="secondary" [text]="true" size="small" (onClick)="authService.logout()" />
      </div>
    </header>
  `,
  styles: `
    .topbar {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 0 1.25rem;
      height: 48px;
      background: var(--p-content-background);
      border-bottom: 1px solid var(--p-content-border-color);
    }
    .topbar-left, .topbar-right {
      display: flex;
      align-items: center;
      gap: 0.75rem;
    }
    .tenant-badge {
      font-size: 0.85rem;
      font-weight: 500;
      color: var(--p-text-color);
    }
    .username {
      font-size: 0.85rem;
      color: var(--p-text-muted-color);
    }
  `,
})
export class TopbarComponent implements OnInit {
  authService = inject(AuthService);
  tenantContext = inject(TenantContextService);
  themeService = inject(ThemeService);
  private tenantApi = inject(TenantApiService);

  private allFlavors = signal<ProcessingFlavor[]>([]);

  activeFlavorsWithDesc = computed(() => {
    const name = this.tenantContext.tenantName();
    if (!name) return [];
    const active = getActiveFlavors(name, this.allFlavors());
    return this.allFlavors().filter((f) => active.has(f.key));
  });

  ngOnInit(): void {
    this.tenantApi.getFlavors().subscribe({
      next: (flavors) => this.allFlavors.set(flavors),
    });
  }
}
