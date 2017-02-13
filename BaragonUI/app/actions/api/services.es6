import { buildApiAction, buildJsonApiAction } from './base';

const buildRequestId = (serviceId) => `${serviceId}-${Date.now()}`;

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

export const DeleteService = buildJsonApiAction(
  'DELETE_SERVICE',
  'DELETE',
  (serviceId, noValidate = false, noReload = false) => ({
    url: `/state/${serviceId}?noValidate=${noValidate}&noReload=${noReload}`,
  })
);

export const ReloadService = buildJsonApiAction(
  'RELOAD_SERVICE',
  'POST',
  (serviceId) => ({
    url: `/state/${serviceId}/reload`,
  })
);

export const RemoveUpstreams = buildJsonApiAction(
  'REMOVE_UPSTREAMS',
  'POST',
  (loadBalancerService, upstreams, noValidate, noReload) => ({
    url: '/request',
    body: {
      loadBalancerService,
      noValidate,
      noReload,
      loadBalancerRequestId: buildRequestId(loadBalancerService.serviceId),
      addUpstreams: [],
      removeUpstreams: upstreams,
    },
  }),
  (serviceId) => serviceId,
);
