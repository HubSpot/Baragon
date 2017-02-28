import React, { Component, PropTypes } from 'react';
import { connect } from 'react-redux';

import { ModifyTargetCount } from '../../../actions/api/groups';

import FormModal from '../modal/FormModal';

class ModifyCountModal extends Component {
  static propTypes = {
    groupName: PropTypes.string.isRequired,
    currentCount: PropTypes.number,
    modifyCount: PropTypes.func.isRequired
  };

  show() {
    this.refs.modifyTargetCount.show();
  }

  render() {
    return (
      <FormModal
        name="Modify Target Count"
        ref="modifyTargetCount"
        action="Modify Target Count"
        onConfirm={this.props.modifyCount}
        buttonStyle="primary"
        formElements={[
          {
            name: 'newCount',
            type: FormModal.INPUT_TYPES.NUMBER,
            label: 'New Count: ',
            defaultValue: this.props.currentCount,
            min: 1,
            isRequired: true
          }]}>
        Please specity the new target agent count.

        If <code>enforceTargetAgentCount</code> is specified in your Baragon
        Service config, requests will not be allowed to process if there are
        fewer agents than the target amount.
      </FormModal>
    );
  }
}

const mapDispatchToProps = (dispatch, ownProps) => ({
  modifyCount: (data) => dispatch(ModifyTargetCount.trigger(ownProps.groupName, data.newCount))
      .then(response => (ownProps.then && ownProps.then(response)))
});

export default connect(
  null,
  mapDispatchToProps,
  null,
  { withRef: true }
)(ModifyCountModal);
