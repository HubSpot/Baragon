import { buildApiAction } from './base';

export const FetchBaragonServiceWorkers = buildApiAction(
  'FETCH_BARAGON_SERVICE_WORKERS',
  {url: '/workers'}
);
