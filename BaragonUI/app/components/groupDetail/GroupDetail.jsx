import React, { Component, PropTypes } from 'react';
import { connect } from 'react-redux';
import classNames from 'classnames';
import { Link } from 'react-router';

import UITable from '../common/table/UITable';
import Column from '../common/table/Column';
import AddTrafficSourceButton from '../common/modalButtons/AddTrafficSourceButton';
import RemoveTrafficSourceButton from '../common/modalButtons/RemoveTrafficSourceButton';

import { refresh } from '../../actions/ui/groupDetail'

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
    knownAgents: React.PropTypes.array,
    editable: React.PropTypes.bool
  };

  render() {
    return (
      <div>
        <div className="row">
          {this.titleBar(this.props.group, this.props.domain,
             this.props.targetCount, this.props.editable)}
         </div>
         <div className="row">
          {this.domains(this.props.domainsServed, this.props.domain)}
        </div>
        <div className="row">
          {this.trafficSources(this.props.trafficSources, this.props.group, this.props.editable)}
        </div>
        <div className="row">
          {this.activeAgents(this.props.agents)}
          {this.knownAgents(this.props.knownAgents, this.props.editable)}
        </div>
        <div className="row">
          {this.basePaths(this.props.basePaths, this.props.domain, this.props.editable)}
        </div>
      </div>
    )
  }

  titleBar(groupName, domainName, targetCount, editable) {
    const modifyTarget = () => {
      if (editable) {
        return <a href="#"><span className="glyphicon glyphicon-edit"></span></a>
      }
    }

    return (
      <div>
        <div className="col-md-4">
          <h3>Group: {groupName}</h3>
        </div>
        <div className="col-md-5">
          <h3>Default Domain: <a href={`http://${domainName}`}>{domainName}</a></h3>
        </div>
        <div className="col-md-3">
          <h3>Target Count: {targetCount} {modifyTarget()}</h3>
        </div>
      </div>
    )
  }

  domains(domainsServed, defaultDomain) {
    if (!domainsServed || !domainsServed.length) {
      return null;
    }

    const domainElement = (domain) => {
      if (domain === defaultDomain) {
        return (
          <li className="list-group-item">
            {defaultDomain}
            <span className="label label-info pull-right">Default</span>
          </li>
        );
      } else {
        return <li className="list-group-item">{domain}</li>;
      }
    }

    const domains = this.asGroups(domainsServed, domainElement);

    return (
      <div className="col-md-12">
        <h4>Domains Served</h4>
        {domains}
      </div>
    );
  }

  trafficSources(trafficSources, groupName, editable) {
    const addButton = () => {
      if (editable) {
        return <AddTrafficSourceButton groupName={groupName} />
      } else {
        return null;
      }
    }
    const removeButton = (trafficSource) => {
      if (editable) {
        return (
          <span className="pull-right">
            <RemoveTrafficSourceButton groupName={groupName} trafficSource={trafficSource} />
          </span>
        );
      } else {
        return null;
      }
    }

    const trafficSourceRenderer = (trafficSource) => {
      return (
        <li className="list-group-item">
          {removeButton(trafficSource)}
          <ul className="list-unstyled">
            <li>Name: {trafficSource.name}</li>
            <li>Type: {trafficSource.type}</li>
          </ul>
        </li>
      );
    }

    const sources = this.asGroups(trafficSources, trafficSourceRenderer);

    return (
      <div className="col-md-12">
        <h4>Traffic Sources</h4>
        {sources}
        {addButton()}
      </div>
    );
  }

  activeAgents(agents) {
    return (
      <div className="col-md-5">
        <h4>Active Agents</h4>
        <UITable
          ref="activeAgents"
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
  }

  knownAgents(knownAgents, editable) {
    const removeColumn = () => {
      if (editable) {
        return (
          <Column
            label=""
            id="removeAgent"
            key="removeAgent"
            cellData={
              (agent) => (
                <a title="Remove Known Agent">
                  <span className="glyphicon glyphicon-remove"></span>
                </a>
              )
            }
            sortable={false}
            />
        );
      } else {
        return null;
      }
    }

    return (
      <div className="col-md-7">
        <h4>Known Agents</h4>
        <UITable
          ref="activeAgents"
          data={knownAgents}
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
          {removeColumn()}
        </UITable>
      </div>
    );
  }

  basePaths(basePaths, defaultDomain, editable) {
    const removeButton = () => {
      if (editable) {
        return (
          // TODO actions
          <a title="Remove Base Path Lock">
            <span className="glyphicon glyphicon-remove"></span>
          </a>
        );
      } else {
        return null;
      }
    }
    const pathToLink = (path) => {
      if (defaultDomain) {
        return <a target="_blank" href={`http://${defaultDomain}${path}`}>{path}</a>;
      } else {
        return <span>{path}</span>;
      }
    }
    const renderBasePath = (element) => {
      return (
        <li className="list-group-item">
          {removeButton()}
          {pathToLink(element)}
        </li>
      );
    }

    const paths = this.asGroups(basePaths, renderBasePath);

    return (
      <div className="col-md-12">
        <h4>Base Paths</h4>
        {paths}
      </div>
    );
  }

  // Map a flat array into an array of arrays of size `size`, where the last
  // array is as long as possible
  // [1, 2, 3, 4, 5, 6, 7], 2 -> [[1, 2], [3, 4], [5, 6], [7]]
  // Precondition: size != 0
  chunk(arr, size) {
    return _.chain(arr)
      .groupBy((elem, index) => Math.floor(index / size))
      .toArray()
      .value();
  }

  asGroups(arr, itemRenderer) {
    const rowRenderer = (row) => {
      return (
        <div className="col-md-3">
          <ul className="list-group">
            { row.map( itemRenderer ) }
          </ul>
        </div>
      );
    }

    return this.chunk(arr, arr.length / 4).map(rowRenderer);
  }
}

export default connect((state, ownProps) => ({
  domain: Utils.maybe(state, ['api', 'group', ownProps.params.groupId, 'data', 'domain']),
  domainsServed: Utils.maybe(state, ['api', 'group', ownProps.params.groupId, 'data', 'domains']),
  group: ownProps.params.groupId,
  basePaths: Utils.maybe(state, ['api', 'basePaths', ownProps.params.groupId, 'data']),
  targetCount: Utils.maybe(state, ['api', 'targetCount', ownProps.params.groupId, 'data']),
  agents: Utils.maybe(state, ['api', 'agents', ownProps.params.groupId, 'data']),
  knownAgents: Utils.maybe(state, ['api', 'knownAgents', ownProps.params.groupId, 'data']),
  trafficSources: Utils.maybe(state, ['api', 'group', ownProps.params.groupId, 'data', 'trafficSources']),
  editable: true // TODO,
}))(rootComponent(GroupDetail, (props) => refresh(props.params.groupId)));
