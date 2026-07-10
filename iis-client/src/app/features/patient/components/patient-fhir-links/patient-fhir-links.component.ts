import {Component, computed, inject, input} from '@angular/core';
import {TenantContextService} from '../../../../core/services/tenant-context.service';
import {environment} from '../../../../../environments/environment';

@Component({
  selector: 'app-patient-fhir-links',
  standalone: true,
  template: `
    <div class="fhir-links">
      <h3>FHIR API Shortcuts</h3>
      <div class="link-list">
        @for (link of links(); track link.label) {
          <div class="link-item">
            <span class="link-label">{{ link.label }}:</span>
            <a [href]="link.url" target="_blank" class="link-url">{{ link.url }}</a>
          </div>
        }
      </div>
    </div>
  `,
  styles: `
    h3 { margin: 0.5rem 0; }
    .link-list {
      display: flex;
      flex-direction: column;
      gap: 0.5rem;
    }
    .link-item {
      display: flex;
      gap: 0.5rem;
      align-items: baseline;
      flex-wrap: wrap;
    }
    .link-label {
      font-size: 0.875rem;
      font-weight: 500;
      white-space: nowrap;
    }
    .link-url {
      font-size: 0.85rem;
      word-break: break-all;
    }
  `,
})
export class PatientFhirLinksComponent {
  private tenantContext = inject(TenantContextService);

  patientId = input.required<string>();

  links = computed(() => {
    const tenant = this.tenantContext.tenantName();
    const base = `${environment.apiBaseUrl}/fhir/${tenant}`;
    const id = this.patientId();
    return [
      {label: 'FHIR Resource', url: `${base}/Patient/${id}`},
      {label: 'Everything (MDM)', url: `${base}/Patient/${id}/$everything?_mdm=true`},
      {label: 'International Patient Summary', url: `${base}/Patient/${id}/$summary`},
      {label: 'All Immunizations', url: `${base}/Immunization?patient:mdm=Patient/${id}`},
      {label: 'All Observations', url: `${base}/Observation?patient:mdm=Patient/${id}`},
      {label: 'Related Patient Records', url: `${base}/$mdm-query-links?goldenResourceId=${id}`},
    ];
  });
}
