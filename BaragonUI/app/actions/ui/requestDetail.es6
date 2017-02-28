import { FetchRequestResponse } from '../api/requests';

export const refresh = (requestId) => dispatch => {
  Promise.all([
    dispatch(FetchRequestResponse.trigger(requestId)),
  ]);
};
