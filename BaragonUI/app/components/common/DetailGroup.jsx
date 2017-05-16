import React, { PropTypes } from 'react';

const DetailGroup = ({name, width = 3, items = [], field = (it) => it, keyGetter = (it) => it}) => {
  let contents;
  if (! items || items.length === 0) {
    contents = <li className="list-group-item">Nothing here!</li>;
  } else {
    contents = items.map((item) =>
      <li className="list-group-item" key={ keyGetter(item) }>
        { field(item) }
      </li>
    );
  }

  return (
    <div className={`col-md-${width}`}>
      <h4>{ name }</h4>
      <ul className="list-group">
        { contents }
      </ul>
    </div>
  );
};

DetailGroup.propTypes = {
  name: PropTypes.string.isRequired,
  width: PropTypes.number,
  items: PropTypes.array,
  field: PropTypes.func,
  keyGetter: PropTypes.func,
};

export default DetailGroup;
