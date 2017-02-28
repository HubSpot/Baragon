import React, { Component, PropTypes } from 'react';

import OverlayTrigger from 'react-bootstrap/lib/OverlayTrigger';
import ToolTip from 'react-bootstrap/lib/Tooltip';
import { Glyphicon } from 'react-bootstrap';

import { getClickComponent } from '../modal/ModalWrapper';

import RemoveUpstreamModal from './RemoveUpstreamModal';

const removeTooltip = (
  <ToolTip id="removeAll">
    Remove this upstream from the service
  </ToolTip>
);

export default class RemoveUpstreamButton extends Component {
  static propTypes = {
    loadBalancerService: PropTypes.object,
    upstream: PropTypes.shape({
      group: PropTypes.string,
      rackId: PropTypes.string,
      requestId: PropTypes.string.isRequired,
      upstream: PropTypes.string.isRequired,
    }),
    children: PropTypes.node,
    afterRemoveUpstream: PropTypes.func,
  };

  static defaultProps = {
    children: (
      <OverlayTrigger placement="top" id="view-delete-overlay" overlay={removeTooltip}>
        <Glyphicon glyph="remove" className="inactive" />
      </OverlayTrigger>
    )
  };

  render() {
    return (
      <span>
        {getClickComponent(this)}
        <RemoveUpstreamModal
          ref="modal"
          loadBalancerService={this.props.loadBalancerService}
          upstream={this.props.upstream}
          then={this.props.afterRemoveUpstream}
        />
      </span>
    );
  }
}
