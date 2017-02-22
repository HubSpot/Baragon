import { FetchElbs } from '../api/elbs';

export const refresh = () => (dispatch) =>
  dispatch(FetchElbs.trigger());
