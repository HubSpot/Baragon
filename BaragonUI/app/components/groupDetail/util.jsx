import React from 'react';
import _ from 'underscore';

// Map a flat array into an array of arrays of size `size`, where the last
// array is as long as possible
// [1, 2, 3, 4, 5, 6, 7], 2 -> [[1, 2], [3, 4], [5, 6], [7]]
// Precondition: size != 0
function chunk(arr, size) {
  return _.chain(arr)
    .groupBy((elem, index) => Math.floor(index / size))
    .toArray()
    .value();
}

export function asGroups(arr, itemRenderer) {
  const rowRenderer = (row, index) => {
    return (
      <div className="col-md-3" key={index}>
        <ul className="list-group">
          { row.map( itemRenderer ) }
        </ul>
      </div>
    );
  }

  return chunk(arr, arr.length / 4).map(rowRenderer);
}
