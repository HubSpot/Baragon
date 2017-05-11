import React, { PropTypes } from 'react';

import Utils from '../../utils';
import AddTrafficSourceButton from '../common/modalButtons/AddTrafficSourceButton';
import RemoveTrafficSourceButton from '../common/modalButtons/RemoveTrafficSourceButton';

const addButton = (editable, groupName, afterAddTrafficSource) => {
  if (editable) {
    return (
      <AddTrafficSourceButton
        className="pull-right"
        groupName={groupName}
        then={afterAddTrafficSource}
      />
    );
  } else {
    return null;
  }
};

const removeButton = (editable, groupName, trafficSource, afterRemoveTrafficSource) => {
  if (editable) {
    return (
      <span className="pull-right">
        <RemoveTrafficSourceButton
          groupName={groupName}
          trafficSource={trafficSource}
          then={afterRemoveTrafficSource}
        />
      </span>
    );
  } else {
    return null;
  }
};

const trafficSourceRenderer = (trafficSource, key, editable, group, afterRemoveTrafficSource) => {
  return (
    <li className="list-group-item" key={key}>
      {removeButton(editable, group, trafficSource, afterRemoveTrafficSource)}
      <ul className="list-unstyled">
        <li>Name: {trafficSource.name}</li>
        <li>Type: {trafficSource.type}</li>
      </ul>
    </li>
  );
};

const GroupTrafficSources = ({trafficSources, group, editable, afterAddTrafficSource, afterRemoveTrafficSource}) => {
  const sourceColumns = _.isEmpty(trafficSources) ?
    (<div className="empty-table-message"><p>No traffic sources for this group</p></div>)
     : Utils.asGroups(trafficSources, 4, (trafficSource, key) => {
       return trafficSourceRenderer(trafficSource, key, editable, group, afterRemoveTrafficSource);
     });

  return (
    <div className="col-md-12">
      <div className="col-md-12">
        <h4>Traffic Sources</h4>
        <span className="pull-right">{addButton(editable, group, afterAddTrafficSource)}</span>
      </div>
      <div className="col-md-12">
        {sourceColumns}
      </div>
    </div>
  );
};

GroupTrafficSources.propTypes = {
  trafficSources: PropTypes.arrayOf(PropTypes.shape({
    name: PropTypes.string,
    type: PropTypes.string,
  })),
  group: PropTypes.string,
  editable: PropTypes.bool,
  afterAddTrafficSource: PropTypes.func,
  afterRemoveTrafficSource: PropTypes.func,
};

export default GroupTrafficSources;
