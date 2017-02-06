import { buildApiAction } from './base';

export const FetchQueuedRequests = buildApiAction(
  'FETCH_QUEUED_REQUESTS',
  {url: '/request'}
);
