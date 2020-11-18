import React, { Component, PropTypes } from 'react';

import { Glyphicon } from 'react-bootstrap';
import OverlayTrigger from 'react-bootstrap/lib/OverlayTrigger';
import ToolTip from 'react-bootstrap/lib/Tooltip';

import { getClickComponent } from '../modal/ModalWrapper';

import PurgeCacheModal from "./PurgeCacheModal";

const purgeCacheTooltip = (
    <ToolTip id="purge-cache">
        PurgeCache
    </ToolTip>
);

export default class PurgeCacheButton extends Component {
    static propTypes = {
        serviceId: PropTypes.string.isRequired,
        children: PropTypes.node,
        then: PropTypes.func
    };

    static defaultProps = {
        children: (
            <OverlayTrigger placement="top" id="view-purge-cache-overlay" overlay={purgeCacheTooltip}>
                <a data-action="purge-cache">
                    <Glyphicon glyph="trash" />
                </a>
            </OverlayTrigger>
        )
    };

    render() {
        return (
            <span>
                {getClickComponent(this)}
                <PurgeCacheModal ref="modal" serviceId={this.props.serviceId} then={this.props.then} />
            </span>
        );
    }
}

