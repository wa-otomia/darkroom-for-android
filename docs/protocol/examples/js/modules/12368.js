__d(function (global, _$$_REQUIRE, _$$_IMPORT_DEFAULT, _$$_IMPORT_ALL, module, exports, _dependencyMap) {
  var _interopRequireDefault = _$$_REQUIRE(_dependencyMap[0]);

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  exports.default = undefined;

  var _classCallCheck2 = _interopRequireDefault(_$$_REQUIRE(_dependencyMap[1]));

  var _createClass2 = _interopRequireDefault(_$$_REQUIRE(_dependencyMap[2]));

  var _aesJs = _interopRequireDefault(_$$_REQUIRE(_dependencyMap[3]));

  var _ht_utils = _interopRequireDefault(_$$_REQUIRE(_dependencyMap[4]));

  var _ht_crypto = _interopRequireDefault(_$$_REQUIRE(_dependencyMap[5]));

  var _ht_model = _$$_REQUIRE(_dependencyMap[6]);

  var HTPackage = function () {
    function HTPackage() {
      (0, _classCallCheck2.default)(this, HTPackage);
      this.version = _ht_model.VERSION_1_01;
      this.reserve = _ht_model.RESERVE;
      this.encryptType = _ht_model.ENCRYPT_ECB;
      this.channelId = _ht_model.CHANNEL_DATA_ID;
      this.interactive = _ht_model.INTERACTIVE_REQUEST;
      this.encoding = _ht_model.ENCODING_JSON;
      this.arcMsgSn = 0;
      this.msgSn = 0;
      this.msgPackageTotal = 0;
      this.currentPackageNum = 0;
      this.aesKey = null;
      this.msgBody = null;
      this.msgLength = 0;
      this.isStream = false;
    }

    (0, _createClass2.default)(HTPackage, [{
      key: "setStream",
      value: function setStream(isStream) {
        this.isStream = isStream;
      }
    }, {
      key: "setVersion",
      value: function setVersion(version) {
        this.version = version;
      }
    }, {
      key: "setRserve",
      value: function setRserve(reserve) {
        this.reserve = reserve;
      }
    }, {
      key: "setChannelId",
      value: function setChannelId(channelId) {
        this.channelId = channelId;
      }
    }, {
      key: "setInteractive",
      value: function setInteractive(interactive) {
        this.interactive = interactive;
      }
    }, {
      key: "setEncoding",
      value: function setEncoding(encoding) {
        this.encoding = encoding;
      }
    }, {
      key: "setArcMsgSn",
      value: function setArcMsgSn(arcMsgSn) {
        this.arcMsgSn = arcMsgSn;
      }
    }, {
      key: "setMsgSn",
      value: function setMsgSn(msgSn) {
        this.msgSn = msgSn;
      }
    }, {
      key: "setMsgPackageTotal",
      value: function setMsgPackageTotal(msgPackageTotal) {
        this.msgPackageTotal = msgPackageTotal;
      }
    }, {
      key: "setCurrentPackageNum",
      value: function setCurrentPackageNum(currentPackageNum) {
        this.currentPackageNum = currentPackageNum;
      }
    }, {
      key: "setEncryptType",
      value: function setEncryptType(encryptType) {
        this.encryptType = encryptType;
      }
    }, {
      key: "setAesKey",
      value: function setAesKey(aesKey) {
        this.aesKey = aesKey;
      }
    }, {
      key: "setMsgBody",
      value: function setMsgBody(msgBody) {
        if (typeof msgBody === 'string') {
          this.msgBody = _aesJs.default.utils.utf8.toBytes(msgBody);
        } else if (_ht_utils.default.isArrayBuffer(msgBody)) {
          this.msgBody = new Uint8Array(msgBody);
        } else if (_ht_utils.default.isUint8Array(msgBody)) {
          this.msgBody = msgBody;
        } else {
          throw new Error('Invalid data, chunk must be a string or ArrayBuffer or Uint8Array');
        }
      }
    }, {
      key: "getView6to13",
      value: function getView6to13() {
        var uint32 = new Uint32Array(2);
        uint32[0] = this.arcMsgSn;
        uint32[1] = this.msgSn;
        return new Uint8Array(uint32.buffer);
      }
    }, {
      key: "getView14to19",
      value: function getView14to19(bodyLength) {
        var uint16 = new Uint16Array(3);
        var msgAttribute = bodyLength;

        switch (this.encryptType) {
          case '000':
            msgAttribute += 0;
            break;

          case '001':
            msgAttribute += 1024;
            break;

          case '010':
            msgAttribute += 2048;
            break;

          case '011':
            msgAttribute += 3072;
            break;

          case '100':
            msgAttribute += 4096;
            break;

          case '101':
            msgAttribute += 5120;
            break;

          case '110':
            msgAttribute += 6144;
            break;
        }

        if (this.msgPackageTotal > 1 || this.isStream) {
          msgAttribute += 8192;
        }

        uint16[0] = this.msgPackageTotal;
        uint16[1] = this.currentPackageNum;
        uint16[2] = msgAttribute;
        return new Uint8Array(uint16.buffer);
      }
    }, {
      key: "encryptBody",
      value: function encryptBody() {
        var binary = this.msgBody;

        switch (this.encryptType) {
          case _ht_model.ENCRYPT_ECB:
            return _ht_crypto.default.encryptECB(this.aesKey, binary);

          default:
            return binary;
        }
      }
    }, {
      key: "build",
      value: function build() {
        var encrypt = arguments.length > 0 && arguments[0] !== undefined ? arguments[0] : false;
        var msgBodyView = this.msgBody;

        if (encrypt) {
          msgBodyView = this.encryptBody();
        }

        var frame = new Uint8Array(22 + msgBodyView.length);
        var frame6to13 = this.getView6to13();
        var frame14to19 = this.getView14to19(msgBodyView.length);
        var sum = 0;
        frame[0] = _ht_model.FRAME_HEAD;
        frame[1] = this.version;
        frame[2] = this.reserve;
        frame[3] = this.channelId;
        frame[4] = this.interactive;
        frame[5] = this.encoding;

        for (var i = 0; i < frame6to13.length; i++) {
          frame[6 + i] = frame6to13[i];
        }

        for (var _i = 0; _i < frame14to19.length; _i++) {
          frame[14 + _i] = frame14to19[_i];
        }

        frame.set(msgBodyView, 20);
        sum = frame.reduce(function (a, b) {
          return a + b;
        });
        frame[frame.length - 2] = sum - 126;
        frame[frame.length - 1] = _ht_model.FRAME_TAIL;
        return frame;
      }
    }]);
    return HTPackage;
  }();

  exports.default = HTPackage;
},12368,[14314,14329,14332,12308,12359,12365,12371]);