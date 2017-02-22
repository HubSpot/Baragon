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
        <p>Are you sure you sure you want to delete this service?</p>
        <pre>{this.props.serviceId}</pre>
        <p>
          Deleting a service will remove the entry from Baragon's state node as
          well as clearing the locks on any associated base paths. It will also
          remove the configs from the load balancer (they will be backed up for
          reference).
        </p>
      </FormModal>
    );
  }
}

const mapDispatchToProps = (dispatch, ownProps) => ({
  deleteService: (data) => dispatch(DeleteService
    .trigger(ownProps.serviceId, !data.noValidate, !data.noReload))
      .then(response => (ownProps.then && ownProps.then(response)))
});

export default connect(
  null,
  mapDispatchToProps,
  null,
  { withRef: true }
)(DeleteServiceModal);
