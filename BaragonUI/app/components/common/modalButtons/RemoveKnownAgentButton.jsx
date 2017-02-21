import React, { Component, PropTypes } from 'react';

import { Glyphicon } from 'react-bootstrap';
import OverlayTrigger from 'react-bootstrap/lib/OverlayTrigger';
import ToolTip from 'react-bootstrap/lib/Tooltip';

import { getClickComponent } from '../modal/ModalWrapper';

import RemoveKnownAgentModal from './RemoveKnownAgentModal';

const removeKnownAgentTooltip = (
  <ToolTip id="removeKnownAgent">
    Remove this agent
  </ToolTip>
);

export default class RemoveKnownAgentButton extends Component {
  static propTypes = {
    groupName: PropTypes.string.isRequired,
    agentId: PropTypes.string.isRequired,
    children: PropTypes.node,
    then: PropTypes.func
  };

  static defaultProps = {
    children: (
      <OverlayTrigger placement="top" id="view-remove-known-agent-overlay" overlay={removeKnownAgentTooltip}>
        <a>
          <Glyphicon glyph="remove" />
        </a>
      </OverlayTrigger>
    )
  };

  render() {
    return (
      <span>
        {getClickComponent(this)}
        <RemoveKnownAgentModal
          ref="modal"
          groupName={this.props.groupName}
          agentId={this.props.agentId}
          then={this.props.then}
        />
      </span>
    );
  }
}
