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
  FetchQueuedRequests
} from '../../actions/api/requests';

const status = buildApiActionReducer(FetchBaragonStatus);
const workers = buildApiActionReducer(FetchBaragonServiceWorkers);
const queuedRequests = buildApiActionReducer(FetchQueuedRequests);

export default combineReducers({
  status,
  workers,
  queuedRequests
});
