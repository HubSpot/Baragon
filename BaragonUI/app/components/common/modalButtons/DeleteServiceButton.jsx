import React, { Component, PropTypes } from 'react';

import { Glyphicon } from 'react-bootstrap';
import OverlayTrigger from 'react-bootstrap/lib/OverlayTrigger';
import ToolTip from 'react-bootstrap/lib/Tooltip';

import { getClickComponent } from '../modal/ModalWrapper';

import DeleteServiceModal from './DeleteServiceModal';

const deleteTooltip = (
  <ToolTip id="delete">
    Delete Service Config
  </ToolTip>
);

export default class DeleteServiceButton extends Component {
  static propTypes = {
    serviceId: PropTypes.string.isRequired,
    children: PropTypes.node,
    then: PropTypes.func
  };

  static defaultProps = {
    children: (
      <OverlayTrigger placement="top" id="view-delete-overlay" overlay={deleteTooltip}>
        <a data-action="delete">
          <Glyphicon glyph="trash" />
        </a>
      </OverlayTrigger>
    )
  };

  render() {
    return (
      <span>
        {getClickComponent(this)}
        <DeleteServiceModal ref="modal" serviceId={this.props.serviceId} then={this.props.then} />
      </span>
    );
  }
}

