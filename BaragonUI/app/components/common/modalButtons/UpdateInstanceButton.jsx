import React, { Component, PropTypes } from 'react';

import { Glyphicon } from 'react-bootstrap';
import OverlayTrigger from 'react-bootstrap/lib/OverlayTrigger';
import ToolTip from 'react-bootstrap/lib/Tooltip';

import { getClickComponent } from '../modal/ModalWrapper';

import UpdateInstanceModal from './UpdateInstanceModal';

const updateInstanceTooltip = (
  <ToolTip id="updateInstance">
    Update
  </ToolTip>
);

export default class UpdateInstanceButton extends Component {
  static propTypes = {
    loadBalancer: PropTypes.string.isRequired,
    instanceId: PropTypes.string,
    action: PropTypes.oneOf(['add', 'remove']).isRequired,
    type: PropTypes.oneOf(['elb', 'alb']).isRequired,
    children: PropTypes.node,
    then: PropTypes.func
  };

  static defaultProps = {
    children: (
      <OverlayTrigger placement="top" id="view-update-instance-overlay" overlay={updateInstanceTooltip}>
        <a data-action="update">
          <Glyphicon glyph="edit" />
        </a>
      </OverlayTrigger>
    )
  };

  render() {
    return (
      <span>
        {getClickComponent(this)}
        <UpdateInstanceModal
          ref="modal"
          loadBalancer={this.props.loadBalancer}
          instanceId={this.props.instanceId}
          action={this.props.action}
          type={this.props.type}
          then={this.props.then}
        />
      </span>
    );
  }
}
