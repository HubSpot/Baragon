import {
  FetchTargetGroup,
  FetchTargetGroupTargets,
  FetchLoadBalancers,
} from '../api/albs';

export const refresh = (groupName) => (dispatch) =>
  Promise.all([
    dispatch(FetchTargetGroup.trigger(groupName)),
    dispatch(FetchTargetGroupTargets.trigger(groupName)),
    dispatch(FetchLoadBalancers.trigger()),
  ]);
