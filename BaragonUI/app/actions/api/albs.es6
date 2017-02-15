import { buildApiAction, buildJsonApiAction } from './base';

export const FetchTargetGroups = buildApiAction(
  'FETCH_TARGET_GROUPS',
  {url: '/albs/target-groups'}
);

export const FetchLoadBalancers = buildApiAction(
  'FETCH_APPLICATION_LOAD_BALANCERS',
  {url: '/albs/load-balancers'}
);
