import React, { Component, PropTypes } from 'react';

import { Glyphicon } from 'react-bootstrap';
import OverlayTrigger from 'react-bootstrap/lib/OverlayTrigger';
import ToolTip from 'react-bootstrap/lib/Tooltip';

import { getClickComponent } from '../modal/ModalWrapper';

import AddTrafficSourceModal from './AddTrafficSourceModal';

const deleteTooltip = (
  <ToolTip id="addTrafficSource">
    Add Traffic Source
  </ToolTip>
);

export default class AddTrafficSourceButton extends Component {
  static propTypes = {
    groupName: PropTypes.string.isRequired,
    children: PropTypes.node,
    then: PropTypes.func
  };

  static defaultProps = {
    children: (
      <OverlayTrigger placement="top" id="view-delete-overlay" overlay={deleteTooltip}>
        <a className="btn btn-primary" data-action="delete">
          + Add Traffic Source
        </a>
      </OverlayTrigger>
    )
  };

  render() {
    return (
      <span>
        {getClickComponent(this)}
        <AddTrafficSourceModal ref="modal" groupName={this.props.groupName} then={this.props.then} />
      </span>
    );
  }
}
