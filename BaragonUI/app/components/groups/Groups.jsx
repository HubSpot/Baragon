import React from 'react';
import { connect } from 'react-redux';
import classNames from 'classnames';
import { Link } from 'react-router';
import rootComponent from '../../rootComponent';
import { refresh } from '../../actions/ui/status';

const Groups = (props) => {
  return (
    <h1>Groups</h1>
  );
};

Groups.propTypes = {
  groups: React.PropTypes.object
};

export default connect((state) => ({
  groups: state.api.groups.data
}))(rootComponent(Groups, refresh));
