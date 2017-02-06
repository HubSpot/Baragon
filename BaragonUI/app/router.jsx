import React from 'react';
import { Provider } from 'react-redux';
import { Router, Route, IndexRoute, useRouterHistory } from 'react-router';
import { createHistory } from 'history';
import { syncHistoryWithStore } from 'react-router-redux';

import Application from './components/common/Application';
import NotFound from './components/common/NotFound';
import StatusPage from './components/status/StatusPage';

const getFilenameFromSplat = (splat) => _.last(splat.split('/'));

const routes = (
  <Route path="/" component={Application}>
    <IndexRoute component={StatusPage} title="Status" />
    <Route path="*" component={NotFound} title="Not Found" />
  </Route>);

const AppRouter = (props) => {
  const syncedHistory = syncHistoryWithStore(props.history, props.store);

  return (
    <Provider store={props.store}>
      <Router history={syncedHistory} routes={routes} />
    </Provider>
  );
};

AppRouter.propTypes = {
  store: React.PropTypes.object.isRequired
};

export default AppRouter;
