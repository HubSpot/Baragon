import React, { PropTypes } from 'react';
import { connect } from 'react-redux';
import rootComponent from '../../rootComponent';
import { refresh } from '../../actions/ui/albs';

import TargetGroupsTable from './TargetGroupsTable';
import LoadBalancersTable from './LoadBalancersTable';

class Albs extends React.Component {
  static propTypes = {
    targetGroups: PropTypes.array,
    loadBalancers: PropTypes.array,
  };

  state = {
    loadBalancersFilter: '',
    targetGroupsFilter: '',
  };

  searchLoadBalancers = (evt) => {
    this.setState({loadBalancersFilter: evt.target.value});
  };

  searchTargetGroups = (evt) => {
    this.setState({targetGroupsFilter: evt.target.value});
  };

  render() {
    return (
      <div>
        <h3>Application Load Balancers</h3>
        <div className="row">
          <div className="col-md-12">
            <h4>Load Balancers</h4>
            <div className="input-group">
              <label>
                Search:
                <input
                  type="search"
                  className="form-control"
                  onKeyUp={this.searchLoadBalancers}
                />
              </label>
            </div>
            <LoadBalancersTable
              loadBalancers={this.props.loadBalancers}
              filter={this.state.loadBalancersFilter}
            />
          </div>
        </div>
        <div className="row">
          <div className="col-md-12">
            <h4>Target Groups</h4>
            <div className="input-group">
              <label>
                Search:
                <input
                  type="search"
                  className="form-control"
                  onKeyUp={this.searchTargetGroups}
                />
              </label>
            </div>
            <TargetGroupsTable
              targetGroups={this.props.targetGroups}
              filter={this.state.targetGroupsFilter}
            />
          </div>
        </div>
      </div>
    );
  }
}

const mapStateToProps = (state) => ({
  loadBalancers: state.api.albs.data,
  targetGroups: state.api.targetGroups.data,
});


export default connect(mapStateToProps)(rootComponent(Albs, refresh));
