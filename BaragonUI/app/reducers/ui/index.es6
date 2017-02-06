import { combineReducers } from 'redux';

import refresh from './refresh';
import form from './form';

export default combineReducers({
  refresh,
  form
});
