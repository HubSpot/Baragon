import React, { Component, PropTypes } from 'react';

import { Glyphicon } from 'react-bootstrap';
import OverlayTrigger from 'react-bootstrap/lib/OverlayTrigger';
import ToolTip from 'react-bootstrap/lib/Tooltip';

import { getClickComponent } from '../modal/ModalWrapper';

import ModifyCountModal from './ModifyCountModal';

const ModifyTargetCountTooltip = (
  <ToolTip id="modifyCount">
    Change Current Target Count
  </ToolTip>
);

export default class ModifyCountButton extends Component {
  static propTypes = {
    groupName: PropTypes.string.isRequired,
    currentCount: PropTypes.number,
    children: PropTypes.node,
    then: PropTypes.func
  };

  static defaultProps = {
    children: (
      <OverlayTrigger placement="top" id="view-modify-target-count-overlay" overlay={ModifyTargetCountTooltip}>
        <a>
          <Glyphicon glyph="edit" />
        </a>
      </OverlayTrigger>
    )
  };

  render() {
    return (
      <span>
        {getClickComponent(this)}
        <ModifyCountModal
          ref="modal"
          groupName={this.props.groupName}
          currentCount={this.props.currentCount}
          then={this.props.then}
        />
      </span>
    );
  }
}
