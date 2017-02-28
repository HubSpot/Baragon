import React from 'react';
import Navigation from './Navigation';
import Title from './Title';

const Application = (props) => {
  return (<div>
    <Title routes={props.routes} params={props.params} />
    <Navigation location={props.location} history={props.history} />
    {props.children}
  </div>);
};

Application.propTypes = {
  location: React.PropTypes.object.isRequired,
  history: React.PropTypes.object.isRequired,
  children: React.PropTypes.object
};

export default Application;
