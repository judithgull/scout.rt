scout.strings = {

  /**
   * @param text
   * @param encodeHtml defaults to true
   */
  nl2br: function(text, encodeHtml) {
    if (text === undefined || text === null) {
      return text;
    }
    text = this.asString(text);
    if (scout.helpers.nvl(encodeHtml, true)) {
      text = scout.strings.encode(text);
    }
    return text.replace(/\n/g, '<br>').replace(/\r/g, '');
  },

  removeAmpersand: function(text) {
    if (text === undefined || text === null) {
      return text;
    }
    text = this.asString(text);
    // Remove single & that are not surrounded by & or &&
    text = text.replace(/(^|[^&]|&&)&($|[^&]|&&)/g, '$1$2');
    // Replace remaining && by a single &
    text = text.replace(/&&/g, '&');
    return text;
  },

  /**
   * @returns true if the given string contains any non-space characters
   */
  hasText: function(text) {
    text = this.asString(text);
    if (typeof text !== 'string' || text.length === 0) {
      return false;
    }
    return !/^\s*$/.test(text);
  },

  repeat: function(pattern, count) {
    if (pattern === undefined || pattern === null) {
      return pattern;
    }
    if (typeof count !== 'number' || count < 1) {
      return '';
    }
    var result = '';
    for (var i = 0; i < count; i++) {
      result += pattern;
    }
    return result;
  },

  padZeroLeft: function(string, padding) {
    string = this.asString(string);
    if (string === undefined || string === null || typeof padding !== 'number' || padding < 1 || (string + '').length >= padding) {
      return string;
    }
    var z = scout.strings.repeat('0', padding) + string;
    return z.slice(-padding);
  },

  startsWith: function(fullString, startString) {
    if (fullString === undefined || fullString === null || startString === undefined || startString === null) {
      return false;
    }
    if (startString.length === 0) {
      return true;
    }
    fullString = this.asString(fullString);
    startString = this.asString(startString);
    return (fullString.substr(0, startString.length) === startString);
  },

  endsWith: function(fullString, endString) {
    if (fullString === undefined || fullString === null || endString === undefined || endString === null) {
      return false;
    }
    if (endString.length === 0) {
      return true;
    }
    fullString = this.asString(fullString);
    endString = this.asString(endString);
    return (fullString.substr(-endString.length) === endString);
  },

  /**
   * Returns the number of occurrences of 'separator' in 'string'
   */
  count: function(string, separator) {
    if (string === undefined || string === null || separator === undefined || separator === null) {
      return 0;
    }
    string = this.asString(string);
    separator = this.asString(separator);
    return string.split(separator).length - 1;
  },

  /**
   * Encodes the html of the given string.
   */
  encode: function(string) {
    if (string === undefined || string === null) {
      return string;
    }
    var div = document.createElement('div');
    div.textContent = string;
    return div.innerHTML;
  },

  /**
   * Joins a list of strings to a single string using the given separator. Elements that are
   * not defined or have zero length are ignored. The default return value is the empty string.
   *
   * @param separator String to use as separator
   * @param varargs List of strings to join
   */
  join: function(separator) {
    separator = this.asString(separator);
    var s = '';
    for (var i = 1; i < arguments.length; i++ ) {
      var arg = this.asString(arguments[i]);
      if (arg) {
        if (s && separator) {
          s += separator;
        }
        s += arg;
      }
    }
    return s;
  },

  /**
   * If the given 'string' has text, it is returned with the 'prefix' and 'suffix'
   * prepended and appended, respectively. Otherwise, the empty string is returned.
   */
  box: function(prefix, string, suffix) {
    prefix = this.asString(prefix);
    string = this.asString(string);
    suffix = this.asString(suffix);
    var s = '';
    if (this.hasText(string)) {
      if (prefix) {
        s += prefix;
      }
      s += string;
      if (suffix) {
        s += suffix;
      }
    }
    return s;
  },

  /**
   * If the given 'string' has text, its first letter is returned in lower case,
   * the remainder is unchanged. Otherwise, the empty string is returned.
   */
  lowercaseFirstLetter: function(string) {
    if (string === undefined || string === null) {
      return string;
    }
    string = this.asString(string);
    var s = '';
    if(this.hasText(string)) {
      s = string.charAt(0).toLowerCase() + string.slice(1);
    }
    return s;
  },

  /**
   * Quotes a string for use in a regular expression, i.e. escapes all characters with special meaning.
   */
  quote: function(string) {
    if (string === undefined || string === null) {
      return string;
    }
    string = this.asString(string);
    // see "escapeRegExp()" from https://developer.mozilla.org/en-US/docs/Web/JavaScript/Guide/Regular_Expressions#Using_special_characters
    return string.replace(/[.*+?^${}()|[\]\\]/g, "\\$&"); // $& = last match
  },

  /**
   * If the given input is not of type string, it is converted to a string (using the standard
   * JavaScript "String()" function). Inputs 'null' and 'undefined' are returned as they are.
   */
  asString: function(input) {
    if (input === undefined || input === null) {
      return input;
    }
    if (typeof input === 'string' || input instanceof String) {
      return input;
    }
    return String(input);
  }
};
