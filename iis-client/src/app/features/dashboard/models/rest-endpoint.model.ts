/**
 * Catalogue of the REST endpoints served under /rest, for the dashboard panel.
 * <p>
 * Transcribed from the @RequestMapping annotations on the controllers in
 * org.immregistries.iis.kernal.controllers.rest. Paths are shown relative to the servlet
 * context (/iis), with {tenantName}, {patientId} and friends left as written in the
 * mappings.
 * <p>
 * The descriptions are first-pass summaries meant to be corrected by hand; the paths and
 * HTTP verbs are taken from the code and should be kept in step with it.
 */
export interface RestOperation {
  /** GET, POST, PUT or DELETE. */
  method: 'GET' | 'POST' | 'PUT' | 'DELETE';
  /** Path suffix appended to the entry's `path`, blank when the verb hits the base path. */
  suffix?: string;
  description: string;
}

export interface RestEndpoint {
  path: string;
  /** Shown when the entry exposes a single operation, or describes the group as a whole. */
  description: string;
  /** Present only where several HTTP operations share the base path. */
  operations?: RestOperation[];
  /** Non-JSON response types worth knowing about before calling. */
  note?: string;
}

export interface RestEndpointGroup {
  name: string;
  endpoints: RestEndpoint[];
}

export const REST_ENDPOINT_GROUPS: RestEndpointGroup[] = [
  {
    name: 'Authentication and users',
    endpoints: [
      {
        path: '/rest/authentication',
        description: 'Current session: authenticated flag, username and granted authorities.',
      },
      {
        path: '/rest/user/me',
        description: 'Self service for the signed-in user. Requires an authenticated session.',
        operations: [
          {method: 'GET', description: 'Read your own profile: username, email, enabled flag and roles.'},
          {
            method: 'POST',
            suffix: '/password',
            description: 'Change your own password, supplying the current one. Answers 204.'
          },
          {method: 'PUT', suffix: '/email', description: 'Set or clear your own email address.'},
        ],
      },
      {
        path: '/rest/admin/users',
        description: 'Administration of every account. Requires ROLE_ADMIN.',
        operations: [
          {method: 'GET', description: 'List every user with their roles and owned tenants.'},
          {method: 'POST', description: 'Create a user with a name, password and optional roles.'},
          {method: 'GET', suffix: '/{userId}', description: 'Read one user.'},
          {
            method: 'PUT',
            suffix: '/{userId}/password',
            description: 'Reset a password without the old one. Answers 204.'
          },
          {
            method: 'PUT',
            suffix: '/{userId}/enabled',
            description: 'Enable or disable an account. The last enabled ADMIN cannot be disabled.'
          },
          {
            method: 'PUT',
            suffix: '/{userId}/roles',
            description: 'Replace the role set. An empty set falls back to USER.'
          },
          {method: 'PUT', suffix: '/{userId}/email', description: 'Set or clear a user email address.'},
        ],
      },
    ],
  },
  {
    name: 'Tenants',
    endpoints: [
      {
        path: '/rest/tenant',
        description: 'Testing environments, each backed by its own FHIR partition.',
        operations: [
          {method: 'GET', description: 'List the tenants owned by the signed-in user.'},
          {method: 'POST', description: 'Create a tenant.'},
          {method: 'GET', suffix: '/{tenantId}', description: 'Read one tenant.'},
        ],
      },
      {
        path: '/rest/flavors',
        description: 'The processing flavors a tenant name can activate, with their behavior descriptions.',
      },
      {
        path: '/rest/tenantCompare',
        description: 'Compare the contents of several tenants.',
        operations: [
          {method: 'GET', description: 'Read the last comparison result.'},
          {
            method: 'POST',
            description: 'Run a comparison across the given tenant ids, optionally including MDM golden records.'
          },
        ],
      },
      {
        path: '/rest/tenant/{tenantName}/code-maps',
        description: 'Code tables from the compiled codebase.',
        operations: [
          {method: 'GET', description: 'List the available code maps.'},
          {method: 'GET', suffix: '/search', description: 'Look up the codes in one table by name.'},
        ],
        note: 'Also mapped without the tenant prefix, but that form fails: the handler requires the tenant request attribute.',
      },
    ],
  },
  {
    name: 'Patients and records',
    endpoints: [
      {
        path: '/rest/tenant/{tenantName}/patient',
        description: 'Patient records held in the tenant partition.',
        operations: [
          {method: 'GET', description: 'List the patients in the tenant.'},
          {method: 'GET', suffix: '/search', description: 'Search patients by the supplied criteria.'},
          {method: 'GET', suffix: '/{patientId}', description: 'Read one patient.'},
          {
            method: 'GET',
            suffix: '/{patientId}/fhir-resource',
            description: 'The patient as its underlying FHIR resource.'
          },
          {method: 'GET', suffix: '/{patientId}/vaccination', description: 'Immunizations recorded for the patient.'},
          {method: 'GET', suffix: '/{patientId}/observations', description: 'Observations recorded for the patient.'},
          {method: 'GET', suffix: '/{patientId}/related', description: 'Records matched or linked to the patient.'},
          {
            method: 'GET',
            suffix: '/{patientId}/recommendation',
            description: 'Forecast recommendations for the patient.'
          },
          {
            method: 'GET',
            suffix: '/{patientId}/sh-link-payload',
            description: 'The SMART Health Link payload built for the patient.'
          },
          {
            method: 'GET',
            suffix: '/{patientId}/sh-link-payload/ips',
            description: 'The same payload as an International Patient Summary.'
          },
        ],
      },
      {
        path: '/rest/tenant/{tenantName}/vaccination',
        description: 'Immunization records in the tenant.',
        operations: [
          {method: 'GET', description: 'List vaccinations in the tenant.'},
          {method: 'GET', suffix: '/{vaccinationId}', description: 'Read one vaccination.'},
          {
            method: 'GET',
            suffix: '/{vaccinationId}/related',
            description: 'Records matched or linked to the vaccination.'
          },
        ],
      },
      {
        path: '/rest/tenant/{tenantName}/message',
        description: 'HL7v2 messages received by the tenant.',
        operations: [
          {method: 'GET', description: 'List received messages.'},
          {method: 'GET', suffix: '/{patientId}', description: 'Messages tied to one patient.'},
        ],
      },
      {
        path: '/rest/tenant/{tenantName}/recommendation',
        description: 'Immunization forecast recommendations.',
        operations: [
          {method: 'GET', description: 'Read the stored recommendations.'},
          {method: 'PUT', description: 'Store or update a recommendation.'},
          {method: 'POST', suffix: '/random', description: 'Generate a randomised forecast, used for demonstration.'},
        ],
      },
      {
        path: '/rest/tenant/{tenantName}/group/$generate',
        description: 'Generate a Group resource covering the tenant population.',
      },
    ],
  },
  {
    name: 'Messaging entry points',
    endpoints: [
      {
        path: '/rest/tenant/{tenantName}/pop',
        description: 'HL7v2 entry point, the one the Pop UI posts to.',
        operations: [
          {method: 'GET', suffix: '/sample', description: 'A sample VXU message to start from.'},
          {method: 'POST', description: 'Submit an HL7v2 message and read the acknowledgement.'},
        ],
        note: 'Responds as text/plain.',
      },
      {
        path: '/rest/fhir-messaging',
        description: 'FHIR messaging entry point. Also mounted under /rest/tenant/{tenantName}.',
        operations: [
          {method: 'GET', suffix: '/sample', description: 'A sample FHIR message bundle.'},
          {method: 'POST', description: 'Submit a FHIR message bundle for processing.'},
          {method: 'POST', suffix: '/soap', description: 'The same processing reached over the SOAP envelope.'},
        ],
        note: 'Responds as text/plain.',
      },
      {
        path: '/rest/soap',
        description: 'CDC SOAP connectivity endpoint. Also mounted under /rest/tenant/{tenantName}.',
      },
      {
        path: '/rest/v2-to-fhir',
        description: 'Convert an HL7v2 message to FHIR resources. Also mounted under /rest/tenant/{tenantName}.',
      },
      {
        path: '/rest/mapping',
        description: 'Convert between the IIS domain model and FHIR.',
        operations: [
          {method: 'POST', suffix: '/fhir', description: 'Map an IIS model payload to its FHIR form.'},
          {method: 'POST', suffix: '/local', description: 'Map a FHIR resource back to the IIS model.'},
        ],
      },
      {
        path: '/rest/tenant/{tenantName}/subscription',
        description: 'FHIR Subscriptions for the tenant.',
        operations: [
          {method: 'GET', description: 'List the subscriptions as a bundle.'},
          {method: 'POST', suffix: '/trigger', description: 'Trigger a subscription with the supplied messages.'},
        ],
      },
      {
        path: '/rest/SubscriptionTopic',
        description: 'The SubscriptionTopic definitions this server publishes.',
        operations: [
          {method: 'GET', description: 'List every topic.'},
          {
            method: 'GET',
            suffix: '/{topicName}',
            description: 'Read one topic: patient, group or data quality issues.'
          },
        ],
      },
    ],
  },
  {
    name: 'SMART Health Links',
    endpoints: [
      {
        path: '/rest/sh-link',
        description: 'SMART Health Links. Also mounted under /rest/tenant/{tenantName}.',
        operations: [
          {method: 'POST', description: 'Generate a link for a patient.'},
          {method: 'GET', description: 'List the links issued in the tenant.'},
          {method: 'GET', suffix: '/{shLinkId}', description: 'Read one link.'},
          {method: 'DELETE', suffix: '/{shLinkId}', description: 'Delete a link.'},
        ],
      },
      {
        path: '/rest/sh-link-files/{contentId}',
        description: 'Fetch the encrypted content a link points at.',
        note: 'Responds as text/plain.',
      },
      {
        path: '/rest/stored-manifest',
        description: 'Stored SMART Health Link manifests.',
        operations: [
          {method: 'GET', description: 'List the stored manifests.'},
          {method: 'GET', suffix: '/{manifestId}', description: 'Read one manifest.'},
          {
            method: 'POST',
            suffix: '/{manifestId}',
            description: 'Resolve a manifest, supplying any passcode it requires.'
          },
          {method: 'GET', suffix: '/$generate', description: 'Generate a manifest.'},
        ],
      },
      {
        path: '/rest/tenant/{tenantName}/patient-manifest',
        description: 'Manifests built directly from a patient record.',
        operations: [
          {method: 'GET', suffix: '/patient/{id}', description: 'Read the manifest for a patient.'},
          {method: 'POST', suffix: '/patient/{id}', description: 'Build the manifest for a patient.'},
          {
            method: 'GET',
            suffix: '/patient/{id}/ips',
            description: 'The same manifest as an International Patient Summary.'
          },
          {method: 'POST', suffix: '/patient/{id}/ips', description: 'Build the IPS form of the manifest.'},
        ],
      },
      {
        path: '/rest/tenant/{tenantName}/patient/{patientId}/patient-sh-link',
        description: 'The link for a patient rendered as a QR code image.',
        note: 'Responds as image/png.',
      },
      {
        path: '/restiis-keys',
        description: 'Signing keys used for health cards.',
        operations: [
          {method: 'GET', description: 'List the keys held for the signed-in user.'},
          {
            method: 'GET',
            suffix: '/{keyId}/$getOrCreate',
            description: 'Read a key, creating it when it does not exist yet. Also mounted under /rest/tenant/{tenantName}iis-keys.'
          },
        ],
        note: 'The missing slash is real: IisKeyRestController concatenates "/rest" with "iis-keys", so the live path is /restiis-keys.',
      },
    ],
  },
  {
    name: 'CLVR',
    endpoints: [
      {
        path: '/rest/tenant/{tenantName}/patient/{patientId}/clvr',
        description: 'CLVR card for a patient.',
        operations: [
          {method: 'GET', suffix: '/qr', description: 'The card as QR payload text.'},
          {method: 'GET', suffix: '/pdf', description: 'The card as a PDF document.'},
        ],
      },
      {
        path: '/rest/tenant/{tenantName}/clvr/test',
        description: 'Test client for building and checking CLVR cards step by step.',
        operations: [
          {method: 'GET', suffix: '/example-key', description: 'An example signing key.'},
          {method: 'POST', suffix: '/load-key', description: 'Load a signing key into the test session.'},
          {method: 'GET', suffix: '/example-fhir', description: 'An example FHIR payload.'},
          {method: 'POST', suffix: '/convert-fhir', description: 'Convert a FHIR payload to the card structure.'},
          {method: 'POST', suffix: '/sign-and-compress', description: 'Sign and compress the card payload.'},
          {method: 'POST', suffix: '/parse-qr', description: 'Parse a QR payload back into a card.'},
          {method: 'POST', suffix: '/check-signature', description: 'Verify a card signature.'},
          {method: 'POST', suffix: '/export-pdf', description: 'Export the card as a PDF.'},
        ],
      },
    ],
  },
  {
    name: 'Tools and legacy',
    endpoints: [
      {
        path: '/rest/query-converter',
        description: 'Convert a VXU message into a QBP query.',
        operations: [
          {method: 'GET', suffix: '/sample', description: 'A sample message to convert.'},
          {method: 'POST', description: 'Convert the submitted message.'},
        ],
        note: 'Responds as text/plain.',
      },
      {
        path: '/rest/lab-converter',
        description: 'Convert an ORU lab message into a VXU. Also mounted under /rest/tenant/{tenantName}.',
        operations: [
          {method: 'GET', suffix: '/sample', description: 'A sample ORU message.'},
          {method: 'POST', description: 'Convert the submitted message.'},
        ],
        note: 'Responds as text/plain.',
      },
      {
        path: '/rest/covid-generate',
        description: 'Generate synthetic COVID-19 HL7 messages.',
        note: 'Responds as text/plain.',
      },
      {
        path: '/rest/tenant/{tenantName}/covid-export',
        description: 'Export the tenant COVID data as the CDC flat file.',
        note: 'Responds as text/plain.',
      },
      {
        path: '/rest/tenant/{tenantName}/vxu-download',
        description: 'Download COVID VXU messages for CDC reporting.',
        note: 'Responds as text/plain.',
      },
      {
        path: '/rest/tenant/{tenantName}/vacDedup',
        description: 'Run vaccination deduplication over the submitted records.',
      },
      {
        path: '/rest/fits/inspect',
        description: 'Parse an RSP message and report what the FITS inspector finds.',
      },
      {
        path: '/rest/fits/example',
        description: 'Example RSP messages for the inspector.',
        operations: [
          {method: 'GET', description: 'Read one example.'},
          {method: 'GET', suffix: '/all', description: 'Read every example.'},
        ],
      },
      {
        path: '/rest/vci-demo',
        description: 'Convert an RSP message into a verifiable credential.',
        operations: [
          {method: 'GET', suffix: '/sample', description: 'A sample RSP message.'},
          {method: 'POST', suffix: '/convert', description: 'Convert the submitted message.'},
        ],
      },
      {
        path: '/rest/$create',
        description: 'Macro endpoint for creating records from a compact payload.',
        operations: [
          {method: 'GET', description: 'Read the macro definition.'},
          {
            method: 'GET',
            suffix: '/StructureDefinition',
            description: 'The StructureDefinition describing the payload.'
          },
          {method: 'POST', description: 'Create records from the submitted payload.'},
        ],
      },
      {
        path: '/rest/tenant/{tenantName}/patientMatchingDatasetConversion',
        description: 'Build a patient matching dataset from tenant records.',
        operations: [
          {method: 'POST', suffix: '/init', description: 'Start a new dataset builder.'},
          {method: 'POST', description: 'Add a bundle to the dataset being built.'},
          {method: 'GET', suffix: '/all', description: 'Download the assembled dataset.'},
        ],
      },
    ],
  },
];
