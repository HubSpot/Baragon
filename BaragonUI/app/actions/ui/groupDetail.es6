import {
  FetchGroup,
  FetchGroupBasePaths,
  FetchGroupTargetCount,
  FetchGroupAgents,
  FetchGroupKnownAgents
} from '../../actions/api/groups'

export const refresh = (groupId) => (dispatch) =>
  Promise.all([
    dispatch(FetchGroup.trigger(groupId)),
    dispatch(FetchGroupBasePaths.trigger(groupId)),
    dispatch(FetchGroupTargetCount.trigger(groupId)),
    dispatch(FetchGroupAgents.trigger(groupId)),
    dispatch(FetchGroupKnownAgents.trigger(groupId))
  ]);
