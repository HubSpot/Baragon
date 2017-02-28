import React, { Component, PropTypes } from 'react';

import OverlayTrigger from 'react-bootstrap/lib/OverlayTrigger';
import ToolTip from 'react-bootstrap/lib/Tooltip';

import { getClickComponent } from '../modal/ModalWrapper';

import RemoveTrafficSourceModal from './RemoveTrafficSourceModal';

const RemoveTrafficSourceTooltip = (
  <ToolTip id="addTrafficSource">
    Remove this Traffic Source
  </ToolTip>
);

export default class AddTrafficSourceButton extends Component {
  static propTypes = {
    groupName: PropTypes.string.isRequired,
    trafficSource: PropTypes.shape({
      name: PropTypes.string,
      type: PropTypes.string
    }).isRequired,
    children: PropTypes.node,
    then: PropTypes.func
  };

  static defaultProps = {
    children: (
      <OverlayTrigger placement="top" id="view-add-traffic-source-overlay" overlay={RemoveTrafficSourceTooltip}>
        <a className="icon badge" title="Remove Traffic Source">
          <span className="glyphicon glyphicon-remove"></span>
        </a>
      </OverlayTrigger>
    )
  };

  render() {
    return (
      <span>
        {getClickComponent(this)}
        <RemoveTrafficSourceModal
          ref="modal"
          groupName={this.props.groupName}
          trafficSource={this.props.trafficSource}
          then={this.props.then}
        />
      </span>
    );
  }
}
