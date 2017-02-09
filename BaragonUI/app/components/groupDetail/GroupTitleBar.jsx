import React from 'react';
import ModifyCountButton from '../common/modalButtons/ModifyCountButton';




export default function GroupTitleBar({group, domain, targetCount, editable, afterModifyTargetCount}) {
  return (
    <div>
      <div className="col-md-4">
        <h3>Group: {group}</h3>
      </div>
      <div className="col-md-5">
        <h3>Default Domain: <a href={`http://${domain}`}>{domain}</a></h3>
      </div>
      <div className="col-md-3">
        <h3>Target Count: {targetCount} {modifyTargetCount(editable, group, targetCount, afterModifyTargetCount)}</h3>
      </div>
    </div>
  )
};

const modifyTargetCount = (editable, groupName, currentCount, afterModifyTargetCount) => {
  if (editable) {
    return (
      <ModifyCountButton
        groupName={groupName}
        currentCount={currentCount}
        then={afterModifyTargetCount}
      />
    );
  } else {
    return null;
  }
}
