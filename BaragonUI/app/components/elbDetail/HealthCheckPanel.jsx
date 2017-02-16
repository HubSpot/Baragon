import React, { PropTypes } from 'react';

const ListItem = ({name, value}) => {
  if (value) {
    return <li className="list-group-item">{name} <span className="badge">{value}</span></li>;
  } else {
    return null;
  }
};

ListItem.propTypes = {
  name: PropTypes.string,
  value: PropTypes.number,
};

const HealthCheckPanel = ({healthCheck: {target, interval, timeout, unhealthyThreshold, healthyThreshold}}) => {
  return (
    <div className="col-md-4">
      <ul className="list-group">
        <li className="list-group-item"><strong>Health Check:</strong> {target}</li>
        <ListItem name="Interval" value={interval} />
        <ListItem name="Timeout" value={timeout} />
        <ListItem name="Unhealthy Threshold" value={unhealthyThreshold} />
        <ListItem name="Healthy Threshold" value={healthyThreshold} />
      </ul>
    </div>
  );
};

HealthCheckPanel.propTypes = {
  healthCheck: PropTypes.shape({
    target: PropTypes.string,
    interval: PropTypes.number,
    timeout: PropTypes.number,
    unhealthyThreshold: PropTypes.number,
    healthyThreshold: PropTypes.number,
  }).isRequired,
};

export default HealthCheckPanel;
