import React, { Component, PropTypes } from 'react';
import { connect } from 'react-redux';
import rootComponent from '../../rootComponent';
import { refresh } from '../../actions/ui/groups';

import GroupsTable from './GroupsTable';

class Groups extends Component {
  static propTypes = {
    groups: PropTypes.arrayOf(PropTypes.shape({
      defaultDomain: PropTypes.string,
      domain: PropTypes.string,
      domains: PropTypes.arrayOf(PropTypes.string),
      name: PropTypes.string,
      trafficSources: PropTypes.arrayOf(PropTypes.shape({
        name: PropTypes.string,
        type: PropTypes.oneOf(['CLASSIC', 'ALB_TARGET_GROUP'])
      }))
    }))
  }

  state = {
    filter: ''
  }

  handleSearch = (evt) => {
    this.setState({filter: evt.target.value});
  }

  render () {
    return (
      <div>
        <h1>Load Balancer Groups</h1>
        <div className="row">
          <div className="col-md-5">
            <div className="input-group">
              <label>
                Search:
                <input
                  type="search"
                  className="form-control"
                  onKeyUp={this.handleSearch}
                />
              </label>
            </div>
          </div>
        </div>
        <div className="row">
          <div className="col-md-12">
            <GroupsTable
              groups={this.props.groups}
              filter={this.state.filter}
            />
          </div>
        </div>
      </div>
    );
  }
}

export default connect((state) => ({
  groups: state.api.groups.data
}))(rootComponent(Groups, refresh));
