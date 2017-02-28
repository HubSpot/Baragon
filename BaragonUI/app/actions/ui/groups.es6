import { FetchBaragonGroups } from '../../actions/api/groups';

export const refresh = () => (dispatch) =>
  dispatch(FetchBaragonGroups.trigger());
