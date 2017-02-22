import React from 'react';
import moment from 'moment';

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

const Utils = {
  isIn(needle, haystack) {
    return !_.isEmpty(haystack) && haystack.indexOf(needle) >= 0;
  },

  humanizeText(text) {
    if (!text) {
      return '';
    }
    text = text.replace(/_/g, ' ');
    text = text.toLowerCase();
    text = text[0].toUpperCase() + text.substr(1);
    return text;
  },

  humanizeFileSize(bytes) {
    const kilo = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB', 'TB', 'PB', 'EB', 'ZB', 'YB'];
    if (bytes === 0) {
      return '0 B';
    }
    const numberOfPowers = Math.min(Math.floor(Math.log(bytes) / Math.log(kilo)), sizes.length - 1);
    return `${+(bytes / Math.pow(kilo, numberOfPowers)).toFixed(2)} ${sizes[numberOfPowers]}`;
  },

  humanizeCamelcase(text) {
    return text.replace(/^[a-z]|[A-Z]/g, (character, key) => (
      key === 0 ? character.toUpperCase() : ` ${character.toLowerCase()}`
    ));
  },

  timestampFromNow(millis) {
    const timeObject = moment(millis);
    return `${timeObject.fromNow()} (${timeObject.format(window.config.timestampFormat)})`;
  },

  absoluteTimestamp(millis) {
    return moment(millis).format(window.config.timestampFormat);
  },

  absoluteTimestampWithSeconds(millis) {
    return moment(millis).format(window.config.timestampWithSecondsFormat);
  },

  timestampWithinSeconds(timestamp, seconds) {
    const before = moment().subtract(seconds, 'seconds');
    const after = moment().add(seconds, 'seconds');
    return moment(timestamp).isBetween(before, after);
  },

  duration(millis) {
    return moment.duration(millis).humanize();
  },

  humanizeWorkerLag(millis) {
    if (millis > 1e6) {
      return 'Not Running';
    } else if (millis > 1e3) {
      return `${moment.duration(millis).asSeconds()} s`;
    } else {
      return `${moment.duration(millis).asMilliseconds()} ms`;
    }
  },

  substituteTaskId(value, taskId) {
    return value.replace('$TASK_ID', taskId);
  },

  isGlobFilter(filter) {
    for (const char of this.GLOB_CHARS) {
      if (filter.indexOf(char) !== -1) {
        return true;
      }
    }
    return false;
  },

  fuzzyFilter(filter, fuzzyObjects, primaryField = (it) => it.id) {
    const maxScore = _.max(fuzzyObjects, (fuzzyObject) => fuzzyObject.score).score;
    _.chain(fuzzyObjects).map((fuzzyObject) => {
      if (primaryField(fuzzyObject.original).toLowerCase().startsWith(filter.toLowerCase())) {
        fuzzyObject.score = fuzzyObject.score * 10;
      } else if (primaryField(fuzzyObject.original).toLowerCase().indexOf(filter.toLowerCase()) > -1) {
        fuzzyObject.score = fuzzyObject.score * 5;
      }
      return fuzzyObject;
    });
    return _.uniq(
      _.pluck(
        _.sortBy(
          _.filter(
            fuzzyObjects,
            (fuzzyObject) => {
              return fuzzyObject.score > (maxScore / 10) && fuzzyObject.score > 20;
            }
          ),
          (fuzzyObject) => {
            return fuzzyObject.score;
          }
        ).reverse(),
        'original'
      )
    );
  },

  deepClone(objectToClone) {
    return $.extend(true, {}, objectToClone);
  },

  ignore404(response) {
    if (response.status === 404) {
      app.caughtError();
    }
  },

  joinPath(firstPart, secondPart) {
    if (!firstPart.endsWith('/')) firstPart += '/';
    if (secondPart.startsWith('/')) secondPart = secondPart.substring(1);
    return `${firstPart}${secondPart}`;
  },

  range(begin, end, interval = 1) {
    const res = [];
    for (let currentValue = begin; currentValue < end; currentValue += interval) {
      res.push(currentValue);
    }
    return res;
  },

  maybe(object, path, defaultValue = undefined) {
    if (!path.length) {
      return object;
    }
    if (!object) {
      return defaultValue;
    }
    if (object.hasOwnProperty(path[0])) {
      return Utils.maybe(
        object[path[0]],
        path.slice(1, path.length)
      );
    }

    return defaultValue;
  },

  queryParams(source) {
    const array = [];
    for (const key in source) {
      if (source[key]) {
        array.push(`${encodeURIComponent(key)}=${encodeURIComponent(source[key])}`);
      }
    }
    return array.join('&');
  },

  buildRequestId(serviceId) {
    return `${serviceId}-${Date.now()}`;
  },

  // Render the elements in an array as `columns` number of columns
  // of list-groups, using `itemRenderer` to turn each item into a
  // list-group-item.
  asGroups(arr, columns, itemRenderer) {
    if (!arr) {
      return [];
    }
    return chunk(arr, arr.length / columns)
      .map(rowRenderer(itemRenderer, columns));
  },

  iconByState(state) {
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
  }
};

export default Utils;
