import {Component, computed, inject, OnInit, signal, viewChild} from '@angular/core';
import {RouterLink} from '@angular/router';
import {Card} from 'primeng/card';
import {Button} from 'primeng/button';
import {Message} from 'primeng/message';
import {Panel} from 'primeng/panel';
import {Tag} from 'primeng/tag';
import {Tooltip} from 'primeng/tooltip';
import {TenantContextService} from '../../core/services/tenant-context.service';
import {TenantApiService} from '../tenant/services/tenant-api.service';
import {TenantSelectorComponent} from '../../shared/components/tenant-selector/tenant-selector.component';
import {getActiveFlavors, ProcessingFlavor} from '../tenant/models/flavor.model';
import {MessageService} from 'primeng/api';
import {TenantCreateDialogComponent} from '../tenant/components/tenant-create-dialog/tenant-create-dialog.component';
import { Divider } from 'primeng/divider';
import { Fieldset } from 'primeng/fieldset';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [RouterLink, Card, Button, Message, Panel, Tag, Tooltip, TenantSelectorComponent, TenantCreateDialogComponent, Divider, Fieldset],
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
            <a [href]="fhirMetadataLink()" target="_blank">
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
          <p-card [header]="'Current Tenant: ' + tenantContext.tenantName()">
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
        } @else {
          <p-card header="Select a Tenant">
            <p>Select a tenant to access patient and messaging features.</p>
            @if (userHasTenant) {
              <app-tenant-selector />
            } @else {
              <p-button label="Create Tenant" icon="pi pi-plus" (onClick)="createDialog().open()" />
            }
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

      @if (tenantContext.hasTenant()) {
        <p-panel header="Legacy Tools" styleClass="mt-4" [toggleable]="true" [collapsed]="false">
          <div class="function-list">
            <a [routerLink]="queryConverterLink()">Query Converter — Convert VXU to QBP query messages</a>
            <a [routerLink]="covidGenerateLink()">COVID Generate — Generate synthetic COVID-19 HL7 messages</a>
            <a [routerLink]="labConverterLink()">Lab Converter — Convert ORU lab messages to VXU</a>
            <!-- <a [routerLink]="vciDemoLink()">VCI Demo — RSP to Verifiable Credential conversion</a> -->
            <a [routerLink]="vacDedupLink()">Vac Dedup — Vaccination deduplication demo</a>
            <a [routerLink]="fitsLink()">FITS Inspector — Parse and inspect RSP messages</a>
            <a [routerLink]="vxuDownloadLink()">VXU Download — Download COVID VXU for CDC reporting</a>
            <a [routerLink]="covidExportLink()">COVID Export — Export COVID flat-file for CDC</a>
          </div>
        </p-panel>
      }
      <p-panel header="Concepts" styleClass="mt-4" [toggleable]="true" [collapsed]="false">
        <p-fieldset legend="HAPIFHIR Server Backend">
        This sandbox uses HAPIFHIR JPA Server framework as an end layer to store records, using an experimental mapping layer to use inherited Hl7v2 based functionalities, current version of HAPIFHIR is a <a href="https://github.com/cerbeor/hapi-fhir-Subscription-custom">modded</a> 6.8.3
        </p-fieldset>
        <p-fieldset legend="Multitenancy">
        Tenants allow separate testing environments, using different Flavors and different partitions of FHIR Server,	Base URLs are formatted as <a href="fhirMetadataLink()"  target="_blank">/iis/fhir/tenantName</a>
        </p-fieldset>
        <p-fieldset legend="Record's Matching:">
        Matching resources using <a href="https://github.com/immregistries/mismo-match"  target="_blank">MISMO</a> for Patients (Activated with a Flavor), <a href="https://github.com/usnistgov/vaccination_deduplication"  target="_blank">vaccination_deduplication</a> for Immunizations
        </p-fieldset>
        <p-fieldset legend="Master Data Management - MDM ">
        Expanded from <a href="https://hapifhir.io/hapi-fhir/docs/server_jpa_mdm/mdm.html"  target="_blank">HAPIFHIR's MDM</a>, Customization includes the above matching logic, a layer to add all the record's identifiers in Golden records, and R5 Support
        </p-fieldset>
        <p-fieldset legend="Consolidated - Golden record:">
        Golden records are generated by MDM system as the reference records to match with and merge information in
        </p-fieldset>
        <p-fieldset legend="Bulk Data exchange server">
        Bulk data <a href="https://build.fhir.org/ig/HL7/bulk-data/export.html"  target="_blank">export</a> implemented, alongside $member-add and $member-remove operations on Groups, SMART authentication is supported, but key registering is still in progress (Currently disabled)
        </p-fieldset>
        <p-fieldset legend=" Immunization Recommendation Forecast">
        <a href="http://hl7.org/fhir/us/immds/STU1/OperationDefinition-ImmDSForecastOperation.html"  target="_blank">ImmDSForecast</a> operations implemented, current result is randomly generated
        </p-fieldset>
      </p-panel>




    <!-- <p-button label="Message display test" (onClick)="showTestMessage()" class="p-mt-2"></p-button> -->
</div>
      <app-tenant-create-dialog [flavors]="allFlavors()" (created)="loadTenants()" />

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
        color: var(--p-primary-color);
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
  private msgSrv = inject(MessageService);

  tenantContext = inject(TenantContextService);
  private tenantApi = inject(TenantApiService);

  allFlavors = signal<ProcessingFlavor[]>([]);
  createDialog = viewChild.required(TenantCreateDialogComponent);

  fhirMetadataLink = computed(() => `/iis/fhir/${this.tenantContext.tenantName()}/metadata`);
  patientsLink = computed(() => `/t/${this.tenantContext.tenantName()}/patients`);
  messagesLink = computed(() => `/t/${this.tenantContext.tenantName()}/messages`);

  queryConverterLink = computed(() => `/t/${this.tenantContext.tenantName()}/legacy/query-converter`);
  covidGenerateLink = computed(() => `/t/${this.tenantContext.tenantName()}/legacy/covid-generate`);
  labConverterLink = computed(() => `/t/${this.tenantContext.tenantName()}/legacy/lab-converter`);
  vciDemoLink = computed(() => `/t/${this.tenantContext.tenantName()}/legacy/vci-demo`);
  vacDedupLink = computed(() => `/t/${this.tenantContext.tenantName()}/legacy/vac-dedup`);
  fitsLink = computed(() => `/t/${this.tenantContext.tenantName()}/legacy/fits`);
  vxuDownloadLink = computed(() => `/t/${this.tenantContext.tenantName()}/legacy/vxu-download`);
  covidExportLink = computed(() => `/t/${this.tenantContext.tenantName()}/legacy/covid-export`);

  activeFlavorsWithDesc = computed(() => {
    const name = this.tenantContext.tenantName();
    if (!name) return [];
    const active = getActiveFlavors(name, this.allFlavors());
    return this.allFlavors().filter((f) => active.has(f.key));
  });

  userHasTenant: boolean = false

  ngOnInit(): void {
    this.tenantApi.getFlavors().subscribe({
      next: (flavors) => this.allFlavors.set(flavors),
    });
    this.loadTenants()
  }

  // Test helper – triggers a simple info toast
  showTestMessage(): void {
    this.msgSrv.add({
      severity: 'info',
      summary: 'Test Message',
      detail: 'This is a test toast from DashboardComponent.',
      sticky: false,
      life: 5000,
    });
  }

  loadTenants() {
    if (!this.tenantContext.hasTenant()) {
      this.tenantApi.getTenants().subscribe({
        next: (tenants) => this.userHasTenant = tenants && tenants.length > 0
      });
    }
  }

}
