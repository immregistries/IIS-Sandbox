import {Component, computed, inject, OnInit, signal} from '@angular/core';
import {RouterLink} from '@angular/router';
import {Card} from 'primeng/card';
import {Button} from 'primeng/button';
import {Message} from 'primeng/message';
import {Panel} from 'primeng/panel';
import {Tag} from 'primeng/tag';
import {Tooltip} from 'primeng/tooltip';
import {TenantContextService} from '../../core/services/tenant-context.service';
import {TenantApiService} from '../tenant/services/tenant-api.service';
import {getActiveFlavors, ProcessingFlavor} from '../tenant/models/flavor.model';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [RouterLink, Card, Button, Message, Panel, Tag, Tooltip],
  template: `
    <div class="dashboard">
      <h1>Dashboard</h1>

      <p-message severity="warn" styleClass="mb-4 w-full">
        <ng-template #messageicon>
          <i class="pi pi-exclamation-triangle"></i>
        </ng-template>
        This system is for testing purposes only. Do not enter real patient data.
      </p-message>

      <div class="cards-grid">
        <p-card header="FHIR Server">
          <p>Running FHIR R4 with HAPI FHIR JPA</p>
          <ng-template #footer>
            <a href="/iis/fhir/metadata" target="_blank">
              <p-button label="FHIR Metadata" icon="pi pi-external-link" severity="secondary" [outlined]="true" size="small" />
            </a>
          </ng-template>
        </p-card>

        <p-card>
          <ng-template #header>
            <div class="card-header-with-badge">
              <span>Tenants</span>
              <p-tag
                value="?"
                [rounded]="true"
                severity="info"
                pTooltip="Tenants are separated testing environments. One Tenant ≘ One IIS equivalent. Different Facilities can be registered as information sources to the Tenants."
                tooltipPosition="right"
              />
            </div>
          </ng-template>
          <p>Manage testing environments. Each tenant has its own FHIR partition.</p>
          <ng-template #footer>
            <p-button label="Manage Tenants" icon="pi pi-building" routerLink="/tenants" size="small" />
          </ng-template>
        </p-card>

        @if (tenantContext.hasTenant()) {
          <p-card [header]="'Tenant: ' + tenantContext.tenantName()">
            <div class="active-flavors">
              @for (flavor of activeFlavorsWithDesc(); track flavor.key) {
                <p-tag [value]="flavor.key" severity="success" [rounded]="true" [pTooltip]="flavor.behaviorDescription" tooltipPosition="top" />
              }
              @if (!activeFlavorsWithDesc().length) {
                <p>Currently working in this tenant context.</p>
              }
            </div>
            <ng-template #footer>
              <div class="button-group">
                <p-button label="Patients" icon="pi pi-users" [routerLink]="patientsLink()" size="small" />
                <p-button label="Messages" icon="pi pi-envelope" [routerLink]="messagesLink()" severity="secondary" size="small" />
              </div>
            </ng-template>
          </p-card>
        }
      </div>

      <p-panel header="Primary Functions" styleClass="mt-4" [toggleable]="true">
        <div class="function-list">
          @if (tenantContext.hasTenant()) {
            <a [routerLink]="patientsLink()">Patients - Search and manage patient records</a>
            <a [routerLink]="messagesLink()">Messages - Review received HL7 messages</a>
          } @else {
            <p class="hint">Select a tenant to access patient and messaging features.</p>
          }
        </div>
      </p-panel>
    </div>
  `,
  styles: `
    .dashboard { max-width: 960px; }
    h1 { margin: 0 0 1rem; }
    .cards-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
      gap: 1rem;
    }
    .card-header-with-badge {
      display: flex;
      align-items: center;
      gap: 0.375rem;
      padding: 1.25rem 1.25rem 0;
      font-weight: 600;
    }
    .active-flavors {
      display: flex;
      flex-wrap: wrap;
      gap: 0.25rem;
    }
    .button-group { display: flex; gap: 0.5rem; }
    .function-list {
      display: flex;
      flex-direction: column;
      gap: 0.5rem;
      a {
        text-decoration: none;
        &:hover { text-decoration: underline; }
      }
    }
    .hint { color: var(--p-text-muted-color); margin: 0; }
    .mb-4 { margin-bottom: 1rem; }
    .mt-4 { margin-top: 1rem; }
    .w-full { width: 100%; }
  `,
})
export class DashboardComponent implements OnInit {
  tenantContext = inject(TenantContextService);
  private tenantApi = inject(TenantApiService);

  private allFlavors = signal<ProcessingFlavor[]>([]);

  patientsLink = computed(() => `/t/${this.tenantContext.tenantName()}/patients`);
  messagesLink = computed(() => `/t/${this.tenantContext.tenantName()}/messages`);

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
