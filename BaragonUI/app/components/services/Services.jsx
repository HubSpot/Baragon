import React from 'react';
import { connect } from 'react-redux';
import classNames from 'classnames';
import { Link } from 'react-router';
import rootComponent from '../../rootComponent';
import { refresh } from '../../actions/ui/status';

const Services = (props) => {
  return (
    <h1>Services</h1>
  );
};

Services.propTypes = {
  services: React.PropTypes.object
};

export default connect((state) => ({
  services: state.api.services.data
}))(rootComponent(Services, refresh));
