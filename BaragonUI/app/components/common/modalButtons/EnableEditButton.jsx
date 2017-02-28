import React, { Component, PropTypes } from 'react';

import { getClickComponent } from '../modal/ModalWrapper';

import EnableEditModal from './EnableEditModal';

export default class EnableEditButton extends Component {
  static propTypes = {
    children: PropTypes.node,
    then: PropTypes.func
  };

  static defaultProps = {
    children: (
      <ul className="nav navbar-nav navbar-right">
        <li><a>Enable Edit</a></li>
      </ul>
    )
  };

  render() {
    return (
      <span>
        {getClickComponent(this)}
        <EnableEditModal ref="modal" then={this.props.then} />
      </span>
    );
  }
}
