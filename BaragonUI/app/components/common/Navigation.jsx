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
  return navbarPath === `/${fragment}`;
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

const NavLink = ({path, name, fragment}) => {
  return (
    <li className={classnames({active: isActive(path, fragment)})}>
      <Link to={path}>{name} {isActive('/services', fragment) && <span className="sr-only">(current)</span>}</Link>
    </li>
  );
};

NavLink.propTypes = {
  path: PropTypes.string.isRequired,
  name: PropTypes.string.isRequired,
  fragment: PropTypes.string.isRequired,
};

const lbLink = (isEnabled, fragment) => {
  if (isEnabled) {
    return <NavLink path="/loadbalancers" name="Load Balancers" fragment={fragment} />;
  } else {
    return null;
  }
};

// put into page wrapper, render children
const Navigation = (props) => {
  const fragment = props.location.pathname.split('/')[1];
  return (
    <nav className="navbar navbar-default">
      <div className="container-fluid">
        <div className="navbar-header">
          <button type="button" className="navbar-toggle" data-toggle="collapse" data-target="#navbar-collapse">
            <span className="sr-only">Toggle Navigation</span>
            <span className="icon-bar" />
            <span className="icon-bar" />
            <span className="icon-bar" />
          </button>
          <Link className="navbar-brand" to="/">{config.title}</Link>
        </div>
        <div className="collapse navbar-collapse" id="navbar-collapse">
          <ul className="nav navbar-nav">
            <NavLink path="/services" name="Services" fragment={fragment} />
            <NavLink path="/groups" name="Groups" fragment={fragment} />
            { lbLink(props.elbEnabled, fragment) }
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
  elbEnabled: PropTypes.bool.isRequired,
  toggleGlobalSearch: React.PropTypes.func
};

const mapStateToProps = () => ({
  allowEdit: config.allowEdit,
  authEnabled: config.authEnabled,
  elbEnabled: config.elbEnabled
});

export default connect(mapStateToProps)(withRouter(Navigation));
