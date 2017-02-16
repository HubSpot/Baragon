import React, { PropTypes } from 'react';
import { connect } from 'react-redux';

import rootComponent from '../../rootComponent';
import JSONButton from '../common/JSONButton';
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


const ElbDetail = ({loadBalancerName, loadBalancer}) => {
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
        <DetailGroup
          name="Instances"
          items={loadBalancer.instances}
          keyGetter={(instance) => instance.instanceId}
          field={(instance) => instance.instanceId}
        />
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
};

const mapStateToProps = (state, ownProps) => ({
  loadBalancerName: ownProps.params.loadBalancerName,
  loadBalancer: Utils.maybe(state, ['api', 'elb', ownProps.params.loadBalancerName, 'data']),
});

export default connect(mapStateToProps)(rootComponent(ElbDetail, (props) =>
  refresh(props.params.loadBalancerName)));
