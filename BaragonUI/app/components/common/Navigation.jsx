import React from 'react';
import { connect } from 'react-redux';
import { withRouter, Link } from 'react-router';
import classnames from 'classnames';

function isActive(navbarPath, fragment) {
  if (navbarPath === '/') {
    return fragment === '';
  }
  return navbarPath === fragment;
}

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
        </div>
      </div>
    </nav>
  );
};


Navigation.propTypes = {
  location: React.PropTypes.object.isRequired,
  router: React.PropTypes.object.isRequired,
  toggleGlobalSearch: React.PropTypes.func
};

function mapDispatchToProps(dispatch) {
  return {
    toggleGlobalSearch: () => dispatch(ToggleVisibility())
  };
}

export default connect(null, mapDispatchToProps)(withRouter(Navigation));
