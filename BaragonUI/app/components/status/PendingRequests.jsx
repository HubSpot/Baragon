import React from 'react';
import { Link } from 'react-router';

const pendingRequest = ({requestId, appRoot}) => {
  return (
    <li key={requestId}>
      <Link to={`/requests/${requestId}`} title={requestId}>{requestId}</Link>
    </li>
  );
};

pendingRequest.propTypes = {
  requestId: React.PropTypes.string,
  appRoot: React.PropTypes.string,
};

const requestsBox = (queuedRequests) => {
  if (queuedRequests && queuedRequests.length) {
    return (
      <ul className="list-group">
        { queuedRequests.map(pendingRequest) }
      </ul>
    );
  } else {
    return <div className="empty-table-message"><p>No pending requests</p></div>;
  }
};

const PendingRequests = ({queuedRequests}) => {
  return (
    <div className="col-md-6">
      <h4>Pending Requests</h4>
      {requestsBox(queuedRequests)}
    </div>
  );
};

PendingRequests.propTypes = {
  queuedRequests: React.PropTypes.arrayOf(React.PropTypes.shape({
    requestId: React.PropTypes.string,
    appRoot: React.PropTypes.string,
  }))
};

export default PendingRequests;
