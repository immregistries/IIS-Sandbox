import {Component, computed, inject} from '@angular/core';
import {Panel} from 'primeng/panel';
import {Button} from 'primeng/button';
import {Card} from 'primeng/card';
import {TenantContextService} from '../../../../core/services/tenant-context.service';
import {environment} from '../../../../../environments/environment';

@Component({
  selector: 'app-wsdl-page',
  standalone: true,
  imports: [Panel, Button, Card],
  template: `
    <div class="wsdl-page">
      <h1>CDC SOAP Endpoint</h1>

      <p>
        This demonstration system supports the use of the
        <a href="http://www.cdc.gov/vaccines/programs/iis/technical-guidance/soap/wsdl.html" target="_blank">CDC WSDL</a>
        which has been defined to support the transport of HL7 messages sent to Immunization Information Systems (IIS).
      </p>

      <p-panel header="Usage Instructions" [toggleable]="true" styleClass="mt-4">
        <h3>WSDL</h3>
        <p><a [href]="soapUrl()" target="_blank">See WSDL</a></p>

        <h3>Authentication</h3>
        <p>
          Authentication credentials can be established by submitting a username and password to a facility not already
          defined in the IIS Sandbox. Submitting new credentials will cause IIS Sandbox to create an organization to
          represent the facility and a user access account for the supplied credentials. Access to this account and
          facility/organization data will be allowed to anyone submitting the correct credentials.
        </p>
        <ul>
          <li><strong>Bad Credentials</strong>: Simply change the password or username for any currently established account and it will generate an unauthorized exception. This can be repeated as often as possible, the account will not lock.</li>
          <li><strong>NPE/NPE</strong>: Using this as the username and password will trigger a Null Pointer Exception. This can be used to simulate the situation where an unexpected error occurs.</li>
        </ul>

        <h3>Content</h3>
        <p>HL7 VXU or QBP message is expected in payload.</p>

        <h3>Multiple Messages</h3>
        <p>If the message contains more than one MSH segment a Message Too Large Fault will be returned. Use this feature to test situations where the IIS can not process more than one message.</p>
      </p-panel>

      <p-panel header="Alternative Behavior" [toggleable]="true" styleClass="mt-4">
        <p>Additional endpoints are available, which provide different behaviors (some good and some bad). These can be used to demonstrate different or bad interactions.</p>

        <div class="endpoints-grid">
          @for (endpoint of endpoints(); track endpoint.path) {
            <p-card [header]="endpoint.name">
              <p>{{ endpoint.description }}</p>
              @if (endpoint.details.length) {
                <ul>
                  @for (detail of endpoint.details; track detail) {
                    <li>{{ detail }}</li>
                  }
                </ul>
              }
              <ng-template #footer>
                <a [href]="endpoint.path + '?wsdl=true'" target="_blank">
                  <p-button [label]="endpoint.path" icon="pi pi-external-link" severity="secondary" [outlined]="true" size="small" />
                </a>
              </ng-template>
            </p-card>
          }
        </div>
      </p-panel>
    </div>
  `,
  styles: `
    .wsdl-page { max-width: 960px; }
    h1 { margin: 0 0 1rem; }
    h3 { margin: 1rem 0 0.5rem; }
    .mt-4 { margin-top: 1rem; }
    .endpoints-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
      gap: 1rem;
      margin-top: 1rem;
    }
    ul { padding-left: 1.25rem; }
    a { text-decoration: none; }
  `,
})
export class WsdlPageComponent {
  private tenantContext = inject(TenantContextService);

  soapUrl = computed(() => `${environment.apiBaseUrl}/tenant/${this.tenantContext.tenantName()}/soap`);

  endpoints = computed(() => {
    const base = 'wsdl-demo';
    return [
      {
        name: 'Default',
        path: `${base}/default`,
        description: 'This default setting responds exactly as expected by NIST test cases. It is completely compliant.',
        details: []
      },
      {
        name: 'No White Space',
        path: `${base}/nowhitespace`,
        description: 'Same as the default but there is no white space in XML.',
        details: []
      },
      {
        name: 'Mistakes',
        path: `${base}/mistakes`,
        description: 'This responds with properly formed XML but with problems with which tags are being used.',
        details: []
      },
      {
        name: 'Bad XML',
        path: `${base}/badxml`,
        description: 'This responds with badly formed XML, in each case the close body tag is omitted.',
        details: []
      },
      {
        name: 'Incorrect Implementation',
        path: `${base}/incorrectImplementation`,
        description: 'This responds with the correct XML but has not properly implemented the methods and the contents are not correct.',
        details: ['submitSingleMessage: Returns a human readable message instead of an acknowledgement.', 'connectivityTestResponse: Sends back message but it does not contain original text sent in.']
      },
      {
        name: 'Additional Tag',
        path: `${base}/additionalTag`,
        description: 'This responds correctly except the response contains an additional tag that is not expected.',
        details: []
      },
      {
        name: 'Base 64',
        path: `${base}/base64`,
        description: 'This responds with the correct XML but has base64 encoded content.',
        details: []
      },
      {
        name: 'URL Encoded',
        path: `${base}/urlEncoded`,
        description: 'This responds with the correct XML but has URL encoded content.',
        details: []
      },
    ];
  });
}
