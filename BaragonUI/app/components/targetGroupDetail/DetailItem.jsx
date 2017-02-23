import React, { PropTypes } from 'react';

const DetailItem = ({name, value}) => {
  return (
    <li className="list-group-item">
      <strong>{ name }: </strong>
      {value}
    </li>
  );
};

DetailItem.propTypes = {
  name: PropTypes.string.isRequired,
  value: PropTypes.oneOfType([
    PropTypes.string,
    PropTypes.number,
  ]).isRequired,
};

export default DetailItem;
