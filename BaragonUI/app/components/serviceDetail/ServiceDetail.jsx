import React, { PropTypes } from 'react';
import { connect } from 'react-redux';
import rootComponent from '../../rootComponent';
import { refresh } from '../../actions/ui/serviceDetail';
import Utils from '../../utils';

import DetailHeader from './DetailHeader';
import OwnersPanel from './OwnersPanel';
import LoadBalancersPanel from './LoadBalancersPanel';
import RecentRequestsPanel from './RecentRequestsPanel';
import UpstreamsPanel from './UpstreamsPanel';

const ServiceDetail = ({service, requestHistory, editable}) => {
  const {service: serviceObject, upstreams} = service;
  const {
    serviceId,
    owners,
    loadBalancerGroups,
    serviceBasePath: basePath
  } = serviceObject;
  return (
    <div>
      <div className="row detail-header">
        <DetailHeader
          id={serviceId}
          object={service}
          basePath={basePath}
          editable={editable}
        />
      </div>
      <div className="row">
        <OwnersPanel owners={owners} />
        <LoadBalancersPanel loadBalancerGroups={loadBalancerGroups} />
      </div>
      <div className="row">
        <RecentRequestsPanel requests={requestHistory} />
      </div>
      <div className="row">
        <UpstreamsPanel
          upstreams={upstreams}
          editable={editable}
        />
      </div>
    </div>
  );
};

ServiceDetail.propTypes = {
  service: PropTypes.object,
  requestHistory: PropTypes.array,
  editable: PropTypes.bool,
};

export default connect((state, ownProps) => ({
  service: Utils.maybe(state, ['api', 'service', ownProps.params.serviceId, 'data']),
  requestHistory: Utils.maybe(state, ['api', 'requestHistory', ownProps.params.serviceId, 'data']),
  editable: true,
}))(rootComponent(ServiceDetail, (props) => refresh(props.params.serviceId)));
