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

const ServiceDetail = ({service, requestHistory, editable,
                        afterRemoveUpstreams, afterRemoveUpstream, afterReload, afterDelete}) => {
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
          afterRemoveUpstreams={afterRemoveUpstreams}
          afterReload={afterReload}
          afterDelete={afterDelete}
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
          afterRemoveUpstream={afterRemoveUpstream}
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
  afterRemoveUpstreams: PropTypes.func.isRequired,
  afterRemoveUpstream: PropTypes.func.isRequired,
  afterReload: PropTypes.func.isRequired,
  afterDelete: PropTypes.func.isRequired,
};

const mapStateToProps = (state, ownProps) => ({
  service: Utils.maybe(state, ['api', 'service', ownProps.params.serviceId, 'data']),
  requestHistory: Utils.maybe(state, ['api', 'requestHistory', ownProps.params.serviceId, 'data']),
  editable: window.config.allowEdit,
});

const mapDispatchToProps = (dispatch, ownProps) => ({
  afterRemoveUpstreams: () => refresh(ownProps.params.serviceId)(dispatch),
  afterRemoveUpstream: () => refresh(ownProps.params.serviceId)(dispatch),
  afterReload: () => refresh(ownProps.params.serviceId)(dispatch),
  afterDelete: () => ownProps.router.push('/services'),
});

export default withRouter(connect(mapStateToProps, mapDispatchToProps)(rootComponent(ServiceDetail, (props) => refresh(props.params.serviceId))));
