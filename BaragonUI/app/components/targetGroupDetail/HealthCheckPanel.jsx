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

const HealthCheckPanel = ({interval, timeout, healthyThreshold, path}) => {
  return (
    <div className="col-md-4">
      <ul className="list-group">
        <li className="list-group-item"><strong>Heath Check: </strong>{ path }</li>
        <ListItem name="Interval" value={interval} />
        <ListItem name="Timeout" value={timeout} />
        <ListItem name="Healthy Threshold" value={healthyThreshold} />
      </ul>
    </div>
  );
};

HealthCheckPanel.propTypes = {
  interval: PropTypes.number.isRequired,
  timeout: PropTypes.number.isRequired,
  healthyThreshold: PropTypes.number.isRequired,
  path: PropTypes.string.isRequired,
};

export default HealthCheckPanel;
