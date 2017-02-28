import React, { Component, PropTypes } from 'react';
import { connect } from 'react-redux';

import { ReloadService } from '../../../actions/api/services';

import FormModal from '../modal/FormModal';

class ReloadServiceModal extends Component {
  static propTypes = {
    serviceId: PropTypes.string.isRequired,
    reloadService: PropTypes.func.isRequired
  };

  show() {
    this.refs.reloadServiceModal.show();
  }

  render() {
    return (
      <FormModal
        name="Reload Service Configs"
        ref="reloadServiceModal"
        action="Reload"
        onConfirm={this.props.reloadService}
        buttonStyle="info"
        formElements={[]}>
        <p>Are you sure you want to reload configs this service?</p>
        <pre>{this.props.serviceId}</pre>
      </FormModal>
    );
  }
}

const mapDispatchToProps = (dispatch, ownProps) => ({
  reloadService: () => dispatch(ReloadService.trigger(ownProps.serviceId)).then(response => (ownProps.then && ownProps.then(response)))
});

export default connect(
  null,
  mapDispatchToProps,
  null,
  { withRef: true }
)(ReloadServiceModal);