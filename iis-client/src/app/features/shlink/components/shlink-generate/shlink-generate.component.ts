import {Component, computed, inject, input, output, signal} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {InputText} from 'primeng/inputtext';
import {Button} from 'primeng/button';
import {Dialog} from 'primeng/dialog';
import {Message} from 'primeng/message';
import {Checkbox} from 'primeng/checkbox';
import {RadioButton} from 'primeng/radiobutton';
import {Select} from 'primeng/select';
import {SelectButton} from 'primeng/selectbutton';
import {Textarea} from 'primeng/textarea';
import {Tooltip} from 'primeng/tooltip';
import {ShLinkApiService} from '../../services/shlink-api.service';
import {PatientApiService} from '../.././../../features/patient/services/patient-api.service';

@Component({
  selector: 'app-shlink-generate',
  standalone: true,
  imports: [FormsModule, InputText, Button, Dialog, Message, Checkbox, RadioButton, Select, SelectButton, Textarea, Tooltip],
  template: `
    <p-dialog header="Generate Smart Health Link" [(visible)]="visible" [modal]="true" [style]="{ width: '850px' }">
      <div class="dialog-layout">
      <div class="form-layout">
        <div class="form-group">
          <label for="patientId">Patient</label>
          <p-select
            id="patientId"
            [(ngModel)]="patientId"
            [options]="patients()"
            optionLabel="displayLabel"
            optionValue="patientId"
            [filter]="true"
            filterBy="displayLabel"
            placeholder="Select a patient"
            styleClass="w-full"
            [loading]="patientsLoading()"
          />
        </div>

        <div class="form-group">
          <label for="exp">Expiration (seconds)</label>
          <input pInputText id="exp" [(ngModel)]="exp" class="w-full" />
        </div>

        <div class="form-group">
          <label>Flag</label>
          <p-selectButton [options]="flagModeOptions" [(ngModel)]="flagMode" />
        </div>


        @if (flagMode() === 'manual') {
          <div class="form-group">
            <label for="flagManual">Flag (manual)</label>
            <input pInputText id="flagManual" [(ngModel)]="flagManual" placeholder="e.g. LU, LP, P" class="w-full" />
          </div>
        } @else {
          <div class="flag-selectors">
            <div class="flag-option" pTooltip="The Smart Health Link is intended for long-term use and manifest content can evolve over time." tooltipPosition="right">
              <p-checkbox [(ngModel)]="longTerm" [binary]="true" inputId="flagL" />
              <label for="flagL">L — Long-term use</label>
            </div>
            <div class="radio-group">
              <div class="flag-option" pTooltip="No access restriction on the Smart Health Link." tooltipPosition="right">
                <p-radioButton name="accessFlag" value="none" [(ngModel)]="accessFlag" inputId="flagNone" />
                <label for="flagNone"></label>
              </div>
              <div class="flag-option" pTooltip="The Smart Health Link URL resolves to a single encrypted file accessible via GET, bypassing the manifest. Cannot be combined with P." tooltipPosition="right">
                <p-radioButton name="accessFlag" value="U" [(ngModel)]="accessFlag" inputId="flagU" />
                <label for="flagU">U — Direct file (no manifest)</label>
              </div>
                          <div class="flag-option" pTooltip="The Smart Health Link requires a passcode to resolve. In IIS Sandbox, the request must be authenticated." tooltipPosition="right">
                <p-radioButton name="accessFlag" value="P" [(ngModel)]="accessFlag" inputId="flagP" />
                <label for="flagP">P — Passcode required</label>
              </div>
            </div>
          </div>
        }


        @if (hasPasscode()) {
          <div class="form-group">
            <label for="passcode">Passcode</label>
            <input pInputText id="passcode" [(ngModel)]="passcode" class="w-full" />
          </div>
        }

        @if (error()) {
          <p-message severity="error" [text]="error()!" styleClass="w-full" />
        }

        @if (qrCodeResult()) {
          <div class="result">
            <h4>Generated SHLink</h4>
            <div class="result-row">
              <code class="result-value">{{ qrCodeResult() }}</code>
              <p-button icon="pi pi-copy" [rounded]="true" [text]="true" size="small" (onClick)="copyResult()" />
            </div>
          </div>
        }
      </div>

      <div class="display-side">
        <h4>Display (optional)</h4>
        <div class="form-group">
          <label for="shlinkLabel">Label</label>
          <input pInputText id="shlinkLabel" [(ngModel)]="label" placeholder="Short description (max 80 chars)" class="w-full" maxlength="80" />
        </div>
        <div class="form-group">
          <label for="shlinkDesc">Description</label>
          <textarea pTextarea id="shlinkDesc" [(ngModel)]="description" placeholder="Detailed description" class="w-full" [rows]="5"></textarea>
        </div>
      </div>
      </div>
      <ng-template #footer>
        <p-button label="Generate" icon="pi pi-link" [loading]="loading()" (onClick)="onGenerate()" />
      </ng-template>
    </p-dialog>
  `,
  styles: `
    .dialog-layout {
      display: flex;
      gap: 1.5rem;
    }
    .form-layout {
      flex: 1;
      display: flex;
      flex-direction: column;
      gap: 0.75rem;
      min-width: 300px;
    }
    .display-side {
      flex: 0 0 250px;
      display: flex;
      flex-direction: column;
      gap: 0.75rem;
      h4 { margin: 0; }
    }
    .form-group {
      label {
        display: block;
        margin-bottom: 0.375rem;
        font-size: 0.875rem;
        font-weight: 500;
      }
    }
    .flag-selectors {
      display: flex;
      flex-direction: column;
      gap: 0.75rem;
    }
    .flag-option {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      label {
        cursor: pointer;
        font-size: 0.875rem;
      }
    }
    .radio-group {
      display: flex;
      flex-direction: column;
      gap: 0.5rem;
    }
    .w-full { width: 100%; }
    .result { margin-top: 0.5rem; }
    .result h4 { margin: 0 0 0.5rem; }
    .result-row {
      display: flex;
      align-items: flex-start;
      gap: 0.25rem;
    }
    .result-value {
      flex: 1;
      display: block;
      font-size: 0.75rem;
      word-break: break-all;
      background: var(--p-content-background);
      padding: 0.5rem;
      border-radius: 4px;
      max-height: 80px;
      overflow-y: auto;
    }
  `,
})
export class ShLinkGenerateComponent {
  private shLinkApi = inject(ShLinkApiService);
  private patientApi = inject(PatientApiService);

  visible = signal(false);
  initialPatientId = input('');

  patients = signal<{ patientId: string; displayLabel: string }[]>([]);
  patientsLoading = signal(false);
  patientId = signal('');
  exp = signal('10000000');
  passcode = signal('');
  label = signal('');
  description = signal('');

  flagMode = signal<'selector' | 'manual'>('selector');
  flagManual = signal('');
  longTerm = signal(false);
  accessFlag = signal<'none' | 'P' | 'U'>('none');

  flagModeOptions = [
    {label: 'Selector', value: 'selector'},
    {label: 'Manual', value: 'manual'},
  ];

  computedFlag = computed(() => {
    if (this.flagMode() === 'manual') return this.flagManual();
    let f = '';
    if (this.longTerm()) f += 'L';
    if (this.accessFlag() !== 'none') f += this.accessFlag();
    return f;
  });

  hasPasscode = computed(() => {
    const flag = this.computedFlag();
    return flag.includes('P');
  });

  loading = signal(false);
  error = signal<string | null>(null);
  qrCodeResult = signal<string | null>(null);

  generated = output<string>();

  open(patientId?: string): void {
    this.patientId.set(patientId || this.initialPatientId() || '');
    this.flagMode.set('selector');
    this.flagManual.set('');
    this.longTerm.set(false);
    this.accessFlag.set('none');
    this.exp.set('10000000');
    this.passcode.set('');
    this.label.set('');
    this.description.set('');
    this.error.set(null);
    this.qrCodeResult.set(null);
    this.visible.set(true);
    this.loadPatients();
  }

  private loadPatients(): void {
    this.patientsLoading.set(true);
    this.patientApi.getPatients().subscribe({
      next: (patients) => {
        this.patients.set(patients.map((p) => ({
          patientId: p.patientId,
          displayLabel: `${p.patientNames?.[0]?.nameLast || ''}, ${p.patientNames?.[0]?.nameFirst || ''} (${p.patientId})`,
        })));
        this.patientsLoading.set(false);
      },
      error: () => this.patientsLoading.set(false),
    });
  }

  onGenerate(): void {
    if (!this.patientId()) return;
    this.loading.set(true);
    this.error.set(null);
    this.qrCodeResult.set(null);

    const flag = this.computedFlag();
    this.shLinkApi.generate({
      patientId: this.patientId(),
      flag: flag || undefined,
      exp: this.exp() || undefined,
      passcode: this.hasPasscode() ? this.passcode() || undefined : undefined,
      label: this.label() || undefined,
      description: this.description() || undefined,
    }).subscribe({
      next: (qrCode) => {
        this.qrCodeResult.set(qrCode);
        this.loading.set(false);
        this.generated.emit(qrCode);
      },
      error: (err) => {
        this.error.set(err.message || 'Failed to generate Smart Health Link');
        this.loading.set(false);
      },
    });
  }

  copyResult(): void {
    if (this.qrCodeResult()) {
      navigator.clipboard.writeText(this.qrCodeResult()!);
    }
  }
}
