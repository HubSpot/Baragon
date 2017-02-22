import React, { Component, PropTypes } from 'react';

import { Glyphicon } from 'react-bootstrap';
import OverlayTrigger from 'react-bootstrap/lib/OverlayTrigger';
import ToolTip from 'react-bootstrap/lib/Tooltip';

import { getClickComponent } from '../modal/ModalWrapper';

import EditHealthCheckModal from './EditHealthCheckModal';

const editTooltip = (
  <ToolTip id="delete">
    Edit this Target Group's Health Check
  </ToolTip>
);

export default class EditHealthCheckButton extends Component {
  static propTypes = {
    targetGroupName: PropTypes.string.isRequired,
    protocol: PropTypes.oneOf(['HTTP', 'HTTPS']).isRequired,
    port: PropTypes.string.isRequired,
    path: PropTypes.string.isRequired,
    interval: PropTypes.number.isRequired,
    timeout: PropTypes.number.isRequired,
    healthyThreshold: PropTypes.number.isRequired,
    unhealthyThreshold: PropTypes.number.isRequired,
    children: PropTypes.node,
    then: PropTypes.func
  };

  static defaultProps = {
    children: (
      <OverlayTrigger placement="top" id="view-delete-overlay" overlay={editTooltip}>
        <a data-action="delete">
          <Glyphicon glyph="edit" />
        </a>
      </OverlayTrigger>
    )
  };

  render() {
    return (
      <span>
        {getClickComponent(this)}
        <EditHealthCheckModal ref="modal" {...this.props} />
      </span>
    );
  }
}
