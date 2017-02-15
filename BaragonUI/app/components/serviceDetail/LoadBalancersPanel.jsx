import React, { PropTypes } from 'react';
import { Link } from 'react-router';

import Utils from '../../utils';

const renderLoadBalancerGroup = (loadBalancerGroup) => {
  return (
    <li className="list-group-item" key={loadBalancerGroup}>
      <Link to={`/groups/${loadBalancerGroup}`}>{loadBalancerGroup}</Link>
    </li>
  );
};

const LoadBalancersPanel = ({loadBalancerGroups}) => {
  return (
    <div className="col-md-6">
      <div className="panel panel-default">
        <div className="panel-heading">
          <h4>Load Balancer Groups</h4>
        </div>
        <div className="panel-body">
          { Utils.asGroups(loadBalancerGroups, 2, renderLoadBalancerGroup) }
        </div>
      </div>
    </div>
  );
};

LoadBalancersPanel.propTypes = {
  loadBalancerGroups: PropTypes.arrayOf(PropTypes.string),
};

export default LoadBalancersPanel;
