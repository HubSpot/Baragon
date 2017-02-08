import React, { Component, PropTypes } from 'react';
import { connect } from 'react-redux';

import {
  RemoveKnownAgent,
  FetchGroupKnownAgents,
  FetchGroupAgents
} from '../../../actions/api/groups';

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
        formElements={[]}>
        Are you sure that you would like to remove this traffic source?
      </FormModal>
    );
  }
}

const mapDispatchToProps = (dispatch, ownProps) => ({
  removeKnownAgent: (data) => dispatch(RemoveKnownAgent.trigger(ownProps.groupName, ownProps.agentId))
      .then(response => dispatch(FetchGroupKnownAgents.trigger(ownProps.groupName)))
      .then(response => dispatch(FetchGroupAgents.trigger(ownProps.groupName)))
      .then(response => (ownProps.then && ownProps.then(response)))
});

export default connect(
  null,
  mapDispatchToProps,
  null,
  { withRef: true }
)(RemoveKnownAgentModal);
