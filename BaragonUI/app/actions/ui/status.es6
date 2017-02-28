import { FetchBaragonStatus } from '../../actions/api/status';
import { FetchBaragonServiceWorkers } from '../../actions/api/workers';
import { FetchQueuedRequests } from '../../actions/api/requests';

export const refresh = () => (dispatch) =>
  Promise.all([
    dispatch(FetchBaragonStatus.trigger()),
    dispatch(FetchBaragonServiceWorkers.trigger()),
    dispatch(FetchQueuedRequests.trigger())
  ]);
