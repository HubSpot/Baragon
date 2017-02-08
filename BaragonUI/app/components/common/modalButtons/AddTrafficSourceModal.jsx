import React, { Component, PropTypes } from 'react';
import { connect } from 'react-redux';

import { AddTrafficSource } from '../../../actions/api/groups';

import FormModal from '../modal/FormModal';

class AddTrafficSourceModal extends Component {
  static propTypes = {
    groupName: PropTypes.string.isRequired,
    addTrafficSource: PropTypes.func.isRequired
  };

  show() {
    this.refs.addTrafficSourceModal.show();
  }

  render() {
    return (
      <FormModal
        name="Add Traffic Source"
        ref="addTrafficSourceModal"
        action="Add Traffic Source"
        onConfirm={this.props.addTrafficSource}
        buttonStyle="primary"
        formElements={[]}>
        Please specificy the traffic source type (ie, either a classic load
        balancer or a target group) and the traffic source's name.
      </FormModal>
    );
  }
}

const mapDispatchToProps = (dispatch, ownProps) => ({
  addTrafficSource: () => dispatch(AddTrafficSource.trigger(ownProps.groupName, {name: "test", type: "CLASSIC"}))
      .then(response => (ownProps.then && ownProps.then(response)))
});

export default connect(
  null,
  mapDispatchToProps,
  null,
  { withRef: true }
)(AddTrafficSourceModal);
