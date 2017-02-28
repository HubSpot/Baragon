import React, { Component, PropTypes } from 'react';
import { connect } from 'react-redux';

import { DisableAuthKey } from '../../../actions/api/auth';

import FormModal from '../modal/FormModal';

class EnableEditModal extends Component {
  static propTypes = {
    disableAuth: PropTypes.func.isRequired
  };

  show() {
    this.refs.disableEditModal.show();
  }

  render() {
    return (
      <FormModal
        name="Disable Edit"
        ref="disableEditModal"
        action="Disable"
        onConfirm={this.props.disableAuth}
        buttonStyle="info"
        formElements={[]}>
        <p>
          Continuing will disable edit mode. You will need to click 'Enable Edit'
          and enter your auth key to re-enable.
        </p>
      </FormModal>
    );
  }
}

const mapDispatchToProps = (dispatch) => ({
  disableAuth: () => dispatch(DisableAuthKey.trigger()),
});

export default connect(
  null,
  mapDispatchToProps,
  null,
  { withRef: true }
)(EnableEditModal);
