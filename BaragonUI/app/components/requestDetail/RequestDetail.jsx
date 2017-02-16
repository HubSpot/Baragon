import React, { PropTypes } from 'react';
import { connect } from 'react-redux';
import rootComponent from '../../rootComponent';
import { refresh } from '../../actions/ui/requestDetail';
import Utils from '../../utils';

import JSONButton from '../common/JSONButton';

import SummaryPanel from './SummaryPanel';
import AgentResponsesPanel from './AgentResponsesPanel';
import OriginalRequestPanel from './OriginalRequestPanel';

const RequestDetail = ({response}) => {
  if (response === undefined || ! response.loadBalancerRequestId) {
    return <div className="centered cushy page-loader"></div>;
  }

  const {
    agentResponses,
    message,
    request,
    loadBalancerRequestId: requestId,
  } = response;
  const {loadBalancerService: {serviceId}} = request;

  return (
    <div>
      <div className="row detail-header">
        <div className="col-md-10">
          <h3>Request: {requestId}</h3>
        </div>
        <div className="col-md-2 button-container">
          <JSONButton object={response} showOverlay={true}>
            <span className="btn btn-default">JSON</span>
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
  response: PropTypes.shape({
    agentResponses: PropTypes.object,
    message: PropTypes.string,
    request: PropTypes.object,
    loadBalancerRequestId: PropTypes.string,
  }),
};

const mapStateToProps = (state, ownProps) => ({
  response: Utils.maybe(state, ['api', 'requestResponse', ownProps.params.requestId, 'data']),
});

export default connect(mapStateToProps)(rootComponent(RequestDetail, (props) => refresh(props.params.requestId)));
