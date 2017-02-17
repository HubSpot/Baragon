import React, { Component, PropTypes } from 'react';
import { connect } from 'react-redux';

import { RemoveKnownAgent } from '../../../actions/api/groups';

import FormModal from '../modal/FormModal';

class RemoveKnownAgentModal extends Component {
  static propTypes = {
    groupName: PropTypes.string.isRequired,
    agentId: PropTypes.string.isRequired,
    removeKnownAgent: PropTypes.func.isRequired
  };

  show() {
    this.refs.removeKnownAgentModal.show();
  }

  render() {
    return (
      <FormModal
        name="Remove Known Agent"
        ref="removeKnownAgentModal"
        action="Remove Known Agent"
        onConfirm={this.props.removeKnownAgent}
        buttonStyle="danger"
        formElements={[]}
      >
        Are you sure you want to delete this known agent?
        <pre>
          {this.props.groupName}: {this.props.agentId}
        </pre>
        Deleting a known agent will remove it from the list of known agents for
        this load balancer group. It will NOT remove it from the list of active
        agents.
      </FormModal>
    );
  }
}

const mapDispatchToProps = (dispatch, ownProps) => ({
  removeKnownAgent: () => dispatch(RemoveKnownAgent.trigger(ownProps.groupName, ownProps.agentId))
      .then(response => (ownProps.then && ownProps.then(response)))
});

export default connect(
  null,
  mapDispatchToProps,
  null,
  { withRef: true }
)(RemoveKnownAgentModal);
