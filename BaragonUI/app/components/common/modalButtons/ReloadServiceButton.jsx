import React, { Component, PropTypes } from 'react';

import { Glyphicon } from 'react-bootstrap';
import OverlayTrigger from 'react-bootstrap/lib/OverlayTrigger';
import ToolTip from 'react-bootstrap/lib/Tooltip';

import { getClickComponent } from '../modal/ModalWrapper';

import ReloadServiceModal from './ReloadServiceModal';

const reloadTooltip = (
  <ToolTip id="reload">
    Reload Service Config
  </ToolTip>
);

export default class ReloadServiceButton extends Component {
  static propTypes = {
    serviceId: PropTypes.string.isRequired,
    children: PropTypes.node,
    then: PropTypes.func
  };

  static defaultProps = {
    children: (
      <OverlayTrigger placement="top" id="view-reload-overlay" overlay={reloadTooltip}>
        <a data-action="reload">
          <Glyphicon glyph="refresh" />
        </a>
      </OverlayTrigger>
    )
  };

  render() {
    return (
      <span>
        {getClickComponent(this)}
        <ReloadServiceModal ref="modal" serviceId={this.props.serviceId} then={this.props.then} />
      </span>
    );
  }
}
