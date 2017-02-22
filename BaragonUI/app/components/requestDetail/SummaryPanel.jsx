import React from 'react';
import { Link } from 'react-router';

const SummaryPanel = ({serviceId, message}) => {
  return (
    <div className="col-md-12">
      <div className="panel panel-default">
        <div className="panel-heading">
          <h4>Summary</h4>
        </div>
        <div className="panel-body">
          <div className="row">
            <div className="col-md-12">
              <h4>Service ID: </h4>
              <h4><Link to={`/services/${serviceId}`}>{serviceId}</Link></h4>
            </div>
          </div>
          <div className="row">
            <div className="col-md-12 list-group-item">
              <h4>Message: </h4><p>{message}</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

SummaryPanel.propTypes = {
  serviceId: React.PropTypes.string,
  message: React.PropTypes.string,
};

export default SummaryPanel;
