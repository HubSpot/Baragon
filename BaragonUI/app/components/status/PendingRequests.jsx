import React from 'react';

const pendingRequest = ({requestId, appRoot}) => {
  return (
    <li key={requestId}>
      <a title={requestId} href={`${appRoot}/requests/${requestId}`}>
        {requestId}
      </a>
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
