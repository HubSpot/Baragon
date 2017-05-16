import { refresh as albs } from './albs';
import { refresh as elbs } from './elbs';

export const refresh = () => (dispatch) =>
  Promise.all([
    albs()(dispatch),
    elbs()(dispatch)
  ]);
