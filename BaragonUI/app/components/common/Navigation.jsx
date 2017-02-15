import React, { PropTypes } from 'react';
import { connect } from 'react-redux';
import { withRouter, Link } from 'react-router';
import classnames from 'classnames';

import EnableEditButton from './modalButtons/EnableEditButton';
import DisableEditButton from './modalButtons/DisableEditButton';

function isActive(navbarPath, fragment) {
  if (navbarPath === '/') {
    return fragment === '';
  }
  return navbarPath === fragment;
}

const EnableEditControls = ({allowEdit, authEnabled}) => {
  if (allowEdit && authEnabled) {
    return (
      <DisableEditButton />
    );
  } else if (authEnabled) {
    return (
      <EnableEditButton />
    );
  } else {
    return null;
  }
};

EnableEditControls.propTypes = {
  allowEdit: PropTypes.bool.isRequired,
  authEnabled: PropTypes.bool.isRequired,
};

// put into page wrapper, render children
const Navigation = (props) => {
  const fragment = props.location.pathname.split('/')[1];
  return (
    <nav className="navbar navbar-default">
      <div className="container-fluid">
        <div className="navbar-header">
          <Link className="navbar-brand" to="/">{config.title}</Link>
        </div>
        <div className="collapse navbar-collapse" id="navbar-collapse">
          <ul className="nav navbar-nav">
            <li className={classnames({active: isActive('/services', fragment)})}>
              <Link to="/services">Services {isActive('/services', fragment) && <span className="sr-only">(current)</span>}</Link>
            </li>
            <li className={classnames({active: isActive('/groups', fragment)})}>
              <Link to="/groups">Groups {isActive('/groups', fragment) && <span className="sr-only">(current)</span>}</Link>
            </li>
          </ul>
          <EnableEditControls
            allowEdit={props.allowEdit}
            authEnabled={props.authEnabled}
          />
        </div>
      </div>
    </nav>
  );
};


Navigation.propTypes = {
  allowEdit: React.PropTypes.bool,
  authEnabled: React.PropTypes.bool,

  location: React.PropTypes.object.isRequired,
  router: React.PropTypes.object.isRequired,
  toggleGlobalSearch: React.PropTypes.func
};

const mapStateToProps = (state, ownProps) => ({
  allowEdit: config.allowEdit,
  authEnabled: config.authEnabled,
});

function mapDispatchToProps(dispatch) {
  return {
    toggleGlobalSearch: () => dispatch(ToggleVisibility())
  };
}

export default connect(mapStateToProps, mapDispatchToProps)(withRouter(Navigation));
