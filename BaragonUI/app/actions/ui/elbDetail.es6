import { FetchElb, FetchElbInstances } from '../api/elbs';

export const refresh = (loadBalancerName) => (dispatch) =>
  Promise.all([
    dispatch(FetchElb.trigger(loadBalancerName)),
    dispatch(FetchElbInstances.trigger(loadBalancerName)),
  ]);
