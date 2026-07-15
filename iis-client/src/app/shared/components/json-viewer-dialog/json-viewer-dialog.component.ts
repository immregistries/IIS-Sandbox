import {Component, signal} from '@angular/core';
import {Dialog} from 'primeng/dialog';
import {NgxJsonViewerModule} from 'ngx-json-viewer';

@Component({
  selector: 'app-json-viewer-dialog',
  standalone: true,
  imports: [Dialog, NgxJsonViewerModule],
  template: `
    <p-dialog header="JSON Viewer" [(visible)]="visible" [modal]="true" [style]="{ width: '80vw', height: '80vh' }">
      <div class="json-viewer-container">
        <ngx-json-viewer [json]="json()" [expanded]="true"></ngx-json-viewer>
      </div>
    </p-dialog>
  `,
  styles: ``,
})
export class JsonViewerDialogComponent {
  visible = signal(false);
  json = signal<object>({});

  open(text: string): void {
    try {
      this.json.set(JSON.parse(text));
    } catch {
      this.json.set({error: 'Invalid JSON', raw: text});
    }
    this.visible.set(true);
  }
}
