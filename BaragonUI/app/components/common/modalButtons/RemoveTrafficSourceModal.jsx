import React, { Component, PropTypes } from 'react';
import { connect } from 'react-redux';

import { RemoveTrafficSource } from '../../../actions/api/groups';

import FormModal from '../modal/FormModal';

class RemoveTrafficSourceModal extends Component {
  static propTypes = {
    groupName: PropTypes.string.isRequired,
    trafficSource: PropTypes.shape({
      name: PropTypes.string,
      type: PropTypes.string
    }).isRequired,
    removeTrafficSource: PropTypes.func.isRequired
  };

  show() {
    this.refs.removeTrafficSourceModal.show();
  }

  render() {
    return (
      <FormModal
        name="Remove Traffic Source"
        ref="removeTrafficSourceModal"
        action="Remove this Traffic Source"
        onConfirm={this.props.removeTrafficSource}
        buttonStyle="danger"
        formElements={[]}>
        Are you sure you want to delete this traffic source?
        <pre>
          Name: {this.props.trafficSource.name}<br />
          Type: {this.props.trafficSource.type}
        </pre>
        Deleting a traffic source means that Baragon will no longer sync
        active agents with the specified traffic source.
      </FormModal>
    );
  }
}

const mapDispatchToProps = (dispatch, ownProps) => ({
  removeTrafficSource: () => dispatch(RemoveTrafficSource.trigger(ownProps.groupName, ownProps.trafficSource))
      .then(response => (ownProps.then && ownProps.then(response)))
});

export default connect(
  null,
  mapDispatchToProps,
  null,
  { withRef: true }
)(RemoveTrafficSourceModal);
