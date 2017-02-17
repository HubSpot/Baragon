import React, { PropTypes } from 'react';
import { connect } from 'react-redux';
import { withRouter, Link } from 'react-router';
import classnames from 'classnames';

function isActive(navbarPath, fragment) {
  if (navbarPath === '/') {
    return fragment === '';
  }
  return navbarPath === `/${fragment}`;
}

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

const elbLink = (isEnabled, fragment) => {
  if (isEnabled) {
    return <NavLink path="/elbs" name="Elbs" fragment={fragment} />;
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
          <Link className="navbar-brand" to="/">{config.title}</Link>
        </div>
        <div className="collapse navbar-collapse" id="navbar-collapse">
          <ul className="nav navbar-nav">
            <NavLink path="/services" name="Services" fragment={fragment} />
            <NavLink path="/groups" name="Groups" fragment={fragment} />
            { elbLink(props.elbEnabled, fragment) }
          </ul>
        </div>
      </div>
    </nav>
  );
};


Navigation.propTypes = {
  location: React.PropTypes.object.isRequired,
  router: React.PropTypes.object.isRequired,
  elbEnabled: PropTypes.bool.isRequired,
  toggleGlobalSearch: React.PropTypes.func
};

const mapStateToProps = () => ({
  elbEnabled: window.config.elbEnabled
});

function mapDispatchToProps(dispatch) {
  return {
    toggleGlobalSearch: () => dispatch(ToggleVisibility())
  };
}


export default connect(mapStateToProps, mapDispatchToProps)(withRouter(Navigation));
