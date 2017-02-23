import React, { PropTypes } from 'react';

import OverlayTrigger from 'react-bootstrap/lib/OverlayTrigger';
import ToolTip from 'react-bootstrap/lib/Tooltip';

import { getClickComponent } from '../modal/ModalWrapper';

import RemoveUpstreamsModal from './RemoveUpstreamsModal';

const removeTooltip = (
  <ToolTip id="removeAll">
    Remove all upstreams from this service
  </ToolTip>
);

export default class RemoveUpstreamsButton extends React.Component {
  static propTypes = {
    loadBalancerService: PropTypes.object,
    upstreams: PropTypes.arrayOf(PropTypes.shape({
      group: PropTypes.string,
      rackId: PropTypes.string,
      requestId: PropTypes.string.isRequired,
      upstream: PropTypes.string.isRequired,
    })),
    children: PropTypes.node,
    afterRemoveUpstreams: PropTypes.func,
  };

  static defaultProps = {
    children: (
      <OverlayTrigger placement="top" id="view-delete-overlay" overlay={removeTooltip}>
        <span>
          <a className="btn btn-warning">Remove Upstreams</a>
        </span>
      </OverlayTrigger>
    )
  };

  render() {
    return (
      <span>
        {getClickComponent(this)}
        <RemoveUpstreamsModal
          ref="modal"
          loadBalancerService={this.props.loadBalancerService}
          upstreams={this.props.upstreams}
          then={this.props.afterRemoveUpstreams}
        />
      </span>
    );
  }
}
