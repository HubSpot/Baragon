import React from 'react';

import Utils from '../../utils';

const workerItem = (worker) => {
  return (
    <li key={worker} className="list-group-item">
      <a href={`${worker}/status`}>{worker}</a>
    </li>
  );
};

const WorkerStatus = ({workerLag, elbWorkerLag, zookeeperState, workers}) => {
  return (
    <div className="col-md-6">
      <ul className="list-group">
        <li className="list-group-item">
          <h4>Request Worker Lag <span className="pull-right">{Utils.humanizeWorkerLag(workerLag)}</span></h4>
        </li>
        <li className="list-group-item">
          <h4>ELB Worker Lag <span className="pull-right">{Utils.humanizeWorkerLag(elbWorkerLag)}</span></h4>
        </li>
        <li className="list-group-item">
          <h4>ZK Connection State <span className="pull-right">{zookeeperState}</span></h4>
        </li>
        <li className="list-group-item">
          <h4>Baragon Service Workers</h4>
          <ul className="list-group">
            {workers.map(workerItem)}
          </ul>
        </li>
      </ul>
    </div>
  );
};

WorkerStatus.propTypes = {
  workerLag: React.PropTypes.number.isRequired,
  elbWorkerLag: React.PropTypes.number.isRequired,
  zookeeperState: React.PropTypes.string.isRequired,
  workers: React.PropTypes.array.isRequired,
};

export default WorkerStatus;
