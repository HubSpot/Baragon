import React, { PropTypes } from 'react';
import { connect } from 'react-redux';
import { withRouter } from 'react-router';
import rootComponent from '../../rootComponent';
import { refresh } from '../../actions/ui/serviceDetail';
import Utils from '../../utils';

import DetailHeader from './DetailHeader';
import OwnersPanel from './OwnersPanel';
import LoadBalancersPanel from './LoadBalancersPanel';
import RecentRequestsPanel from './RecentRequestsPanel';
import UpstreamsPanel from './UpstreamsPanel';

const ServiceDetail = ({service, requestHistory, editable, navigateToRequest, redirectToServicesList}) => {
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
          serviceJson={service}
          upstreams={upstreams}
          loadBalancerService={serviceObject}
          basePath={basePath}
          editable={editable}
          afterRemoveUpstreams={navigateToRequest}
          afterReload={navigateToRequest}
          afterDelete={redirectToServicesList}
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
          loadBalancerService={serviceObject}
          upstreams={upstreams}
          afterRemoveUpstream={navigateToRequest}
          editable={editable}
        />
      </div>
    </div>
  );
};

ServiceDetail.propTypes = {
  service: PropTypes.object,
  requestHistory: PropTypes.arrayOf(PropTypes.shape({
    agentResponses: PropTypes.object,
    loadBalancerRequestId: PropTypes.string,
    loadBalancerState: PropTypes.string,
    message: PropTypes.string,
    request: PropTypes.object,
  })),
  editable: PropTypes.bool,
  navigateToRequest: PropTypes.func.isRequired,
  redirectToServicesList: PropTypes.func.isRequired,
};

const mapStateToProps = (state, ownProps) => ({
  service: Utils.maybe(state, ['api', 'service', ownProps.params.serviceId, 'data']),
  requestHistory: Utils.maybe(state, ['api', 'requestHistory', ownProps.params.serviceId, 'data']),
  editable: window.config.allowEdit,
});

const mapDispatchToProps = (dispatch, ownProps) => ({
  navigateToRequest: (response) => ownProps.router.push(`/requests/${response.data.loadBalancerRequestId}`),
  redirectToServicesList: () => ownProps.router.push('/services'),
});

export default withRouter(connect(mapStateToProps, mapDispatchToProps)(rootComponent(ServiceDetail, (props) => refresh(props.params.serviceId))));
