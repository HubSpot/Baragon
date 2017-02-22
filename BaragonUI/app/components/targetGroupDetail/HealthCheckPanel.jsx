import React, { PropTypes } from 'react';

import EditHeathCheckButton from '../common/modalButtons/EditHealthCheckButton';

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

const HeathCheckButton = ({editable, healthCheck}) => {
  if (editable) {
    return (
      <span className="pull-right button-container">
        <EditHeathCheckButton {...healthCheck} />
      </span>
    );
  } else {
    return null;
  }
};

HeathCheckButton.propTypes = {
  editable: PropTypes.bool,
  healthCheck: PropTypes.shape({
    targetGroupName: PropTypes.string.isRequired,
    protocol: PropTypes.oneOf(['HTTP', 'HTTPS']).isRequired,
    port: PropTypes.string.isRequired,
    interval: PropTypes.number.isRequired,
    timeout: PropTypes.number.isRequired,
    healthyThreshold: PropTypes.number.isRequired,
    path: PropTypes.string.isRequired,
  }).isRequired,
};

const HealthCheckPanel = ({editable, healthCheck}) => {
  const {port, protocol, path, interval, timeout, healthyThreshold} = healthCheck;
  return (
    <div className="col-md-4">
      <ul className="list-group">
        <li className="list-group-item">
          <strong>Heath Check: </strong>{ `[${port}] ${protocol}:${path}` }
          <HeathCheckButton editable={editable} healthCheck={healthCheck} />
        </li>
        <ListItem name="Interval" value={interval} />
        <ListItem name="Timeout" value={timeout} />
        <ListItem name="Healthy Threshold" value={healthyThreshold} />
      </ul>
    </div>
  );
};

HealthCheckPanel.propTypes = {
  editable: PropTypes.bool.isRequired,
  healthCheck: PropTypes.shape({
    targetGroupName: PropTypes.string.isRequired,
    protocol: PropTypes.oneOf(['HTTP', 'HTTPS']).isRequired,
    port: PropTypes.string.isRequired,
    interval: PropTypes.number.isRequired,
    timeout: PropTypes.number.isRequired,
    healthyThreshold: PropTypes.number.isRequired,
    path: PropTypes.string.isRequired,
  }).isRequired,
};

export default HealthCheckPanel;
