import { buildApiAction, buildJsonApiAction } from './base';

export const FetchBaragonGroups = buildApiAction(
  'FETCH_BARAGON_GROUPS',
  {url: '/load-balancer/all'}
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
  'FETCH_GROUP_KNOWN_AGENTS',
  (groupId, renderNotFoundIf404) => ({
    url: `/load-balancer/${groupId}/known-agents`,
    renderNotFoundIf404
  }),
  (groupId) => groupId
);

export const AddTrafficSource = buildJsonApiAction(
  'ADD_GROUP_TRAFFIC_SOURCE',
  'POST',
  (groupId, requestData) => ({
    url: `/load-balancer/${groupId}/traffic-source`,
    body: requestData
  }),
  (groupId) => groupId
);

export const RemoveTrafficSource = buildJsonApiAction(
  'REMOVE_GROUP_TRAFFIC_SOURCE',
  'DELETE',
  (groupId, requestData) => ({
    url: `/load-balancer/${groupId}/traffic-source`,
    body: requestData
  }),
  (groupId) => groupId
);

export const RemoveKnownAgent = buildApiAction(
  'REMOVE_GROUP_KNOWN_AGENT',
  (groupId, agentId) => ({
    url: `/load-balancer/${groupId}/known-agents/${agentId}`,
    method: 'DELETE'
  }),
  (groupId) => groupId
);

export const RemoveBasePath = buildApiAction(
  'REMOVE_GROUP_BASE_PATH',
  (groupId, basePath) => ({
    url: `/load-balancer/${groupId}/base-path?basePath=${basePath}`,
    method: 'DELETE'
  }),
  (groupId) => groupId
);

export const ModifyTargetCount = buildApiAction(
  'MODIFY_GROUP_TARGET_COUNT',
  (groupId, newTargetCount) => ({
    url: `/load-balancer/${groupId}/count?count=${newTargetCount}`,
    method: 'POST'
  }),
  (groupId) => groupId
)
