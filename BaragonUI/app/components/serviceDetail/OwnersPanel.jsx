import React, { PropTypes } from 'react';

import { asGroups } from './util';

const renderOwner = (owner, index) => {
  return (
    <li className="list-group-item" key={index}>
      {owner}
    </li>
  );
};

const OwnersPanel = ({owners}) => {
  return (
    <div className="col-md-6">
      <div className="panel panel-default">
        <div className="panel-heading">
          <h4>Owners</h4>
        </div>
        <div className="panel-body">
          { asGroups(owners, 2, renderOwner) }
        </div>
      </div>
    </div>
  );
};

OwnersPanel.propTypes = {
  owners: PropTypes.array,
};

export default OwnersPanel;
