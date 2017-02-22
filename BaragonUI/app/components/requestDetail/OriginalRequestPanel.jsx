import React from 'react';

const OriginalRequestPanel = ({request}) => {
  return (
    <div className="col-md-12">
      <div className="panel panel-default">
        <div className="panel-heading">
          <h4>Original Request</h4>
        </div>
        <div className="panel-body">
          <pre>
            { JSON.stringify(request, undefined, 4) }
          </pre>
        </div>
      </div>
    </div>
  );
};

OriginalRequestPanel.propTypes = {
  request: React.PropTypes.object,
};

export default OriginalRequestPanel;
