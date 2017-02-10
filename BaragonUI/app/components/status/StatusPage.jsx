import React from 'react';
import { connect } from 'react-redux';
import { withRouter } from 'react-router';
import rootComponent from '../../rootComponent';
import { refresh } from '../../actions/ui/status';

import WorkerStatus from './WorkerStatus';
import PendingRequests from './PendingRequests';
import RequestSearch from './RequestSearch';

const navigateToRequest = (router) => (requestId) => {
  router.push(`/requests/${requestId}`);
};

const StatusPage = ({status, workers, queuedRequests, router}) => {
  return (
    <div>
      <div className="row">
        <WorkerStatus
          workerLag={status.workerLagMs}
          elbWorkerLag={status.elbWorkerLagMs}
          zookeeperState={status.zookeeperState}
          workers={workers}
        />
      <PendingRequests
        queuedRequests={queuedRequests}
      />
      </div>
      <div className="row">
        <RequestSearch
          onSearch={navigateToRequest(router)}
        />
      </div>
    </div>
  );
};

StatusPage.propTypes = {
  status: React.PropTypes.object,
  workers: React.PropTypes.array,
  queuedRequests: React.PropTypes.array,
  router: React.PropTypes.object,
};

export default withRouter(connect((state, ownProps) => ({
  status: state.api.status.data,
  workers: state.api.workers.data,
  queuedRequests: state.api.queuedRequests.data,
  router: ownProps.router,
}))(rootComponent(StatusPage, refresh)));
