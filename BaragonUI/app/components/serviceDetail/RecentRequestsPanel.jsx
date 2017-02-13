import React, { PropTypes } from 'react';
import { Link } from 'react-router';

import UITable from '../common/table/UITable';
import Column from '../common/table/Column';
import JSONButton from '../common/JSONButton';

import { iconByState } from './util';

const requestJSONButton = (request) => {
  if (request) {
    return (
      <JSONButton object={request}>
        <span>{'{ }'}</span>
      </JSONButton>
    );
  } else {
    return <span>{'{ }'}</span>;
  }
};

const iconForRequest = (request) => {
  return <span className={`${iconByState(request.loadBalancerState)}`}></span>;
};

const requestLink = ({loadBalancerRequestId}) => {
  return (
    <Link to={`/requests/${loadBalancerRequestId}`}>
      {loadBalancerRequestId}
    </Link>
  );
};

const HistoryTable = ({history}) => {
  if (! history) {
    return (
      <div className="empty-table-message">
        <p>No recent requests</p>
      </div>
    );
  }

  return (
    <UITable
      data={history}
      keyGetter={(request) => request.loadBalancerRequestId}
      paginated={false}
    >
      <Column
        label=""
        id="requestIcon"
        cellData={iconForRequest}
        className="hidden-xs icons-column"
      />
      <Column
        label="ID"
        id="requestId"
        cellData={requestLink}
      />
      <Column
        label="Action"
        id="requestAction"
        cellData={(request) => request.action || 'UPDATE'}
      />
      <Column
        label="Result"
        id="requestResult"
        cellData={(request) => request.loadBalancerState}
      />
      <Column
        label=""
        id="requestShowJson"
        cellData={requestJSONButton}
      />
    </UITable>
  );
};

HistoryTable.propTypes = {
  history: PropTypes.arrayOf(PropTypes.object),
};

const RecentRequestsPanel = ({requests}) => {
  return (
    <div className="col-md-12">
      <div className="panel panel-default">
        <div className="panel-heading">
          <h4>Recent Requests</h4>
        </div>
        <div className="panel-body">
          <HistoryTable history={requests} />
        </div>
      </div>
    </div>
  );
};

RecentRequestsPanel.propTypes = {
  requests: PropTypes.arrayOf(PropTypes.object),
};

export default RecentRequestsPanel;
