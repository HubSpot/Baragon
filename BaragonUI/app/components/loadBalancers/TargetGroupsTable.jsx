import React, { PropTypes } from 'react';
import { Link } from 'react-router';
import fuzzy from 'fuzzy';

import UITable from '../common/table/UITable';
import Column from '../common/table/Column';
import JSONButton from '../common/JSONButton';
import Utils from '../../utils';

const targetGroupName = (targetGroup) => {
  const name = targetGroup.targetGroupName;
  return <Link to={`/albs/target-groups/${name}`}>{name}</Link>;
};

const targetGroupJson = (targetGroup) => {
  return (
    <JSONButton object={targetGroup}>
      <span className="pull-right">{'{ }'}</span>
    </JSONButton>
  );
};

const TargetGroupsTable = ({targetGroups, filter}) => {
  let tableContent = [];
  if (filter === '') {
    tableContent = targetGroups;
  } else {
    const fuzzyObjects = fuzzy.filter(filter, targetGroups, {
      extract: (targetGroup) => targetGroup.targetGroupName,
      returnMatchInfo: true
    });
    tableContent = Utils.fuzzyFilter(filter, fuzzyObjects, (targetGroup) => targetGroup.targetGroupName);
  }

  return (
    <UITable
      data={tableContent}
      keyGetter={(group) => group.targetGroupArn}
      paginated={true}
      rowChunkSize={5}
    >
      <Column
        label="Name"
        id="targetGroupName"
        cellData={targetGroupName}
      />
      <Column
        label="VPC ID"
        id="vpcId"
        cellData={(group) => group.vpcId}
      />
      <Column
        label="Protocol"
        id="protocol"
        cellData={(group) => group.protocol}
      />
      <Column
        label="Port"
        id="port"
        cellData={(group) => group.port}
      />
      <Column
        label=""
        id="json"
        cellData={targetGroupJson}
      />
    </UITable>
  );
};

TargetGroupsTable.propTypes = {
  targetGroups: PropTypes.array,
  filter: PropTypes.string,
};


export default TargetGroupsTable;
