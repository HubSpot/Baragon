import React, { Component, PropTypes } from 'react';
import { connect } from 'react-redux';
import { Link } from 'react-router';
import { Tooltip, OverlayTrigger } from 'react-bootstrap';
import rootComponent from '../../rootComponent';
import { refresh } from '../../actions/ui/services';

import UITable from '../common/table/UITable';
import Column from '../common/table/Column';

import JSONButton from '../common/JSONButton';

import ReloadServiceButton from '../common/modalButtons/ReloadServiceButton';
import DeleteServiceButton from '../common/modalButtons/DeleteServiceButton';
import RemoveUpstreamsButton from '../common/modalButtons/RemoveUpstreamsButton';

const StatusColumn = (
  <Column
    label=""
    id="statusIcon"
    key="statusIcon"
    cellData={(service) => {
      const upstreams = service.upstreams.length;
      if (upstreams === 0) {
        return <span className="glyphicon glyphicon-ban-circle inactive" title="No active upstreams" />;
      } else if (upstreams === 1) {
        return <span className="glyphicon glyphicon-ok-circle active" title="1 active upstream" />;
      } else {
        return <span className="glyphicon glyphicon-ok-circle active" title={`${upstreams} active upstreams`} />;
      }
    }}
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
        return rowData.service.loadBalancerGroups.map((groupId) => {
          return (
            <Link
              to={`group/${groupId}`}
              title={`Details for ${groupId}`}
              key={groupId}
            >
            {groupId}
            </Link>
          );
        });
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

        const removeTooltip = (
          <Tooltip id="removeUpstreams">
            Remove all upstreams from this service
          </Tooltip>
        );

        const removeUpstreams = (
          <RemoveUpstreamsButton loadBalancerService={cellData.service} upstreams={cellData.upstreams}>
            <OverlayTrigger placement="top" overlay={removeTooltip}>
              <a>
                <span className="glyphicon glyphicon-remove-circle" />
              </a>
            </OverlayTrigger>
          </RemoveUpstreamsButton>
        );

        const onlyIfUpstreams = (button) => {
          if (cellData.upstreams.length > 0) {
            return button;
          }

          return null;
        }

        return (
          <div className="hidden-xs">
            {onlyIfUpstreams(reloadService)}
            {onlyIfUpstreams(removeUpstreams)}
            {deleteService}
            <JSONButton className="inline" object={cellData} showOverlay={true}>
              {'{ }'}
            </JSONButton>
          </div>
        );
      }
    }
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
            { StatusColumn }
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
          { StatusColumn }
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
