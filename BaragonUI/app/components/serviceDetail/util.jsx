import React from 'react';
import fuzzy from 'fuzzy';

// [T], PosInt -> [[T]]
// [1, 2, 3, 4, 5, 6, 7], 2 -> [[1, 2], [3, 4], [5, 6], [7]]
const chunk = (arr, size) => {
  return _.chain(arr)
    .groupBy((elem, index) => Math.floor(index / size))
    .toArray()
    .value();
};

// ((T, PosInt) -> DOMElement) -> ([T], PosInt) -> DOMElement
const rowRenderer = (itemRenderer, columns) => (row, index) => {
  return (
    <div className={`col-md-${12 / columns}`} key={index}>
      <ul className="list-group">
        { row.map(itemRenderer) }
      </ul>
    </div>
  );
};

// [T], ((T, PosInt) -> DOMElement) -> DOMElement
export const asGroups = (arr, columns, itemRenderer) => {
  return chunk(arr, arr.length / columns)
    .map(rowRenderer(itemRenderer, columns));
};

export const iconByState = (state) => {
  switch (state) {
    case 'SUCCESS':
      return 'glyphicon glyphicon-ok-circle active';
    case 'FAILED':
      return 'glyphicon glyphicon-ban-circle inactive';
    case 'WAITING':
      return 'glyphicon glyphicon-time';
    case 'CANCELING':
      return 'glyphicon glyphicon-remove-circle inactive';
    case 'INVALID_REQUEST_NOOP':
      return 'glyphicon glyphicon-exclamation-sign inactive';
    default:
      return 'glyphicon glyphicon-question-sign';
  }
};


export const matches = (filter, elements) => {
  return fuzzy.filter(filter, elements, {
    extract: (element) => element.loadBalancerRequestId,
    returnMatchInfo: true
  }).map((match) => match.original);
};
