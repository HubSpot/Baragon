import { buildApiAction } from './base';

export const FetchQueuedRequests = buildApiAction(
  'FETCH_QUEUED_REQUESTS',
  {url: '/request'}
);

export const FetchRequestHistory = buildApiAction(
  'FETCH_REQUEST_HISTORY',
  (serviceId, renderNotFoundIf404) => ({
    url: `/request/history/${serviceId}`,
    renderNotFoundIf404
  }),
  (serviceId) => serviceId
);