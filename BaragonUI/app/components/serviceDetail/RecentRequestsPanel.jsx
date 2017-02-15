import React, { Component, PropTypes } from 'react';
import { Link } from 'react-router';
import fuzzy from 'fuzzy';

import UITable from '../common/table/UITable';
import Column from '../common/table/Column';
import JSONButton from '../common/JSONButton';
import Utils from '../../utils';

const requestJSONButton = (request) => {
  if (request) {
    return (
      <JSONButton object={request}>
        <span>{'{ }'}</span>
      </JSONButton>
    );
  } else {
    return <span>{'{ }'}</span>;
  }
};

const iconForRequest = (request) => {
  return <span className={`${Utils.iconByState(request.loadBalancerState)}`}></span>;
};

const requestLink = ({loadBalancerRequestId}) => {
  return (
    <Link to={`/requests/${loadBalancerRequestId}`}>
      {loadBalancerRequestId}
    </Link>
  );
};

const HistoryTable = ({history, filter}) => {
  if (! history) {
    return (
      <div className="empty-table-message">
        <p>No recent requests</p>
      </div>
    );
  }

  let tableContent;
  if (filter.trim().length === 0) {
    tableContent = history;
  } else {
    const fuzzyObjects = fuzzy.filter(filter, history, {
      extract: (request) => request.loadBalancerRequestId,
      returnMatchInfo: true
    });
    tableContent = Utils.fuzzyFilter(filter, fuzzyObjects, (request) => {
      return request.loadBalancerRequestId;
    });
  }

  return (
    <UITable
      data={tableContent}
      keyGetter={(request) => request.loadBalancerRequestId}
      paginated={true}
      rowChunkSize={5}
    >
      <Column
        label=""
        id="requestIcon"
        cellData={iconForRequest}
        className="hidden-xs icons-column"
      />
      <Column
        label="ID"
        id="requestId"
        cellData={requestLink}
      />
      <Column
        label="Action"
        id="requestAction"
        cellData={(request) => request.action || 'UPDATE'}
      />
      <Column
        label="Result"
        id="requestResult"
        cellData={(request) => request.loadBalancerState}
      />
      <Column
        label=""
        id="requestShowJson"
        cellData={requestJSONButton}
      />
    </UITable>
  );
};

HistoryTable.propTypes = {
  history: PropTypes.arrayOf(PropTypes.object),
  filter: PropTypes.string,
};


export default class RecentRequestsPanel extends Component {
  static propTypes = {
    requests: PropTypes.arrayOf(PropTypes.object)
  }

  state = {
    filter: ''
  }

  handleSearch = (evt) => {
    this.setState({filter: evt.target.value});
  }

  render() {
    return (
      <div className="col-md-12">
        <div className="panel panel-default">
          <div className="panel-heading">
            <h4>Recent Requests</h4>
          </div>
          <div className="panel-body">
            <div className="row">
              <div className="col-md-5">
                <label>
                  Search:
                  <input
                    type="search"
                    className="form-control"
                    onKeyUp={this.handleSearch}
                  />
                </label>
              </div>
            </div>
            <div className="row">
              <div className="col-md-12">
                <HistoryTable
                  history={this.props.requests}
                  filter={this.state.filter}
                />
              </div>
            </div>
          </div>
        </div>
      </div>
    );
  }
}
