import React, { Component, PropTypes } from 'react';
import Modal from 'react-bootstrap/lib/Modal';
import Button from 'react-bootstrap/lib/Button';
import OverlayTrigger from 'react-bootstrap/lib/OverlayTrigger';
import ToolTip from 'react-bootstrap/lib/Tooltip';
import {FetchRenderedConfigs} from "../../actions/api/services";
import {connect} from "react-redux";
import Utils from "../../utils";

class RenderedConfigsButton extends Component {
    static propTypes = {
        serviceId: PropTypes.string,
        children: PropTypes.oneOfType([
            PropTypes.arrayOf(PropTypes.node),
            PropTypes.node
        ]).isRequired,
        renderedConfigs: PropTypes.arrayOf(PropTypes.shape({
            fullPath: PropTypes.string,
            content: PropTypes.string,
        })),
        showOverlay: PropTypes.bool,
        className: PropTypes.string,
        linkClassName: PropTypes.string,
        fetchRenderedConfigs: PropTypes.func.isRequired
    };

    constructor() {
        super();
        this.state = {
            modalOpen: false
        };

        this.showRenderedConfigs = this.showRenderedConfigs.bind(this);
        this.hideRenderedConfigs = this.hideRenderedConfigs.bind(this);
    }

    componentDidMount() {
    }

    showRenderedConfigs() {
        this.props.fetchRenderedConfigs();
        this.setState({
            modalOpen: true
        });
    }

    hideRenderedConfigs() {
        this.setState({
            modalOpen: false
        });
    }

    render() {
        const renderedConfigsToolTip = (
            <ToolTip id="view-rendered-configs-tooltip">
                Rendered Configs
            </ToolTip>
        );
        const button = (
            <a className={this.props.linkClassName} onClick={this.showRenderedConfigs} alt="Show RenderedConfigs">{this.props.children}</a>
        );
        return (
            <span className={this.props.className}>
        {this.props.showOverlay ? (
            <OverlayTrigger placement="top" id="view-rendered-configs-overlay" overlay={renderedConfigsToolTip}>
                {button}
            </OverlayTrigger>) : button
        }
                <Modal show={this.state.modalOpen} onHide={this.hideRenderedConfigs} bsSize="large">
          <Modal.Body>
            <div className="constrained-modal rendered-configs-modal">

                {this.props.renderedConfigs ? this.props.renderedConfigs.map(renderedConfig => {
                    return (
                        <div>
                            <span>{renderedConfig.fullPath}</span>
                            <pre>{renderedConfig.content}</pre>
                        </div>
                    )
                }) : <span>Loading or no Rendered Configs could be found.</span>}
            </div>
          </Modal.Body>
          <Modal.Footer>
            <Button bsStyle="info" onClick={this.hideRenderedConfigs}>Close</Button>
          </Modal.Footer>
        </Modal>
      </span>
        );
    }
}
const mapStateToProps = (state, ownProps) => ({
    renderedConfigs: Utils.maybe(state, ['api', 'renderedConfigs', ownProps.serviceId, 'data']),
});

const mapDispatchToProps = (dispatch, ownProps) => ({
    fetchRenderedConfigs: () => dispatch(FetchRenderedConfigs
        .trigger(ownProps.serviceId))
});

export default connect(
    mapStateToProps,
    mapDispatchToProps,
    null,
    { withRef: true }
)(RenderedConfigsButton);
