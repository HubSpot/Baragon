import React, { Component, PropTypes } from 'react';
import { connect } from 'react-redux';
import rootComponent from '../../rootComponent';
import { refresh } from '../../actions/ui/elbs';

import Utils from '../../utils';

import ElbsTable from './ElbsTable';

class Elbs extends Component {
  static propTypes = {
    elbs: PropTypes.arrayOf(PropTypes.object),
  }

  state = {
    filter: ''
  }

  handleSearch = (evt) => {
    this.setState({filter: evt.target.value});
  }

  render() {
    return (
      <div>
        <h2>Current Elastic Load Balancers</h2>
        <div className="row">
          <div className="col-md-5">
            <div className="input-group">
              <label>Search:
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
            <ElbsTable
              elbs={this.props.elbs}
              filter={this.state.filter}
            />
          </div>
        </div>
      </div>
    );
  }


}


export default connect((state) => ({
  elbs: Utils.maybe(state, ['api', 'elbs', 'data'])
}))(rootComponent(Elbs, refresh));
