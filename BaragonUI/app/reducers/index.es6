import { combineReducers } from 'redux';
import { routerReducer as routing } from 'react-router-redux';

import api from './api';
import ui from './ui';

export default combineReducers({
  api,
  ui,
  routing
});
