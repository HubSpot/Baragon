import fetch from 'isomorphic-fetch';
import Messenger from 'messenger';

/*
This specifically doesn't use `buildApiAction` because it needs to make a
impure change when the fetch resolves. In particular, it needs to store the key
that was given in local storage if it worked. It also modifies `window.config`
for the logged in / not logged in state.
*/

export const TestAuthKey = (function TestAuthKey() {
  const ACTION = 'TEST_AUTH_KEY';
  const STARTED = 'TEST_AUTH_KEY_STARTED';
  const ERROR = 'TEST_AUTH_KEY_ERROR';
  const SUCCESS = 'TEST_AUTH_KEY_SUCCESS';
  const CLEAR = 'TEST_AUTH_KEY_CLEAR';

  function clear() {
    return {type: CLEAR};
  }

  function started() {
    return {type: STARTED};
  }

  function error(err, apiResponse) {
    const action = {
      type: ERROR,
      error: err,
      statusCode: apiResponse,
    };

    if (apiResponse === 502) {
      Messenger().info({
        message: 'Baragon is deploying; your request could not be handled. Things should resolve in a few seconds, so hang tight!'
      });
    } else if (apiResponse === 403) {
      Messenger().error('Not a valid key!');
    }

    return action;
  }

  function success(authKey, statusCode) {
    localStorage.setItem('baragonAuthKey', authKey);
    config.allowEdit = true;

    return {type: SUCCESS, data: authKey, statusCode};
  }

  function clearData() {
    return (dispatch) => dispatch(clear());
  }

  function trigger(authKey) {
    return (dispatch) => {
      dispatch(started());

      return fetch(`${config.apiRoot}/auth/key/verify?authkey=${authKey}`)
        .then(response => {
          if (response.status === 204) {
            return dispatch(success(authKey, response.status));
          } else {
            return dispatch(error(response, response.status));
          }
        });
    };
  }

  return {
    ACTION,
    STARTED,
    ERROR,
    SUCCESS,
    CLEAR,
    clear,
    started,
    error,
    success,
    clearData,
    trigger,
  };
})();


export const DisableAuthKey = (function DisableAuthKey() {
  const ACTION = 'DISABLE_AUTH_KEY';

  function trigger() {
    return (dispatch) => {
      config.allowEdit = false;
      localStorage.removeItem('baragonAuthKey');
      return dispatch({type: ACTION});
    };
  }

  return {
    ACTION,
    trigger,
  };
})();
