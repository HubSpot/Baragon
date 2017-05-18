import React, { PropTypes } from 'react';
import { connect } from 'react-redux';
import { Link } from 'react-router';
import { Glyphicon } from 'react-bootstrap';

import { refresh } from '../../actions/ui/targetGroupDetail';
import Utils from '../../utils';
import rootComponent from '../../rootComponent';
import DetailGroup from '../common/DetailGroup';
import JSONButton from '../common/JSONButton';
import UpdateInstanceButton from '../common/modalButtons/UpdateInstanceButton';

import HealthCheckPanel from './HealthCheckPanel';
import DetailItem from './DetailItem';

const renderLoadBalancer = (loadBalancerArnsToNames) => (arn) => {
  const name = loadBalancerArnsToNames[arn];
  if (name) {
    return <Link to={`/albs/load-balancers/${name}`}>{name}</Link>;
  }

  return arn;
};

const TargetGroupDetail = ({targets, targetGroup, loadBalancerArnsToNames, editable}) => {
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
          editable={editable}
          healthCheck={({
            targetGroupName: targetGroup.targetGroupName,
            protocol: targetGroup.healthCheckProtocol,
            port: targetGroup.healthCheckPort,
            path: targetGroup.healthCheckPath,
            interval: targetGroup.healthCheckIntervalSeconds,
            timeout: targetGroup.healthCheckTimeoutSeconds,
            healthyThreshold: targetGroup.healthyThresholdCount,
            unhealthyThreshold: targetGroup.unhealthyThresholdCount
          })}
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
        <div className="col-md-4">
          <DetailGroup width={12} name="Instances" items={targets} keyGetter={(instance) => instance.target.id} field={
              (instance) => (
                <ul className="list-unstyled">
                  <li><strong>ID:</strong> {instance.target.id}</li>
                  <li><strong>Port</strong>: {instance.target.port}</li>
                  <li><strong>Status</strong>: {instance.targetHealth.state}</li>
                  { instance.targetHealth.reason && <li><strong>Reason</strong>: {instance.targetHealth.reason}</li>}
                  { editable &&
                    <UpdateInstanceButton
                      type='alb'
                      action='remove'
                      loadBalancer={targetGroup.targetGroupName}
                      instanceId={instance.target.id}
                      then={refresh(targetGroup.targetGroupName)}
                    >
                      <a data-action="update">
                        <Glyphicon glyph="remove" />
                      </a>
                    </UpdateInstanceButton>
                  }
                </ul>
              )
            }
          />
          { editable &&
            <UpdateInstanceButton
              type='alb'
              action='add'
              loadBalancer={targetGroup.targetGroupName}
              then={refresh(targetGroup.targetGroupName)}
            >
              <a data-action="update">
                <Glyphicon glyph="plus" />
              </a>
            </UpdateInstanceButton>
          }
        </div>
        <DetailGroup
          name="Associated Load Balancers"
          items={targetGroup.loadBalancerArns}
          width={4}
          field={renderLoadBalancer(loadBalancerArnsToNames)}
        />
      </div>
    </div>
  );
};

TargetGroupDetail.propTypes = {
  targets: PropTypes.arrayOf(PropTypes.shape({
    target: PropTypes.shape({
      id: PropTypes.string,
      port: PropTypes.oneOfType([PropTypes.number, PropTypes.string]),
    }),
    targetHealth: PropTypes.shape({
      state: PropTypes.oneOf(['healthy', 'initial', 'unhealthy', 'unused', 'draining']),
      reason: PropTypes.string,
    })
  })),
  targetGroup: PropTypes.object,
  loadBalancerArnsToNames: PropTypes.object,
  editable: PropTypes.bool.isRequired,
};

const mapLoadBalancerArnsToNames = (loadBalancers) => {
  const map = {};
  loadBalancers.forEach(({loadBalancerArn, loadBalancerName}) => {
    map[loadBalancerArn] = loadBalancerName;
  });
  return map;
};

const mapStateToProps = (state, ownProps) => ({
  targets: Utils.maybe(state, ['api', 'targetGroupTargets', ownProps.params.targetGroupName, 'data']),
  targetGroup: Utils.maybe(state, ['api', 'targetGroup', ownProps.params.targetGroupName, 'data']),
  loadBalancerArnsToNames: mapLoadBalancerArnsToNames(state.api.albs.data),
  editable: config.allowEdit,
});

export default connect(mapStateToProps)(rootComponent(TargetGroupDetail, (props) => {
  return refresh(props.params.targetGroupName);
}));
