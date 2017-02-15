import React, { PropTypes } from 'react';
import { Link } from 'react-router';

import UITable from '../common/table/UITable';
import Column from '../common/table/Column';
import Utils from '../../utils.es6';

const loadBalancerName = (loadBalancer) => {
  const name = loadBalancer.loadBalancerName;
  return <Link to={`/albs/load-balancers/${name}`}>{name}</Link>;
};

const LoadBalancersTable = ({loadBalancers}) => {
  return (
    <div className="col-md-12">
      <UITable
        data={loadBalancers}
        keyGetter={(loadBalancer) => /* ?? */ loadBalancer.loadBalancerName}
        paginated={true}
        rowChunkSize={5}
      >
        <Column
          label="Name"
          id="loadBalancerName"
          cellData={loadBalancerName}
        />
        <Column
          label="ARN"
          id="loadBalancerArn"
          cellData={(loadBalancer) => loadBalancer.loadBalancerArn}
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
      </UITable>
    </div>
  );
};

LoadBalancersTable.propTypes = {
  loadBalancers: PropTypes.array,
};


export default LoadBalancersTable;
