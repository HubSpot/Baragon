import React, { Component, PropTypes } from 'react';
import { connect } from 'react-redux';

import { RemoveUpstreams } from '../../../actions/api/services';

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
          },
          {
            name: 'noReload',
            type: FormModal.INPUT_TYPES.BOOLEAN,
            label: 'Reload configuration after applying changes',
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

const mapDispatchToProps = (dispatch, ownProps) => ({
  removeUpstream: (data) => dispatch(RemoveUpstreams
    .trigger(ownProps.loadBalancerService, [ownProps.upstream], data.noValidate, data.noReload))
      .then(response => (ownProps.then && ownProps.then(response)))
});

export default connect(
  null,
  mapDispatchToProps,
  null,
  { withRef: true }
)(RemoveUpstreamModal);
