import React, { PropTypes } from 'react';

import ButtonContainer from './ButtonContainer';

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
