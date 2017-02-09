import React from 'react';

import RemoveBasePathButton from '../common/modalButtons/RemoveBasePathButton';

import { asGroups } from './util';


export default function GroupBasePaths({basePaths, domain, group, editable, afterRemoveBasePath}) {
  const pathColumns = asGroups(basePaths, (path, key) => {
    return renderBasePath(path, key, domain, group, editable, afterRemoveBasePath);
  });

  return (
    <div className="col-md-12">
      <h4>Base Paths</h4>
      {pathColumns}
    </div>
  );
};

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
}

const pathToLink = (path, defaultDomain) => {
  if (defaultDomain) {
    return <a target="_blank" href={`http://${defaultDomain}${path}`}>{path}</a>;
  } else {
    return <span>{path}</span>;
  }
}

const renderBasePath = (path, key, defaultDomain, group, editable, afterRemoveBasePath) => {
  return (
    <li className="list-group-item" key={key}>
      {removeButton(editable, group, path, afterRemoveBasePath)}
      {pathToLink(path, defaultDomain)}
    </li>
  );
}
