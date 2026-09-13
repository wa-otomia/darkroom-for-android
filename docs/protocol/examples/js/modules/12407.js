__d(function (global, _$$_REQUIRE, _$$_IMPORT_DEFAULT, _$$_IMPORT_ALL, module, exports, _dependencyMap) {
  var _interopRequireWildcard = _$$_REQUIRE(_dependencyMap[0]);

  var _interopRequireDefault = _$$_REQUIRE(_dependencyMap[1]);

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  exports.default = undefined;

  var _regenerator = _interopRequireDefault(_$$_REQUIRE(_dependencyMap[2]));

  var _classCallCheck2 = _interopRequireDefault(_$$_REQUIRE(_dependencyMap[3]));

  var _createClass2 = _interopRequireDefault(_$$_REQUIRE(_dependencyMap[4]));

  var _reactNative = _$$_REQUIRE(_dependencyMap[5]);

  var _aesJs = _interopRequireDefault(_$$_REQUIRE(_dependencyMap[6]));

  var _base64Js = _interopRequireDefault(_$$_REQUIRE(_dependencyMap[7]));

  var sha256 = _interopRequireWildcard(_$$_REQUIRE(_dependencyMap[8]));

  var _reactNativeTcpSocket = _interopRequireDefault(_$$_REQUIRE(_dependencyMap[9]));

  var _ht_utils = _interopRequireDefault(_$$_REQUIRE(_dependencyMap[10]));

  var _ht_crypto = _interopRequireDefault(_$$_REQUIRE(_dependencyMap[11]));

  var _ht_package = _interopRequireDefault(_$$_REQUIRE(_dependencyMap[12]));

  var _logUtils = _interopRequireDefault(_$$_REQUIRE(_dependencyMap[13]));

  var _DynamicUtil = _interopRequireDefault(_$$_REQUIRE(_dependencyMap[14]));

  var _miot = _$$_REQUIRE(_dependencyMap[15]);

  var _buffer = _$$_REQUIRE(_dependencyMap[16]);

  var _bigInteger = _interopRequireDefault(_$$_REQUIRE(_dependencyMap[17]));

  var _consts = _$$_REQUIRE(_dependencyMap[18]);

  var _ht_model = _$$_REQUIRE(_dependencyMap[19]);

  var HANDSHAKE_STEP = {
    STEP_START: 0,
    STEP_RECEIVE_INFO: 1,
    STEP_CONFIRM: 2,
    STEP_SUCESS: 3
  };
  var HANDSHAKE_CONFIRM_MESSAGE = {
    HANDSHAKE_CONFIRM_MESSAGE_OK: 'ok',
    HANDSHAKE_CONFIRM_MESSAGE_NOK: 'nok'
  };
  var MAX_FAIL_TIMES_LIMIT = 3;
  var MAX_PACKAGE_NUM_PER_TIME = 3;

  var HTClassicBluetooth = function () {
    function HTClassicBluetooth() {
      var _this = this;

      (0, _classCallCheck2.default)(this, HTClassicBluetooth);
      this.classicBlueBondState = 10;
      this.classicBlueConnectionState = 0;
      this.msgSn = 0;
      this.requestTimeout = 1500;
      this.allowUpload = true;
      this.bodyLength = 992;
      this.bodyMessageLength = 988;
      this.buffers = [];
      this.listener = null;
      this.sendStart = 0;
      this.sendEnd = 0;
      this.lastTimeNode = 0;
      this.currentTimeNode = 0;
      this.time1 = 0;
      this.time2 = 0;
      this.time2_1 = 0;
      this.time2_2 = 0;
      this.time2_3 = 0;
      this.time3 = 0;
      this.time4 = 0;
      this.time5 = 0;
      this.isTestTime = true;
      this.callback = null;
      this.encryptType = _ht_model.ENCRYPT_ECB;
      this.isSendFile = false;
      this.isCanceled = false;
      this.isHandShaking = false;
      this.currentHandShakeStep = HANDSHAKE_STEP.STEP_START;
      this.failedTime = 0;
      this.timer = null;
      this.key = null;
      this.currentSavedPackageNum = 0;
      this.packageBuffer = [];
      this.queue = [];
      this.isProcessing = false;

      if (HTClassicBluetooth.instance) {
        return HTClassicBluetooth.instance;
      }

      _logUtils.default.reportLog("HTClassicBluetooth init");

      _miot.Host.storage.get(_consts.MINT_KEY_COMPATIBLE_WITH_LOW_FW_VERSION).then(function (res) {
        _logUtils.default.reportLog("MINT_KEY_COMPATIBLE_WITH_LOW_FW_VERSION res = " + res);

        if (res) {
          _this.encryptType = _ht_model.ENCRYPT_NO;
        }
      }).catch(function (error) {
        _logUtils.default.reportLog("MINT_KEY_COMPATIBLE_WITH_LOW_FW_VERSION error = " + error);
      });

      this.classicBlueBondStateChanged = _miot.ClassicBluetoothEvent.classicBlueBondStateChanged.addListener(function (data) {
        _logUtils.default.reportLog("classicBlueBondStateChanged data = " + JSON.stringify(data));

        _DynamicUtil.default.trackEvent(_consts.BIGDATA_KEYS.APP_CONN_BT_EVT_BONDSTATECHANGE, {
          state: data ? data.state : -1
        });

        _this.classicBlueBondState = data.state;
        _this.callback && _this.callback.onBondStateChanged(data.state);
      });
      this.classicBlueConnectionStateChanged = _miot.ClassicBluetoothEvent.classicBlueConnectionStateChanged.addListener(function (data) {
        _logUtils.default.reportLog("classicBlueConnectionStateChanged data = " + JSON.stringify(data));

        _DynamicUtil.default.trackEvent(_consts.BIGDATA_KEYS.APP_CONN_BT_EVT_CONNSTATECHANGE, {
          state: data ? data.state : -1
        });

        _this.handleBlueConnectionStateChange(data.state);
      });
      this.classicBlueReceivedData = _miot.ClassicBluetoothEvent.classicBlueReceivedData.addListener(function (data) {
        _this.handleReceivedData(data.data);
      });
      HTClassicBluetooth.instance = this;
    }

    (0, _createClass2.default)(HTClassicBluetooth, [{
      key: "handleBlueConnectionStateChange",
      value: function handleBlueConnectionStateChange(state) {
        _logUtils.default.reportLog("handleBlueConnectionStateChange this.classicBlueConnectionState = " + this.classicBlueConnectionState + " state = " + state);

        if (this.classicBlueConnectionState == state || this.classicBlueConnectionState == _consts.MINT_BLUETOOTH_STATE.AUTH && state == _consts.MINT_BLUETOOTH_STATE.CONNECTED) {
          return;
        }

        this.classicBlueConnectionState = state;
        this.callback && this.callback.onConnectStateChanged(state);

        if (state === _consts.MINT_BLUETOOTH_STATE.CONNECTED) {
          _logUtils.default.reportLog("connected this.encryptType = " + this.encryptType);

          if (this.encryptType == _ht_model.ENCRYPT_ECB) {
            this.sayHello();
          } else {
            this.classicBlueConnectionState = _consts.MINT_BLUETOOTH_STATE.AUTH;

            _DynamicUtil.default.trackEvent(_consts.BIGDATA_KEYS.APP_CONN_BT_EVT_CONNSTATECHANGE, {
              state: _consts.MINT_BLUETOOTH_STATE.AUTH
            });

            this.callback && this.callback.onConnectStateChanged(_consts.MINT_BLUETOOTH_STATE.AUTH);
          }
        }
      }
    }, {
      key: "setCallback",
      value: function setCallback(callback) {
        this.callback = callback;
      }
    }, {
      key: "handleReceivedData",
      value: function handleReceivedData(data) {
        var byteArray = this.hexStringToByteArray(data);

        for (var i = 0; i < byteArray.length; i++) {
          this.buffers.push(byteArray[i]);
        }

        var result = _ht_utils.default.getFrames(this.buffers);

        this.buffers = result.buffer;

        for (var _i = 0; _i < result.frames.length; _i++) {
          if (this.isHandShaking) {
            if (this.currentHandShakeStep == HANDSHAKE_STEP.STEP_RECEIVE_INFO) {
              this.handleInfo(result.frames[_i]);
            } else if (this.currentHandShakeStep == HANDSHAKE_STEP.STEP_CONFIRM) {
              this.handleConfirm(result.frames[_i]);
            }
          } else if (this.listener) {
            this.listener.apply(null, [result.frames[_i]]);
          }
        }
      }
    }, {
      key: "getMixStatus",
      value: function getMixStatus() {
        var _this2 = this;

        var msgSn = this.getMsgSn();
        return new Promise(function (resolve, reject) {
          var data = {
            'method': 'mixed_status',
            'id': msgSn,
            'params': {}
          };

          _this2.handleCommond(data).then(function (res) {
            resolve(res);
          }).catch(function (error) {
            reject(error);
          });
        });
      }
    }, {
      key: "getDeviceInfo",
      value: function getDeviceInfo() {
        var _this3 = this;

        var msgSn = this.getMsgSn();
        return new Promise(function (resolve, reject) {
          var data = {
            'method': 'get_prop',
            'id': msgSn,
            'params': ["device_info"]
          };

          _this3.handleCommond(data).then(function (res) {
            resolve(res);
          }).catch(function (error) {
            reject(error);
          });
        });
      }
    }, {
      key: "getBigData",
      value: function getBigData() {
        var _this4 = this;

        var msgSn = this.getMsgSn();
        return new Promise(function (resolve, reject) {
          var data = {
            'method': 'get_prop',
            'id': msgSn,
            'params': ["big_data"]
          };

          _this4.handleCommond(data).then(function (res) {
            resolve(res);
          }).catch(function (error) {
            reject(error);
          });
        });
      }
    }, {
      key: "cancelJob",
      value: function cancelJob(jobId) {
        var _this5 = this;

        var msgSn = this.getMsgSn();
        return new Promise(function (resolve, reject) {
          var data = {
            'method': 'cancel_job',
            'id': msgSn,
            'params': [jobId]
          };

          _this5.handleCommond(data).then(function (res) {
            _this5.isCanceled = true;
            resolve(res);
          }).catch(function (error) {
            reject(error);
          });
        });
      }
    }, {
      key: "resumePrinter",
      value: function resumePrinter() {
        var _this6 = this;

        var msgSn = this.getMsgSn();
        return new Promise(function (resolve, reject) {
          var data = {
            'method': 'resume_printer',
            'id': msgSn,
            'params': {}
          };

          _this6.handleCommond(data).then(function (res) {
            resolve(res);
          }).catch(function (error) {
            reject(error);
          });
        });
      }
    }, {
      key: "printJob",
      value: function printJob() {
        var _this7 = this;

        var msgSn = this.getMsgSn();
        return new Promise(function (resolve, reject) {
          var data = {
            'method': 'print_job',
            'id': msgSn,
            'params': {
              file_size: 1,
              copies: 1,
              job_type: 1,
              channel: 1
            }
          };

          _this7.handleCommond(data).then(function (res) {
            resolve(res);
          }).catch(function (error) {
            reject(error);
          });
        });
      }
    }, {
      key: "printJob2",
      value: function printJob2(path, copies, jobType, callback) {
        var _this8 = this;

        this.isCanceled = false;
        var msgSn = this.getMsgSn();
        var channel = 0;

        if (_reactNative.Platform.OS === "ios") {
          channel = 64;
        } else {
          channel = 576;
        }

        if (path.indexOf('file:///') !== -1) {
          path = path.toString().replace('file:///', '/');
        }

        if (path.indexOf(_miot.Host.file.storageBasePath) !== -1) {
          path = path.toString().replace(_miot.Host.file.storageBasePath, '');
        }

        _miot.Host.file.readFileSegmentToBase64(path, 0, 1).then(function (result) {
          var data = {
            'method': 'print_job',
            'id': msgSn,
            'params': {
              file_size: result.totalLength,
              copies: copies,
              job_type: jobType,
              channel: channel
            }
          };

          _this8.handleCommond(data).then(function (res) {
            if (res.hasOwnProperty('result')) {
              var jobId = res.result.job_id;
              callback.onCreatJobSuccess(jobId);

              _this8.sendFile(path, jobId, result.totalLength, callback);
            } else if (res.hasOwnProperty('error')) {
              callback.onCreatJobFailed(res.error.code, '');
            } else {
              callback.onCreatJobFailed(-1, res);
            }
          }).catch(function (error) {
            callback.onCreatJobFailed(-2, error);
          });
        }).catch(function (error) {
          callback.onCreatJobFailed(-3, error);
        });
      }
    }, {
      key: "fwUpgrade",
      value: function fwUpgrade(path, callback) {
        var _this9 = this;

        var msgSn = this.getMsgSn();
        var channel = 0;

        if (_reactNative.Platform.OS === "ios") {
          channel = 64;
        } else {
          channel = 576;
        }

        if (path.indexOf('file:///') !== -1) {
          path = path.toString().replace('file:///', '/');
        }

        if (path.indexOf(_miot.Host.file.storageBasePath) !== -1) {
          path = path.toString().replace(_miot.Host.file.storageBasePath, '');
        }

        _miot.Host.file.readFileSegmentToBase64(path, 0, 1).then(function (result) {
          var data = {
            'method': 'print_job',
            'id': msgSn,
            'params': {
              file_size: result.totalLength,
              copies: 1,
              job_type: 100,
              channel: channel
            }
          };

          _this9.handleCommond(data).then(function (res) {
            if (res.hasOwnProperty('result')) {
              var jobId = res.result.job_id;
              callback.onCreatJobSuccess(jobId);

              _this9.sendFile(path, jobId, result.totalLength, callback);
            } else if (res.hasOwnProperty('error')) {
              callback.onCreatJobFailed(res.error.code, '');
            } else {
              callback.onCreatJobFailed(-1, res);
            }
          }).catch(function (error) {
            callback.onCreatJobFailed(-2, error);
          });
        }).catch(function (error) {
          callback.onCreatJobFailed(-3, error);
        });
      }
    }, {
      key: "sendFile",
      value: function sendFile(path, jobId, length, callback) {
        var fileLength, segmentLength, count, totalCount, thisCount, i, start, len, base64, binary, frameCount, j, begin, end, frame, newFrame, encryptFrame, htPackage, msgSn, messageFrame, finalString;
        return _regenerator.default.async(function sendFile$(_context) {
          while (1) {
            switch (_context.prev = _context.next) {
              case 0:
                _logUtils.default.reportLog("sendFile path = " + path + " jobId = " + jobId);

                this.allowUpload = true;
                this.isCanceled = false;
                this.sendStart = Date.now();
                this.isSendFile = true;

                if (this.isTestTime) {
                  this.time1 = 0;
                  this.time2_1 = 0;
                  this.time2_2 = 0;
                  this.time2_3 = 0;
                  this.time2 = 0;
                  this.time3 = 0;
                  this.time4 = 0;
                  this.time5 = 0;
                }

                _context.prev = 6;
                fileLength = length;
                segmentLength = this.bodyMessageLength * 1061;
                count = Math.ceil(fileLength / segmentLength);
                totalCount = Math.ceil(fileLength / this.bodyMessageLength);
                thisCount = 0;
                i = 0;

              case 13:
                if (!(i < count)) {
                  _context.next = 71;
                  break;
                }

                if (!this.isCanceled) {
                  _context.next = 17;
                  break;
                }

                this.isSendFile = false;
                return _context.abrupt("return");

              case 17:
                start = segmentLength * i;
                len = i === count - 1 ? fileLength - start : segmentLength;
                _context.next = 21;
                return _regenerator.default.awrap(_miot.Host.file.readFileSegmentToBase64(path, start, len));

              case 21:
                base64 = _context.sent;
                binary = _base64Js.default.toByteArray(base64.content);
                frameCount = Math.ceil(binary.length / this.bodyMessageLength);

                if (this.isTestTime) {
                  this.lastTimeNode = Date.now();
                }

                j = 0;

              case 26:
                if (!(j < frameCount)) {
                  _context.next = 68;
                  break;
                }

                if (!this.isCanceled) {
                  _context.next = 30;
                  break;
                }

                this.isSendFile = false;
                return _context.abrupt("return");

              case 30:
                thisCount++;
                begin = this.bodyMessageLength * j;
                end = j === frameCount - 1 ? binary.length : this.bodyMessageLength * (j + 1);
                frame = binary.subarray(begin, end);
                newFrame = this.addBytes(this.int2Bytes(jobId), frame);

                if (this.isTestTime) {
                  this.currentTimeNode = Date.now();
                  this.time1 += this.currentTimeNode - this.lastTimeNode;
                  this.lastTimeNode = this.currentTimeNode;
                }

                _context.next = 38;
                return _regenerator.default.awrap(this.encryptBinary(newFrame));

              case 38:
                encryptFrame = _context.sent;

                if (this.isTestTime) {
                  this.currentTimeNode = Date.now();
                  this.time2 += this.currentTimeNode - this.lastTimeNode;
                  this.lastTimeNode = this.currentTimeNode;
                }

                htPackage = new _ht_package.default();
                msgSn = this.getMsgSn();
                htPackage.setChannelId(this.getChannleID(true));
                htPackage.setEncoding(_ht_model.ENCODING_HEX);
                htPackage.setEncryptType(this.encryptType);
                htPackage.setMsgSn(msgSn);
                htPackage.setArcMsgSn(msgSn);
                htPackage.setMsgBody(encryptFrame);
                htPackage.setCurrentPackageNum(thisCount);
                htPackage.setMsgPackageTotal(totalCount);
                messageFrame = htPackage.build();

                if (this.isTestTime) {
                  this.currentTimeNode = Date.now();
                  this.time3 += this.currentTimeNode - this.lastTimeNode;
                  this.lastTimeNode = this.currentTimeNode;
                }

                Array.prototype.push.apply(this.packageBuffer, messageFrame);
                this.currentSavedPackageNum++;

                if (!(this.currentSavedPackageNum >= MAX_PACKAGE_NUM_PER_TIME || j === frameCount - 1 || _reactNative.Platform.OS == 'ios')) {
                  _context.next = 64;
                  break;
                }

                this.currentSavedPackageNum = 0;
                finalString = this.byteArrayToHexString(this.packageBuffer);
                this.packageBuffer = [];

                if (this.isTestTime) {
                  this.currentTimeNode = Date.now();
                  this.time4 += this.currentTimeNode - this.lastTimeNode;
                  this.lastTimeNode = this.currentTimeNode;
                }

                _context.next = 61;
                return _regenerator.default.awrap(this.writeWithTimeout(finalString, 3000));

              case 61:
                if (this.isTestTime) {
                  this.currentTimeNode = Date.now();
                  this.time5 += this.currentTimeNode - this.lastTimeNode;
                  this.lastTimeNode = this.currentTimeNode;
                }

                _context.next = 64;
                break;

              case 64:
                if (thisCount % 50 == 0 || thisCount == totalCount) {
                  callback.onProgress(thisCount, totalCount, jobId);
                }

              case 65:
                j++;
                _context.next = 26;
                break;

              case 68:
                i++;
                _context.next = 13;
                break;

              case 71:
                callback.onTransferSuccess(jobId);
                this.sendEnd = Date.now();

                if (this.isTestTime) {
                  _logUtils.default.reportLog("time1 = " + this.time1 + " time2_1 = " + this.time2_1 + " time2_2 = " + this.time2_2 + " time2_3 = " + this.time2_3 + " time2 = " + this.time2 + " time3 = " + this.time3 + " time4 = " + this.time4 + " time5 = " + this.time5);
                }

                this.isSendFile = false;
                _context.next = 81;
                break;

              case 77:
                _context.prev = 77;
                _context.t0 = _context["catch"](6);
                callback.onFailed(_context.t0, jobId);
                this.isSendFile = false;

              case 81:
              case "end":
                return _context.stop();
            }
          }
        }, null, this, [[6, 77]]);
      }
    }, {
      key: "timeout",
      value: function timeout(ms) {
        return new Promise(function (_, reject) {
          return setTimeout(function () {
            return reject('Operation timed out');
          }, ms);
        });
      }
    }, {
      key: "writeWithTimeout",
      value: function writeWithTimeout(finalString, timeoutDuration) {
        return _regenerator.default.async(function writeWithTimeout$(_context2) {
          while (1) {
            switch (_context2.prev = _context2.next) {
              case 0:
                _context2.prev = 0;
                _context2.next = 3;
                return _regenerator.default.awrap(Promise.race([_miot.ClassicBluetooth.write(finalString), this.timeout(timeoutDuration)]));

              case 3:
                _context2.next = 8;
                break;

              case 5:
                _context2.prev = 5;
                _context2.t0 = _context2["catch"](0);

                _logUtils.default.reportLog("writeWithTimeout error.message " + _context2.t0);

              case 8:
              case "end":
                return _context2.stop();
            }
          }
        }, null, this, [[0, 5]]);
      }
    }, {
      key: "simulateSleep",
      value: function simulateSleep(milliseconds) {
        return new Promise(function (resolve) {
          return setTimeout(resolve, milliseconds);
        });
      }
    }, {
      key: "getChannleID",
      value: function getChannleID(isSendFile) {
        if (this.encryptType == _ht_model.ENCRYPT_NO) {
          if (isSendFile) {
            return _ht_model.CHANNEL_FILE_ID;
          } else {
            return _ht_model.CHANNEL_DATA_ID;
          }
        } else {
          if (isSendFile) {
            return _ht_model.CHANNEL_FILE_ENCRYPT_ID;
          } else {
            return _ht_model.CHANNEL_DATA_ENCRYPT_ID;
          }
        }
      }
    }, {
      key: "getJobInfo",
      value: function getJobInfo(jobId) {
        var _this10 = this;

        var msgSn = this.getMsgSn();
        return new Promise(function (resolve, reject) {
          var data = {
            'method': 'job_info',
            'id': msgSn,
            'params': [jobId]
          };

          _this10.handleCommond(data).then(function (res) {
            resolve(res);
          }).catch(function (error) {
            reject(error);
          });
        });
      }
    }, {
      key: "confirmJob",
      value: function confirmJob(jobId) {
        var _this11 = this;

        var msgSn = this.getMsgSn();
        return new Promise(function (resolve, reject) {
          var data = {
            'method': 'confirm_job',
            'id': msgSn,
            'params': [jobId]
          };

          _this11.handleCommond(data).then(function (res) {
            resolve(res);
          }).catch(function (e) {
            reject(e);
          });
        });
      }
    }, {
      key: "getFwVersion",
      value: function getFwVersion() {
        var _this12 = this;

        var msgSn = this.getMsgSn();
        return new Promise(function (resolve, reject) {
          var data = {
            'method': 'get_prop',
            'id': msgSn,
            'params': ["bt_fw_ver"]
          };

          _this12.handleCommond(data).then(function (res) {
            resolve(res);
          }).catch(function (error) {
            reject(error);
          });
        });
      }
    }, {
      key: "clean",
      value: function clean() {
        var _this13 = this;

        var msgSn = this.getMsgSn();
        return new Promise(function (resolve, reject) {
          var data = {
            'method': 'clean',
            'id': msgSn,
            'params': ["mint"]
          };

          _this13.handleCommond(data).then(function (res) {
            resolve(res);
          }).catch(function (e) {
            reject(e);
          });
        });
      }
    }, {
      key: "cleanData",
      value: function cleanData() {
        var _this14 = this;

        var msgSn = this.getMsgSn();
        return new Promise(function (resolve, reject) {
          var data = {
            'method': 'clean_data',
            'id': msgSn,
            'params': {
              "delay_times": 20
            }
          };

          _this14.handleCommond(data).then(function (res) {
            resolve(res);
          }).catch(function (e) {
            reject(e);
          });
        });
      }
    }, {
      key: "handleCommond",
      value: function handleCommond(data) {
        var _this15 = this;

        return new Promise(function (resolve, reject) {
          _this15.queue.push({
            data: data,
            resolve: resolve,
            reject: reject
          });

          _this15.processQueue();
        });
      }
    }, {
      key: "processQueue",
      value: function processQueue() {
        var _this16 = this;

        if (this.isProcessing || this.queue.length === 0) {
          return;
        }

        this.isProcessing = true;

        var _this$queue$shift = this.queue.shift(),
            data = _this$queue$shift.data,
            resolve = _this$queue$shift.resolve,
            reject = _this$queue$shift.reject;

        try {
          var jsonString = JSON.stringify(data);

          var binary = _buffer.Buffer.from(jsonString, 'utf-8');

          this.encryptBinary(binary).then(function (tempBinary) {
            var htPackage = new _ht_package.default();
            htPackage.setChannelId(_this16.getChannleID(false));
            htPackage.setMsgSn(data.id);
            htPackage.setArcMsgSn(data.id);
            htPackage.setEncryptType(_this16.encryptType);
            htPackage.setMsgBody(tempBinary);
            var frame = htPackage.build(false);
            var isTimeout = false;
            var timer = setTimeout(function () {
              isTimeout = true;
              _this16.isProcessing = false;
              reject('command response timeout');

              _this16.processQueue();
            }, _this16.requestTimeout);

            _this16.listener = function (Receiveddata) {
              if (isTimeout) return;
              _this16.listener = null;
              clearTimeout(timer);

              _this16.decryptBinary(Receiveddata.body).then(function (res) {
                var jsonString = _buffer.Buffer.from(res).toString('utf-8');

                var jsonData = JSON.parse(jsonString);
                var checkIdResult = true;

                if (data && jsonData && data.id && jsonData.id) {
                  if (data.id != jsonData.id) {
                    checkIdResult = false;
                  }
                }

                if (checkIdResult) {
                  resolve(jsonData);
                  _this16.isProcessing = false;

                  _this16.processQueue();
                } else {
                  _DynamicUtil.default.trackEvent(_consts.BIGDATA_KEYS.APP_CONN_EVT_CHECKIDFAIL, {});

                  reject("check id fail");
                  _this16.isProcessing = false;

                  _this16.processQueue();
                }
              }).catch(function (error) {
                reject(error);
                _this16.isProcessing = false;

                _this16.processQueue();
              });
            };

            var dataString = _this16.byteArrayToHexString(frame);

            _miot.ClassicBluetooth.write(dataString).then(function (res) {}).catch(function (error) {
              reject(error);
              _this16.isProcessing = false;

              _this16.processQueue();
            });
          }).catch(function (err) {
            reject(err);
            _this16.isProcessing = false;

            _this16.processQueue();
          });
        } catch (e) {
          reject(e);
          this.isProcessing = false;
          this.processQueue();
        }
      }
    }, {
      key: "sayHello",
      value: function sayHello() {
        var _this17 = this;

        _logUtils.default.reportLog("sayHello this.isHandShaking = " + this.isHandShaking + " this.encryptType = " + this.encryptType);

        if (this.isHandShaking) {
          return;
        } else {
          this.start();
        }

        var msgSn = this.getMsgSn();
        var htPackage = new _ht_package.default();
        htPackage.setChannelId(_ht_model.CHANNEL_AUTH_ID);
        htPackage.setInteractive(_ht_model.INTERACTIVE_CLIENT_HELLO_DH);
        htPackage.setMsgSn(msgSn);
        htPackage.setArcMsgSn(msgSn);
        htPackage.setEncryptType(_ht_model.ENCRYPT_ECB);
        htPackage.setEncoding(_ht_model.ENCODING_JSON);
        htPackage.setMsgBody('hello');
        var frame = htPackage.build();
        var dataString = this.byteArrayToHexString(frame);

        _miot.ClassicBluetooth.write(dataString).then(function (res) {}).catch(function (error) {
          _this17.handleFail(1);
        });
      }
    }, {
      key: "setEncryptType",
      value: function setEncryptType(encryptType) {
        this.encryptType = encryptType;
      }
    }, {
      key: "getEncryptType",
      value: function getEncryptType() {
        return this.encryptType;
      }
    }, {
      key: "handleConfirm",
      value: function handleConfirm(msg) {
        if (msg.channelId == _ht_model.CHANNEL_AUTH_ID && msg.interactive == _ht_model.INTERACTIVE_SERVER_CONFIRM_DH) {
          var newBody = _ht_utils.default.handleTailZero(msg.body);

          var confirmMeg = _ht_utils.default.byteArrayToString(newBody);

          if (confirmMeg === HANDSHAKE_CONFIRM_MESSAGE.HANDSHAKE_CONFIRM_MESSAGE_OK) {
            _logUtils.default.reportLog("confirmMeg OK");

            this.currentHandShakeStep = HANDSHAKE_STEP.STEP_CONFIRM;
            this.end();

            _DynamicUtil.default.trackEvent(_consts.BIGDATA_KEYS.APP_CONN_BT_EVT_HANDSHAKESUCCESS, {});

            this.classicBlueConnectionState = _consts.MINT_BLUETOOTH_STATE.AUTH;

            _DynamicUtil.default.trackEvent(_consts.BIGDATA_KEYS.APP_CONN_BT_EVT_CONNSTATECHANGE, {
              state: _consts.MINT_BLUETOOTH_STATE.AUTH
            });

            this.callback && this.callback.onConnectStateChanged(_consts.MINT_BLUETOOTH_STATE.AUTH);
          } else if (confirmMeg === HANDSHAKE_CONFIRM_MESSAGE.HANDSHAKE_CONFIRM_MESSAGE_NOK) {
            _logUtils.default.reportLog("confirmMeg NOK");

            this.handleFail(2);
          } else {
            _logUtils.default.reportLog("confirmMeg ERROR");

            this.handleFail(3);
          }
        } else {
          this.handleFail(4);
        }
      }
    }, {
      key: "handleInfo",
      value: function handleInfo(msg) {
        var _this18 = this;

        if (msg.channelId == _ht_model.CHANNEL_AUTH_ID && msg.interactive == _ht_model.INTERACTIVE_SERVER_HELLO_DH) {
          if (!msg.body || msg.body.length != 36) {
            this.handleFail(5);
            return;
          }

          var G = msg.body.slice(0, 4);
          var P = msg.body.slice(4, 20);
          var RA = msg.body.slice(20, 36);

          var G_trimmed = _ht_utils.default.handleTailZero(G);

          var g_string = _ht_utils.default.byteArrayToString(G_trimmed);

          var p_string = _ht_utils.default.byteArrayToString(P);

          var ra_string = _ht_utils.default.byteArrayToString(RA);

          var g_bigInteger = _ht_utils.default.byteArrayToBigInteger(G_trimmed);

          var p_bigInteger = _ht_utils.default.byteArrayToBigInteger(P);

          var b_bigInteger = _ht_utils.default.nextRandomBigInteger(p_bigInteger.subtract((0, _bigInteger.default)(2)));

          var ra_bigInteger = _ht_utils.default.byteArrayToBigInteger(RA);

          var rb_bigInteger = g_bigInteger.modPow(b_bigInteger, p_bigInteger);

          var rb_bytes = _ht_utils.default.bigIntegerToBytes(rb_bigInteger).toJSON().data;

          if (rb_bytes.length < 16) {
            rb_bytes = _ht_utils.default.arrayPacking16(rb_bytes);
          } else {}

          var k_bigInteger = ra_bigInteger.modPow(b_bigInteger, p_bigInteger);

          var k_bytes = _ht_utils.default.bigIntegerToBytes(k_bigInteger).toJSON().data;

          if (k_bytes.length < 16) {
            k_bytes = _ht_utils.default.arrayPacking16AtEnd(k_bytes);
          } else {}

          this.key = k_bytes;

          var p_en_bytes = _ht_crypto.default.encryptECB(k_bytes, P);

          var p_en_bytes16 = new Uint8Array(16);

          for (var i = 0; i < p_en_bytes16.length; i++) {
            p_en_bytes16[i] = p_en_bytes[i];
          }

          var mesbody = new Uint8Array(32);
          mesbody.set(rb_bytes, 0);
          mesbody.set(p_en_bytes16, 16);
          var msgSn = this.getMsgSn();
          var htPackage = new _ht_package.default();
          htPackage.setChannelId(_ht_model.CHANNEL_AUTH_ID);
          htPackage.setInteractive(_ht_model.INTERACTIVE_CLIENT_CONFIRM_DH);
          htPackage.setMsgSn(msgSn);
          htPackage.setArcMsgSn(msgSn);
          htPackage.setEncryptType(_ht_model.ENCRYPT_ECB);
          htPackage.setEncoding(_ht_model.ENCODING_JSON);
          htPackage.setMsgBody(mesbody);
          var frame = htPackage.build();
          var dataString = this.byteArrayToHexString(frame);
          this.currentHandShakeStep = HANDSHAKE_STEP.STEP_CONFIRM;

          _miot.ClassicBluetooth.write(dataString).then(function (res) {}).catch(function (error) {
            _this18.handleFail(6);
          });
        } else {
          this.handleFail(7);
        }
      }
    }, {
      key: "handleFail",
      value: function handleFail(code) {
        _DynamicUtil.default.trackEvent(_consts.BIGDATA_KEYS.APP_CONN_BT_EVT_HANDSHAKEFAIL, {
          code: code
        });

        _logUtils.default.reportLog("handleFail");

        if (this.failedTime < MAX_FAIL_TIMES_LIMIT) {
          this.failedTime++;
          this.end();
          this.sayHello();
        } else {
          this.end();
          this.failedTime = 0;
          this.classicBlueConnectionState = _consts.MINT_BLUETOOTH_STATE.AUTH_FAILED;

          _DynamicUtil.default.trackEvent(_consts.BIGDATA_KEYS.APP_CONN_BT_EVT_CONNSTATECHANGE, {
            state: _consts.MINT_BLUETOOTH_STATE.AUTH_FAILED
          });

          this.callback && this.callback.onConnectStateChanged(_consts.MINT_BLUETOOTH_STATE.AUTH_FAILED);
          this.disconnect();

          if (_reactNative.Platform.OS == 'ios') {
            this.connect();
          }
        }
      }
    }, {
      key: "start",
      value: function start() {
        var _this19 = this;

        _DynamicUtil.default.trackEvent(_consts.BIGDATA_KEYS.APP_CONN_BT_EVT_HANDSHAKESTART, {});

        this.currentHandShakeStep = HANDSHAKE_STEP.STEP_RECEIVE_INFO;
        this.isHandShaking = true;
        this.timer && clearTimeout(this.timer);
        this.timer = setTimeout(function () {
          _this19.handleFail(8);
        }, 3000);
      }
    }, {
      key: "end",
      value: function end() {
        this.isHandShaking = false;
        this.timer && clearTimeout(this.timer);
      }
    }, {
      key: "decryptBinary",
      value: function decryptBinary(binary) {
        var _this20 = this;

        return new Promise(function (resolve, reject) {
          if (_this20.encryptType == _ht_model.ENCRYPT_MIJIA) {
            var tempResult = _this20.byteArrayToHexString(binary);

            _miot.Device.getBluetoothLE().securityLock.decryptMessageWithToken(tempResult).then(function (res) {
              var tempBinary2 = _this20.hexStringToByteArray(res.result);

              resolve(tempBinary2);
            }).catch(function (error) {
              reject(error);
            });
          } else if (_this20.encryptType == _ht_model.ENCRYPT_NO) {
            resolve(binary);
          } else if (_this20.encryptType == _ht_model.ENCRYPT_ECB) {
            if (_this20.key) {
              resolve(_ht_crypto.default.decryptECB(_this20.key, binary, true));
            } else {
              reject("key is null");
            }
          } else {
            resolve(binary);
          }
        });
      }
    }, {
      key: "encryptBinary",
      value: function encryptBinary(binary) {
        var _this21 = this;

        return new Promise(function (resolve, reject) {
          if (_this21.encryptType == _ht_model.ENCRYPT_MIJIA) {
            var tempHex = _this21.byteArrayToHexString(binary);

            if (_this21.isTestTime) {
              _this21.currentTimeNode = Date.now();
              _this21.time2_1 += _this21.currentTimeNode - _this21.lastTimeNode;
              _this21.lastTimeNode = _this21.currentTimeNode;
            }

            _miot.Device.getBluetoothLE().securityLock.encryptMessageWithToken(tempHex).then(function (res) {
              if (_this21.isTestTime) {
                _this21.currentTimeNode = Date.now();
                _this21.time2_2 += _this21.currentTimeNode - _this21.lastTimeNode;
                _this21.lastTimeNode = _this21.currentTimeNode;
              }

              var tempBinary = _this21.hexStringToByteArray(res.result);

              if (_this21.isTestTime) {
                _this21.currentTimeNode = Date.now();
                _this21.time2_3 += _this21.currentTimeNode - _this21.lastTimeNode;
                _this21.lastTimeNode = _this21.currentTimeNode;
              }

              resolve(tempBinary);
            }).catch(function (err) {
              reject(err);
            });
          } else if (_this21.encryptType == _ht_model.ENCRYPT_NO) {
            resolve(binary);
          } else if (_this21.encryptType == _ht_model.ENCRYPT_ECB) {
            if (_this21.key) {
              resolve(_ht_crypto.default.encryptECB(_this21.key, binary));
            } else {
              reject('encrypt key is not exist');
            }
          } else {
            resolve(binary);
          }
        });
      }
    }, {
      key: "getMsgSn",
      value: function getMsgSn() {
        this.msgSn++;
        return this.msgSn;
      }
    }, {
      key: "create",
      value: function create() {
        return new Promise(function (resolve, reject) {
          _miot.ClassicBluetooth.create().then(function (res) {
            resolve(res);
          }).catch(function (error) {
            reject(error);
          });
        });
      }
    }, {
      key: "connect",
      value: function connect() {
        var _this22 = this;

        var macAddress = _miot.Device.mac;
        var transportUUID = '00001101-0000-1000-8000-00805f9b34fb';
        return new Promise(function (resolve, reject) {
          _miot.ClassicBluetooth.connectSocket(macAddress, transportUUID).then(function (res) {
            if (_reactNative.Platform.OS == 'ios') {
              _this22.handleBlueConnectionStateChange(_consts.MINT_BLUETOOTH_STATE.CONNECTED);
            }

            resolve(res);
          }).catch(function (error) {
            if (_reactNative.Platform.OS == 'ios') {
              _this22.handleBlueConnectionStateChange(_consts.MINT_BLUETOOTH_STATE.DISCONNECTED);
            }

            reject(error);
          });
        });
      }
    }, {
      key: "disconnect",
      value: function disconnect() {
        return new Promise(function (resolve, reject) {
          _miot.ClassicBluetooth.disconnectSocket().then(function (res) {
            resolve(res);
          }).catch(function (error) {
            reject(error);
          });
        });
      }
    }, {
      key: "destroy",
      value: function destroy() {
        return new Promise(function (resolve, reject) {
          _miot.ClassicBluetooth.destroy().then(function (res) {
            resolve(res);
          }).catch(function (error) {
            reject(error);
          });
        });
      }
    }, {
      key: "getConnectionState",
      value: function getConnectionState() {
        return this.classicBlueConnectionState;
      }
    }, {
      key: "getIsSendingFile",
      value: function getIsSendingFile() {
        return this.isSendFile;
      }
    }, {
      key: "hexStringToByteArray",
      value: function hexStringToByteArray(hexString) {
        if (hexString.length % 2 !== 0) {
          _logUtils.default.reportLog("hexStringToByteArray Hex string must have an even length hexString.length = " + hexString.length);

          throw new Error("Hex string must have an even length");
        }

        var byteArray = new Uint8Array(hexString.length / 2);

        for (var i = 0; i < hexString.length; i += 2) {
          byteArray[i / 2] = parseInt(hexString.substr(i, 2), 16);
        }

        return byteArray;
      }
    }, {
      key: "byteArrayToHexString",
      value: function byteArrayToHexString(byteArray) {
        return Array.from(byteArray, function (byte) {
          return byte.toString(16).padStart(2, '0');
        }).join('');
      }
    }, {
      key: "int2Bytes",
      value: function int2Bytes(val) {
        var bytes = new Uint8Array(4);
        bytes[0] = val & 0xFF;
        bytes[1] = val >> 8 & 0xFF;
        bytes[2] = val >> 16 & 0xFF;
        bytes[3] = val >> 24 & 0xFF;
        return bytes;
      }
    }, {
      key: "addBytes",
      value: function addBytes(data1, data2) {
        var data3 = new Uint8Array(data1.length + data2.length);
        data3.set(data1, 0);
        data3.set(data2, data1.length);
        return data3;
      }
    }], [{
      key: "getInstance",
      value: function getInstance() {
        if (!HTClassicBluetooth.instance) {
          HTClassicBluetooth.instance = new HTClassicBluetooth();
        }

        return HTClassicBluetooth.instance;
      }
    }]);
    return HTClassicBluetooth;
  }();

  exports.default = HTClassicBluetooth;
  HTClassicBluetooth.instance = null;
},12407,[14317,14314,14683,14329,14332,10033,12308,10207,12290,12344,12359,12365,12368,12194,12197,10074,22402,12362,12173,12371]);