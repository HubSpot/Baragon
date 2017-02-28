import {
  FetchTargetGroups,
  FetchLoadBalancers
} from '../api/albs';

export const refresh = () => (dispatch) =>
  Promise.all([
    dispatch(FetchTargetGroups.trigger()),
    dispatch(FetchLoadBalancers.trigger()),
  ]);
