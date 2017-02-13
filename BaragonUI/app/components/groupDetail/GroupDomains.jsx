import React, { PropTypes } from 'react';
import { asGroups } from './util';

const domainElement = (domain, key, defaultDomain) => {
  if (domain === defaultDomain) {
    return (
      <li className="list-group-item" key={key}>
        {defaultDomain}
        <span className="label label-info pull-right">Default</span>
      </li>
    );
  } else {
    return <li className="list-group-item" key={key}>{domain}</li>;
  }
};

const GroupDomains = ({domains, defaultDomain}) => {
  if (!domains) {
    return null;
  }

  const domainColumns = asGroups(domains, (domain, key) => {
    return domainElement(domain, key, defaultDomain);
  });

  return (
    <div className="col-md-12">
      <h4>Domains Served</h4>
      {domainColumns}
    </div>
  );
};

GroupDomains.propTypes = {
  domains: PropTypes.array,
  defaultDomain: PropTypes.string,
};

export default GroupDomains;
