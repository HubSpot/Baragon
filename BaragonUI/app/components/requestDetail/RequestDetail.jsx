import React, { Component, PropTypes } from 'react';
import { connect } from 'react-redux';
import classNames from 'classnames';
import { Link } from 'react-router';
import rootComponent from '../../rootComponent';
import { refresh } from '../../actions/ui/serviceDetail';
import Utils from '../../utils';

class RequestDetail extends Component {
  render() {
    // TODO - render out info nicely + link to service page/group page
    return <h1>Request {this.props.params.requestId}</h1>
  }

  static propTypes = {
    params: PropTypes.object.isRequired,
    response: React.PropTypes.object
  };
}

export default connect((state, ownProps) => ({
  response: Utils.maybe(state, ['api', 'requestResponse', ownProps.params.requestId, 'data']),
}))(rootComponent(RequestDetail, (props) => refresh(props.params.requestId)));
