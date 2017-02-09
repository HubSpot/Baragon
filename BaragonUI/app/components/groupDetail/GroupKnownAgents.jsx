import React from 'react';

import UITable from '../common/table/UITable';
import Column from '../common/table/Column';

import RemoveKnownAgentButton from '../common/modalButtons/RemoveKnownAgentButton';

export default function GroupKnownAgents({knownAgents, editable, group, afterRemoveKnownAgent}) {
  if (editable) {
    return (
      <div className="col-md-7">
        <h4>Known Agents</h4>
        <UITable
          data={knownAgents}
          keyGetter={(agent) => agent.agentId}
          paginated={false}
          >
          {agentColumn}
          {uriColumn}
          {lastSeenColumn}
          {removeColumn(group, afterRemoveKnownAgent)}
        </UITable>
      </div>
    );
  } else {
    return (
      <div className="col-md-7">
        <h4>Known Agents</h4>
        <UITable
          data={knownAgents}
          keyGetter={(agent) => agent.agentId}
          paginated={false}
          >
          {agentColumn}
          {uriColumn}
          {lastSeenColumn}
        </UITable>
      </div>
    );
  }
}

const removeColumn = (groupName, removeKnownAgent) => (
  <Column
    label=""
    id="removeAgent"
    key="removeAgent"
    cellData={
      (agent) => (
        <RemoveKnownAgentButton
          groupName={groupName}
          agentId={agent.agentId}
          then={RemoveKnownAgentButton}
        />)
    }
    sortable={false}
    />
);

const agentColumn = (
  <Column
    label="ID"
    id="activeAgentId"
    key="activeAgentId"
    cellData={
      (agent) => agent.agentId
    }
    sortable={true}
  />
);

const uriColumn = (
  <Column
    label="Base URI"
    id="activeAgentBaseUri"
    key="activeAgentBaseUri"
    cellData={
      (agent) => (<a href={`${agent.baseAgentUri}/status`}>{agent.baseAgentUri}</a>)
    }
    sortable={true}
  />
);

const lastSeenColumn = (
  <Column
    lable="Last Seen"
    id="lastSeen"
    key="lastSeen"
    cellData={
      // TODO timestamp from now
      (agent) => (agent.lastSeenAt)
    }
    sortable={true}
  />
);
