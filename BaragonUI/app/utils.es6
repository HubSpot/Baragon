import moment from 'moment';

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

  fuzzyFilter(filter, fuzzyObjects) {
    const maxScore = _.max(fuzzyObjects, (fuzzyObject) => fuzzyObject.score).score;
    _.chain(fuzzyObjects).map((fuzzyObject) => {
        if (fuzzyObject.original.id.toLowerCase().startsWith(filter.toLowerCase())) {
          fuzzyObject.score = fuzzyObject.score * 10;
        } else if (fuzzyObject.original.id.toLowerCase().indexOf(filter.toLowerCase()) > -1) {
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
  }
};

export default Utils;
