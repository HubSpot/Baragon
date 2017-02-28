import React, { PropTypes } from 'react';

import Utils from '../../utils';


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
          { Utils.asGroups(owners, 2, renderOwner) }
        </div>
      </div>
    </div>
  );
};

OwnersPanel.propTypes = {
  owners: PropTypes.arrayOf(PropTypes.string),
};

export default OwnersPanel;
