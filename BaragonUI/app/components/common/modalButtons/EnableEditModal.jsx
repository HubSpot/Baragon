import React, { Component, PropTypes } from 'react';
import { connect } from 'react-redux';

import { TestAuthKey } from '../../../actions/api/auth';

import FormModal from '../modal/FormModal';

class EnableEditModal extends Component {
  static propTypes = {
    testAuthKey: PropTypes.func.isRequired
  };

  show() {
    this.refs.enableEditModal.show();
  }

  render() {
    return (
      <FormModal
        name="Enable Edit"
        ref="enableEditModal"
        action="Enable Edit"
        onConfirm={this.props.testAuthKey}
        buttonStyle="info"
        formElements={[
          {
            name: 'authKey',
            type: FormModal.INPUT_TYPES.PASSWORD,
            label: 'Auth Key: '
          }
        ]}>
        <p>
          This Baragon API has auth enabled. You need to specify an <strong>
          Auth Key</strong> in order to initiate any actions, eg <code>test-key</code>.
        </p>
        <p>
          This can be changed at any time in the JS console with
        </p>
        <pre>localStorage.setItem("baragonAuthKey", "test-key")</pre>
      </FormModal>
    );
  }
}

const mapDispatchToProps = (dispatch) => ({
  testAuthKey: (data) => (dispatch(TestAuthKey.trigger(data.authKey))),
});

export default connect(
  null,
  mapDispatchToProps,
  null,
  { withRef: true }
)(EnableEditModal);
