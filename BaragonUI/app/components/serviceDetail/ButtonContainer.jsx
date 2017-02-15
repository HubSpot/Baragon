import React, { PropTypes } from 'react';

import JSONButton from '../common/JSONButton';
import ReloadServiceButton from '../common/modalButtons/ReloadServiceButton';
import RemoveUpstreamsButton from '../common/modalButtons/RemoveUpstreamsButton';
import DeleteServiceButton from '../common/modalButtons/DeleteServiceButton';

const showJSONButton = (serviceJson) => {
  return (
    <JSONButton object={serviceJson} overlay={true}>
      <span className="btn btn-default">JSON</span>
    </JSONButton>
  );
};

const ButtonContainer = ({editable, serviceJson, upstreams,
                          afterRemoveUpstreams, afterReload, afterDelete}) => {
  if (!editable) {
    return (
      <div className="col-md-5 button-container">
        {showJSONButton(serviceJson)}
      </div>
    );
  }

  return (
    <div className="col-md-5 button-container">
      {showJSONButton(serviceJson)}
      <ReloadServiceButton
        serviceId={serviceJson.service.serviceId}
        then={afterReload}
      >
        <span className="btn btn-primary">Reload Configs</span>
      </ReloadServiceButton>
      <RemoveUpstreamsButton
        loadBalancerService={serviceJson.service}
        upstreams={upstreams}
        afterRemoveUpstreams={afterRemoveUpstreams}
      />
      <DeleteServiceButton
        serviceId={serviceJson.service.serviceId}
        then={afterDelete}
      >
        <span className="btn btn-danger">Delete</span>
      </DeleteServiceButton>
    </div>
  );
};

ButtonContainer.propTypes = {
  editable: PropTypes.bool.isRequired,
  serviceJson: PropTypes.object.isRequired,
  upstreams: PropTypes.arrayOf(PropTypes.shape({
    group: PropTypes.string,
    rackId: PropTypes.string,
    requestId: PropTypes.string.isRequired,
    upstream: PropTypes.string.isRequired,
  })).isRequired,
  afterRemoveUpstreams: PropTypes.func,
  afterReload: PropTypes.func,
  afterDelete: PropTypes.func,
};

export default ButtonContainer;
