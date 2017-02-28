import { buildApiAction } from './base';

export const FetchBaragonStatus = buildApiAction(
  'FETCH_BARAGON_STATUS',
  {url: '/status/master'}
);
