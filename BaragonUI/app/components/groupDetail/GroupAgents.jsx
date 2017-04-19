import React, { PropTypes } from 'react';

import UITable from '../common/table/UITable';
import Column from '../common/table/Column';
import Utils from '../../utils';

const GroupAgents = ({agents}) => {
  return (
    <div className="col-md-5">
      <h4>Active Agents</h4>
      <UITable
        data={agents}
        keyGetter={(agent) => agent.agentId}
        paginated={false}
        >
        <Column
          label="ID"
          id="activeAgentId"
          key="activeAgentId"
          cellData={
            (agent) => (<a href={`${agent.baseAgentUri}/status`}>{agent.agentId}</a>)
          }
          sortable={true}
        />
        <Column
          label="Instance ID"
          id="instanceId"
          key="instanceId"
          cellData={
            (agent) => Utils.maybe(agent, ['ec2', 'instanceId'], "")
          }
          sortable={true}
        />
      </UITable>
    </div>
  );
};

GroupAgents.propTypes = {
  agents: PropTypes.array,
};

export default GroupAgents;
