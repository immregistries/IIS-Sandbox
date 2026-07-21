import {Component, inject, OnInit, signal} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {Button} from 'primeng/button';
import {Card} from 'primeng/card';
import {Message} from 'primeng/message';
import {InputText} from 'primeng/inputtext';
import {Select} from 'primeng/select';
import {TableModule} from 'primeng/table';
import {Tooltip} from 'primeng/tooltip';
import {Tag} from 'primeng/tag';
import {FitsApiService, FitsInspectResult} from '../../services/fits-api.service';
import {InputEditorComponent} from '../../../../../shared/components/input-editor/input-editor.component';

@Component({
  selector: 'app-fits',
  standalone: true,
  imports: [FormsModule, Button, Card, Message, InputText, Select, TableModule, Tooltip, Tag, InputEditorComponent],
  template: `
    <div class="fits">
      <h1>FITS RSP Inspector</h1>

      <p-message severity="info" styleClass="mb-4 w-full">
        Parse and inspect RSP (immunization response) messages. Extract forecast data, evaluation status, and generate JUnit test code.
      </p-message>

      <p-card header="RSP Message">
        <div class="form-layout">
          <div class="toolbar">
            <div class="example-select">
              <p-select
                [(ngModel)]="selectedExample"
                [options]="exampleOptions()"
                optionLabel="label"
                optionValue="value"
                placeholder="Load an example..."
                [showClear]="true"
                (onChange)="onExampleSelect()"
              />
            </div>
            <span class="spacer"></span>
            <p-button icon="pi pi-copy" [rounded]="true" [text]="true" size="small" pTooltip="Copy" (onClick)="copyToClipboard(messageData())" />
          </div>
          <app-input-editor [(content)]="messageData" placeholder="Paste RSP message here..." height="360px" />
          <div class="field">
            <label for="testName">JUnit Test Name</label>
            <input pInputText id="testName" [(ngModel)]="testName" placeholder="testCaseName" />
          </div>
          <div class="actions">
            <p-button label="Inspect" icon="pi pi-search" (onClick)="onInspect()" [loading]="inspecting()" />
            <p-button label="Reset" icon="pi pi-refresh" severity="secondary" [outlined]="true" (onClick)="onReset()" />
          </div>
        </div>
      </p-card>

      @if (result()) {
        <p-card header="Forecast Results" styleClass="mt-4">
          <p-table [value]="result()!.forecastActuals" [tableStyle]="{'min-width': '50rem'}">
            <ng-template #header>
              <tr>
                <th>Vaccine Group</th>
                <th>Admin Status</th>
                <th>Valid Date</th>
                <th>Due Date</th>
                <th>Overdue Date</th>
                <th>Vaccine CVX</th>
              </tr>
            </ng-template>
            <ng-template #body let-row>
              <tr>
                <td>{{ row.vaccineGroup }}</td>
                <td><p-tag [value]="row.adminStatus" [severity]="getStatusSeverity(row.adminStatus)" /></td>
                <td>{{ row.validDate }}</td>
                <td>{{ row.dueDate }}</td>
                <td>{{ row.overdueDate }}</td>
                <td>{{ row.vaccineCvx }}</td>
              </tr>
            </ng-template>
          </p-table>
        </p-card>

        @if (result()!.parseDebugLines.length) {
          <p-card header="Parse Debug" styleClass="mt-4">
            <p-table [value]="result()!.parseDebugLines" [tableStyle]="{'min-width': '40rem'}">
              <ng-template #header>
                <tr>
                  <th style="width: 8rem">Status</th>
                  <th>Line</th>
                  <th>Reason</th>
                </tr>
              </ng-template>
              <ng-template #body let-row>
                <tr>
                  <td><p-tag [value]="row.lineStatus" [severity]="getDebugSeverity(row.lineStatus)" /></td>
                  <td class="mono">{{ row.line }}</td>
                  <td>{{ row.lineStatusReason }}</td>
                </tr>
              </ng-template>
            </p-table>
          </p-card>
        }

        @if (result()!.vaccineGroupCounts.length) {
          <p-card header="Vaccine Groups Represented" styleClass="mt-4">
            <p-table [value]="result()!.vaccineGroupCounts" [tableStyle]="{'min-width': '20rem'}">
              <ng-template #header>
                <tr>
                  <th>Vaccine Group</th>
                  <th>Count</th>
                </tr>
              </ng-template>
              <ng-template #body let-row>
                <tr>
                  <td>{{ row.label }}</td>
                  <td>{{ row.count }}</td>
                </tr>
              </ng-template>
            </p-table>
          </p-card>
        }

        @if (result()!.junitCode) {
          <p-card header="JUnit Test Code" styleClass="mt-4">
            <div class="toolbar">
              <span class="spacer"></span>
              <p-button icon="pi pi-copy" [rounded]="true" [text]="true" size="small" pTooltip="Copy" (onClick)="copyToClipboard(result()!.junitCode)" />
            </div>
            <app-input-editor [content]="result()!.junitCode" [readonly]="true" language="none" height="288px" />
          </p-card>
        }
      }

      @if (error()) {
        <p-message severity="error" [text]="error()!" styleClass="mt-4 w-full" />
      }
    </div>
  `,
  styles: `
    .fits { max-width: 960px; }
    h1 { margin: 0 0 1rem; }
    .form-layout { display: flex; flex-direction: column; gap: 1rem; }
    .toolbar { display: flex; align-items: center; gap: 0.25rem; margin-bottom: -0.5rem; }
    .example-select { min-width: 250px; }
    .spacer { flex: 1; }
    .field {
      display: flex; flex-direction: column; gap: 0.375rem;
      label { font-size: 0.875rem; font-weight: 500; }
    }
    .actions { display: flex; gap: 0.5rem; }
    .mono { font-family: 'Cascadia Code', 'Fira Code', 'Consolas', monospace; font-size: 0.8rem; }
    .mb-4 { margin-bottom: 1rem; }
    .mt-4 { margin-top: 1rem; }
    .w-full { width: 100%; }
  `,
})
export class FitsComponent implements OnInit {
  private api = inject(FitsApiService);

  messageData = signal('');
  testName = signal('');
  selectedExample = signal<string | null>(null);
  exampleOptions = signal<{ label: string; value: string }[]>([]);
  result = signal<FitsInspectResult | null>(null);
  error = signal<string | null>(null);
  inspecting = signal(false);

  ngOnInit(): void {
    this.api.getExamples().subscribe({
      next: (examples) => {
        this.exampleOptions.set(
          Object.keys(examples).map((name) => ({label: name, value: name})),
        );
      },
    });
  }

  onExampleSelect(): void {
    const name = this.selectedExample();
    if (!name) return;

    this.api.getExample(name).subscribe({
      next: (rsp) => {
        this.messageData.set(rsp);
        this.testName.set(name);
      },
    });
  }

  onInspect(): void {
    if (!this.messageData()) return;

    this.inspecting.set(true);
    this.error.set(null);
    this.result.set(null);

    this.api.inspect(this.messageData(), this.testName() || undefined).subscribe({
      next: (res) => {
        this.result.set(res);
        this.inspecting.set(false);
      },
      error: (err) => {
        this.error.set(err.message || 'Inspection failed');
        this.inspecting.set(false);
      },
    });
  }

  onReset(): void {
    this.result.set(null);
    this.error.set(null);
  }

  getStatusSeverity(status: string): 'success' | 'info' | 'warn' | 'danger' | 'secondary' {
    switch (status?.toUpperCase()) {
      case 'COMPLETE':
        return 'success';
      case 'DUE':
        return 'info';
      case 'OVERDUE':
        return 'warn';
      case 'CONTRAINDICATED':
        return 'danger';
      default:
        return 'secondary';
    }
  }

  getDebugSeverity(status: string): 'success' | 'info' | 'warn' | 'danger' | 'secondary' {
    switch (status?.toUpperCase()) {
      case 'OK':
        return 'success';
      case 'PROBLEM':
        return 'danger';
      default:
        return 'secondary';
    }
  }

  copyToClipboard(text: string): void {
    navigator.clipboard.writeText(text);
  }
}
