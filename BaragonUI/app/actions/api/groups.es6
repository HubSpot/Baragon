import { buildApiAction } from './base';

export const FetchBaragonGroups = buildApiAction(
  'FETCH_BARAGON_GROUPS',
  {url: '/load-balancer'}
);

export const FetchGroup = buildApiAction(
  'FETCH_GROUP',
  (groupId, renderNotFoundIf404) => ({
    url: `/load-balancer/${groupId}`,
    renderNotFoundIf404
  }),
  (groupId) => groupId
);

export const FetchGroupBasePaths = buildApiAction(
  'FETCH_GROUP_BASE_PATHS',
  (groupId, renderNotFoundIf404) => ({
    url: `/load-balancer/${groupId}/base-path/all`,
    renderNotFoundIf404
  }),
  (groupId) => groupId
);

export const FetchGroupTargetCount = buildApiAction(
  'FETCH_GROUP_TARGET_COUNT',
  (groupId, renderNotFoundIf404) => ({
    url: `/load-balancer/${groupId}/count`,
    renderNotFoundIf404
  }),
  (groupId) => groupId
);

export const FetchGroupAgents = buildApiAction(
  'FETCH_GROUP_AGENTS',
  (groupId, renderNotFoundIf404) => ({
    url: `/load-balancer/${groupId}/agents`,
    renderNotFoundIf404
  }),
  (groupId) => groupId
);

export const FetchGroupKnownAgents = buildApiAction(
  'FETCH_GROUP',
  (groupId, renderNotFoundIf404) => ({
    url: `/load-balancer/${groupId}/known-agents`,
    renderNotFoundIf404
  }),
  (groupId) => groupId
);