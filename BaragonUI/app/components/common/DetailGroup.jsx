import React, { PropTypes } from 'react';

const DetailGroup = ({name, width = 3, items = [], field = (it) => it}) => {
  if (! items || items.length === 0) {
    return null;
  }

  return (
    <div className={`col-md-${width}`}>
      <h4>{ name }</h4>
      <ul className="list-group">
        { items.map((item) => (
          <li className="list-group-item" key={ field(item) }>
            { field(item) }
          </li>)) }
      </ul>
    </div>
  );
};

DetailGroup.propTypes = {
  name: PropTypes.string.isRequired,
  width: PropTypes.number,
  items: PropTypes.array,
  field: PropTypes.func,
};

export default DetailGroup;
