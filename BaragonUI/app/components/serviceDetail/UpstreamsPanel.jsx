import React, { PropTypes } from 'react';
import { Glyphicon } from 'react-bootstrap';

import { asGroups } from './util';

const renderUpstream = (editable) => ({upstream, rackId, group}) => {
  const editButton = (
    <a className="pull-left">
      <Glyphicon glyph="remove" className="inactive" />
    </a>
  );

  return (
    <h4 key={upstream}>
      {editable ? editButton : null}
      {upstream} {rackId ? `(${rackId})` : ''} {group ? `(${group})` : ''}
    </h4>
  );
};


const UpstreamsPanel = ({upstreams, editable}) => {
  return (
    <div className="col-md-12">
      <div className="panel panel-default">
        <div className="panel-heading">
          <h4>Upstreams</h4>
        </div>
        <div className="panel-body">
          { asGroups(upstreams, 2, renderUpstream(editable)) }
        </div>
      </div>
    </div>
  );
};

UpstreamsPanel.propTypes = {
  upstreams: PropTypes.array,
  editable: PropTypes.bool,
};

export default UpstreamsPanel;
