import {Component, computed, input, output} from '@angular/core';
import {OrganizationChart} from 'primeng/organizationchart';
import {Tag} from 'primeng/tag';
import {TreeNode} from 'primeng/api';
import {IisPatient} from '../../models/patient.model';
import {MdmLink} from '../../models/mdm-link.model';

interface ChartNodeData {
  patient: IisPatient;
  isGolden: boolean;
  matchResult?: string;
}

@Component({
  selector: 'app-patient-mdm-chart',
  standalone: true,
  imports: [OrganizationChart, Tag],
  template: `
    <p-organizationChart [value]="chartData()" [collapsible]="false">
      <ng-template pTemplate="golden" let-node>
        <div class="chart-node golden-node" (click)="onNodeClick(node)">
          <p-tag value="Golden" severity="warn" [rounded]="true" />
          <span class="node-name">{{ node.label }}</span>
          <span class="node-id">ID: {{ node.data?.patient?.patientId }}</span>
        </div>
      </ng-template>
      <ng-template pTemplate="reported" let-node>
        <div class="chart-node reported-node" (click)="onNodeClick(node)">
          <span class="match-label" [class.possible]="node.data?.matchResult === 'POSSIBLE_MATCH'">
            {{ node.data?.matchResult === 'POSSIBLE_MATCH' ? 'Possible Match' : 'Match' }}
          </span>
          <p-tag value="Reported" severity="info" [rounded]="true" />
          <span class="node-name">{{ node.label }}</span>
          <span class="node-id">ID: {{ node.data?.patient?.patientId }}</span>
        </div>
      </ng-template>
    </p-organizationChart>
  `,
  styles: `
    .chart-node {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 0.25rem;
      padding: 0.5rem 1rem;
      cursor: pointer;
      min-width: 140px;
    }
    .node-name {
      font-weight: 600;
      font-size: 0.875rem;
    }
    .node-id {
      font-size: 0.75rem;
      opacity: 0.8;
    }
    .match-label {
      font-size: 0.7rem;
      font-weight: 700;
      text-transform: uppercase;
      letter-spacing: 0.05em;
      background: rgba(255, 255, 255, 0.3);
      padding: 0.125rem 0.5rem;
      border-radius: 4px;
    }
  `,
})
export class PatientMdmChartComponent {
  patient = input.required<IisPatient>();
  relatedPatients = input.required<IisPatient[]>();
  mdmLinks = input.required<MdmLink[]>();
  isGolden = input(true);

  selected = output<IisPatient>();

  chartData = computed<TreeNode<ChartNodeData>[]>(() => {
    const current = this.patient();
    const related = this.relatedPatients();
    const links = this.mdmLinks();

    if (this.isGolden()) {
      const children: TreeNode<ChartNodeData>[] = related.map((p) => {
        const link = links.find((l) => l.sourceResourceId === p.patientId);
        return {
          type: 'reported',
          styleClass: 'mdm-node-reported',
          label: this.formatName(p),
          expanded: true,
          data: {patient: p, isGolden: false, matchResult: link?.matchResult || 'MATCH'},
        };
      });
      return [{
        type: 'golden',
        styleClass: 'mdm-node-golden',
        label: this.formatName(current),
        expanded: true,
        data: {patient: current, isGolden: true},
        children,
      }];
    } else {
      const goldenPatient = related[0];
      if (!goldenPatient) return [];
      return [{
        type: 'golden',
        styleClass: 'mdm-node-golden',
        label: this.formatName(goldenPatient),
        expanded: true,
        data: {patient: goldenPatient, isGolden: true},
        children: [{
          type: 'reported',
          styleClass: 'mdm-node-reported',
          label: this.formatName(current),
          expanded: true,
          data: {patient: current, isGolden: false, matchResult: 'MATCH'},
        }],
      }];
    }
  });

  onNodeClick(node: TreeNode<ChartNodeData>): void {
    if (node.data) this.selected.emit(node.data.patient);
  }

  formatName(patient: IisPatient): string {
    const name = patient.patientNames?.[0];
    if (!name) return `Unknown (${patient.patientId})`;
    return `${name.nameLast}, ${name.nameFirst} (${patient.patientId})`;
  }
}
