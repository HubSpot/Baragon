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
} from '../../actions/api/groups'

import {
  FetchBaragonServices,
  FetchService,
  DeleteService,
  ReloadService
} from '../../actions/api/services';

import {
  FetchElbs,
  FetchElb
} from '../../actions/api/elbs';

import {
  FetchLoadBalancers,
  FetchTargetGroups,
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
const requestResponse = buildKeyedApiActionReducer(FetchRequestResponse, []);
const submitRequest = buildKeyedApiActionReducer(SubmitRequest, []);
const elbs = buildApiActionReducer(FetchElbs, []);
const elb = buildKeyedApiActionReducer(FetchElb, []);
const loadBalancers = buildApiActionReducer(FetchLoadBalancers, []);
const targetGroups = buildApiActionReducer(FetchTargetGroups, []);

export default combineReducers({
  status,
  workers,
  queuedRequests,
  groups,
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
  loadBalancers,
  targetGroups,
});
