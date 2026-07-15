import {Component, computed, inject} from '@angular/core';
import {RouterLink, RouterLinkActive} from '@angular/router';
import {TenantContextService} from '../../../core/services/tenant-context.service';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [RouterLink, RouterLinkActive],
  template: `
    <nav class="sidebar">
      <div class="sidebar-header">
        <span class="sidebar-title">IIS Sandbox</span>
      </div>
      <ul class="sidebar-nav">
        <li>
          <a routerLink="/dashboard" routerLinkActive="active" class="nav-link">
            <i class="pi pi-home"></i>
            <span>Dashboard</span>
          </a>
        </li>
        <li>
          <a routerLink="/tenants" routerLinkActive="active" class="nav-link">
            <i class="pi pi-building"></i>
            <span>Tenants</span>
          </a>
        </li>
        @if (tenantContext.hasTenant()) {
          <li class="nav-section">{{ tenantContext.tenantName() }}</li>
          <li>
            <a [routerLink]="patientsLink()" routerLinkActive="active" class="nav-link">
              <i class="pi pi-users"></i>
              <span>Patients</span>
            </a>
          </li>
          <li>
            <a [routerLink]="messagesLink()" routerLinkActive="active" class="nav-link">
              <i class="pi pi-envelope"></i>
              <span>Messages</span>
            </a>
          </li>
          <li>
            <a [routerLink]="popLink()" routerLinkActive="active" class="nav-link">
              <i class="pi pi-send"></i>
              <span>Send Now</span>
            </a>
          </li>
          <li>
            <a [routerLink]="fhirMessagingLink()" routerLinkActive="active" class="nav-link">
              <i class="pi pi-comment"></i>
              <span>FHIR Messaging</span>
            </a>
          </li>
          <li>
            <a [routerLink]="v2ToFhirLink()" routerLinkActive="active" class="nav-link">
              <i class="pi pi-arrows-h"></i>
              <span>V2 to FHIR</span>
            </a>
          </li>
          <li>
            <a [routerLink]="shLinkLink()" routerLinkActive="active" class="nav-link">
              <i class="pi pi-link"></i>
              <span>Smart Health Link</span>
            </a>
          </li>
          <li>
            <a [routerLink]="wsdlLink()" routerLinkActive="active" class="nav-link">
              <i class="pi pi-code"></i>
              <span>CDC WSDL</span>
            </a>
          </li>
        }
      </ul>
    </nav>
  `,
  styles: `
    .sidebar {
      width: 240px;
      height: 100%;
      background: #1e293b;
      color: #e2e8f0;
      display: flex;
      flex-direction: column;
    }
    .sidebar-header {
      padding: 1.25rem 1rem;
      border-bottom: 1px solid #334155;
    }
    .sidebar-title {
      font-size: 1.25rem;
      font-weight: 700;
      color: #f8fafc;
    }
    .sidebar-nav {
      list-style: none;
      padding: 0.5rem 0;
      margin: 0;
    }
    .nav-link {
      display: flex;
      align-items: center;
      gap: 0.75rem;
      padding: 0.625rem 1rem;
      color: #cbd5e1;
      text-decoration: none;
      transition: background 0.15s;
      &:hover { background: #334155; }
      &.active { background: var(--p-primary-color); color: #fff; }
      i { font-size: 1rem; }
    }
    .nav-section {
      padding: 1rem 1rem 0.25rem;
      font-size: 0.7rem;
      text-transform: uppercase;
      letter-spacing: 0.05em;
      color: #64748b;
      font-weight: 600;
    }
  `,
})
export class SidebarComponent {
  tenantContext = inject(TenantContextService);

  patientsLink = computed(() => `/t/${this.tenantContext.tenantName()}/patients`);
  messagesLink = computed(() => `/t/${this.tenantContext.tenantName()}/messages`);
  popLink = computed(() => `/t/${this.tenantContext.tenantName()}/pop`);
  fhirMessagingLink = computed(() => `/t/${this.tenantContext.tenantName()}/fhir-messaging`);
  v2ToFhirLink = computed(() => `/t/${this.tenantContext.tenantName()}/v2-to-fhir`);
  shLinkLink = computed(() => `/t/${this.tenantContext.tenantName()}/sh-link`);
  wsdlLink = computed(() => `/t/${this.tenantContext.tenantName()}/wsdl`);
}
