import { FetchElb } from '../api/elbs';

export const refresh = (loadBalancerName) => (dispatch) =>
  dispatch(FetchElb.trigger(loadBalancerName));
