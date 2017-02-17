import React, { PropTypes } from 'react';
import { Link } from 'react-router';

import UITable from '../common/table/UITable';
import Column from '../common/table/Column';
import JSONButton from '../common/JSONButton';

const targetGroupName = (targetGroup) => {
  const name = targetGroup.targetGroupName;
  return <Link to={`/albs/target-groups/${name}`}>{name}</Link>;
};

const targetGroupJson = (targetGroup) => {
  return (
    <JSONButton object={targetGroup}>
      <span>{'{ }'}</span>
    </JSONButton>
  );
};

const TargetGroupsTable = ({targetGroups}) => {
  return (
    <div>
      <UITable
        data={targetGroups}
        keyGetter={(group) => /* ?? */ group.targetGroupName}
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
    </div>
  );
};

TargetGroupsTable.propTypes = {
  targetGroups: PropTypes.array,
};


export default TargetGroupsTable;
