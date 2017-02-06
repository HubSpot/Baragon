import React from 'react';
import { connect } from 'react-redux';
import classNames from 'classnames';
import { Link } from 'react-router';
import rootComponent from '../../rootComponent';
import { refresh } from '../../actions/ui/status';

const StatusPage = (props) => {
  return (
    // TODO - nice layout of data
    // - search by request ID? Link to page for individual request?
    <h1>Status</h1>
  );
};

StatusPage.propTypes = {
  status: React.PropTypes.object,
  workers: React.PropTypes.array,
  queuedRequests: React.PropTypes.array
};

export default connect((state) => ({
  status: state.api.status.data,
  workers: state.api.workers.data,
  queuedRequests: state.api.queuedRequests.data
}))(rootComponent(StatusPage, refresh));
