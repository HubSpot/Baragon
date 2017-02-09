import React, { Component, PropTypes } from 'react';
import { connect } from 'react-redux';
import classNames from 'classnames';
import { Link } from 'react-router';



import GroupTitleBar from './GroupTitleBar';
import GroupDomains from './GroupDomains';
import GroupTrafficSources from './GroupTrafficSources';
import GroupBasePaths from './GroupBasePaths';
import GroupAgents from './GroupAgents';
import GroupKnownAgents from './GroupKnownAgents';

import { refresh } from '../../actions/ui/groupDetail'
import {
  FetchGroupTargetCount,
  FetchGroup
} from '../../actions/api/groups';

import rootComponent from '../../rootComponent';
import Utils from '../../utils';

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

    afterModifyTargetCount: React.PropTypes.func.isRequired,
    afterAddTrafficSource: React.PropTypes.func.isRequired,
    afterRemoveTrafficSource: React.PropTypes.func.isRequired,
    afterRemoveBasePath: React.PropTypes.func.isRequired,
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
            afterModifyTargetCount={this.props.afterModifyTargetCount}
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
            afterAddTrafficSource={this.props.afterAddTrafficSource}
            afterRemoveTrafficSource={this.props.afterRemoveTrafficSource}
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
            afterRemoveKnownAgent={this.props.afterRemoveKnownAgent}
          />
        </div>
        <div className="row">
          <GroupBasePaths
            basePaths={this.props.basePaths}
            domain={this.props.domain}
            group={this.props.group}
            editable={this.props.editable}
            afterRemoveBasePath={this.props.afterRemoveBasePath}
          />
        </div>
      </div>
    )
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
  editable: true
});

const mapDispatchToProps = (dispatch, ownProps) => ({
  afterModifyTargetCount: (data) => dispatch(FetchGroupTargetCount.trigger(ownProps.params.groupId)),
  afterAddTrafficSource: (data) => dispatch(FetchGroup.trigger(ownProps.params.groupId)),
  afterRemoveTrafficSource: (data) => dispatch(FetchGroup.trigger(ownProps.params.groupId)),
  afterRemoveKnownAgent: (data) => dispatch(FetchGroupKnownAgents.trigger(ownProps.params.groupId))
                                     .then(FetchGroupAgents.trigger(ownProps.params.groupId)),
  afterRemoveBasePath: (data) => dispatch(FetchGroupBasePaths.trigger(ownProps.params.groupId)),
});

export default connect(mapStateToProps, mapDispatchToProps)(rootComponent(GroupDetail, (props) => refresh(props.params.groupId)));
