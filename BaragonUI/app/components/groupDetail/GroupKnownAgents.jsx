import React, { PropTypes } from 'react';

import UITable from '../common/table/UITable';
import Column from '../common/table/Column';
import Utils from '../../utils';

import RemoveKnownAgentButton from '../common/modalButtons/RemoveKnownAgentButton';

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
          then={removeKnownAgent}
        />)
    }
    sortable={false}
  />
);

const uriColumn = (
  <Column
    label="Base URI"
    id="activeAgentBaseUri"
    key="activeAgentBaseUri"
    cellData={
      (agent) => (<a href={`${agent.baseAgentUri}/status`}>{agent.agentId}</a>)
    }
    sortable={true}
  />
);

const instanceIdColumn = (
  <Column
    label="Instance ID"
    id="instanceId"
    key="instanceId"
    cellData={
      (agent) => Utils.maybe(agent, ['ec2', 'instanceId'], "")
    }
    sortable={true}
  />
);

const lastSeenColumn = (
  <Column
    label="Last Seen"
    id="lastSeen"
    key="lastSeen"
    cellData={
      (agent) => (Utils.timestampFromNow(agent.lastSeenAt))
    }
    sortable={true}
  />
);


const GroupKnownAgents = ({knownAgents, editable, group, afterRemoveKnownAgent}) => {
  if (editable) {
    return (
      <div className="col-md-7">
        <h4>Known Agents</h4>
        <UITable
          data={knownAgents}
          keyGetter={(agent) => agent.agentId}
          paginated={false}
          >
          {uriColumn}
          {instanceIdColumn}
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
          { uriColumn }
          { instanceIdColumn }
          { lastSeenColumn }
        </UITable>
      </div>
    );
  }
};

GroupKnownAgents.propTypes = {
  knownAgents: PropTypes.array,
  editable: PropTypes.bool,
  group: PropTypes.string,
  afterRemoveKnownAgent: PropTypes.func,
};

export default GroupKnownAgents;
