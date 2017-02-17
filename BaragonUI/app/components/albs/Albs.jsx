import React, { PropTypes } from 'react';
import { connect } from 'react-redux';
import rootComponent from '../../rootComponent';
import { refresh } from '../../actions/ui/albs';

import TargetGroupsTable from './TargetGroupsTable';
import LoadBalancersTable from './LoadBalancersTable';

const Albs = ({targetGroups, loadBalancers}) => {
  return (
    <div>
      <h3>Application Load Balancers</h3>
      <div className="row">
        <div className="col-md-12">
          <h4>Load Balancers</h4>
          <LoadBalancersTable loadBalancers={loadBalancers} />
        </div>
      </div>
      <div className="row">
        <div className="col-md-12">
          <h4>Target Groups</h4>
          <TargetGroupsTable targetGroups={targetGroups} />
        </div>
      </div>
    </div>
  );
};

Albs.propTypes = {
  targetGroups: PropTypes.array,
  loadBalancers: PropTypes.array,
};

const mapStateToProps = (state) => ({
  loadBalancers: state.api.loadBalancers.data,
  targetGroups: state.api.targetGroups.data,
});


export default connect(mapStateToProps)(rootComponent(Albs, refresh));
