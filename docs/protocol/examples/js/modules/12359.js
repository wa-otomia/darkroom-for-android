__d(function (global, _$$_REQUIRE, _$$_IMPORT_DEFAULT, _$$_IMPORT_ALL, module, exports, _dependencyMap) {
  var _interopRequireDefault = _$$_REQUIRE(_dependencyMap[0]);

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  exports.default = undefined;

  var _bigInteger = _interopRequireDefault(_$$_REQUIRE(_dependencyMap[1]));

  var _buffer = _$$_REQUIRE(_dependencyMap[2]);

  function getFrameHeadIndex(frame) {
    if (frame.length === 0) {
      return -1;
    }

    if (frame.length === 1) {
      return frame[0] === 126 ? 0 : -1;
    }

    if (frame.length === 2) {
      return frame[0] === 126 && (frame[1] === 100 || frame[1] === 1) ? 0 : -1;
    }

    for (var i = 0; i < frame.length; i++) {
      if (i === frame.length - 1) {
        return frame[i] === 126 ? i : -1;
      }

      if (i === frame.length - 2) {
        return frame[i] === 126 && (frame[i + 1] === 100 || frame[i + 1] === 1) ? i : -1;
      }

      if (frame[i] === 126 && (frame[i + 1] === 100 || frame[i + 1] === 1) && frame[i + 2] === 0) {
        return i;
      }
    }

    return -1;
  }

  function getFrameEndIndex(frame) {
    if (frame.length < 22) {
      return -1;
    }

    var attributeBuffer = new Uint8Array([frame[18], frame[19]]).buffer;
    var attributeNumber = new Uint16Array(attributeBuffer)[0];
    var binaryString = attributeNumber.toString(2);
    var binaryArray = binaryString.split('');
    var length = binaryArray.length;
    var bodyLength = 0;

    for (var i = 0; i < 16 - length; i++) {
      binaryArray.unshift('0');
    }

    for (var j = 15; j > 5; j--) {
      if (binaryArray[j] === '1') {
        bodyLength += Math.pow(2, 15 - j);
      }
    }

    if (bodyLength > 992) {
      return -2;
    }

    var flagEndIndex = 21 + bodyLength;

    if (frame.length < flagEndIndex + 1) {
      return -1;
    }

    if (frame[flagEndIndex] !== 126) {
      return -2;
    }

    return flagEndIndex;
  }

  function getSingleFrame(frameBuffer, frameGroup) {
    var frameHeadIndex = getFrameHeadIndex(frameBuffer);

    if (frameHeadIndex !== -1) {
      frameBuffer.splice(0, frameHeadIndex);
    } else {
      frameBuffer = [];
      return;
    }

    var frameEndIndex = getFrameEndIndex(frameBuffer);

    if (frameEndIndex === -1) {
      return;
    } else if (frameEndIndex === -2) {
      frameBuffer.splice(0, 3);
      return getSingleFrame(frameBuffer, frameGroup);
    } else {
      var frame = frameBuffer.slice(0, frameEndIndex + 1);
      frameGroup.push(unpack(frame));
      frameBuffer.splice(0, frameEndIndex + 1);
      return getSingleFrame(frameBuffer, frameGroup);
    }
  }

  function unpack(buffer) {
    var frame = buffer;
    var result = {
      channelId: frame[3],
      interactive: frame[4],
      encryptType: '',
      msgPackageTotal: new Uint16Array(new Uint8Array([frame[14], frame[15]]).buffer)[0],
      msgPackageNum: new Uint16Array(new Uint8Array([frame[16], frame[17]]).buffer)[0],
      body: null
    };
    var msgAttribute = new Uint16Array(new Uint8Array([frame[18], frame[19]]).buffer)[0];
    var binaryString = msgAttribute.toString(2).padStart(16, '0');
    result.encryptType = binaryString.substr(3, 3);
    var body = buffer.slice(20, buffer.length - 2);
    result.body = body;
    return result;
  }

  var HTUtils = {
    uint8Concat: function uint8Concat(arg1, arg2) {
      var result = new Uint8Array(arg1.length + arg2.length);
      result.set(arg1, 0);
      result.set(arg2, arg1.length);
      return result;
    },
    compareUint8: function compareUint8(arg1, arg2) {
      if (arg1.length === arg2.length) {
        for (var i = 0; i < arg1.length; i++) {
          if (arg1[i] !== arg2[i]) {
            return false;
          }
        }

        return true;
      }

      return false;
    },
    randomBytes: function randomBytes(length) {
      var bytesArray = [];

      for (var i = 0; i < length; i++) {
        bytesArray.push(Math.floor(Math.random() * 256));
      }

      return Uint8Array.from(bytesArray);
    },
    isUint8Array: function isUint8Array(value) {
      var hasUint8Array = typeof Uint8Array === 'function';
      var toString = Object.prototype.toString;
      return hasUint8Array && (value instanceof Uint8Array || toString.call(value) === '[object Uint8Array]');
    },
    isArrayBuffer: function isArrayBuffer(value) {
      var hasArrayBuffer = typeof ArrayBuffer === 'function';
      var toString = Object.prototype.toString;
      return hasArrayBuffer && (value instanceof ArrayBuffer || toString.call(value) === '[object ArrayBuffer]');
    },
    getFrames: function getFrames(data) {
      var frameGroup = [];
      getSingleFrame(data, frameGroup);
      var result = {
        frames: frameGroup,
        buffer: data
      };
      return result;
    },
    byteArrayToString: function byteArrayToString(buffer) {
      return _buffer.Buffer.from(buffer).toString();
      ;
    },
    byteArrayToBigInteger: function byteArrayToBigInteger(buffer) {
      return (0, _bigInteger.default)(this.byteArrayToString(buffer), 16);
    },
    nextRandomBigInteger: function nextRandomBigInteger(n) {
      var bitLength = n.bitLength();
      var result;

      do {
        result = _bigInteger.default.randBetween((0, _bigInteger.default)(2).pow(bitLength - 1), n.minus(1));
      } while (result.compare(n) >= 0 || result.compare((0, _bigInteger.default)(2)) <= 0);

      return result;
    },
    bigIntegerToBytes: function bigIntegerToBytes(bigInteger) {
      return _buffer.Buffer.from(this.bigIntegerToString(bigInteger), 'utf-8');
    },
    bigIntegerToString: function bigIntegerToString(bigInteger) {
      return bigInteger.toString(16).toUpperCase();
    },
    arrayPacking16: function arrayPacking16(src) {
      var temp = new Uint8Array(16);

      for (var i = 0; i < temp.length; i++) {
        temp[i] = 48;
      }

      temp.set(src, 16 - src.length);
      return temp;
    },
    arrayPacking16AtEnd: function arrayPacking16AtEnd(buffer) {
      var temp = new Uint8Array(16);

      for (var i = 0; i < temp.length; i++) {
        temp[i] = 0x00;
      }

      temp.set(buffer, 0);
      return temp;
    },
    handleTailZero: function handleTailZero(a) {
      var end = 0;

      for (var i = 0; i < a.length; i++) {
        if (a[i] === 0) {
          end = i;
          break;
        }
      }

      var result = a.slice(0, end);
      return result;
    }
  };
  var _default = HTUtils;
  exports.default = _default;
},12359,[14314,12362,22402]);