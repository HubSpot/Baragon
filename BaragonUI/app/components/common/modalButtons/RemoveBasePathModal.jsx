import React, { Component, PropTypes } from 'react';
import { connect } from 'react-redux';

import { RemoveBasePath } from '../../../actions/api/groups';

import FormModal from '../modal/FormModal';

class RemoveBasePathModal extends Component {
  static propTypes = {
    groupName: PropTypes.string.isRequired,
    basePath: PropTypes.string.isRequired,
    removeBasePath: PropTypes.func.isRequired
  };

  show() {
    this.refs.removeBasePath.show();
  }

  render() {
    return (
      <FormModal
        name="Remove Base Path"
        ref="removeBasePath"
        action="Remove Base Path"
        onConfirm={this.props.removeBasePath}
        buttonStyle="danger"
        formElements={[]}>
        Are you sure you want to remove this base path?
        <pre>
          {this.props.groupName}: {this.props.basePath}
        </pre>
        Removing a base path will only remove the lock associated with that base
        path for Baragon requests. It will not alter the load balancer configuration
        in any way.
      </FormModal>
    );
  }
}

const mapDispatchToProps = (dispatch, ownProps) => ({
  removeBasePath: () => dispatch(RemoveBasePath.trigger(ownProps.groupName, ownProps.basePath))
      .then(response => (ownProps.then && ownProps.then(response)))
});

export default connect(
  null,
  mapDispatchToProps,
  null,
  { withRef: true }
)(RemoveBasePathModal);
