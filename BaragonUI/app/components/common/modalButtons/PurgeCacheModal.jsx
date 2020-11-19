import React, { Component, PropTypes } from 'react';
import { connect } from 'react-redux';

import { PurgeCache } from '../../../actions/api/services';

import FormModal from '../modal/FormModal';

class PurgeCacheModal extends Component {
    static propTypes = {
        serviceId: PropTypes.string.isRequired,
        purgeCache: PropTypes.func.isRequired
    };

    show() {
        this.refs.purgeCacheModal.show();
    }

    render() {
        return (
            <FormModal
                name="Purge Cache"
                ref="purgeCacheModal"
                action="Purge Cache"
                onConfirm={this.props.purgeCache}
                buttonStyle="info"
                formElements={[]}>
                <p>Are you sure you want to purge this service's cache?</p>
                <pre>{this.props.serviceId}</pre>
            </FormModal>
        );
    }
}

const mapDispatchToProps = (dispatch, ownProps) => ({
    purgeCache: () => dispatch(PurgeCache.trigger(ownProps.serviceId)).then(response => (ownProps.then && ownProps.then(response)))
});

export default connect(
    null,
    mapDispatchToProps,
    null,
    { withRef: true }
)(PurgeCacheModal);