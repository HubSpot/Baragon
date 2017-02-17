import React, { PropTypes } from 'react';
import { Link } from 'react-router';

const Action = ({targetGroupArn: target, action, targetGroupsMap}) => {
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
  action: PropTypes.string,
  targetGroupsMap: PropTypes.object,
};

const Listener = ({protocol, port, defaultActions, targetGroupsMap}) => {
  return (
      <ul className="list-unstyled">
        <li><strong>Protocol:</strong> {protocol}</li>
        <li><strong>Port:</strong> {port}</li>
        <li>
          <strong>Actions:</strong>
          <ul className="list-group">
            {
              defaultActions.map((action) => (
                <li className="list-group-item" key={action.targetGroupArn}>
                  <Action {...action} targetGroupsMap={targetGroupsMap} />
                </li>
              ))
            }
          </ul>
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
};

const ListenerPanel = ({listeners, targetGroupsMap}) => {
  return (
    <div className="col-md-6">
      <h4>Listeners</h4>
      <ul className="list-group">
        {
          listeners.map((listener) => (
            <li className="list-group-item" key={listener.listenerArn}>
              <Listener {...listener} targetGroupsMap={targetGroupsMap} />
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
};

export default ListenerPanel;
