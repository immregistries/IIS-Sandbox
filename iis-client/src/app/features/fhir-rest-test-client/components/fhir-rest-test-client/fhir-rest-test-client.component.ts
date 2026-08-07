import {Component, computed, inject, OnInit, signal, viewChild} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {Select} from 'primeng/select';
import {InputText} from 'primeng/inputtext';
import {Button} from 'primeng/button';
import {Card} from 'primeng/card';
import {Message} from 'primeng/message';
import {Tooltip} from 'primeng/tooltip';
import {JsonViewerDialogComponent} from '../../../../shared/components/json-viewer-dialog/json-viewer-dialog.component';
import {InputEditorComponent} from '../../../../shared/components/input-editor/input-editor.component';
import {FhirRestTestClientService} from '../../services/fhir-rest-test-client.service';
import {TenantContextService} from '../../../../core/services/tenant-context.service';
import {tap} from 'rxjs';

/**
 * Component providing a UI to perform basic FHIR REST CRUD operations.
 * Mirrors the layout and styling of FhirMessagingSendComponent for consistency.
 */
@Component({
  selector: 'app-fhir-rest-test-client',
  standalone: true,
  imports: [
    FormsModule,
    Select,
    InputText,
    Button,
    Card,
    Message,
    Tooltip,
    JsonViewerDialogComponent,
    InputEditorComponent,
  ],
  template: `
    <div class="fhir-rest-test-client">
      <a [href]="fhirMetadataLink()" target="_blank">
        <p-button label="FHIR Server Metadata" icon="pi pi-external-link" severity="secondary" [outlined]="true" size="small" />
      </a>
      <h1>FHIR REST Test Client (BETA)</h1>

      <p-message severity="warn" styleClass="mb-4 w-full">
        <ng-template #messageicon>
          <i class="pi pi-exclamation-triangle"></i>
        </ng-template>
        Test against a development FHIR server. Do not send real patient data.
      </p-message>

      <p-card header="Operation">
        <div class="form-layout">
          <div class="options-bar">
            <div class="field">
              <label for="operationSelect">Operation</label>
              <p-select
                id="operationSelect"
                [options]="operationOptions"
                [(ngModel)]="operation"
                optionLabel="label"
                [filter]="true"
                filterBy="value"
                optionValue="value"
                placeholder="Select operation"
                styleClass="w-full"
              ></p-select>
            </div>
            <div class="field">
              <label for="resourceSelect">Resource Type</label>
              <p-select
                id="resourceSelect"
                [options]="resourceOptions"
                [(ngModel)]="resourceType"
                optionLabel="value"
                optionValue="value"
                [filter]="true"
                filterBy="value"
                placeholder="Select resource"
                styleClass="w-full"
              ></p-select>
            </div>
          </div>

          <!-- Conditional ID field for read/update/delete -->
          @if (requiresId()) {
            <div class="field">
              <label for="resourceId">Resource ID</label>
              <input pInputText id="resourceId"
              [(ngModel)]="resourceId" placeholder="Enter ID" />
            </div>
          }

          <!-- Request body editor for create/update/search -->
          @if (needsBody()) {
            <app-input-editor [(content)]="requestBody" language="json" placeholder="JSON payload..." height="300px" />
          }

          <div class="actions">
            <p-button label="Submit" icon="pi pi-send" (onClick)="onSubmit()" [loading]="loading()" />
            <p-button label="Reset" icon="pi pi-refresh" [outlined]="true" (onClick)="onReset()" />
          </div>
        </div>
      </p-card>

      @if (response()) {
        <p-card header="Response" styleClass="mt-4">
          <div class="options-bar">
            <span class="spacer"></span>
            <p-button icon="pi pi-copy" [rounded]="true" [text]="true" size="small" pTooltip="Copy" (onClick)="copyToClipboard(response()!)" />
            <p-button icon="pi pi-eye" [rounded]="true" [text]="true" size="small" pTooltip="View JSON" (onClick)="jsonViewer().open(response()!)" />
          </div>
          <app-input-editor [content]="response()!!" [readonly]="true" language="json" height="300px" />
        </p-card>
      }

      @if (error()) {
        <p-message severity="error" [text]="error()!" styleClass="mt-4 w-full" />
      }

      <app-json-viewer-dialog />
    </div>
  `,
  styles: `
    .fhir-rest-test-client { max-width: 960px; }
    .form-layout { display: flex; flex-direction: column; gap: 1rem; }
    .options-bar { display: flex; gap: 1rem; flex-wrap: wrap; }
    .field { flex: 1; min-width: 200px; }
    .actions { display: flex; gap: 0.5rem; }
    .mb-4 { margin-bottom: 1rem; }
    .mt-4 { margin-top: 1rem; }
    .w-full { width: 100%; }
    .spacer { flex: 1; }
  `,
})
export class FhirRestTestClientComponent implements OnInit{
  private service = inject(FhirRestTestClientService);
  private tenantContext = inject(TenantContextService);

  fhirMetadataLink = computed(() => `/iis/fhir/${this.tenantContext.tenantName()}/metadata`);

  jsonViewer = viewChild.required(JsonViewerDialogComponent);

  // UI state signals
  operation = signal<'create' | 'read' | 'update' | 'delete' | 'search'>('create');
  resourceType = signal<string>('Patient');
  resourceId = signal<string>('');
  requestBody = signal<string>('');
  response = signal<string | null>(null);
  error = signal<string | null>(null);
  loading = signal(false);

  // Dropdown option definitions (placeholder values)
  operationOptions = [
    {label: 'Create', value: 'create'},
    {label: 'Read', value: 'read'},
    {label: 'Update', value: 'update'},
    {label: 'Delete', value: 'delete'},
    {label: 'Search', value: 'search'},
  ];

  resourceOptions = [
    {value: 'Patient'},
    {value: 'Observation'},
    {value: 'Immunization'},
  ];

  // Helpers to decide which UI parts to show
  requiresId() {
    return ['read', 'update', 'delete'].includes(this.operation());
  }

  needsBody() {
    return ['create', 'update', 'search'].includes(this.operation());
  }

  onSubmit() {
    this.loading.set(true);
    this.response.set(null);
    this.error.set(null);
    const op = this.operation();
    const resType = this.resourceType();
    const id = this.resourceId();
    const body = this.requestBody();
    this.service.execute(op, resType, id, body).subscribe({
      next: (res) => {
        this.response.set(JSON.stringify(res, null, 2));
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(err.message || 'Operation failed');
        this.loading.set(false);
      },
    });
  }

  ngOnInit(): void {
    this.loadMetadata()
  }

  loadMetadata() {
    this.service.metadata()
    .pipe(
      tap((metadata) => this.resourceOptions = metadata.rest[0].resource.map((r: any) => ({ value: r.type}))),
    ).subscribe()
  }

  onReset() {
    this.response.set(null);
    this.error.set(null);
  }

  copyToClipboard(text: string): void {
    navigator.clipboard.writeText(text);
  }
}
