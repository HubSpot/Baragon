import {
  FetchLoadBalancer,
  FetchLoadBalancerListeners,
  FetchTargetGroups,
} from '../api/albs';


export const refresh = (loadBalancerName) => (dispatch) =>
  Promise.all([
    dispatch(FetchLoadBalancer.trigger(loadBalancerName)),
    dispatch(FetchLoadBalancerListeners.trigger(loadBalancerName)),
    dispatch(FetchTargetGroups.trigger()),
  ]);
