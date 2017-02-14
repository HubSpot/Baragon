import React, { PropTypes } from 'react';

const DetailGroup = ({name, items, field}) => {
  if (! items || items.length === 0) {
    return null;
  }

  return (
    <div className="col-md-3">
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
  items: PropTypes.array,
  field: PropTypes.func,
};

DetailGroup.defaultProps = {
  field: (it) => it,
};

export default DetailGroup;
