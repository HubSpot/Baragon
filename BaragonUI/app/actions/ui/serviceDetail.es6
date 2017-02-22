import { FetchService } from '../../actions/api/services';
import { FetchRequestHistory } from '../../actions/api/requests';

export const refresh = (serviceId) => (dispatch) =>
  Promise.all([
    dispatch(FetchService.trigger(serviceId)),
    dispatch(FetchRequestHistory.trigger(serviceId))
  ]);
