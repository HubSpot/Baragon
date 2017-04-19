import { combineReducers } from 'redux';
import buildApiActionReducer from './base';
import buildKeyedApiActionReducer from './keyed';

import {
  FetchBaragonStatus
} from '../../actions/api/status';

import {
  FetchBaragonServiceWorkers
} from '../../actions/api/workers';

import {
  FetchQueuedRequests,
  FetchRequestHistory,
  FetchRequestResponse,
  SubmitRequest
} from '../../actions/api/requests';

import {
  FetchBaragonGroups,
  FetchGroup,
  FetchGroupBasePaths,
  FetchGroupTargetCount,
  FetchGroupAgents,
  FetchGroupKnownAgents
} from '../../actions/api/groups';

import {
  FetchBaragonServices,
  FetchService,
  DeleteService,
  ReloadService,
} from '../../actions/api/services';

import {
  FetchElbs,
  FetchElb,
  FetchElbInstances,
  AddToElb,
  RemoveFromElb
} from '../../actions/api/elbs';

import {
  FetchTargetGroups,
  FetchTargetGroup,
  FetchTargetGroupTargets,
  FetchLoadBalancers,
  FetchLoadBalancer,
  FetchLoadBalancerListeners,
  FetchListenerRules,
  AddToTargetGroup,
  RemoveFromTargetGroup
} from '../../actions/api/albs';

const status = buildApiActionReducer(FetchBaragonStatus);
const workers = buildApiActionReducer(FetchBaragonServiceWorkers, []);
const queuedRequests = buildApiActionReducer(FetchQueuedRequests, []);
const groups = buildApiActionReducer(FetchBaragonGroups, []);
const group = buildKeyedApiActionReducer(FetchGroup, []);
const basePaths = buildKeyedApiActionReducer(FetchGroupBasePaths, []);
const targetCount = buildKeyedApiActionReducer(FetchGroupTargetCount, 0);
const agents = buildKeyedApiActionReducer(FetchGroupAgents, []);
const knownAgents = buildKeyedApiActionReducer(FetchGroupKnownAgents, []);
const services = buildApiActionReducer(FetchBaragonServices, []);
const service = buildKeyedApiActionReducer(FetchService, []);
const deleteService = buildKeyedApiActionReducer(DeleteService, []);
const reloadService = buildKeyedApiActionReducer(ReloadService, []);
const requestHistory = buildKeyedApiActionReducer(FetchRequestHistory, []);
const requestResponse = buildKeyedApiActionReducer(FetchRequestResponse, {});
const submitRequest = buildKeyedApiActionReducer(SubmitRequest, []);
const elbs = buildApiActionReducer(FetchElbs, []);
const elb = buildKeyedApiActionReducer(FetchElb, []);
const elbInstances = buildKeyedApiActionReducer(FetchElbInstances, []);
const targetGroups = buildApiActionReducer(FetchTargetGroups, []);
const targetGroup = buildKeyedApiActionReducer(FetchTargetGroup, {});
const targetGroupTargets = buildKeyedApiActionReducer(FetchTargetGroupTargets, []);
const albs = buildApiActionReducer(FetchLoadBalancers, []);
const alb = buildKeyedApiActionReducer(FetchLoadBalancer, {});
const listeners = buildKeyedApiActionReducer(FetchLoadBalancerListeners, []);
const rules = buildKeyedApiActionReducer(FetchListenerRules, []);
const addToTargetGroup = buildKeyedApiActionReducer(AddToTargetGroup, []);
const removeFromTargetGroup = buildKeyedApiActionReducer(RemoveFromTargetGroup, []);
const addToElb = buildKeyedApiActionReducer(AddToElb, []);
const removeFromElb = buildKeyedApiActionReducer(RemoveFromElb, []);

export default combineReducers({
  status,
  workers,
  queuedRequests,
  groups,
  group,
  basePaths,
  targetCount,
  agents,
  knownAgents,
  services,
  service,
  deleteService,
  reloadService,
  requestHistory,
  requestResponse,
  submitRequest,
  elbs,
  elb,
  elbInstances,
  targetGroups,
  targetGroup,
  targetGroupTargets,
  albs,
  alb,
  listeners,
  rules,
  addToTargetGroup,
  removeFromTargetGroup,
  addToElb,
  RemoveFromElb
});
