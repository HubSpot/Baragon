import React, { Component, PropTypes } from 'react';

import { Glyphicon } from 'react-bootstrap';
import OverlayTrigger from 'react-bootstrap/lib/OverlayTrigger';
import ToolTip from 'react-bootstrap/lib/Tooltip';

import { getClickComponent } from '../modal/ModalWrapper';

import RemoveBasePathModal from './RemoveBasePathModal';

const RemoveBasePathTooltip = (
  <ToolTip id="removeBasePath">
    Remove this base path
  </ToolTip>
);

export default class RemoveBasePathButton extends Component {
  static propTypes = {
    groupName: PropTypes.string.isRequired,
    basePath: PropTypes.string.isRequired,
    children: PropTypes.node,
    then: PropTypes.func
  };

  static defaultProps = {
    children: (
      <OverlayTrigger placement="top" id="view-removeBasePath-overlay" overlay={RemoveBasePathTooltip}>
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
        <RemoveBasePathModal
          ref="modal"
          groupName={this.props.groupName}
          basePath={this.props.basePath}
          then={this.props.then}
        />
      </span>
    );
  }
}
