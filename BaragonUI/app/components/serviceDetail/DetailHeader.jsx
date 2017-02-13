import React, { PropTypes } from 'react';

import JSONButton from '../common/JSONButton';

const showJSONButton = (object) => {
  return (
    <JSONButton object={object} overlay={true}>
      <span className="btn btn-default">JSON</span>
    </JSONButton>
  );
};

const ButtonContainer = ({editable, object}) => {
  if (!editable) {
    return (
      <div className="col-md-5 button-container">
        {showJSONButton(object)}
      </div>
    );
  }

  return (
    <div className="col-md-5 button-container">
      {showJSONButton(object)}
      <span className="btn btn-primary">Reload Configs</span>
      <span className="btn btn-warning">Remove Upstream</span>
      <span className="btn btn-danger">Delete</span>
    </div>
  );
};

ButtonContainer.propTypes = {
  editable: PropTypes.bool,
  object: PropTypes.object,
};

const DetailHeader = ({id, basePath, editable, object}) => {
  return (
    <div>
      <div className="col-md-5">
        <h3>{ id }</h3>
      </div>
      <div className="col-md-2">
        <h3>{ basePath }</h3>
      </div>
      <ButtonContainer editable={editable} object={object} />
    </div>
  );
};

DetailHeader.propTypes = {
  id: PropTypes.string,
  basePath: PropTypes.string,
  editable: PropTypes.bool,
  object: PropTypes.object,
};

export default DetailHeader;
