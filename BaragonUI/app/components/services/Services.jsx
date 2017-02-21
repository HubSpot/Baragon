import React, { Component, PropTypes } from 'react';
import { connect } from 'react-redux';
import { Link } from 'react-router';
import rootComponent from '../../rootComponent';
import { refresh } from '../../actions/ui/services';

import UITable from '../common/table/UITable';
import Column from '../common/table/Column';

import JSONButton from '../common/JSONButton';

import ReloadServiceButton from '../common/modalButtons/ReloadServiceButton';
import DeleteServiceButton from '../common/modalButtons/DeleteServiceButton';

const ButtonsColumn = (
  <Column
    label=""
    id="actions"
    key="actions"
    className="actions-column"
    cellRender={
      (cellData, rowData) => {
        const deleteService = (
          <DeleteServiceButton serviceId={rowData.service.serviceId} />
        );

        const reloadService = (
          <ReloadServiceButton serviceId={rowData.service.serviceId} />
        );

        return (
          <div className="hidden-xs">
            {deleteService}
            {reloadService}
            <JSONButton className="inline" object={cellData} showOverlay={true}>
              {'{ }'}
            </JSONButton>
          </div>
        );
      }
    }
  />
);

const IdColumn = (
  <Column
    label="ID"
    id="serviceId"
    key="serviceId"
    cellData={
      (rowData) => (<Link to={`services/${rowData.service.serviceId}`} title={`Details for ${rowData.service.serviceId}`}>{rowData.service.serviceId}</Link>)
    }
    sortable={true}
  />
);

const RequestColumn = (
  <Column
    label="Request"
    id="requestId"
    key="requestId"
    className="keep-in-check"
    cellData={
      (rowData) => rowData.service.serviceBasePath
    }
    sortable={true}
  />
);

const GroupsColumn = (
  <Column
    label="Groups"
    id="groups"
    key="groups"
    cellData={
      (rowData) => {
        return rowData.service.loadBalancerGroups.map((groupId) => {return (<Link to={`group/${groupId}`} title={`Details for ${groupId}`} key={groupId}>{groupId}</Link>);})
      }
    }
    sortable={false}
  />
);

const UpstreamsColumn = (
  <Column
    label="Upstreams"
    id="upstreams"
    key="upstreams"
    cellData={
      (rowData) => rowData.upstreams.length
    }
    sortable={true}
  />
);

class Services extends Component {
  static propTypes = {
    services: PropTypes.array,
    allowEdit: PropTypes.bool,
  };

  render() {
    if (this.props.allowEdit) {
      return (
        <div>
          <h1>Services</h1>
          <UITable
            ref="table"
            data={this.props.services}
            keyGetter={(service) => service.service.serviceId}
            paginated={true}
            rowChunkSize={15}
          >
            { IdColumn }
            { RequestColumn }
            { GroupsColumn }
            { UpstreamsColumn }
            { ButtonsColumn }
          </UITable>
        </div>
      );
    }

    return (
      <div>
        <h1>Services</h1>
        <UITable
          ref="table"
          data={this.props.services}
          keyGetter={(service) => service.service.serviceId}
          paginated={true}
          rowChunkSize={15}
        >
          { IdColumn }
          { RequestColumn }
          { GroupsColumn }
          { UpstreamsColumn }
        </UITable>
      </div>
    );
  }
}

export default connect((state) => ({
  services: state.api.services.data,
  allowEdit: config.allowEdit,
}))(rootComponent(Services, refresh));
