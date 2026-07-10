import {Component, inject} from '@angular/core';
import {Button} from 'primeng/button';
import {Tag} from 'primeng/tag';
import {AuthService} from '../../../core/services/auth.service';
import {TenantContextService} from '../../../core/services/tenant-context.service';

@Component({
  selector: 'app-topbar',
  standalone: true,
  imports: [Button, Tag],
  template: `
    <header class="topbar">
      <div class="topbar-left">
        <p-tag value="FHIR R4" severity="info" />
        @if (tenantContext.hasTenant()) {
          <span class="tenant-badge">{{ tenantContext.tenantName() }}</span>
        }
      </div>
      <div class="topbar-right">
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
      background: #fff;
      border-bottom: 1px solid #e2e8f0;
    }
    .topbar-left, .topbar-right {
      display: flex;
      align-items: center;
      gap: 0.75rem;
    }
    .tenant-badge {
      font-size: 0.85rem;
      font-weight: 500;
      color: #475569;
    }
    .username {
      font-size: 0.85rem;
      color: #64748b;
    }
  `,
})
export class TopbarComponent {
  authService = inject(AuthService);
  tenantContext = inject(TenantContextService);
}
