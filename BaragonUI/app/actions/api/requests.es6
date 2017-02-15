import { buildApiAction, buildJsonApiAction } from './base';

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

export const SubmitRequest = buildJsonApiAction(
 'SUBMIT_REQUEST',
  'POST',
  (body) => ({
    url: '/request',
    body
  }),
  (request) => request.loadBalancerRequestId
);

export const FetchRequestResponse = buildApiAction(
  'FETCH_REQUEST_RESPONSE',
  (requestId, renderNotFoundIf404 = true) => ({
    url: `/request/${requestId}`,
    renderNotFoundIf404
  }),
  (requestId) => requestId
);
