import { buildApiAction, buildJsonApiAction } from './base';

export const FetchTargetGroups = buildApiAction(
  'FETCH_TARGET_GROUPS',
  {url: '/albs/target-groups'}
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

export const RemoveFromTargetGroup = buildJsonApiAction(
  'REMOVE_FROM_TARGET_GROUP',
  'DELETE',
  (groupName, instanceId) => ({
    url: `/albs/target-groups/${groupName}/targets/${instanceId}`,
  }),
);

export const AddToTargetGroup = buildJsonApiAction(
  'ADD_TO_TARGET_GROUP',
  'POST',
  (groupName, instanceId) => ({
    url: `/albs/target-group/${groupName}/targets?instanceId=${instanceId}`,
  }),
);

export const ModifyTargetGroup = buildJsonApiAction(
  'MODIFY_TARGET_GROUP',
  'POST',
  (groupName, body) => ({
    body,
    url: `/albs/target-groups/${groupName}`,
  }),
);

export const FetchLoadBalancers = buildApiAction(
  'FETCH_APPLICATION_LOAD_BALANCERS',
  {url: '/albs/load-balancers'}
);

export const FetchLoadBalancer = buildApiAction(
  'FETCH_APPLICATION_LOAD_BALANCER',
  (loadBalancerName) => ({
    url: `/albs/load-balancers/${loadBalancerName}`
  }),
  (loadBalancerName) => loadBalancerName
);

export const FetchLoadBalancerListeners = buildApiAction(
  'FETCH_APPLICATION_LOAD_BALANCER_LISTENERS',
  (loadBalancerName) => ({
    url: `/albs/load-balancers/${loadBalancerName}/listeners`
  }),
  (loadBalancerName) => loadBalancerName
);

export const FetchListenerRules = buildApiAction(
  'FETCH_LISTENER_RULES',
  (listenerArn) => ({
    url: `/albs/listeners/rules/${listenerArn}`
  }),
  (listenerArn) => listenerArn
);
