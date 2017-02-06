import _ from 'underscore';
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
  FetchRequestHistory
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
  FetchService
} from '../../actions/api/services';



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
const requestHistory = buildKeyedApiActionReducer(FetchRequestHistory, [])

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
  requestHistory
});
