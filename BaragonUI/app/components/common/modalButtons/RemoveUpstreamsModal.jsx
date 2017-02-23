import React, { Component, PropTypes } from 'react';
import { connect } from 'react-redux';

import { SubmitRequest } from '../../../actions/api/requests';
import Utils from '../../../utils';

import FormModal from '../modal/FormModal';

class RemoveUpstreamsModal extends Component {
  static propTypes = {
    loadBalancerService: PropTypes.object.isRequired,
    upstreams: PropTypes.array.isRequired,
    removeUpstreams: PropTypes.func.isRequired,
    then: PropTypes.func,
  };

  show() {
    this.refs.removeUpstreamsModal.show();
  }

  render() {
    return (
      <FormModal
        name="Remove all Upstreams"
        ref="removeUpstreamsModal"
        action="Remove"
        onConfirm={this.props.removeUpstreams}
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
        <p>Are you sure you want to remove all upstreams for this service?</p>
        <pre>{this.props.loadBalancerService.serviceId}</pre>
        <p>
          This will post a new request to remove all the current upstreams
          for a service. This will effectively 'undo' the request by creating
          empty config files.
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
  removeUpstreams: ownProps.upstreams,
});

const mapDispatchToProps = (dispatch, ownProps) => ({
  removeUpstreams: (data) => dispatch(SubmitRequest
    .trigger(buildRequest(ownProps, data)))
      .then(response => (ownProps.then && ownProps.then(response)))
});

export default connect(
  null,
  mapDispatchToProps,
  null,
  { withRef: true }
)(RemoveUpstreamsModal);
