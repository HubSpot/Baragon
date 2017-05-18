import React, { PropTypes } from 'react';
import { connect } from 'react-redux';

import { refresh } from '../../actions/ui/albDetail';
import Utils from '../../utils';
import rootComponent from '../../rootComponent';

import JSONButton from '../common/JSONButton';
import DetailGroup from '../common/DetailGroup';

import ListenerPanel from './ListenerPanel';

const AlbDetail = ({loadBalancer, listeners, targetGroupsArnsToNames, rulesMap}) => {
  const {
    loadBalancerName: name,
    availabilityZones: azs,
    securityGroups
  } = loadBalancer;
  return (
    <div>
      <div className="row detail-header">
        <div className="col-md-8">
          <h3>{ name }</h3>
        </div>
        <div className="col-md-4 button-container">
          <JSONButton object={loadBalancer}>
            <span className="btn btn-default">JSON</span>
          </JSONButton>
        </div>
      </div>
      <div className="row">
        <div className="col-md-4">
          <h4>Status</h4>
          <ul className="list-group">
            <li className="list-group-item"><strong>Hosted Zone ID:</strong> {loadBalancer.canonicalHostedZoneId}</li>
            <li className="list-group-item"><strong>VPC ID:</strong> {loadBalancer.vpcId}</li>
            <li className="list-group-item"><strong>Scheme:</strong> {loadBalancer.scheme}</li>
            <li className="list-group-item"><strong>Created:</strong> {Utils.timestampFromNow(loadBalancer.createdTime)}</li>
          </ul>
        </div>
        <DetailGroup
          width={4}
          name="Availibility Zones"
          items={azs}
          keyGetter={(zone) => zone.zoneName}
          field={(zone) => (
            <ul className="list-unstyled">
              <li><strong>Zone:</strong> {zone.zoneName}</li>
              <li><strong>Subnet:</strong> {zone.subnetId}</li>
            </ul>
          )}
        />
        <DetailGroup
          width={4}
          name="Security Groups"
          items={securityGroups}
        />
      </div>
      <div className="row">
        <ListenerPanel
          listeners={listeners}
          targetGroupsMap={targetGroupsArnsToNames}
          rulesMap={rulesMap}
        />
      </div>
    </div>
  );
};

AlbDetail.propTypes = {
  loadBalancer: PropTypes.object,
  listeners: PropTypes.array,
  targetGroupsArnsToNames: PropTypes.object,
  rulesMap: PropTypes.object,
};

const buildTargetGroupMap = (targetGroups) => {
  const map = {};
  targetGroups.forEach(({targetGroupArn, targetGroupName}) => {
    map[targetGroupArn] = targetGroupName;
  });
  return map;
};

const getRules = (state, ownProps) => {
  const listeners = Utils.maybe(state, ['api', 'listeners', ownProps.params.albName, 'data']) || [];
  const rulesMap = {};
  listeners.forEach(({listenerArn}) => {
    rulesMap[listenerArn] = Utils.maybe(state, ['api', 'rules', listenerArn, 'data']);
  });
  return rulesMap;
};


const mapStateToProps = (state, ownProps) => ({
  loadBalancer: Utils.maybe(state, ['api', 'alb', ownProps.params.albName, 'data']),
  listeners: Utils.maybe(state, ['api', 'listeners', ownProps.params.albName, 'data']),
  targetGroupsArnsToNames: buildTargetGroupMap(Utils.maybe(state, ['api', 'targetGroups', 'data'])),
  rulesMap: getRules(state, ownProps),
});

export default connect(mapStateToProps)(rootComponent(AlbDetail, (props) => {
  return refresh(props.params.albName);
}));
