import { buildApiAction, buildJsonApiAction } from './base';

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

export const FetchRenderedConfigs = buildApiAction(
    'FETCH_RENDERED_CONFIGS',
    (serviceId) => ({
        url: `/renderedConfigs/${serviceId}`
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

export const PurgeCache = buildJsonApiAction(
    'PURGE_CACHE',
    'POST',
    (serviceId) => ({
        url: `/purgeCache/${serviceId}`,
    })
);

export const ReloadService = buildJsonApiAction(
  'RELOAD_SERVICE',
  'POST',
  (serviceId) => ({
    url: `/state/${serviceId}/reload`,
  })
);
