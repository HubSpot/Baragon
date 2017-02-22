import {
  FetchLoadBalancer,
  FetchLoadBalancerListeners,
  FetchTargetGroups,
  FetchListenerRules,
} from '../api/albs';


export const refresh = (loadBalancerName) => (dispatch) =>
  Promise.all([
    dispatch(FetchLoadBalancer.trigger(loadBalancerName)),
    dispatch(FetchLoadBalancerListeners.trigger(loadBalancerName))
      .then(listeners => Promise.all(listeners.data.map(({listenerArn}) =>
        dispatch(FetchListenerRules.trigger(listenerArn))
      ))),
    dispatch(FetchTargetGroups.trigger()),
  ]);
