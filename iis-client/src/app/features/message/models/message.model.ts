export interface MessageReceived {
  messageReceivedId: number;
  messageRequest: string;
  messageResponse: string;
  patientReportedId: string | null;
  reportedDate: string | null;
  categoryRequest: string;
  categoryResponse: string;
}
