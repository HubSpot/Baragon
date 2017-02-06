import React, { Component, PropTypes } from 'react';
import { connect } from 'react-redux';

import { DeleteService } from '../../../actions/api/services';

import FormModal from '../modal/FormModal';

class DeleteServiceModal extends Component {
  static propTypes = {
    serviceId: PropTypes.string.isRequired,
    deleteService: PropTypes.func.isRequired
  };

  show() {
    this.refs.deleteServiceModal.show();
  }

  render() {
    return (
      <FormModal
        name="Delete Service"
        ref="deleteServiceModal"
        action="Delete"
        onConfirm={this.props.deleteService}
        buttonStyle="danger"
        formElements={[]}>
        <p>Are you sure you want to reload configs this service?</p>
        <pre>{this.props.serviceId}</pre>
      </FormModal>
    );
  }
}

const mapDispatchToProps = (dispatch, ownProps) => ({
  deleteService: () => dispatch(DeleteService.trigger(ownProps.serviceId)).then(response => (ownProps.then && ownProps.then(response)))
});

export default connect(
  null,
  mapDispatchToProps,
  null,
  { withRef: true }
)(DeleteServiceModal);