import { buildApiAction } from './base';

export const FetchElbs = buildApiAction(
  'FETCH_ELBS',
  {url: '/elbs'},
);

export const FetchElb = buildApiAction(
  'FETCH_ELB',
  (elbName, renderNotFoundIf404) => ({
    url: `/elbs/${elbName}`,
    renderNotFoundIf404
  }),
  (elbName) => elbName,
);
