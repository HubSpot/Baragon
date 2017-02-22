import React, { Component, PropTypes } from 'react';
import { connect } from 'react-redux';

import { SubmitRequest } from '../../../actions/api/requests';
import Utils from '../../../utils';

import FormModal from '../modal/FormModal';

class RemoveUpstreamModal extends Component {
  static propTypes = {
    loadBalancerService: PropTypes.object.isRequired,
    upstream: PropTypes.object.isRequired,
    removeUpstream: PropTypes.func.isRequired,
    then: PropTypes.func,
  };

  show() {
    this.refs.removeUpstreamModal.show();
  }

  render() {
    return (
      <FormModal
        name="Remove all Upstreams"
        ref="removeUpstreamModal"
        action="Remove"
        onConfirm={this.props.removeUpstream}
        buttonStyle="warning"
        formElements={[
          {
            name: 'noValidate',
            type: FormModal.INPUT_TYPES.BOOLEAN,
            label: 'Validate new configuration after applying changes',
            defaultValue: true
          },
          {
            name: 'noReload',
            type: FormModal.INPUT_TYPES.BOOLEAN,
            label: 'Reload configuration after applying changes',
            defaultValue: true
          },
        ]}>
        <p>Are you sure you want to remove this upstream?</p>
        <pre>{this.props.upstream.upstream}</pre>
        <p>
          This will post a new request to remove this upstream from the nginx
          config. It will not alter any other upstreams or options.
        </p>
      </FormModal>
    );
  }
}

const buildRequest = (ownProps, data) => ({
  loadBalancerService: ownProps.loadBalancerService,
  noValidate: !data.noValidate,
  noReload: !data.noReload,
  loadBalancerRequestId: Utils.buildRequestId(ownProps.loadBalancerService.serviceId),
  addUpstreams: [],
  removeUpstreams: [ownProps.upstream]
});

const mapDispatchToProps = (dispatch, ownProps) => ({
  removeUpstream: (data) => dispatch(SubmitRequest
    .trigger(buildRequest(ownProps, data)))
      .then(response => (ownProps.then && ownProps.then(response)))
});

export default connect(
  null,
  mapDispatchToProps,
  null,
  { withRef: true }
)(RemoveUpstreamModal);
