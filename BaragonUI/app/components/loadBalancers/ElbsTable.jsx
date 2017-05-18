import React, { PropTypes } from 'react';
import { Link } from 'react-router';

import fuzzy from 'fuzzy';

import UITable from '../common/table/UITable';
import Column from '../common/table/Column';
import JSONButton from '../common/JSONButton';
import Utils from '../../utils';

const elbIcon = (elb) => {
  const instances = elb.instances;
  if (! instances || instances.length === 0) {
    return (
      <span
        title="No registered instances"
        className="glyphicon glyphicon-ban-circle inactive"
      >
      </span>
    );
  } else if (instances.length === 1) {
    return (
      <span
        title="1 registered instance"
        className="glyphicon glyphicon-ok-circle active"
      >
      </span>
    );
  } else {
    return (
      <span
        title={`${instances.length} registered instances`}
        className="glyphicon glyphicon-ok-circle active"
      >
      </span>
    );
  }
};

const loadBalancerName = (elb) => {
  const name = elb.loadBalancerName;
  return (
    <Link to={`/elbs/${name}`}>
      {name}
    </Link>
  );
};

const instanceCount = (elb) => {
  if (! elb.instances || elb.instances.length === 0) {
    return 0;
  } else {
    return elb.instances.length;
  }
};

const jsonButton = (elb) => {
  return (
    <JSONButton object={elb}>
      <span className="pull-right">{'{ }'}</span>
    </JSONButton>
  );
};

const ElbsTable = ({elbs, filter, rowCount = 15}) => {
  let tableContent = [];
  if (filter === '') {
    tableContent = elbs;
  } else {
    const fuzzyObjects = fuzzy.filter(filter, elbs, {
      extract: (elb) => elb.loadBalancerName,
      returnMatchInfo: true
    });
    tableContent = Utils.fuzzyFilter(filter, fuzzyObjects, (elb) => elb.loadBalancerName);
  }

  return (
    <UITable
      data={tableContent}
      keyGetter={(elb) => elb.dnsname}
      paginated={true}
      rowChunkSize={rowCount}
    >
      <Column
        label=""
        id="elbIcon"
        cellData={elbIcon}
      />
      <Column
        label="Name"
        id="loadBalancerName"
        cellData={loadBalancerName}
      />
      <Column
        label="DNS Name"
        id="dnsname"
        cellData={(elb) => elb.dnsname}
      />
      <Column
        label="VPC ID"
        id="vpcid"
        cellData={(elb) => elb.vpcid}
      />
      <Column
        label="Instance Count"
        id="instanceCount"
        cellData={instanceCount}
      />
      <Column
        label=""
        id="viewJson"
        cellData={jsonButton}
      />
    </UITable>
  );
};

ElbsTable.propTypes = {
  elbs: PropTypes.arrayOf(PropTypes.shape({
    loadBalancerName: PropTypes.string.isRequired,
    instances: PropTypes.arrayOf(PropTypes.shape({
      instanceId: PropTypes.string.isRequired,
    })),
    dnsname: PropTypes.string.isRequired,
    vpcid: PropTypes.string.isRequired,
  })),
  filter: PropTypes.string,
  rowCount: PropTypes.number,
};

export default ElbsTable;
