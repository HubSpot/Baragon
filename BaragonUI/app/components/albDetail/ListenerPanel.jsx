import React, { PropTypes } from 'react';
import { Link } from 'react-router';

const Action = ({targetGroupArn: target, type: action, targetGroupsMap}) => {
  const targetName = targetGroupsMap[target];

  if (targetName) {
    return (
      <ul className="list-unstyled">
        <li><strong>Action:</strong> {action}</li>
        <li><strong>Target:</strong>
          <Link to={`/albs/target-groups/${targetName}`}> {targetName}</Link>
        </li>
      </ul>
    );
  } else {
    return (
      <ul className="list-unstyled">
        <li><strong>Action:</strong> {action}</li>
        <li><strong>Target:</strong> {target}</li>
      </ul>
    );
  }
};

Action.propTypes = {
  targetGroupArn: PropTypes.string,
  type: PropTypes.string,
  targetGroupsMap: PropTypes.object,
};

const Rule = ({ruleConditions, isDefault, actions, targetGroupsMap}) => {
  return (
    <div>
      {
        isDefault ?
          <h5>Default Actions:</h5> :
          <h5>Action Rules: {ruleConditions.map((condition) => condition.values[0])}</h5>
      }
      <ul className="list-group row">
        {
          actions.map((action) => (
            <li className="list-group-item col-md-4" key={action.targetGroupArn}>
              <Action {...action} targetGroupsMap={targetGroupsMap} />
            </li>
          ))
        }
      </ul>
    </div>
  );
};

Rule.propTypes = {
  ruleConditions: PropTypes.array,
  isDefault: PropTypes.bool,
  priority: PropTypes.string,
  actions: PropTypes.arrayOf(PropTypes.shape({
    targetGroupArn: PropTypes.string,
    action: PropTypes.string,
  })),
  targetGroupsMap: PropTypes.object,
};

const Listener = ({listenerArn, protocol, port, targetGroupsMap, rules}) => {
  return (
      <ul className="list-unstyled">
        <li><strong>ARN:</strong> {listenerArn}</li>
        <li><strong>Protocol:</strong> {protocol}</li>
        <li><strong>Port:</strong> {port}</li>
        <li>
          {
            rules.map((rule) => <Rule {...rule} targetGroupsMap={targetGroupsMap} key={rule.ruleArn} />)
          }
        </li>
      </ul>
  );
};

Listener.propTypes = {
  protocol: PropTypes.string,
  port: PropTypes.number,
  defaultActions: PropTypes.arrayOf(PropTypes.shape({
    target: PropTypes.string,
    action: PropTypes.string,
  })),
  listenerArn: PropTypes.string,
  targetGroupsMap: PropTypes.object,
  rules: PropTypes.array,
};

const ListenerPanel = ({listeners, targetGroupsMap, rulesMap}) => {
  return (
    <div className="col-md-12">
      <h4>Listeners</h4>
      <ul className="list-group">
        {
          listeners.map((listener) => (
            <li className="list-group-item" key={listener.listenerArn}>
              <Listener {...listener} targetGroupsMap={targetGroupsMap} rules={rulesMap[listener.listenerArn]} />
            </li>
          ))
         }
      </ul>
    </div>
  );
};

ListenerPanel.propTypes = {
  listeners: PropTypes.array,
  targetGroupsMap: PropTypes.object,
  rulesMap: PropTypes.object,
};

export default ListenerPanel;
