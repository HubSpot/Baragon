import React, { PropTypes } from 'react';

import UITable from '../common/table/UITable';
import Column from '../common/table/Column';

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
            (agent) => agent.agentId
          }
          sortable={true}
        />
        <Column
          label="Base URI"
          id="activeAgentBaseUri"
          key="activeAgentBaseUri"
          cellData={
            (agent) => (<a href={`${agent.baseAgentUri}/status`}>{agent.baseAgentUri}</a>)
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
