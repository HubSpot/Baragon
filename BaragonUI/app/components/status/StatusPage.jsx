import React from 'react';
import { connect } from 'react-redux';
import classNames from 'classnames';
import { Link } from 'react-router';
import rootComponent from '../../rootComponent';
import { refresh } from '../../actions/ui/status';

import { FetchBaragonStatus } from '../../actions/api/status';
import { FetchBaragonServiceWorkers } from '../../actions/api/workers';
import { FetchQueuedRequests } from '../../actions/api/requests';

const StatusPage = (props) => {
  return (
    <h1>Status</h1>
  );
};

StatusPage.propTypes = {
  status: React.PropTypes.object,
  workers: React.PropTypes.array,
  queuedRequests: React.PropTypes.array
};

export default connect((state) => ({
  status: state.api.status.data,
  workers: state.api.workers.data,
  queuedRequests: state.api.queuedRequests.data
}))(rootComponent(StatusPage, refresh));
