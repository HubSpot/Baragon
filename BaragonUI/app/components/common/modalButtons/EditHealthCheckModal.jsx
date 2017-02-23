import React, { Component, PropTypes } from 'react';
import { connect } from 'react-redux';

import { ModifyTargetGroup } from '../../../actions/api/albs';

import FormModal from '../modal/FormModal';

class EditHealthCheckModal extends Component {
  static propTypes = {
    targetGroupName: PropTypes.string.isRequired,
    protocol: PropTypes.oneOf(['HTTP', 'HTTPS']),
    port: PropTypes.string,
    path: PropTypes.string,
    interval: PropTypes.number,
    timeout: PropTypes.number,
    healthyThreshold: PropTypes.number,
    unhealthyThreshold: PropTypes.number,
    modifyHealthCheck: PropTypes.func.isRequired
  };

  show() {
    this.refs.editHealthCheckModal.show();
  }

  render() {
    return (
      <FormModal
        name={`Modify ${this.props.targetGroupName}'s Health Check Settings`}
        ref="editHealthCheckModal"
        action="Change"
        onConfirm={this.props.modifyHealthCheck}
        buttonStyle="primary"
        formElements={[
          {
            name: 'healthCheckProtocol',
            type: FormModal.INPUT_TYPES.RADIO,
            label: 'Health Check Protocol',
            values: [
              {value: 'HTTP', label: 'HTTP'},
              {value: 'HTTPS', label: 'HTTPS'},
            ],
            defaultValue: this.props.protocol,
            isRequired: true,
          },
          {
            name: 'healthCheckPort',
            label: 'Health Check Port',
            type: FormModal.INPUT_TYPES.STRING,
            defaultValue: this.props.port,
            isRequired: true,
          },
          {
            name: 'healthCheckPath',
            label: 'Health Check Path',
            type: FormModal.INPUT_TYPES.STRING,
            defaultValue: this.props.path,
            isRequired: true,
          },
          {
            name: 'healthCheckIntervalSeconds',
            label: 'Health Check Interval (seconds)',
            type: FormModal.INPUT_TYPES.NUMBER,
            defaultValue: this.props.interval,
            isRequired: true,
            minValue: 5,
            maxValue: 300,
          },
          {
            name: 'healthCheckTimeoutSeconds',
            label: 'Health Check Timeout (seconds)',
            type: FormModal.INPUT_TYPES.NUMBER,
            defaultValue: this.props.timeout,
            isRequired: true,
            minValue: 2,
            maxValue: 60,
          },
          {
            name: 'healthyThresholdCount',
            label: 'Healthy Threshold Count',
            type: FormModal.INPUT_TYPES.NUMBER,
            defaultValue: this.props.healthyThreshold,
            isRequired: true,
            minValue: 2,
            maxValue: 10,
          },
          {
            name: 'unhealthyThresholdCount',
            label: 'Unhealthy Threshold Count',
            type: FormModal.INPUT_TYPES.NUMBER,
            defaultValue: this.props.unhealthyThreshold,
            isRequired: true,
            minValue: 2,
            maxValue: 10,
          },
        ]}
      />
    );
  }
}

const mapDispatchToProps = (dispatch, ownProps) => ({
  modifyHealthCheck: (data) => dispatch(ModifyTargetGroup.trigger(ownProps.targetGroupName, data))
    .then(response => ownProps.then && ownProps.then(response))
});

export default connect(
  null,
  mapDispatchToProps,
  null,
  { withRef: true }
)(EditHealthCheckModal);
