import React, { Component, PropTypes } from 'react';
import { connect } from 'react-redux';

import { RemoveFromTargetGroup, AddToTargetGroup } from '../../../actions/api/albs';
import { AddToElb, RemoveFromElb } from '../../../actions/api/elbs';

import FormModal from '../modal/FormModal';

class UpdateInstanceModal extends Component {
  static propTypes = {
    instanceId: PropTypes.string,
    type: PropTypes.oneOf(['elb', 'alb']).isRequired,
    loadBalancer: PropTypes.string.isRequired,
    action: PropTypes.oneOf(['add', 'remove']).isRequired
  };

  show() {
    this.refs.updateLoadBalancer.show();
  }

  render() {
    const formElements = [];
    let message;
    if (this.props.action == 'add') {
      formElements.push({
        name: 'instanceId',
        type: FormModal.INPUT_TYPES.STRING,
        label: 'Instance ID: '
      });
      message = "";
    } else {
      message = (
        <div>
          "Are you sure you want to remove this instance?"
          <pre>
            {this.props.loadBalancer}: {this.props.instanceId}
          </pre>
        </div>
      );
    }
    return (
      <FormModal
        name="Update Load Balancer"
        ref="updateLoadBalancer"
        action="Update"
        onConfirm={(data) => this.props.updateLoadBalancer(data)}
        buttonStyle="info"
        formElements={formElements}
      >
        {message}
      </FormModal>
    );
  }
}

const mapDispatchToProps = (dispatch, ownProps) => ({
  updateLoadBalancer: (data) => {
    if (ownProps.action === 'add') {
      if (ownProps.type === 'elb') {
        return dispatch(AddToElb.trigger(ownProps.loadBalancer, data.instanceId));
      } else {
        return dispatch(AddToTargetGroup.trigger(ownProps.loadBalancer, data.instanceId)).then(response => (ownProps.then && ownProps.then(response)));
      }
    } else {
      if (ownProps.type === 'elb') {
        return dispatch(RemoveFromElb.trigger(ownProps.loadBalancer, ownProps.instanceId)).then(response => (ownProps.then && ownProps.then(response)));
      } else {
        return dispatch(RemoveFromTargetGroup.trigger(ownProps.loadBalancer, ownProps.instanceId)).then(response => (ownProps.then && ownProps.then(response)));
      }
    }
  }
});

export default connect(
  null,
  mapDispatchToProps,
  null,
  { withRef: true }
)(UpdateInstanceModal);
