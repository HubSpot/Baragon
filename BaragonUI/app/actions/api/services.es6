import { buildApiAction } from './base';

export const FetchBaragonServices = buildApiAction(
  'FETCH_BARAGON_SERVICES',
  {url: '/state'}
);

export const FetchService = buildApiAction(
  'FETCH_SERVICE',
  (serviceId, renderNotFoundIf404) => ({
    url: `/state/${serviceId}`,
    renderNotFoundIf404
  }),
  (serviceId) => serviceId
);