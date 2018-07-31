import React, { Component, PropTypes } from 'react';
import { connect } from 'react-redux';

import { AddTrafficSource } from '../../../actions/api/groups';

import FormModal from '../modal/FormModal';

class AddTrafficSourceModal extends Component {
  static propTypes = {
    groupName: PropTypes.string.isRequired,
    addTrafficSource: PropTypes.func.isRequired
  };

  show() {
    this.refs.addTrafficSourceModal.show();
  }

  render() {
    return (
      <FormModal
        name="Add Traffic Source"
        ref="addTrafficSourceModal"
        action="Add Traffic Source"
        onConfirm={this.props.addTrafficSource}
        buttonStyle="primary"
        formElements={[
          {
            name: 'type',
            type: FormModal.INPUT_TYPES.RADIO,
            label: 'Type: ',
            values: [
              {value: 'CLASSIC', label: 'Classic Load Balancer'},
              {value: 'ALB_TARGET_GROUP', label: 'Target Group'}],
            isRequired: true
          },
          {
            name: 'registerBy',
            type: FormModal.INPUT_TYPES.RADIO,
            label: 'Register By: ',
            values: [
              {value: 'INSTANCE_ID', label: 'Instance Id'},
              {value: 'PRIVATE_IP', label: 'Private Ip'}],
            isRequired: true
          },
          {
            name: 'name',
            type: FormModal.INPUT_TYPES.STRING,
            label: 'Name: '
          }
        ]}>
        Please specify the traffic source type and the traffic source's name.
      </FormModal>
    );
  }
}

const mapDispatchToProps = (dispatch, ownProps) => ({
  addTrafficSource: (data) => dispatch(AddTrafficSource.trigger(ownProps.groupName, data))
      .then(response => (ownProps.then && ownProps.then(response)))
});

export default connect(
  null,
  mapDispatchToProps,
  null,
  { withRef: true }
)(AddTrafficSourceModal);
