import { FetchBaragonServices } from '../../actions/api/services';

export const refresh = () => (dispatch) =>
  dispatch(FetchBaragonServices.trigger())