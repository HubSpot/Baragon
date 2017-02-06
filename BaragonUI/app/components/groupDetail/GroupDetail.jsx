import React, { Component, PropTypes } from 'react';
import { connect } from 'react-redux';
import classNames from 'classnames';
import { Link } from 'react-router';
import rootComponent from '../../rootComponent';
import { refresh } from '../../actions/ui/groupDetail';
import Utils from '../../utils';

class GroupDetail extends Component {
  render() {
    return <h1>Group Detail</h1>
  }

  static propTypes = {
    params: PropTypes.object.isRequired,
    group: React.PropTypes.object,
    basePaths: React.PropTypes.array,
    targetCount: React.PropTypes.number,
    agents: React.PropTypes.array,
    knownAgents: React.PropTypes.array
  };
}

export default connect((state, ownProps) => ({
  group: Utils.maybe(state, ['api', 'group', ownProps.params.groupId, 'data']),
  basePaths: Utils.maybe(state, ['api', 'basePaths', ownProps.params.groupId, 'data']),
  targetCount: Utils.maybe(state, ['api', 'targetCount', ownProps.params.groupId, 'data']),
  agents: Utils.maybe(state, ['api', 'agents', ownProps.params.groupId, 'data']),
  knownAgents: Utils.maybe(state, ['api', 'knownAgents', ownProps.params.groupId, 'data']),
}))(rootComponent(GroupDetail, (props) => refresh(props.params.groupId)));