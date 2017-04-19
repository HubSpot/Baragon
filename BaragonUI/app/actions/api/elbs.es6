import { buildApiAction, buildJsonApiAction } from './base';

export const FetchElbs = buildApiAction(
  'FETCH_ELBS',
  {url: '/elbs'},
);

export const FetchElb = buildApiAction(
  'FETCH_ELB',
  (elbName, renderNotFoundIf404) => ({
    url: `/elbs/${elbName}`,
    renderNotFoundIf404
  }),
  (elbName) => elbName,
);

export const FetchElbInstances = buildApiAction(
  'FETCH_ELB_INSTANCES',
  (elbName) => ({
    url: `/elbs/${elbName}/instances`
  }),
  (elbName) => elbName
);

export const AddToElb = buildJsonApiAction(
  'ADD_TO_ELB',
  'POST',
  (elbName, instanceId) => ({
    url: `/elbs/${elbName}/update?instanceId=${instanceId}`
  })
);

export const RemoveFromElb = buildApiAction(
  'REMOVE_FROM_ELB',
  (elbName, instanceId) => ({
    method: 'DELETE',
    url: `/elbs/${elbName}/update?instanceId=${instanceId}`
  })
);
