import React, { PropTypes } from 'react';
import { Link } from 'react-router';

import UITable from '../common/table/UITable';
import Column from '../common/table/Column';

const targetGroupName = (targetGroup) => {
  const name = targetGroup.targetGroupName;
  return <Link to={`/albs/target-groups/${name}`}>{name}</Link>;
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
          label="ARN"
          id="targetGroupArn"
          cellData={(group) => group.targetGroupArn}
        />
        <Column
          label="VPC ID"
          id="vpcId"
          cellData={(group) => group.vpcId}
        />
      </UITable>
    </div>
  );
};

TargetGroupsTable.propTypes = {
  targetGroups: PropTypes.array,
};


export default TargetGroupsTable;
