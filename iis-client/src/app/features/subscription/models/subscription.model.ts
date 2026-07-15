export interface SubscriptionItem {
  id: string;
  name: string;
  identifier: string;
  endpoint: string;
  status: string;
  topic: string;
  contentType: string;
}

export interface TriggerRequest {
  subscriptionId: string;
  messages: string[];
  httpVerbs: string[];
}
