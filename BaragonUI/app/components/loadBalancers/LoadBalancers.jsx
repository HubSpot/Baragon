import React, { PropTypes } from 'react';
import { connect } from 'react-redux';
import rootComponent from '../../rootComponent';
import { refresh } from '../../actions/ui/loadBalancers';

import TargetGroupsTable from './TargetGroupsTable';
import LoadBalancersTable from './LoadBalancersTable';
import ElbsTable from './ElbsTable';

class LoadBalancers extends React.Component {
  static propTypes = {
    targetGroups: PropTypes.array,
    albs: PropTypes.array,
    elbs: PropTypes.array,
  };

  state = {
    albsFilter: '',
    elbsFilter: '',
    targetGroupsFilter: '',
  };

  globalSearch = (evt) => {
    const searchTerm = evt.target.value;
    this.setState({
      albsFilter: searchTerm,
      elbsFilter: searchTerm,
      targetGroupsFilter: searchTerm
    });
  }

  searchElbs = (evt) => {
    this.setState({elbsFilter: evt.target.value});
  };

  searchAlbs = (evt) => {
    this.setState({albsFilter: evt.target.value});
  };

  searchTargetGroups = (evt) => {
    this.setState({targetGroupsFilter: evt.target.value});
  };

  render() {
    return (
      <div>
        <h3>Load Balancers</h3>
        <div className="row">
          <div className="row">
            <div className="col-md-5">
              <h4>Application Load Balancers</h4>
            </div>
            <div className="col-md-7">
              <div className="input-group pull-right">
                <label>
                  Search:
                  <input
                    type="search"
                    className="form-control"
                    onKeyUp={this.searchAlbs}
                  />
                </label>
              </div>
            </div>
          </div>
          <div className="row">
            <div className="col-md-12">
              <LoadBalancersTable
                loadBalancers={this.props.albs}
                filter={this.state.albsFilter}
              />
            </div>
          </div>
        </div>
        <div className="row">
          <div className="row">
            <div className="col-md-5">
              <h4>Elastic Load Balancers</h4>
            </div>
            <div className="col-md-7">
              <div className="input-group pull-right">
                <label>
                  Search:
                  <input
                    type="search"
                    className="form-control"
                    onKeyUp={this.searchElbs}
                  />
                </label>
              </div>
            </div>
          </div>
          <div className="row">
            <div className="col-md-12">
              <ElbsTable
                elbs={this.props.elbs}
                filter={this.state.elbsFilter}
                rowCount={5}
              />
            </div>
          </div>
        </div>
        <div className="row">
          <div className="row">
            <div className="col-md-5">
              <h4>Target Groups (in Application Load Balancers)</h4>
            </div>
            <div className="col-md-7">
              <div className="input-group pull-right">
                <label>
                  Search:
                  <input
                    type="search"
                    className="form-control"
                    onKeyUp={this.searchTargetGroups}
                  />
                </label>
              </div>
            </div>
          </div>
          <div className="row">
            <div className="col-md-12">
              <TargetGroupsTable
                targetGroups={this.props.targetGroups}
                filter={this.state.targetGroupsFilter}
              />
            </div>
          </div>
        </div>
      </div>
    );
  }
}

const mapStateToProps = (state) => ({
  albs: state.api.albs.data,
  elbs: state.api.elbs.data,
  targetGroups: state.api.targetGroups.data,
});


export default connect(mapStateToProps)(rootComponent(LoadBalancers, refresh));
