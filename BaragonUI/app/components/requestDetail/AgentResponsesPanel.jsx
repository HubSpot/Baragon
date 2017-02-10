import React from 'react';

const AgentDetailContent = ({content}) => {
  if (content) {
    return (
      <div className="row">
        <div className="col-md-12">
          <strong>Message:</strong> {content}
        </div>
      </div>
    );
  } else {
    return null;
  }
};

AgentDetailContent.propTypes = {
  content: React.PropTypes.string,
};

const AgentDetailException = ({exception}) => {
  if (exception) {
    return (
      <div className="row">
        <div className="col-md-12">
          <strong>Exception:</strong> {exception}
        </div>
      </div>
    );
  } else {
    return null;
  }
};

AgentDetailException.propTypes = {
  exception: React.PropTypes.string,
};

const agentDetail = ({url, attempt, statusCode, content, exception}) => {
  return (
    <li className="list-group-item" key={url}>
      <div className="row">
        <div className="col-md-4">
          <strong>Agent: </strong>
          <a href={url}>{url}</a>
        </div>
        <div className="col-md-4">
          <strong>Attempt:</strong> {attempt}
        </div>
        <div className="col-md-4">
          <strong>Status Code:</strong> {statusCode}
        </div>
      </div>
      <AgentDetailContent content={content} />
      <AgentDetailException exception={exception} />
    </li>
  );
};

agentDetail.propTypes = {
  url: React.PropTypes.string.isRequired,
  attempt: React.PropTypes.number.isRequired,
  statusCode: React.PropTypes.string.isRequred,
  content: React.PropTypes.string,
  exception: React.PropTypes.string,
};

const agent = (value, key) => {
  return (
    <li className="list-group-item" key={key}>
      <h4>{key}</h4>
      <ul className="list-group">
        { value.map(agentDetail) }
      </ul>
    </li>
  );
};

const AgentResponsesPanel = ({agentResponses}) => {
  return (
    <div className="col-md-12">
      <div className="panel panel-default">
        <div className="panel-heading">
          <h4>Agent Responses</h4>
        </div>
        <div className="panel-body">
          <ul className="list-group">
            { _.values(_.mapObject(agentResponses, agent)) }
          </ul>
        </div>
      </div>
    </div>
  );
};

AgentResponsesPanel.propTypes = {
  agentResponses: React.PropTypes.object,
};

export default AgentResponsesPanel;
