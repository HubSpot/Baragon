import React, { PropTypes } from 'react';
import { connect } from 'react-redux';
import { Glyphicon } from 'react-bootstrap';

import rootComponent from '../../rootComponent';
import JSONButton from '../common/JSONButton';
import UpdateInstanceButton from '../common/modalButtons/UpdateInstanceButton';
import { refresh } from '../../actions/ui/elbDetail';
import Utils from '../../utils';

import HealthCheckPanel from './HealthCheckPanel';
import DetailGroup from '../common/DetailGroup';

const DetailItem = ({name, value}) => {
  return (
    <li className="list-group-item">
      <strong>{ name }: </strong>
      {value}
    </li>
  );
};

DetailItem.propTypes = {
  name: PropTypes.string.isRequired,
  value: PropTypes.oneOfType([
    PropTypes.string,
    PropTypes.number,
  ]).isRequired,
};


const ElbDetail = ({loadBalancerName, loadBalancer, instances, editable}) => {
  return (
    <div>
      <div className="row detail-header">
        <div className="col-md-8">
          <h3>{ loadBalancerName }</h3>
        </div>
        <div className="col-md-4 button-container">
          <JSONButton object={loadBalancer}>
            <span className="btn btn-default">JSON</span>
          </JSONButton>
        </div>
      </div>
      <div className="row">
        <HealthCheckPanel healthCheck={loadBalancer.healthCheck} />
        <div className="col-md-8">
          <ul className="list-group">
            <DetailItem name="DNS Name" value={loadBalancer.dnsname} />
            <DetailItem name="Hosted Zone Name" value={loadBalancer.canonicalHostedZoneName} />
            <DetailItem name="Hosted Zone ID" value={loadBalancer.canonicalHostedZoneNameID} />
            <DetailItem name="VpcID" value={loadBalancer.vpcid} />
            <DetailItem name="Created" value={Utils.timestampFromNow(loadBalancer.createdTime)} />
          </ul>
        </div>
      </div>
      <div className="row">
        <div className="col-md-3">
          <DetailGroup
            width={12}
            name="Instances"
            items={instances}
            keyGetter={(instance) => instance.instanceId}
            field={(instance) =>
              <ul className="list-unstyled">
                <li><strong>ID</strong>: {instance.instanceId}</li>
                <li><strong>State</strong>: {instance.state}</li>
                {instance.description !== 'N/A' && <li><strong>Reason</strong>: {instance.description}</li> }
                {editable && <UpdateInstanceButton
                  type="elb"
                  action="remove"
                  loadBalancer={loadBalancerName}
                  instanceId={instance.instanceId}
                  then={refresh(loadBalancerName)}
                  >
                    <a data-action="update">
                      <Glyphicon glyph="remove" />
                    </a>
                  </UpdateInstanceButton>
                }
              </ul>
            }
          />
          {editable && <UpdateInstanceButton
            type="elb"
            action="add"
            loadBalancer={loadBalancerName}
            then={refresh(loadBalancerName)}
            >
              <a data-action="update">
                <Glyphicon glyph="plus" />
              </a>
            </UpdateInstanceButton>
          }
        </div>
        <DetailGroup name="Availibility Zones" items={loadBalancer.availabilityZones} />
        <DetailGroup name="Security Groups" items={loadBalancer.securityGroups} />
        <DetailGroup name="Subnets" items={loadBalancer.subnets} />
      </div>
    </div>
  );
};

ElbDetail.propTypes = {
  loadBalancerName: PropTypes.string,
  loadBalancer: PropTypes.object,
  instances: PropTypes.arrayOf(PropTypes.shape({
    instanceId: PropTypes.string,
    state: PropTypes.oneOf(['InService', 'OutOfService', 'Unknown']),
    reason: PropTypes.reason,
  })),
  editable: PropTypes.bool.isRequired
};

const mapStateToProps = (state, ownProps) => ({
  loadBalancerName: ownProps.params.loadBalancerName,
  loadBalancer: Utils.maybe(state, ['api', 'elb', ownProps.params.loadBalancerName, 'data']),
  instances: Utils.maybe(state, ['api', 'elbInstances', ownProps.params.loadBalancerName, 'data']),
  editable: config.allowEdit,
});

export default connect(mapStateToProps)(rootComponent(ElbDetail, (props) =>
  refresh(props.params.loadBalancerName)));
