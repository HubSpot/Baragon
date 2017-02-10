import React from 'react';
import { connect } from 'react-redux';
import rootComponent from '../../rootComponent';
import { refresh } from '../../actions/ui/status';

import WorkerStatus from './WorkerStatus';
import PendingRequests from './PendingRequests';
import RequestSearch from './RequestSearch';

const StatusPage = ({status, workers, queuedRequests}) => {
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
          onSearch={(text) => {
            console.log(text);
          }}
        />
      </div>
    </div>
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
