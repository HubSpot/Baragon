import React, { PropTypes } from 'react';
import { Link } from 'react-router';
import fuzzy from 'fuzzy';

import UITable from '../common/table/UITable';
import Column from '../common/table/Column';
import JSONButton from '../common/JSONButton';
import Utils from '../../utils';

const loadBalancerName = (loadBalancer) => {
  const name = loadBalancer.loadBalancerName;
  return <Link to={`/albs/load-balancers/${name}`}>{name}</Link>;
};

const loadBalancerSymbol = (loadBalancer) => {
  const state = loadBalancer.state.code;

  if (state === 'active') {
    return <span className="glyphicon glyphicon-ok-circle active" />;
  } else if (state === 'provisioning') {
    return <span className="glyphicon glyphicon-time" />;
  } else if (state === 'failed') {
    return <span className="glyphicon glyphicon-ban-circle inactive" />;
  } else {
    return <span className="glyphicon glyphicon-question-sign" />;
  }
};

const loadBalancerJson = (loadBalancer) => {
  return (
    <JSONButton object={loadBalancer}>
      <span className="pull-right">{'{ }'}</span>
    </JSONButton>
  );
};

const LoadBalancersTable = ({loadBalancers, filter}) => {
  let tableContent = [];
  if (filter === '') {
    tableContent = loadBalancers;
  } else {
    const fuzzyObjects = fuzzy.filter(filter, loadBalancers, {
      extract: (alb) => alb.loadBalancerName,
      returnMatchInfo: true
    });
    tableContent = Utils.fuzzyFilter(filter, fuzzyObjects, (alb) => alb.loadBalancerName);
  }

  return (
    <div>
      <UITable
        data={tableContent}
        keyGetter={(loadBalancer) => loadBalancer.loadBalancerName}
        paginated={true}
        rowChunkSize={5}
      >
        <Column
          label=""
          id="state"
          cellData={loadBalancerSymbol}
        />
        <Column
          label="Name"
          id="loadBalancerName"
          cellData={loadBalancerName}
        />
        <Column
          label="DNS Name"
          id="dnsname"
          cellData={(loadBalancer) => loadBalancer.dnsname}
        />
        <Column
          label="VPC ID"
          id="vpcId"
          cellData={(loadBalancer) => loadBalancer.vpcId}
        />
        <Column
          label="Created"
          id="createdAgo"
          cellData={(loadBalancer) => Utils.timestampFromNow(loadBalancer.createdTime)}
        />
        <Column
          label=""
          id="json"
          cellData={loadBalancerJson}
        />
      </UITable>
    </div>
  );
};

LoadBalancersTable.propTypes = {
  loadBalancers: PropTypes.array,
  filter: PropTypes.string,
};


export default LoadBalancersTable;
