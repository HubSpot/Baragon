import React, { PropTypes } from 'react';
import { connect } from 'react-redux';
import rootComponent from '../../rootComponent';
import { refresh } from '../../actions/ui/requestDetail';
import Utils from '../../utils';

import JSONButton from '../common/JSONButton';

import SummaryPanel from './SummaryPanel';
import AgentResponsesPanel from './AgentResponsesPanel';
import OriginalRequestPanel from './OriginalRequestPanel';

const RequestDetail = ({requestId, serviceId, agentResponses, message, request, response}) => {
  return (
    <div>
      <div className="row detail-header">
        <div className="col-md-10">
          <h3>Request: {requestId}</h3>
        </div>
        <div className="col-md-2 button-container">
          <JSONButton object={response} showOverlay={true}>
            <a className="btn btn-default">JSON</a>
          </JSONButton>
        </div>
      </div>
      <div className="row">
        <SummaryPanel
          serviceId={serviceId}
          message={message}
        />
      </div>
      <div className="row">
        <AgentResponsesPanel agentResponses={agentResponses} />
      </div>
      <div className="row">
        <OriginalRequestPanel request={request} />
      </div>
    </div>
  );
};

RequestDetail.propTypes = {
  requestId: PropTypes.string,
  serviceId: PropTypes.string,
  agentResponses: PropTypes.object,
  message: PropTypes.string,
  request: PropTypes.object,
  response: PropTypes.object,
};

const mapStateToProps = (state, ownProps) => ({
  requestId: Utils.maybe(state, ['api', 'requestResponse', ownProps.params.requestId, 'data', 'loadBalancerRequestId']),
  serviceId: Utils.maybe(state, ['api', 'requestResponse', ownProps.params.requestId, 'data', 'request', 'loadBalancerService', 'serviceId']),
  agentResponses: Utils.maybe(state, ['api', 'requestResponse', ownProps.params.requestId, 'data', 'agentResponses']),
  message: Utils.maybe(state, ['api', 'requestResponse', ownProps.params.requestId, 'data', 'message']),
  request: Utils.maybe(state, ['api', 'requestResponse', ownProps.params.requestId, 'data', 'request']),
  response: Utils.maybe(state, ['api', 'requestResponse', ownProps.params.requestId, 'data']),
});

export default connect(mapStateToProps)(rootComponent(RequestDetail, (props) => refresh(props.params.requestId)));
