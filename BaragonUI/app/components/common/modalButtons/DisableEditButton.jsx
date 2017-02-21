import React, { Component, PropTypes } from 'react';

import { getClickComponent } from '../modal/ModalWrapper';

import DisableEditModal from './DisableEditModal';

export default class DisableEditButton extends Component {
  static propTypes = {
    children: PropTypes.node,
    then: PropTypes.func
  };

  static defaultProps = {
    children: (
      <ul className="nav navbar-nav navbar-right">
        <li><a>Disable Edit</a></li>
      </ul>
    )
  };

  render() {
    return (
      <span>
        {getClickComponent(this)}
        <DisableEditModal ref="modal" then={this.props.then} />
      </span>
    );
  }
}
