import React, { PropTypes } from 'react';

import Utils from '../../utils';

import RemoveBasePathButton from '../common/modalButtons/RemoveBasePathButton';

const removeButton = (editable, group, basePath, afterRemoveBasePath) => {
  if (editable) {
    return (
      <RemoveBasePathButton
        groupName={group}
        basePath={basePath}
        then={afterRemoveBasePath}
      />
  );
  } else {
    return null;
  }
};

const pathToLink = (path, defaultDomain) => {
  if (defaultDomain) {
    return <a target="_blank" href={`http://${defaultDomain}${path}`}>{path}</a>;
  } else {
    return <span>{path}</span>;
  }
};

const renderBasePath = (path, key, defaultDomain, group, editable, afterRemoveBasePath) => {
  return (
    <li className="list-group-item" key={key}>
      {removeButton(editable, group, path, afterRemoveBasePath)}
      {pathToLink(path, defaultDomain)}
    </li>
  );
};

const GroupBasePaths = ({basePaths, domain, group, editable, afterRemoveBasePath}) => {
  const pathColumns = Utils.asGroups(basePaths, 4, (path, key) => {
    return renderBasePath(path, key, domain, group, editable, afterRemoveBasePath);
  });

  return (
    <div className="col-md-12">
      <h4>Base Paths</h4>
      {pathColumns}
    </div>
  );
};

GroupBasePaths.propTypes = {
  basePaths: PropTypes.array,
  domain: PropTypes.string,
  group: PropTypes.string,
  editable: PropTypes.bool,
  afterRemoveBasePath: PropTypes.func,
};

export default GroupBasePaths;
