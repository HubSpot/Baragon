import React, { PropTypes } from 'react';
import { connect } from 'react-redux';

import { refresh } from '../../actions/ui/targetGroupDetail';
import Utils from '../../utils';
import rootComponent from '../../rootComponent';
import DetailGroup from '../common/DetailGroup';
import JSONButton from '../common/JSONButton';

import HealthCheckPanel from './HealthCheckPanel';
import DetailItem from './DetailItem';

const TargetGroupDetail = ({targets, targetGroup}) => {
  return (
    <div>
      <div className="row detail-header">
        <div className="col-md-8">
          <h3>{targetGroup.targetGroupName}</h3>
        </div>
        <div className="col-md-4 button-container">
          <JSONButton object={targetGroup}>
            <span className="btn btn-default">JSON</span>
          </JSONButton>
        </div>
      </div>
      <div className="row">
        <HealthCheckPanel
          path={targetGroup.healthCheckPath}
          interval={targetGroup.healthCheckIntervalSeconds}
          timeout={targetGroup.healthCheckTimeoutSeconds}
          healthyThreshold={targetGroup.healthyThresholdCount}
        />
        <div className="col-md-8">
          <ul className="list-group">
            <DetailItem name="ARN" value={targetGroup.targetGroupArn} />
            <DetailItem name="Protocol" value={targetGroup.protocol} />
            <DetailItem name="Port" value={targetGroup.port} />
            <DetailItem name="VPC ID" value={targetGroup.vpcId} />
          </ul>
        </div>
      </div>
      <div className="row">
        <DetailGroup name="Instances" items={targets} keyGetter={(instance) => instance.id} field={
            (instance) => (
              <ul className="list-unstyled">
                <li><strong>ID:</strong> {instance.id}</li>
                <li><strong>Port</strong>: {instance.port}</li>
              </ul>
            )
          }
        />
        <DetailGroup name="Associated Load Balancers" items={targetGroup.loadBalancerArns} width={9} />
      </div>
    </div>
  );
};

TargetGroupDetail.propTypes = {
  targets: PropTypes.arrayOf(PropTypes.shape({
    id: PropTypes.string,
    port: PropTypes.number
  })),
  targetGroup: PropTypes.object,
};

const mapStateToProps = (state, ownProps) => ({
  targets: Utils.maybe(state, ['api', 'targetGroupTargets', ownProps.params.targetGroupName, 'data']),
  targetGroup: Utils.maybe(state, ['api', 'targetGroup', ownProps.params.targetGroupName, 'data'])
});

export default connect(mapStateToProps)(rootComponent(TargetGroupDetail, (props) => {
  return refresh(props.params.targetGroupName);
}));
