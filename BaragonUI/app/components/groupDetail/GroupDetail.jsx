import React, { Component, PropTypes } from 'react';
import { connect } from 'react-redux';

import {
  FetchGroupTargetCount,
  FetchGroup,
  FetchGroupAgents,
  FetchGroupKnownAgents,
  FetchGroupBasePaths,
} from '../../actions/api/groups';
import { refresh } from '../../actions/ui/groupDetail';

import rootComponent from '../../rootComponent';
import Utils from '../../utils';

import GroupTitleBar from './GroupTitleBar';
import GroupDomains from './GroupDomains';
import GroupTrafficSources from './GroupTrafficSources';
import GroupBasePaths from './GroupBasePaths';
import GroupAgents from './GroupAgents';
import GroupKnownAgents from './GroupKnownAgents';


class GroupDetail extends Component {
  static propTypes = {
    domain: React.PropTypes.string,
    domainsServed: React.PropTypes.array,
    params: PropTypes.object.isRequired,
    group: React.PropTypes.string,
    basePaths: React.PropTypes.array,
    targetCount: React.PropTypes.number,
    agents: React.PropTypes.array,
    trafficSources: React.PropTypes.array,
    knownAgents: React.PropTypes.array,
    editable: React.PropTypes.bool,

    refreshTargetCount: PropTypes.func.isRequired,
    refreshBasePaths: PropTypes.func.isRequired,
    refreshAgents: PropTypes.func.isRequired,
    refreshGroup: PropTypes.func.isRequired,
  };

  render() {
    return (
      <div>
        <div className="row">
          <GroupTitleBar
            group={this.props.group}
            domain={this.props.domain}
            targetCount={this.props.targetCount}
            editable={this.props.editable}
            afterModifyTargetCount={this.props.refreshTargetCount}
          />
        </div>
        <div className="row">
           <GroupDomains
             domains={this.props.domainsServed}
             defaultDomain={this.props.domain}
           />
        </div>
        <div className="row">
          <GroupTrafficSources
            trafficSources={this.props.trafficSources}
            group={this.props.group}
            editable={this.props.editable}
            afterAddTrafficSource={this.props.refreshGroup}
            afterRemoveTrafficSource={this.props.refreshGroup}
          />
        </div>
        <div className="row">
          <GroupAgents
            agents={this.props.agents}
          />
          <GroupKnownAgents
            knownAgents={this.props.knownAgents}
            group={this.props.group}
            editable={this.props.editable}
            afterRemoveKnownAgent={this.props.refreshAgents}
          />
        </div>
        <div className="row">
          <GroupBasePaths
            basePaths={this.props.basePaths}
            domain={this.props.domain}
            group={this.props.group}
            editable={this.props.editable}
            afterRemoveBasePath={this.props.refreshBasePaths}
          />
        </div>
      </div>
    );
  }
}

const mapStateToProps = (state, ownProps) => ({
  domain: Utils.maybe(state, ['api', 'group', ownProps.params.groupId, 'data', 'domain']),
  domainsServed: Utils.maybe(state, ['api', 'group', ownProps.params.groupId, 'data', 'domains']),
  group: ownProps.params.groupId,
  basePaths: Utils.maybe(state, ['api', 'basePaths', ownProps.params.groupId, 'data']),
  targetCount: Utils.maybe(state, ['api', 'targetCount', ownProps.params.groupId, 'data']),
  agents: Utils.maybe(state, ['api', 'agents', ownProps.params.groupId, 'data']),
  knownAgents: Utils.maybe(state, ['api', 'knownAgents', ownProps.params.groupId, 'data']),
  trafficSources: Utils.maybe(state, ['api', 'group', ownProps.params.groupId, 'data', 'trafficSources']),
  editable: window.config.allowEdit,
});

const mapDispatchToProps = (dispatch, ownProps) => ({
  refreshTargetCount: () => dispatch(FetchGroupTargetCount.trigger(ownProps.params.groupId)),
  refreshAgents: () => Promise.all([
    dispatch(FetchGroupKnownAgents.trigger(ownProps.params.groupId)),
    dispatch(FetchGroupAgents.trigger(ownProps.params.groupId))
  ]),
  refreshBasePaths: () => dispatch(FetchGroupBasePaths.trigger(ownProps.params.groupId)),
  refreshGroup: () => dispatch(FetchGroup.trigger(ownProps.params.groupId)),
});

export default connect(mapStateToProps, mapDispatchToProps)(rootComponent(GroupDetail, (props) => refresh(props.params.groupId)));
