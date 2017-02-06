import React from 'react';
import { connect } from 'react-redux';
import classNames from 'classnames';
import { Link } from 'react-router';
import rootComponent from '../../rootComponent';
import { refresh } from '../../actions/ui/groups';
import Utils from '../../utils';

import UITable from '../common/table/UITable';
import Column from '../common/table/Column';
import JSONButton from '../common/JSONButton';

const Groups = (props) => {
  return (
    <div>
      <h1>Load Balancer Groups</h1>
      <UITable
        ref="table"
        data={props.groups}
        keyGetter={(group) => group.name}
      >
        <Column
          label="Name"
          id="name"
          key="name"
          cellData={
            (rowData) => (<Link to={`group/${rowData.name}`} title={`Details for ${rowData.name}`}>{rowData.name}</Link>)
          }
          sortable={true}
        />
        <Column
          label="Traffic Sources"
          id="trafficSources"
          key="trafficSources"
          cellData={
            (rowData) => Utils.maybe(rowData, ['sources'], []).length
          }
          sortable={true}
        />
        <Column
          label="Default Domain"
          id="domain"
          key="domain"
          cellData={
            (rowData) => rowData.defaultDomain
          }
          sortable={true}
        />
        <Column
          label=""
          id="actions"
          key="actions"
          className="actions-column"
          cellRender={
            (cellData, rowData) => {
              return (
                <JSONButton className="inline" object={cellData} showOverlay={true}>
                  {'{ }'}
                </JSONButton>
              );
            }
          }
        />
      </UITable>
    </div>
  );
};

Groups.propTypes = {
  groups: React.PropTypes.array
};

export default connect((state) => ({
  groups: state.api.groups.data
}))(rootComponent(Groups, refresh));
