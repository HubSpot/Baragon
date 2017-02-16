import { buildApiAction, buildJsonApiAction } from './base';

export const FetchTargetGroups = buildApiAction(
  'FETCH_TARGET_GROUPS',
  {url: '/albs/target-groups'}
);

export const FetchLoadBalancers = buildApiAction(
  'FETCH_APPLICATION_LOAD_BALANCERS',
  {url: '/albs/load-balancers'}
);

export const FetchTargetGroup = buildApiAction(
  'FETCH_TARGET_GROUP',
  (groupName, renderNotFoundIf404) => ({
    url: `/albs/target-groups/${groupName}`,
    renderNotFoundIf404
  }),
  (groupName) => groupName
);

export const FetchTargetGroupTargets = buildApiAction(
  'FETCH_TARGET_GROUP_TARGETS',
  (groupName) => ({
    url: `/albs/target-groups/${groupName}/targets`
  }),
  (groupName) => groupName
);
