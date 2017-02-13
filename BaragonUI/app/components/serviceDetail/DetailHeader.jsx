import React, { PropTypes } from 'react';

import JSONButton from '../common/JSONButton';
import RemoveUpstreamsButton from '../common/modalButtons/RemoveUpstreamsButton';
import ReloadServiceButton from '../common/modalButtons/ReloadServiceButton';
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
  // TODO be more specific
  upstreams: PropTypes.array.isRequired,
  afterRemoveUpstreams: PropTypes.func,
  afterReload: PropTypes.func,
  afterDelete: PropTypes.func,
};

const DetailHeader = ({id, basePath, editable, serviceJson, upstreams,
                      afterRemoveUpstreams, afterReload, afterDelete}) => {
  return (
    <div>
      <div className="col-md-5">
        <h3>{ id }</h3>
      </div>
      <div className="col-md-2">
        <h3>{ basePath }</h3>
      </div>
      <ButtonContainer
        editable={editable}
        serviceJson={serviceJson}
        upstreams={upstreams}
        afterRemoveUpstreams={afterRemoveUpstreams}
        afterReload={afterReload}
        afterDelete={afterDelete}
      />
    </div>
  );
};

DetailHeader.propTypes = {
  id: PropTypes.string,
  basePath: PropTypes.string,
  editable: PropTypes.bool,
  serviceJson: PropTypes.object,
  upstreams: PropTypes.array,
  afterRemoveUpstreams: PropTypes.func,
  afterReload: PropTypes.func,
  afterDelete: PropTypes.func,
};

export default DetailHeader;
