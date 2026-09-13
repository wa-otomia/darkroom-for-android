__d(function (global, _$$_REQUIRE, _$$_IMPORT_DEFAULT, _$$_IMPORT_ALL, module, exports, _dependencyMap) {
  'use strict';

  var _interopRequireDefault = _$$_REQUIRE(_dependencyMap[0]);

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  exports.default = undefined;

  var _objectSpread2 = _interopRequireDefault(_$$_REQUIRE(_dependencyMap[1]));

  var _classCallCheck2 = _interopRequireDefault(_$$_REQUIRE(_dependencyMap[2]));

  var _createClass2 = _interopRequireDefault(_$$_REQUIRE(_dependencyMap[3]));

  var _miot = _$$_REQUIRE(_dependencyMap[4]);

  var _spec = _interopRequireDefault(_$$_REQUIRE(_dependencyMap[5]));

  var _reactNative = _$$_REQUIRE(_dependencyMap[6]);

  var _consts = _$$_REQUIRE(_dependencyMap[7]);

  var _farAndNearFieldHelper = _interopRequireDefault(_$$_REQUIRE(_dependencyMap[8]));

  var _fileHelper = _interopRequireDefault(_$$_REQUIRE(_dependencyMap[9]));

  var _reactNativeRootToast = _interopRequireDefault(_$$_REQUIRE(_dependencyMap[10]));

  var _socketConnectManger = _interopRequireDefault(_$$_REQUIRE(_dependencyMap[11]));

  var _logUtils = _interopRequireDefault(_$$_REQUIRE(_dependencyMap[12]));

  var _JsonUtil = _interopRequireDefault(_$$_REQUIRE(_dependencyMap[13]));

  var _deviceDataFormat = _interopRequireDefault(_$$_REQUIRE(_dependencyMap[14]));

  var _HTClassicBluetooth = _interopRequireDefault(_$$_REQUIRE(_dependencyMap[15]));

  var _resources = _interopRequireDefault(_$$_REQUIRE(_dependencyMap[16]));

  var _ThirdPartyHeadFile = _$$_REQUIRE(_dependencyMap[17]);

  var _ht_model = _$$_REQUIRE(_dependencyMap[18]);

  var _deviceConfig = _interopRequireDefault(_$$_REQUIRE(_dependencyMap[19]));

  var _DynamicUtil = _interopRequireDefault(_$$_REQUIRE(_dependencyMap[20]));

  var instance = null;
  var _did = _miot.Device.did;
  var _total_job = 0;
  var PrinterStateSubscription = null;

  var BluetoothPrintManager = function () {
    function BluetoothPrintManager() {
      var _this = this;

      (0, _classCallCheck2.default)(this, BluetoothPrintManager);

      this._handleAppStateChange = function (nextAppState) {
        if (nextAppState != null && nextAppState === 'active') {
          if (_this.flage) {
            _this.log('PrintManager --> App has come to the foreground!');

            _DynamicUtil.default.trackEvent(_consts.BIGDATA_KEYS.APP_PERF_USG_BACKGROUND, {
              type: 0
            });

            _this.initPolingStatus();

            if (_this.activePrinterStaus == true) {
              _this.queryDeviceState();
            }
          }

          _this.flage = false;

          _this.updateHoldLocalToDisk();

          if (_miot.Host.isIOS) {
            _HTClassicBluetooth.default.getInstance().connect().then(function (result) {
              _this.log("BluetoothPrintManager --> init connect -- success" + JSON.stringify(result));
            }).catch(function (failure) {
              _this.log("BluetoothPrintManager --> init connect -- failure" + JSON.stringify(failure));

              reject(failure);
            });
          }
        } else if (nextAppState != null && nextAppState === 'background') {
          _this.log('PrintManager --> App has come to the background!');

          _DynamicUtil.default.trackEvent(_consts.BIGDATA_KEYS.APP_PERF_USG_BACKGROUND, {
            type: 1
          });

          _this.flage = true;

          _this.updateHoldLocalToDisk();
        }
      };

      this.initPolingStatus = function () {
        _this.reportLog('BluetoothPrintManager --> initPolingStatus');

        _this.timer && clearTimeout(_this.timer);
        _this.timer = setInterval(function () {
          _this.log("BluetoothPrintManager --> initPolingStatus activePrinterStaus = " + _this.activePrinterStaus + " isSendFile = " + _HTClassicBluetooth.default.getInstance().getIsSendingFile());

          if (_this.activePrinterStaus == true) {
            _this.queryDeviceState();
          }
        }, 3000);
      };

      this.flage = false;
      this.timer = null;
      this.holdDeviceStatus = {};
      this.holdJobList = null;
      this.holdJobID = 0;
      this.activePrinterStaus = true;
      this.activeJobDetail = false;
      this.activeQueueDetail = false;
      this.holdJobState = 0;
      this.holdQueueArray = [];
      this.lastProgressing = 0;
      this.lastConnectState = _consts.MINT_BLUETOOTH_STATE.DISCONNECTED;
      this.currentJob = null;
      this.currentJobKey = _consts.MINT_CURRENT_JOB_KEY + _miot.Service.account.ID;
      this.localJobsKey = _consts.MINT_LOCAL_JOBS_KEY + _miot.Service.account.ID;
      this.errorJobsKey = _consts.MINT_ERROR_JOBS_KEY + _miot.Service.account.ID;
      this.myDeviceMacKey = _consts.MY_DEVICE_MAC_KEY;
      this.localJobs = [];
      this.errorJobs = [];
      this.allowToPrint = false;
      this.deviceInfo = null;
      this.isWaitingReverseConnect = false;
      this.reverseConnectTimer = null;
      this.reverseListener = null;
      this.server = null;
      this.startConnectTime = 0;
      this.retryConnectTimer = null;
      this.androidRetryInterval = 20000;
      this.iosRetryInterval = 30000;
      this.retryState0Time = 180000;
      this.retryState1Time = 600000;
      this.mac = null;
      this.bluetoothReadyToConnect = false;
      this.isBatteryNormal = true;

      if (!instance) {
        instance = this;
      }

      return instance;
    }

    (0, _createClass2.default)(BluetoothPrintManager, [{
      key: "init1",
      value: function init1() {
        var _this2 = this;

        this.reportLog('BluetoothPrintManager --> init1');
        this.displayState(false, _consts.MINT_BLUETOOTH_STATE.DISCONNECTED, _resources.default.getString('bt_disconnect_title'));

        _miot.Host.storage.get(_consts.MY_DEVICE_MAC_KEY).then(function (res) {
          _this2.log("\u83B7\u53D6\u5230\u4FDD\u5B58\u7684\u672C\u673AMAC =  " + res);

          _this2.mac = res;
        }).catch(function (err) {
          _this2.log("\u83B7\u53D6\u4FDD\u5B58\u7684\u672C\u673AMAC\u5931\u8D25 =  " + err);
        });
      }
    }, {
      key: "init",
      value: function init() {
        this.reportLog('BluetoothPrintManager --> init');
        this.displayState(false, _consts.MINT_BLUETOOTH_STATE.CONNECTING, _resources.default.getString('toast_bt_connecting'));
        this.connectAction();

        _reactNative.AppState.addEventListener('change', this._handleAppStateChange);

        this.activePrinterStaus = true;
        this.initLocalQueue();
        this.initPolingStatus();
      }
    }, {
      key: "connectAction",
      value: function connectAction() {
        var _this3 = this;

        this.startConnectTime = Date.now();

        _DynamicUtil.default.trackEvent(_consts.BIGDATA_KEYS.APP_CONN_BT_USG_START, {
          isFirst: true
        });

        this.connect().then(function (result) {
          _DynamicUtil.default.trackEvent(_consts.BIGDATA_KEYS.APP_CONN_BT_USG_SUCCESS, {
            isFirst: true
          });

          _this3.log("BluetoothPrintManager --> init -- success" + JSON.stringify(result));

          _this3.getDeviceInfo();
        }).catch(function (failure) {
          _DynamicUtil.default.trackEvent(_consts.BIGDATA_KEYS.APP_CONN_BT_USG_FAIL, {
            isFirst: true,
            errorMsg: JSON.stringify(failure)
          });

          _this3.log("BluetoothPrintManager --> init -- failure" + JSON.stringify(failure));

          if (_miot.Host.isIOS) {
            _DynamicUtil.default.trackEvent(_consts.BIGDATA_KEYS.APP_CONN_BT_USG_START, {
              isFirst: false
            });

            _this3.reverseConnect().then(function (result) {
              _DynamicUtil.default.trackEvent(_consts.BIGDATA_KEYS.APP_CONN_BT_USG_SUCCESS, {
                isFirst: false
              });

              _this3.log("BluetoothPrintManager --> init retryConnectTimer -- success" + JSON.stringify(result));
            }).catch(function (failure) {
              _DynamicUtil.default.trackEvent(_consts.BIGDATA_KEYS.APP_CONN_BT_USG_FAIL, {
                isFirst: false,
                errorMsg: JSON.stringify(failure)
              });

              _this3.log("BluetoothPrintManager --> init retryConnectTimer -- failure" + JSON.stringify(failure));
            });
          }
        });

        if (_miot.Host.isAndroid) {
          this.setConnectTimer();
        } else {
          _miot.Host.storage.get(_consts.MY_DEVICE_MAC_KEY).then(function (res) {
            _this3.log("\u83B7\u53D6\u5230\u4FDD\u5B58\u7684\u672C\u673AMAC =  " + res);

            _this3.mac = res;

            if (_this3.mac) {
              _this3.log("\u5F00\u59CB\u8FDE\u63A5\u91CD\u8BD5 this.mac = " + _this3.mac);

              _this3.setConnectTimer();
            } else {
              _this3.log("不开始连接重试");
            }
          }).catch(function (err) {
            _this3.log("\u83B7\u53D6\u4FDD\u5B58\u7684\u672C\u673AMAC\u5931\u8D25 =  " + err);
          });
        }
      }
    }, {
      key: "setConnectTimer",
      value: function setConnectTimer() {
        var _this4 = this;

        this.retryConnectTimer && clearInterval(this.retryConnectTimer);
        this.retryConnectTimer = setInterval(function () {
          if (_HTClassicBluetooth.default.getInstance().getConnectionState() === _consts.MINT_BLUETOOTH_STATE.AUTH) {
            _this4.log("当前已连接，取消timer");

            _this4.retryConnectTimer && clearInterval(_this4.retryConnectTimer);
          } else if (_this4.getConnectStage() == _consts.CONNECT_STAGE_RICOTTA.CONNECTING_AFTER_10_MINS) {
            _this4.log("当前已超过重试上限时间，取消timer");

            _this4.retryConnectTimer && clearInterval(_this4.retryConnectTimer);
          } else {
            if (_this4.bluetoothReadyToConnect) {
              if (_miot.Host.isIOS) {
                _DynamicUtil.default.trackEvent(_consts.BIGDATA_KEYS.APP_CONN_BT_USG_START, {
                  isFirst: false
                });

                _this4.reverseConnect().then(function (result) {
                  _DynamicUtil.default.trackEvent(_consts.BIGDATA_KEYS.APP_CONN_BT_USG_SUCCESS, {
                    isFirst: false
                  });

                  _this4.log("BluetoothPrintManager --> init retryConnectTimer -- success" + JSON.stringify(result));
                }).catch(function (failure) {
                  _DynamicUtil.default.trackEvent(_consts.BIGDATA_KEYS.APP_CONN_BT_USG_FAIL, {
                    isFirst: false,
                    errorMsg: JSON.stringify(failure)
                  });

                  _this4.log("BluetoothPrintManager --> init retryConnectTimer -- failure" + JSON.stringify(failure));
                });
              } else {
                _DynamicUtil.default.trackEvent(_consts.BIGDATA_KEYS.APP_CONN_BT_USG_START, {
                  isFirst: false
                });

                _this4.connect().then(function (result) {
                  _DynamicUtil.default.trackEvent(_consts.BIGDATA_KEYS.APP_CONN_BT_USG_SUCCESS, {
                    isFirst: false
                  });

                  _this4.log("BluetoothPrintManager --> init retryConnectTimer-- success" + JSON.stringify(result));
                }).catch(function (failure) {
                  _DynamicUtil.default.trackEvent(_consts.BIGDATA_KEYS.APP_CONN_BT_USG_FAIL, {
                    isFirst: false,
                    errorMsg: JSON.stringify(failure)
                  });

                  _this4.log("BluetoothPrintManager --> init retryConnectTimer-- failure" + JSON.stringify(failure));
                });
              }
            } else {
              _this4.log('当前APP还未可以连接1');
            }
          }
        }, _miot.Host.isIOS ? this.iosRetryInterval : this.androidRetryInterval);
      }
    }, {
      key: "getConnectStage",
      value: function getConnectStage() {
        var now = Date.now();

        if (_miot.Host.isIOS && !this.mac) {
          return _consts.CONNECT_STAGE_RICOTTA.CONNECTING_AFTER_10_MINS;
        } else if (now > this.startConnectTime) {
          if (now - this.startConnectTime < this.retryState0Time) {
            return _consts.CONNECT_STAGE_RICOTTA.CONNECTING_BEFORE_3_MINS;
          } else if (now - this.startConnectTime < this.retryState1Time) {
            return _consts.CONNECT_STAGE_RICOTTA.CONNECTING_BEFORE_10_MINS;
          } else if (now - this.startConnectTime >= this.retryState1Time) {
            return _consts.CONNECT_STAGE_RICOTTA.CONNECTING_AFTER_10_MINS;
          }
        } else {
          return _consts.CONNECT_STAGE_RICOTTA.CONNECTING_UNKNOWN;
        }
      }
    }, {
      key: "setBluetoothReady",
      value: function setBluetoothReady() {
        if (!this.bluetoothReadyToConnect) {
          this.init();
        }

        this.bluetoothReadyToConnect = true;
      }
    }, {
      key: "connectDevice",
      value: function connectDevice() {
        var _this5 = this;

        this.reportLog('BluetoothPrintManager --> connectDevice');
        this.displayState(false, _consts.MINT_BLUETOOTH_STATE.CONNECTING, _resources.default.getString('toast_bt_connecting'));
        this.connect().then(function (result) {
          _this5.log("BluetoothPrintManager --> connectDevice -- success" + JSON.stringify(result));

          _this5.getDeviceInfo();

          _this5.initPolingStatus();
        }).catch(function (failure) {
          _this5.log("BluetoothPrintManager --> connectDevice -- failure" + JSON.stringify(failure));
        });
      }
    }, {
      key: "connect",
      value: function connect() {
        var _this6 = this;

        this.reportLog('BluetoothPrintManager --> connect');
        return new Promise(function (resolve, reject) {
          _HTClassicBluetooth.default.getInstance().setCallback({
            onBondStateChanged: function onBondStateChanged(bondState) {
              _this6.log("BluetoothPrintManager --> \u76D1\u542C\u7ED1\u5B9A\u72B6\u6001:" + bondState);

              if (bondState == 12) {
                _this6.log("BluetoothPrintManager --> \u76D1\u542C\u7ED1\u5B9A\u72B6\u6001:\u5DF2\u7ED1\u5B9A");
              } else if (bondState == 11) {
                _this6.log("BluetoothPrintManager --> \u76D1\u542C\u7ED1\u5B9A\u72B6\u6001:\u7ED1\u5B9A\u4E2D");
              } else if (bondState == 10) {
                _this6.log("BluetoothPrintManager --> \u76D1\u542C\u7ED1\u5B9A\u72B6\u6001:\u672A\u7ED1\u5B9A");
              } else {
                _this6.log("BluetoothPrintManager --> \u76D1\u542C\u7ED1\u5B9A\u72B6\u6001:\u672A\u77E5\u72B6\u6001");
              }
            },
            onConnectStateChanged: function onConnectStateChanged(connectState) {
              _this6.log("BluetoothPrintManager --> \u76D1\u542C\u8FDE\u63A5\u72B6\u6001:" + connectState);

              if (connectState == _consts.MINT_BLUETOOTH_STATE.DISCONNECTED) {
                _this6.log("BluetoothPrintManager --> \u76D1\u542C\u8FDE\u63A5\u72B6\u6001:\u672A\u8FDE\u63A5 this.lastConnectState = " + _this6.lastConnectState);

                _DynamicUtil.default.trackEvent(_consts.BIGDATA_KEYS.APP_CONN_BT_USG_DISCONN, {});

                _this6.deviceInfo = null;

                if (_this6.lastConnectState != _consts.MINT_BLUETOOTH_STATE.DISCONNECTING) {
                  reject("connectState == MINT_BLUETOOTH_STATE.DISCONNECTED");
                } else {
                  _this6.log("BluetoothPrintManager --> \u4E0A\u4E00\u6B21\u72B6\u6001\u4E3A DISCONNECTING");
                }
              } else if (connectState == _consts.MINT_BLUETOOTH_STATE.CONNECTING) {
                _this6.log("BluetoothPrintManager --> \u76D1\u542C\u8FDE\u63A5\u72B6\u6001:\u8FDE\u63A5\u4E2D");
              } else if (connectState == _consts.MINT_BLUETOOTH_STATE.CONNECTED) {
                _this6.log("BluetoothPrintManager --> \u76D1\u542C\u8FDE\u63A5\u72B6\u6001:\u5DF2\u8FDE\u63A5");
              } else if (connectState == _consts.MINT_BLUETOOTH_STATE.DISCONNECTING) {
                _this6.log("BluetoothPrintManager --> \u76D1\u542C\u8FDE\u63A5\u72B6\u6001:\u53D6\u6D88\u8FDE\u63A5\u4E2D");
              } else if (connectState == _consts.MINT_BLUETOOTH_STATE.NO_STATE) {
                _this6.log("BluetoothPrintManager --> \u76D1\u542C\u8FDE\u63A5\u72B6\u6001:\u672A\u77E5");
              } else if (connectState == _consts.MINT_BLUETOOTH_STATE.AUTH) {
                _this6.log("BluetoothPrintManager --> \u76D1\u542C\u8FDE\u63A5\u72B6\u6001:\u63E1\u624B\u6210\u529F");

                _this6.retryConnectTimer && clearTimeout(_this6.retryConnectTimer);

                if (_this6.reverseListener) {
                  _this6.reverseListener.apply(null, [null]);
                }

                resolve();
              } else if (connectState == _consts.MINT_BLUETOOTH_STATE.AUTH_FAILED) {
                _this6.log("BluetoothPrintManager --> \u76D1\u542C\u8FDE\u63A5\u72B6\u6001:\u63E1\u624B\u5931\u8D25");
              } else {
                _this6.log("BluetoothPrintManager --> \u76D1\u542C\u8FDE\u63A5\u72B6\u6001:\u672A\u77E5\u72B6\u6001");
              }

              _this6.lastConnectState = connectState;
            }
          });

          _HTClassicBluetooth.default.getInstance().create().then(function (result) {
            _this6.log("BluetoothPrintManager --> init create:" + JSON.stringify(result));

            _HTClassicBluetooth.default.getInstance().connect().then(function (result) {
              _this6.log("BluetoothPrintManager --> init connect -- success" + JSON.stringify(result));
            }).catch(function (failure) {
              _this6.log("BluetoothPrintManager --> init connect -- failure" + JSON.stringify(failure));

              reject(failure);
            });
          }).catch(function (failure) {
            _this6.log("BluetoothPrintManager --> init create -- failure" + JSON.stringify(failure));

            reject(failure);
          });
        });
      }
    }, {
      key: "destory",
      value: function destory() {
        var _this7 = this;

        this.log('BluetoothPrintManager --> destory');

        _HTClassicBluetooth.default.getInstance().disconnect().then(function (result) {
          _this7.log("BluetoothPrintManager --> destory -- success" + JSON.stringify(result));
        }).catch(function (failure) {
          _this7.log("BluetoothPrintManager --> destory -- failure" + JSON.stringify(failure));
        });

        this.timer && clearTimeout(this.timer);
        this.retryConnectTimer && clearTimeout(this.retryConnectTimer);

        _reactNative.AppState.removeEventListener('change', this._handleAppStateChange);
      }
    }, {
      key: "did",
      value: function did() {
        return _miot.Device.deviceID;
      }
    }, {
      key: "enablePrinterStatus",
      value: function enablePrinterStatus() {
        this.activePrinterStaus = true;
      }
    }, {
      key: "disablePrinterStatus",
      value: function disablePrinterStatus() {
        this.activePrinterStaus = false;
      }
    }, {
      key: "enableJobDetail",
      value: function enableJobDetail() {
        this.activeJobDetail = true;
      }
    }, {
      key: "disableJobDetail",
      value: function disableJobDetail() {
        this.activeJobDetail = false;
      }
    }, {
      key: "enableQueueDetail",
      value: function enableQueueDetail() {
        this.activeQueueDetail = true;
      }
    }, {
      key: "disableQueueDetail",
      value: function disableQueueDetail() {
        this.activeQueueDetail = false;
        this.updateHoldLocalToDisk();
      }
    }, {
      key: "initLocalQueue",
      value: function initLocalQueue() {
        var _this8 = this;

        this.log("BluetoothPrintManager --> initLocalQueue");

        _miot.Host.storage.get(this.currentJobKey).then(function (result) {
          _this8.log("BluetoothPrintManager --> \u83B7\u53D6\u5F53\u524D\u4EFB\u52A1\u6570\u636E :" + JSON.stringify(result));

          _this8.currentJob = result ? result : null;
        }).catch(function (failure) {
          _this8.log("BluetoothPrintManager --> \u83B7\u53D6\u5F53\u524D\u4EFB\u52A1\u6570\u636E -- failure" + JSON.stringify(failure));
        });

        _miot.Host.storage.get(this.localJobsKey).then(function (result) {
          _this8.log("BluetoothPrintManager --> \u83B7\u53D6\u672C\u5730\u4EFB\u52A1\u6570\u636E :" + JSON.stringify(result));

          var temp = result ? result : [];
          _this8.localJobs = temp.map(function (item) {
            return (0, _objectSpread2.default)({}, item, {
              taskStatus: _consts.MINT_TASK_STATUS.MINT_TASK_STATUS_UNSTARTED
            });
          });
        }).catch(function (failure) {
          _this8.log("BluetoothPrintManager --> \u83B7\u53D6\u672C\u5730\u4EFB\u52A1\u6570\u636E -- failure" + JSON.stringify(failure));
        });

        _miot.Host.storage.get(this.errorJobsKey).then(function (result) {
          _this8.log("BluetoothPrintManager --> \u83B7\u53D6\u9519\u8BEF\u4EFB\u52A1\u6570\u636E :" + JSON.stringify(result));

          _this8.errorJobs = result ? result : [];

          _this8.checkAndDeletaOutDateJob();
        }).catch(function (failure) {
          _this8.log("BluetoothPrintManager --> \u83B7\u53D6\u9519\u8BEF\u4EFB\u52A1\u6570\u636E -- failure" + JSON.stringify(failure));
        });
      }
    }, {
      key: "checkAndDeletaOutDateJob",
      value: function checkAndDeletaOutDateJob() {
        var _this9 = this;

        this.reportLog("PrintManager --> checkAndDeletaOutDateJob");

        if (this.errorJobs) {
          this.log("PrintManager --> checkAndDeletaOutDateJob1 ------------" + JSON.stringify(this.errorJobs.length));
          var newArray1 = this.errorJobs.filter(function (job) {
            return job !== null;
          });
          this.log("PrintManager --> checkAndDeletaOutDateJob2 ------------" + JSON.stringify(newArray1.length));
          var newArray = newArray1.filter(function (jobinfo, i) {
            var sendTime = jobinfo.localId - 0;
            var nowTime = Date.now().toString() - 0;
            var date3 = nowTime - sendTime;
            var leave1 = date3 > 86400000;

            _this9.log("PrintManager --> checkAndDeletaOutDateJob3 : " + JSON.stringify(date3) + "---" + JSON.stringify(leave1));

            if (leave1) {
              return false;
            } else {
              return true;
            }
          });
          this.log("PrintManager --> checkAndDeletaOutDateJob4 ------------" + JSON.stringify(newArray.length));
          this.errorJobs = newArray;

          _miot.Host.storage.set(this.errorJobsKey, this.errorJobs);

          this.log("PrintManager --> checkAndDeletaOutDateJob6");
        }
      }
    }, {
      key: "updateHoldLocalToDisk",
      value: function updateHoldLocalToDisk() {
        this.log("PrintManager --> updateHoldLocalToDisk");

        _miot.Host.storage.set(this.localJobsKey, this.localJobs);

        _miot.Host.storage.set(this.errorJobsKey, this.errorJobs);
      }
    }, {
      key: "queryDeviceState",
      value: function queryDeviceState() {
        var _this10 = this;

        this.reportLog("BluetoothPrintManager --> getDeviceStatus HTClassicBluetooth.getInstance().getConnectionState() = " + _HTClassicBluetooth.default.getInstance().getConnectionState() + " ");

        if (_HTClassicBluetooth.default.getInstance().getConnectionState() === _consts.MINT_BLUETOOTH_STATE.AUTH) {
          this.startConnectTime = 0;

          if (_HTClassicBluetooth.default.getInstance().getIsSendingFile()) {
            this.reportLog("BluetoothPrintManager --> getDeviceStatus \u5F53\u524D\u6B63\u5728\u4F20\u8F93\u6587\u4EF6 \u4E0D\u67E5\u8BE2\u72B6\u6001");
            this.displayState(false, _HTClassicBluetooth.default.getInstance().getConnectionState(), _resources.default.getString("get_status_title"));
          } else {
            if (this.deviceInfo) {
              _HTClassicBluetooth.default.getInstance().getMixStatus().then(function (res) {
                _this10.reportLog("getMixStatus res " + JSON.stringify(res));

                var state = res.result;
                state.isSuccess = true;
                state.connectionState = _HTClassicBluetooth.default.getInstance().getConnectionState();

                _this10.handlePrinterState(state);
              }).catch(function (error) {
                _this10.reportLog("getMixStatus error " + error);

                _DynamicUtil.default.trackEvent(_consts.BIGDATA_KEYS.APP_DEVICE_REQ_EVT_MIXSTATUSFAIL, {
                  error: JSON.stringify(error)
                });

                _this10.displayState(false, _HTClassicBluetooth.default.getInstance().getConnectionState(), _resources.default.getString("get_status_title"));
              });
            } else {
              this.reportLog("当前仍未获取到设备状态，先获取设备状态");
              this.getDeviceInfo();
            }
          }
        } else if (_HTClassicBluetooth.default.getInstance().getConnectionState() === _consts.MINT_BLUETOOTH_STATE.CONNECTING) {
          this.displayState(false, _HTClassicBluetooth.default.getInstance().getConnectionState(), _resources.default.getString('toast_bt_connecting'));
        } else {
          if (this.getConnectStage() == _consts.CONNECT_STAGE_RICOTTA.CONNECTING_BEFORE_3_MINS) {
            this.log("当前状态未连接，处于3分钟内，页面展示连接中");
            this.displayState(false, _consts.MINT_BLUETOOTH_STATE.CONNECTING, _resources.default.getString('toast_bt_connecting'));
          } else if (this.getConnectStage() == _consts.CONNECT_STAGE_RICOTTA.CONNECTING_BEFORE_10_MINS) {
            this.log("当前状态未连接，处于10分钟内，页面展示连接中");
            this.displayState(false, _consts.MINT_BLUETOOTH_STATE.CONNECTING_NEED_ALERT, _resources.default.getString('toast_bt_connecting'));
          } else if (this.getConnectStage() == _consts.CONNECT_STAGE_RICOTTA.CONNECTING_AFTER_10_MINS) {
            this.log("当前状态未连接，大于10分钟，页面展示未连接");
            this.displayState(false, _HTClassicBluetooth.default.getInstance().getConnectionState(), _resources.default.getString('bt_disconnect_title'));
          } else {
            this.log("当前状态未连接，页面展示未连接");
            this.displayState(false, _HTClassicBluetooth.default.getInstance().getConnectionState(), _resources.default.getString('bt_disconnect_title'));
          }
        }
      }
    }, {
      key: "displayState",
      value: function displayState(isSuccess, connectionState, reason) {
        var code = arguments.length > 3 && arguments[3] !== undefined ? arguments[3] : 0;
        var state = {};
        state.isSuccess = false;
        state.connectionState = connectionState;
        state.reason = reason;
        state.code = code;
        this.handlePrinterState(state);
      }
    }, {
      key: "handlePrinterState",
      value: function handlePrinterState(state) {
        var _this11 = this;

        if (_JsonUtil.default.areObjectsEqualExcludingTimeLeft(this.holdDeviceStatus, state)) {
          this.log("设备状态没有变化");
        } else {
          this.log("设别状态发生了变化");

          _DynamicUtil.default.trackEvent(_consts.BIGDATA_KEYS.APP_DEVICE_EVT_MIXSTATUS, state);

          if (this.holdDeviceStatus && state && this.holdDeviceStatus.isSuccess && state.isSuccess && this.holdDeviceStatus.category != "error" && state.category == "error") {
            this.log("设备状态从非错误状态转变成了错误状态");

            if (this.currentJob && this.currentJob.print_job_id && this.currentJob.print_job_id > 0) {
              _HTClassicBluetooth.default.getInstance().getJobInfo(this.currentJob.print_job_id).then(function (result) {
                _this11.log("BluetoothPrintManager --> \u83B7\u53D6\u4EFB\u52A1\u4FE1\u606F\u6210\u529F1 = " + JSON.stringify(result));

                if (result && result.result && result.result[0]) {
                  var thisJobState = result.result[0];

                  _DynamicUtil.default.trackEvent(_consts.BIGDATA_KEYS.FW_PRINT_PROP_JOBINFO, thisJobState);
                } else {
                  _this11.log("BluetoothPrintManager->\u5F53\u524D\u4EFB\u52A1\u72B6\u6001\u5F02\u5E383");

                  _DynamicUtil.default.trackEvent(_consts.BIGDATA_KEYS.APP_PRINT_EVT_JOBINFOFAIL, {
                    currentJob: _this11.currentJob ? _this11.currentJob : {},
                    error: result ? result : {}
                  });
                }
              }).catch(function (error) {
                _this11.log("BluetoothPrintManager --> \u83B7\u53D6\u4EFB\u52A1\u5931\u8D254 = " + JSON.stringify(error));

                _DynamicUtil.default.trackEvent(_consts.BIGDATA_KEYS.APP_PRINT_EVT_JOBINFOFAIL, {
                  currentJob: _this11.currentJob ? _this11.currentJob : {},
                  error: error ? error : {}
                });
              });
            }
          }
        }

        this.holdDeviceStatus = state;
        this.notifyPrinterStatus(state);
        this.log("BluetoothPrintManager --> \u5F00\u59CB\u5904\u7406\u961F\u5217 - " + JSON.stringify(state));

        if (state.category === _consts.MINT_PRINTER_STATUS_CATEGORY.PRINTER_STATUS_CATEGORY_IDLE && !state.hasOwnProperty("job_id")) {
          this.updateQuene();
        } else if (state.category === _consts.MINT_PRINTER_STATUS_CATEGORY.PRINTER_STATUS_CATEGORY_PROCESSING) {
            this.allowToPrint = true;

            if (this.currentJob && this.currentJob.print_job_id > 0 && state.job_id && state.job_id > 0 && this.currentJob.print_job_id != state.job_id) {
              this.reportLog("\u6253\u5370\u673A\u5DF2\u7ECF\u5728\u6253\u5370\u4E0B\u4E00\u4E2A\u4EFB\u52A1\uFF0C\u4E24\u4E2A\u4EFB\u52A1\u4E4B\u95F4\u6CA1\u6709\u51FA\u73B0idle\u72B6\u6001\u3002\u9700\u8981\u66F4\u65B0\u961F\u5217\u4EFB\u52A1\u72B6\u6001");
              this.reportLog("this.currentJob = " + JSON.stringify(this.currentJob));
              this.updateQuene();
            }
          } else {
            this.log("BluetoothPrintManager --> \u5F53\u524D\u8BBE\u5907\u72B6\u6001\u4E0B\uFF0C\u4E0D\u9700\u8981\u66F4\u65B0\u961F\u5217 this.currentJob = " + JSON.stringify(this.currentJob));
          }
      }
    }, {
      key: "getDeviceInfo",
      value: function getDeviceInfo() {
        var _this12 = this;

        this.reportLog("BluetoothPrintManager --> getDeviceInfo");

        if (_HTClassicBluetooth.default.getInstance().getConnectionState() === _consts.MINT_BLUETOOTH_STATE.AUTH) {
          _HTClassicBluetooth.default.getInstance().getDeviceInfo().then(function (result) {
            _this12.reportLog("getDeviceInfo result " + JSON.stringify(result));

            if (result.method == "event.rpt_err") {
              _this12.reportLog("BluetoothPrintManager --> getMixStatus method = " + result.method);

              if (result.params && result.params.code == _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_RPT_ERROR) {
                _this12.reportLog("BluetoothPrintManager --> getMixStatus code = " + result.params.code);

                _this12.displayState(false, _HTClassicBluetooth.default.getInstance().getConnectionState(), _resources.default.getString('bt_disconnect_title'), _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_RPT_ERROR);
              }
            } else {
              _this12.deviceInfo = result.result[0];

              _this12.reportLog("getDeviceInfo deviceInfo " + JSON.stringify(_this12.deviceInfo));

              _DynamicUtil.default.trackEvent(_consts.BIGDATA_KEYS.FW_DEVICE_PROP_DEVICEINFO_COMBO, _this12.deviceInfo);

              _this12.saveMac(_this12.deviceInfo);

              _this12.checkFWVersion(_this12.deviceInfo);

              _this12.getBigData();

              _this12.initPolingStatus();
            }
          }).catch(function (error) {
            _this12.reportLog("getDeviceInfo error " + error);

            _DynamicUtil.default.trackEvent(_consts.BIGDATA_KEYS.APP_DEVICE_REQ_EVT_DEVICEINFOFAIL, {
              error: JSON.stringify(error)
            });
          });
        }
      }
    }, {
      key: "getBigData",
      value: function getBigData() {
        var _this13 = this;

        return new Promise(function (resolve, reject) {
          _HTClassicBluetooth.default.getInstance().getBigData().then(function (result) {
            _this13.reportLog("getBigData result " + JSON.stringify(result));

            _DynamicUtil.default.trackEvent(_consts.BIGDATA_KEYS.FW_DEVICE_USG_BIGDATA_COMBO, result.result[0]);

            resolve();
          }).catch(function (error) {
            _this13.reportLog("getBigData error " + error);

            reject(error);
          });
        });
      }
    }, {
      key: "saveMac",
      value: function saveMac(deviceInfo) {
        if (deviceInfo && deviceInfo.hasOwnProperty('bt-phone-mac')) {
          this.reportLog("BluetoothPrintManager --> saveMac deviceInfo['bt-phone-mac'] = " + deviceInfo['bt-phone-mac']);

          _miot.Host.storage.set(this.myDeviceMacKey, deviceInfo['bt-phone-mac']);

          this.mac = deviceInfo['bt-phone-mac'];
        }
      }
    }, {
      key: "checkFWVersion",
      value: function checkFWVersion(deviceInfo) {
        try {
          var fwVer = deviceInfo.fw_ver;
          var lastFourDigits = fwVer.split('_')[1];
          var numericValue = parseInt(lastFourDigits, 10);
          this.reportLog("\u5F53\u524D\u8BBE\u5907\u7248\u672C\u4E3A\uFF1AfwVer = " + fwVer + " numericValue = " + numericValue);
          var isGreaterOrEqualToThree = numericValue >= 3;

          if (isGreaterOrEqualToThree) {
            this.reportLog("\u5F53\u524D\u8BBE\u5907\u7248\u672C\u5927\u4E8E\u7B49\u4E8E3\uFF0C\u652F\u6301\u52A0\u5BC6");

            if (_HTClassicBluetooth.default.getInstance().getEncryptType() == _ht_model.ENCRYPT_NO) {
              this.reportLog("\u5F53\u524D\u65E0\u52A0\u5BC6\u65B9\u5F0F\uFF0C\u5F00\u59CB\u63E1\u624B");

              _HTClassicBluetooth.default.getInstance().setEncryptType(_ht_model.ENCRYPT_ECB);

              this.sayHello();
            } else {
              this.reportLog("\u5F53\u524D\u5DF2\u6709\u52A0\u5BC6\u65B9\u5F0F");
            }
          } else {
            this.reportLog("\u5F53\u524D\u8BBE\u5907\u7248\u672C\u4F4E\u4E8E3\uFF0C\u4E0D\u652F\u6301\u52A0\u5BC6");
          }
        } catch (error) {
          this.reportLog("checkFWVersion error = " + error);
        }
      }
    }, {
      key: "sayHello",
      value: function sayHello() {
        _HTClassicBluetooth.default.getInstance().sayHello();
      }
    }, {
      key: "isPrinterAvalable",
      value: function isPrinterAvalable() {
        var bluetoothConnected = this.isPrinterConnected();
        var printerIdle = this.isPrinterIdle();
        var printerQueueEmpty = this.isPrinterQueueEmpty();
        this.log("bluetoothConnected = " + bluetoothConnected + " printerIdle = " + printerIdle + " printerQueueEmpty = " + printerQueueEmpty + " ");
        return bluetoothConnected && printerIdle && printerQueueEmpty;
      }
    }, {
      key: "isPrinterIdle",
      value: function isPrinterIdle() {
        var printerIdle = this.holdDeviceStatus && (this.holdDeviceStatus.category === _consts.MINT_PRINTER_STATUS_CATEGORY.PRINTER_STATUS_CATEGORY_IDLE || this.holdDeviceStatus.category === _consts.MINT_PRINTER_STATUS_CATEGORY.PRINTER_STATUS_CATEGORY_SLEEP);
        return printerIdle;
      }
    }, {
      key: "isPrinterConnected",
      value: function isPrinterConnected() {
        return _HTClassicBluetooth.default.getInstance().getConnectionState() === _consts.MINT_BLUETOOTH_STATE.AUTH;
      }
    }, {
      key: "isPrinterLowPower",
      value: function isPrinterLowPower() {
        var printerLowPower = this.holdDeviceStatus && (this.holdDeviceStatus.battery === _consts.MINT_BATTERY.POWER_CAP_CRITICAL || this.holdDeviceStatus.battery === _consts.MINT_BATTERY.POWER_CAP_CHARGE_CRITICAL || this.holdDeviceStatus.battery === _consts.MINT_BATTERY.POWER_CAP_POWER_OFF || this.holdDeviceStatus.battery === _consts.MINT_BATTERY.POWER_CAP_CHARGE_POWER_OFF);
        return printerLowPower;
      }
    }, {
      key: "isPrinterQueueEmpty",
      value: function isPrinterQueueEmpty() {
        var hasPrintingJob = this.currentJob ? true : false;
        var hasToPrintJob = this.localJobs.length > 0;
        return !hasPrintingJob && !hasToPrintJob;
      }
    }, {
      key: "isPrinterNeedClean",
      value: function isPrinterNeedClean() {
        if (this.holdDeviceStatus) {
          if (this.holdDeviceStatus.hasOwnProperty("clean_remain")) {
            if (this.holdDeviceStatus.clean_remain == 0) {
              return true;
            } else {
              return false;
            }
          } else {
            return false;
          }
        } else {
          return false;
        }
      }
    }, {
      key: "addErrorJob",
      value: function addErrorJob(errorJob, code) {
        if (errorJob.job_type == _consts.JOB_TYPE.PRINTER_JOB_TYPE_CLEAN) {
          this.log('当前任务为清洁任务，不添加到错误任务列表');
        } else {
          _DynamicUtil.default.trackEvent(_consts.BIGDATA_KEYS.APP_PRINT_EVT_ADDTOERRQUEUE, {
            code: code
          });

          this.errorJobs.push(errorJob);
        }
      }
    }, {
      key: "updateQuene",
      value: function updateQuene() {
        var _this14 = this;

        this.log("BluetoothPrintManager --> updateQuene this.currentJob = " + JSON.stringify(this.currentJob));

        if (this.currentJob) {
          this.log("BluetoothPrintManager --> \u5B58\u5728\u5F53\u524D\u4EFB\u52A1");

          if (this.currentJob.taskStatus === _consts.MINT_TASK_STATUS.MINT_TASK_STATUS_TRANSFERRING) {
            this.log("BluetoothPrintManager --> \u5F53\u524D\u4EFB\u52A1\u72B6\u6001\u4E3A\u4F20\u8F93\u4E2D");

            if (this.currentJob) {
              this.addErrorJob(this.currentJob, 1);
            }

            this.currentJob = null;

            _miot.Host.storage.set(this.currentJobKey, this.currentJob);

            _miot.Host.storage.set(this.localJobsKey, this.localJobs);

            _miot.Host.storage.set(this.errorJobsKey, this.errorJobs);

            var thisJobState = {
              job_state: _consts.MINT_JOB_STATUS.PRINTER_JOB_STATUS_ABORTED
            };
            this.notifyPrintingJobStatus(thisJobState);
          } else {
            _HTClassicBluetooth.default.getInstance().getJobInfo(this.currentJob.print_job_id).then(function (result) {
              _this14.log("BluetoothPrintManager --> \u83B7\u53D6\u4EFB\u52A1\u4FE1\u606F\u6210\u529F = " + JSON.stringify(result));

              if (result && result.result && result.result[0]) {
                var _thisJobState = result.result[0];

                if (_thisJobState.job_state == _consts.MINT_JOB_STATUS.PRINTER_JOB_STATUS_ABORTED) {
                  _this14.log("BluetoothPrintManager --> \u5F53\u524D\u4EFB\u52A1\u72B6\u6001\u4E3Aaborted this.currentJob = " + JSON.stringify(_this14.currentJob));

                  _DynamicUtil.default.trackEvent(_consts.BIGDATA_KEYS.FW_PRINT_PROP_JOBINFO, _thisJobState);

                  if (_this14.currentJob) {
                    _this14.addErrorJob(_this14.currentJob, 2);
                  }

                  _this14.currentJob = null;

                  _miot.Host.storage.set(_this14.currentJobKey, _this14.currentJob);

                  _miot.Host.storage.set(_this14.localJobsKey, _this14.localJobs);

                  _miot.Host.storage.set(_this14.errorJobsKey, _this14.errorJobs);
                } else if (_thisJobState.job_state == _consts.MINT_JOB_STATUS.PRINTER_JOB_STATUS_FINISHED || _thisJobState.job_state == _consts.MINT_JOB_STATUS.PRINTER_JOB_STATUS_CANCELED) {
                  _this14.log("BluetoothPrintManager --> \u5F53\u524D\u4EFB\u52A1\u72B6\u6001\u4E3A" + _thisJobState.job_state + " this.currentJob = " + JSON.stringify(_this14.currentJob));

                  _DynamicUtil.default.trackEvent(_consts.BIGDATA_KEYS.FW_PRINT_PROP_JOBINFO, _thisJobState);

                  _this14.currentJob = null;

                  _miot.Host.storage.set(_this14.currentJobKey, _this14.currentJob);
                } else {
                  _this14.log("BluetoothPrintManager->\u5F53\u524D\u4EFB\u52A1\u72B6\u6001\u5F02\u5E381");

                  if (_this14.currentJob) {
                    _this14.addErrorJob(_this14.currentJob, 3);
                  }

                  _DynamicUtil.default.trackEvent(_consts.BIGDATA_KEYS.APP_PRINT_EVT_JOBINFOFAIL, {
                    currentJob: _this14.currentJob ? _this14.currentJob : {},
                    error: result ? result : {}
                  });

                  _this14.currentJob = null;

                  _miot.Host.storage.set(_this14.currentJobKey, _this14.currentJob);

                  _miot.Host.storage.set(_this14.localJobsKey, _this14.localJobs);

                  _miot.Host.storage.set(_this14.errorJobsKey, _this14.errorJobs);

                  _thisJobState = {
                    job_state: _consts.MINT_JOB_STATUS.PRINTER_JOB_STATUS_ABORTED
                  };
                }

                _this14.notifyPrintingJobStatus(_thisJobState);

                _this14.getBigData().then(function (result) {
                  _this14.log("BluetoothPrintManager --> \u83B7\u53D6bigdata\u6210\u529F = " + JSON.stringify(result));

                  _DynamicUtil.default.trackInit();
                }).catch(function (error) {
                  _this14.log("BluetoothPrintManager --> \u83B7\u53D6bigdata\u5931\u8D25 = " + JSON.stringify(error));
                });
              } else {
                _this14.log("BluetoothPrintManager->\u5F53\u524D\u4EFB\u52A1\u72B6\u6001\u5F02\u5E382");

                if (_this14.currentJob) {
                  _this14.addErrorJob(_this14.currentJob, 4);
                }

                _DynamicUtil.default.trackEvent(_consts.BIGDATA_KEYS.APP_PRINT_EVT_JOBINFOFAIL, {
                  currentJob: _this14.currentJob ? _this14.currentJob : {},
                  error: result ? result : {}
                });

                _this14.currentJob = null;

                _miot.Host.storage.set(_this14.currentJobKey, _this14.currentJob);

                _miot.Host.storage.set(_this14.localJobsKey, _this14.localJobs);

                _miot.Host.storage.set(_this14.errorJobsKey, _this14.errorJobs);

                var _thisJobState2 = {
                  job_state: _consts.MINT_JOB_STATUS.PRINTER_JOB_STATUS_ABORTED
                };

                _this14.notifyPrintingJobStatus(_thisJobState2);

                _this14.getBigData().then(function (result) {
                  _this14.log("BluetoothPrintManager --> \u83B7\u53D6bigdata\u6210\u529F = " + JSON.stringify(result));

                  _DynamicUtil.default.trackInit();
                }).catch(function (error) {
                  _this14.log("BluetoothPrintManager --> \u83B7\u53D6bigdata\u5931\u8D25 = " + JSON.stringify(error));
                });
              }
            }).catch(function (error) {
              _this14.log("BluetoothPrintManager --> \u83B7\u53D6\u4EFB\u52A1\u5931\u8D25 = " + JSON.stringify(error));

              if (_this14.currentJob) {
                _this14.addErrorJob(_this14.currentJob, 5);
              }

              _DynamicUtil.default.trackEvent(_consts.BIGDATA_KEYS.APP_PRINT_EVT_JOBINFOFAIL, {
                currentJob: _this14.currentJob ? _this14.currentJob : {},
                error: error ? error : {}
              });

              _this14.currentJob = null;

              _miot.Host.storage.set(_this14.currentJobKey, _this14.currentJob);

              _miot.Host.storage.set(_this14.localJobsKey, _this14.localJobs);

              _miot.Host.storage.set(_this14.errorJobsKey, _this14.errorJobs);

              var thisJobState = {
                job_state: _consts.MINT_JOB_STATUS.PRINTER_JOB_STATUS_ABORTED
              };

              _this14.notifyPrintingJobStatus(thisJobState);
            });
          }
        } else {
          this.log("BluetoothPrintManager --> \u4E0D\u5B58\u5728\u5F53\u524D\u4EFB\u52A1\uFF0C\u5C1D\u8BD5\u4ECE\u5F85\u6253\u5370\u4EFB\u52A1\u4E2D\u53D1\u8D77\u65B0\u4EFB\u52A1");

          if (this.allowToPrint && this.localJobs.length !== 0 && this.localJobs[0].taskStatus === _consts.MINT_TASK_STATUS.MINT_TASK_STATUS_UNSTARTED) {
            this.log("BluetoothPrintManager --> \u5F85\u6253\u5370\u4EFB\u52A1\u4E0D\u4E3A\u7A7A\uFF0C\u5C1D\u8BD5\u53D1\u8D77\u65B0\u4EFB\u52A1 this.localJobs.length = " + this.localJobs.length);
            this.preProcess();
            this.localJobs[0].taskStatus = _consts.MINT_TASK_STATUS.MINT_TASK_STATUS_CREATING;

            _DynamicUtil.default.trackEvent(_consts.BIGDATA_KEYS.APP_PRINT_EVT_CREATEJOB, this.localJobs[0]);

            _HTClassicBluetooth.default.getInstance().printJob2(this.localJobs[0].sendPath, this.localJobs[0].copies, this.localJobs[0].job_type, {
              onCreatJobSuccess: function onCreatJobSuccess(jobId) {
                _this14.log("onCreatJobSuccess:" + jobId);

                _this14.localJobs[0].taskStatus = _consts.MINT_TASK_STATUS.MINT_TASK_STATUS_TRANSFERRING;
                _this14.localJobs[0].print_job_id = jobId;
                _this14.currentJob = _this14.localJobs.shift();

                _DynamicUtil.default.trackEvent(_consts.BIGDATA_KEYS.APP_PRINT_EVT_CREATEJOBSUCCESS, _this14.currentJob);

                _miot.Host.storage.set(_this14.currentJobKey, _this14.currentJob);

                _miot.Host.storage.set(_this14.localJobsKey, _this14.localJobs);

                _this14.notifyCreateJobResult(true);
              },
              onCreatJobFailed: function onCreatJobFailed(code, error) {
                _this14.log("onCreatJobFailed:code = " + code + " error = " + error);

                _this14.allowToPrint = false;
                _this14.localJobs[0].taskStatus = _consts.MINT_TASK_STATUS.MINT_TASK_STATUS_UNSTARTED;

                var errorJob = _this14.localJobs.shift();

                var tempErrorJob = {};

                if (errorJob) {
                  _this14.addErrorJob(errorJob, 6);

                  tempErrorJob = errorJob;
                }

                tempErrorJob.code = code;
                tempErrorJob.error = error;

                _DynamicUtil.default.trackEvent(_consts.BIGDATA_KEYS.APP_PRINT_EVT_CREATEJOBFAIL, tempErrorJob);

                _miot.Host.storage.set(_this14.localJobsKey, _this14.localJobs);

                _miot.Host.storage.set(_this14.errorJobsKey, _this14.errorJobs);

                _this14.notifyCreateJobResult(false, code);
              },
              onProgress: function onProgress(send, total, jobId) {
                _this14.log("onProgress:send = " + send + " total = " + total + " jobId:" + jobId);

                _this14.notifyTransferringProcess(Math.floor(100 * send / total));
              },
              onTransferSuccess: function onTransferSuccess(jobId) {
                _this14.log("onTransferSuccess:" + jobId);

                _this14.currentJob.taskStatus = _consts.MINT_TASK_STATUS.MINT_TASK_STATUS_PRINTING;

                _miot.Host.storage.set(_this14.currentJobKey, _this14.currentJob);

                _DynamicUtil.default.trackEvent(_consts.BIGDATA_KEYS.APP_PRINT_EVT_TRANSFERJOBSUCCESS, _this14.currentJob);
              },
              onTransferFailed: function onTransferFailed(error, jobId) {
                _this14.log("onTransferFailed:" + error + " jobId:" + jobId);

                _this14.allowToPrint = false;

                if (_this14.currentJob) {
                  _this14.addErrorJob(_this14.currentJob, 7);
                }

                _DynamicUtil.default.trackEvent(_consts.BIGDATA_KEYS.APP_PRINT_EVT_TRANSFERJOBFAIL, {
                  currentJob: _this14.currentJob ? _this14.currentJob : {},
                  error: error,
                  jobId: jobId
                });

                _this14.currentJob = null;

                _miot.Host.storage.set(_this14.currentJobKey, _this14.currentJob);

                _miot.Host.storage.set(_this14.localJobsKey, _this14.localJobs);

                _miot.Host.storage.set(_this14.errorJobsKey, _this14.errorJobs);
              },
              onConfirmJobSuccess: function onConfirmJobSuccess(jobId) {
                _this14.log("onConfirmJobSuccess:" + jobId);
              },
              onConfirmJobFailed: function onConfirmJobFailed(error, jobId) {
                _this14.log("onConfirmJobFailed:" + error + " jobId:" + jobId);

                _this14.allowToPrint = false;

                if (_this14.currentJob) {
                  _this14.addErrorJob(_this14.currentJob, 8);
                }

                _this14.currentJob = null;

                _miot.Host.storage.set(_this14.currentJobKey, _this14.currentJob);

                _miot.Host.storage.set(_this14.localJobsKey, _this14.localJobs);

                _miot.Host.storage.set(_this14.errorJobsKey, _this14.errorJobs);
              },
              onFailed: function onFailed(error, jobId) {
                _this14.log("onFailed:" + error + " jobId:" + jobId);

                _this14.allowToPrint = false;

                if (_this14.currentJob) {
                  _this14.addErrorJob(_this14.currentJob, 9);
                }

                _DynamicUtil.default.trackEvent(_consts.BIGDATA_KEYS.APP_PRINT_EVT_TRANSFERJOBFAIL, {
                  currentJob: _this14.currentJob ? _this14.currentJob : {},
                  error: error,
                  jobId: jobId
                });

                _this14.currentJob = null;

                _miot.Host.storage.set(_this14.currentJobKey, _this14.currentJob);

                _miot.Host.storage.set(_this14.localJobsKey, _this14.localJobs);

                _miot.Host.storage.set(_this14.errorJobsKey, _this14.errorJobs);
              }
            });
          } else {
            this.log("BluetoothPrintManager --> \u4E0D\u6EE1\u8DB3\u6761\u4EF6\uFF0C\u4E0D\u5F00\u59CB\u521B\u5EFA\u4EFB\u52A1 this.startPrint = " + this.allowToPrint + " \n        this.localJobs = " + JSON.stringify(this.localJobs) + " this.errorJobs = " + JSON.stringify(this.errorJobs));

            if (this.localJobs[0]) {
              this.log("this.localJobs[0].taskStatus = " + this.localJobs[0].taskStatus);
            }
          }
        }
      }
    }, {
      key: "addJob",
      value: function addJob(job) {
        job.taskStatus = _consts.MINT_TASK_STATUS.MINT_TASK_STATUS_UNSTARTED;

        _DynamicUtil.default.trackEvent(_consts.BIGDATA_KEYS.APP_PRINT_EVT_ADDTOQUEUE, job);

        var newlength = this.localJobs.push(job);
        this.log("BluetoothPrintManager --> addJob newlength = " + newlength);

        _miot.Host.storage.set(this.localJobsKey, this.localJobs);
      }
    }, {
      key: "preProcess",
      value: function preProcess() {
        this.log("进行发送前的图片处理");
      }
    }, {
      key: "getCurrentJob",
      value: function getCurrentJob() {
        return this.currentJob;
      }
    }, {
      key: "getLoaclJobs",
      value: function getLoaclJobs() {
        return this.localJobs;
      }
    }, {
      key: "getErrorJobs",
      value: function getErrorJobs() {
        return this.errorJobs;
      }
    }, {
      key: "cancelCurrentJob",
      value: function cancelCurrentJob() {
        var _this15 = this;

        this.log("取消当前任务");
        return new Promise(function (resolve, reject) {
          if (!_this15.currentJob) {
            reject("no job");
          } else {
            _HTClassicBluetooth.default.getInstance().cancelJob(_this15.currentJob.print_job_id).then(function (result) {
              _this15.log("\u53D6\u6D88\u4EFB\u52A1\u6210\u529F result = " + result);

              _this15.currentJob = null;

              _miot.Host.storage.set(_this15.currentJobKey, _this15.currentJob);

              resolve(result);
            }).catch(function (e) {
              _this15.log("\u53D6\u6D88\u4EFB\u52A1\u5931\u8D25 e = " + e);

              reject(e);
            });
          }
        });
      }
    }, {
      key: "cancelLocalJob",
      value: function cancelLocalJob(item) {
        this.localJobs = this.localJobs.filter(function (job) {
          return job.localId !== item.localId;
        });

        _miot.Host.storage.set(this.localJobsKey, this.localJobs);

        var lists = {
          localJobs: this.localJobs,
          errorJobs: this.errorJobs
        };
        this.notifyPrinterQueueStatus(lists);
      }
    }, {
      key: "reprintJob",
      value: function reprintJob(item) {
        this.errorJobs = this.errorJobs.filter(function (job) {
          return job.localId !== item.localId;
        });
        this.addJob(item);

        _miot.Host.storage.set(this.localJobsKey, this.localJobs);

        _miot.Host.storage.set(this.errorJobsKey, this.errorJobs);

        var lists = {
          localJobs: this.localJobs,
          errorJobs: this.errorJobs
        };
        this.notifyPrinterQueueStatus(lists);
      }
    }, {
      key: "setAllowToPrint",
      value: function setAllowToPrint(allow) {
        this.allowToPrint = allow;
      }
    }, {
      key: "getAllowToPrint",
      value: function getAllowToPrint() {
        return this.allowToPrint;
      }
    }, {
      key: "getHoldPrinterState",
      value: function getHoldPrinterState() {
        return this.holdDeviceStatus;
      }
    }, {
      key: "getFwVersion",
      value: function getFwVersion() {
        if (this.deviceInfo) {
          return this.deviceInfo.fw_ver;
        } else {
          return null;
        }
      }
    }, {
      key: "fwUpgrade",
      value: function fwUpgrade(fwPath) {
        var _this16 = this;

        _DynamicUtil.default.trackEvent(_consts.BIGDATA_KEYS.APP_FWUPDATE_EVT_CREATEJOB, {});

        _HTClassicBluetooth.default.getInstance().fwUpgrade(fwPath, {
          onCreatJobSuccess: function onCreatJobSuccess(jobId) {
            _this16.log("onCreatJobSuccess:" + jobId);

            _DynamicUtil.default.trackEvent(_consts.BIGDATA_KEYS.APP_FWUPDATE_EVT_CREATEJOBSUCCESS, {});

            _this16.notifyFwUpgradeResult(true, 0, 0);
          },
          onCreatJobFailed: function onCreatJobFailed(code, error) {
            _this16.log("onCreatJobFailed:" + code + " error:" + error);

            _DynamicUtil.default.trackEvent(_consts.BIGDATA_KEYS.APP_FWUPDATE_EVT_CREATEJOBFAIL, {
              code: code
            });

            _this16.notifyFwUpgradeResult(false, 0, code);
          },
          onProgress: function onProgress(send, total, jobId) {
            _this16.log("onProgress:send = " + send + " total = " + total + " jobId:" + jobId);

            _this16.notifyFwUpgradeResult(true, Math.floor(100 * send / total), 0);
          },
          onTransferSuccess: function onTransferSuccess(jobId) {
            _this16.log("onTransferSuccess:" + jobId);

            _DynamicUtil.default.trackEvent(_consts.BIGDATA_KEYS.APP_FWUPDATE_EVT_TRANSFERSUCCESS, {});

            _this16.initPolingStatus();
          },
          onTransferFailed: function onTransferFailed(error, jobId) {
            _this16.log("onTransferFailed:" + error + " jobId:" + jobId);

            _this16.notifyFwUpgradeResult(false, 0, -1);
          },
          onConfirmJobSuccess: function onConfirmJobSuccess(jobId) {
            _this16.log("onConfirmJobSuccess:" + jobId);
          },
          onConfirmJobFailed: function onConfirmJobFailed(error, jobId) {
            _this16.log("onConfirmJobFailed:" + error + " jobId:" + jobId);

            _this16.notifyFwUpgradeResult(false, 0, -2);
          },
          onFailed: function onFailed(error, jobId) {
            _this16.log("onFailed:" + error + " jobId:" + jobId);

            _DynamicUtil.default.trackEvent(_consts.BIGDATA_KEYS.APP_FWUPDATE_EVT_TRANSFERFAIL, {
              error: error
            });

            _this16.notifyFwUpgradeResult(false, 0, -3);
          }
        });
      }
    }, {
      key: "cleanData",
      value: function cleanData() {
        var _this17 = this;

        return new Promise(function (resolve, reject) {
          _HTClassicBluetooth.default.getInstance().cleanData().then(function (result) {
            _this17.log("\u8BBE\u7F6E\u6E05\u6D01\u6B21\u6570\u6210\u529F result = " + JSON.stringify(result));

            resolve(result);
          }).catch(function (e) {
            _this17.log("\u8BBE\u7F6E\u6E05\u6D01\u6B21\u6570\u5931\u8D25 e = " + e);

            reject(e);
          });
        });
      }
    }, {
      key: "clean",
      value: function clean() {
        var _this18 = this;

        return new Promise(function (resolve, reject) {
          _HTClassicBluetooth.default.getInstance().clean().then(function (result) {
            _this18.log("\u5F00\u59CB\u6E05\u6D01\u6210\u529F result = " + JSON.stringify(result));

            resolve(result);
          }).catch(function (e) {
            _this18.log("\u5F00\u59CB\u6E05\u6D01\u5931\u8D25 e = " + e);

            reject(e);
          });
        });
      }
    }, {
      key: "addCleanJob",
      value: function addCleanJob(jobId) {
        var jobInfo = {
          localId: Date.now(),
          imagePath: "",
          tempPath: "",
          finalPath: '',
          sendPath: "",
          editType: 0,
          isEdited: false,
          taskStatus: _consts.MINT_TASK_STATUS.MINT_TASK_STATUS_PRINTING,
          print_job_id: jobId,
          job_state: '',
          printing_copies: 0,
          job_type: _consts.JOB_TYPE.PRINTER_JOB_TYPE_CLEAN,
          file_name: _resources.default.getString("zink_clean_title"),
          file_size: 0,
          file_type: 0,
          copies: 1
        };
        this.currentJob = jobInfo;
      }
    }, {
      key: "getMac",
      value: function getMac() {
        return this.mac;
      }
    }, {
      key: "reverseConnect",
      value: function reverseConnect() {
        var _this19 = this;

        var bt = _miot.Device.getBluetoothLE();

        return new Promise(function (resolve, reject) {
          _miot.Host.storage.get(_consts.MY_DEVICE_MAC_KEY).then(function (res) {
            _this19.log("\u83B7\u53D6\u5230\u4FDD\u5B58\u7684\u672C\u673AMAC =  " + res);

            _this19.mac = res;

            if (!_this19.mac) {
              _this19.log('当前未保存本机MAC');

              reject("no mac");
            } else {
              if (bt.isConnected) {
                _this19.log("\u5F53\u524Dble\u4E3A\u8FDE\u63A5\u72B6\u6001\uFF0C\u5148\u65AD\u5F00");

                bt.disconnect();
              }

              bt.connect(-1).then(function (res) {
                _this19.log("ble connect \u6210\u529F: " + JSON.stringify(res));

                var mint_uuid = res.uuid;
                var data = {
                  objects: [{
                    siid: 6,
                    piid: 2,
                    value: _this19.mac,
                    type: 10
                  }]
                };
                data = JSON.stringify(data);

                _this19.log("bleWrite data: " + data);

                _miot.Bluetooth.spec.setPropertiesValue(mint_uuid, data).then(function (res) {
                  _this19.log("bleWrite \u6210\u529F: " + JSON.stringify(res));

                  if (bt.isConnected) {
                    bt.disconnect();

                    _this19.reportLog('bleWrite 写成功后disconnect');
                  }

                  _this19.isWaitingReverseConnect = true;
                  _this19.reverseConnectTimer = setTimeout(function () {
                    _this19.isWaitingReverseConnect = false;
                    _this19.reverseListener = null;
                    reject('waiting reverse connect timeout');
                  }, 6000);

                  _this19.reverseListener = function () {
                    if (!_this19.isWaitingReverseConnect) {
                      _this19.log("\u7B49\u5F85\u7ECF\u5178\u84DD\u7259\u53CD\u8FDE\u5DF2\u8D85\u65F6\uFF0C\u4E0D\u518D\u5904\u7406");

                      return;
                    }

                    _this19.log("\u53CD\u8FDE\u6210\u529F");

                    _this19.reverseListener = null;
                    clearTimeout(_this19.reverseConnectTimer);
                    resolve();
                  };
                }).catch(function (err) {
                  _this19.log("bleWrite \u5931\u8D25 " + JSON.stringify(err));

                  if (bt.isConnected) {
                    bt.disconnect();

                    _this19.log('bleWrite 写失败后disconnect');
                  }

                  reject("setPropertiesValue failed " + JSON.stringify(err));
                });
              }).catch(function (error) {
                _this19.log("ble connect failed: " + JSON.stringify(error));

                if (error.code == -16) {
                  _this19.displayState(false, _HTClassicBluetooth.default.getInstance().getConnectionState(), _ThirdPartyHeadFile.language.getString("bt_disconnect_title"), _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_RPT_ERROR);
                }

                reject("ble connect failed " + JSON.stringify(error));
              });
            }
          }).catch(function (error) {
            _this19.log("\u83B7\u53D6\u5230\u4FDD\u5B58\u7684\u672C\u673AMAC " + JSON.stringify(error));

            reject("MY_DEVICE_MAC_KEY failed " + JSON.stringify(error));
          });
        });
      }
    }, {
      key: "notifyPrinterStatus",
      value: function notifyPrinterStatus(result) {
        _reactNative.DeviceEventEmitter.emit(_consts.PRINTER_STATE_CHANGE, result);
      }
    }, {
      key: "notifyPrinterQueueStatus",
      value: function notifyPrinterQueueStatus(jobList) {
        _reactNative.DeviceEventEmitter.emit(_consts.PRINTER_QUEUE_CHANGE, jobList);
      }
    }, {
      key: "notifyPrintingJobStatus",
      value: function notifyPrintingJobStatus(jobinfo) {
        _reactNative.DeviceEventEmitter.emit(_consts.PRINTERP_RINTING_JOB_STATE_CHANGE, jobinfo);
      }
    }, {
      key: "notifyCreateJobResult",
      value: function notifyCreateJobResult(isSuccess, errorCode) {
        var data = {
          isSuccess: isSuccess,
          errorCode: errorCode
        };

        _reactNative.DeviceEventEmitter.emit(_consts.MINT_EVENT_CREATE_JOB_RESULT, data);
      }
    }, {
      key: "notifyTransferringProcess",
      value: function notifyTransferringProcess(process) {
        _reactNative.DeviceEventEmitter.emit(_consts.MINT_EVENT_TRANSFERRING_PROGRESS, process);
      }
    }, {
      key: "notifyFwUpgradeResult",
      value: function notifyFwUpgradeResult(isSuccess, progress, error) {
        var data = {
          isSuccess: isSuccess,
          progress: progress,
          error: error
        };

        _reactNative.DeviceEventEmitter.emit(_consts.MINT_EVENT_FW_UPGRADE_PROGRESS, data);
      }
    }, {
      key: "getTitleFromState",
      value: function getTitleFromState(state) {
        this.log("getTitleFromState state = " + JSON.stringify(state));
        var title = '';

        if (state.category === _consts.MINT_PRINTER_STATUS_CATEGORY.PRINTER_STATUS_CATEGORY_IDLE) {
          title = _resources.default.getString("standby_title");
        } else if (state.category === _consts.MINT_PRINTER_STATUS_CATEGORY.PRINTER_STATUS_CATEGORY_PROCESSING) {
          if (state.sub_category === _consts.MINT_PRINTER_SUB_CATEGORY.PRINTER_STATUS_SUB_CATEGORY_SMART_SHEET) {
            title = _resources.default.getString("calibrate_txt");
          } else if (state.sub_category === _consts.MINT_PRINTER_SUB_CATEGORY.PRINTER_STATUS_SUB_CATEGORY_CLEANING) {
            title = _resources.default.getString("print_head_cleaning");
          } else {
            title = _resources.default.getString("printing_title");
          }
        } else if (state.category === _consts.MINT_PRINTER_STATUS_CATEGORY.PRINTER_STATUS_CATEGORY_ERROR) {
          if (_deviceConfig.default.isMint()) {
            if (state.error === _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_DECODE_ERROR) {
              title = _resources.default.getString("image_decode_title");
            } else if (state.error === _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_SYSTEM_ERROR) {
              title = _resources.default.getString("default_error_title");
            } else if (state.error === _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_PAPER_EMPTY) {
              title = _resources.default.getString("OOP_title");
            } else if (state.error === _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_PAPER_JAM) {
              title = _resources.default.getString("paper_jam_title");
            } else if (state.error === _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_PAPER_MISMATCH) {
              if (state.job_type === _consts.JOB_TYPE.PRINTER_JOB_TYPE_CLEAN) {
                title = _resources.default.getString("clean_error_title");
              } else {
                title = _resources.default.getString("zink_genuine_title");
              }
            } else if (state.error === _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_HW_ERROR) {
              title = _resources.default.getString("default_error_title");
            } else if (state.error === _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_COVER_OPEN) {
              title = _resources.default.getString("cover_open_title");
            } else if (state.error === _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_PAPER_LOAD) {
              title = _resources.default.getString("zink_paper_pick_title");
            } else if (state.error === _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_OUT_OF_MEMORY) {
              title = _resources.default.getString("memory_out_title");
            } else if (state.error === _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_NO_SMARTSHEET) {
              title = _resources.default.getString("smartsheet_load_title");
            }
          } else if (_deviceConfig.default.isRicottaType()) {
            title = this.getRicottaErrorTitle(state.error);
          }
        } else if (state.category === _consts.MINT_PRINTER_STATUS_CATEGORY.PRINTER_STATUS_CATEGORY_SLEEP) {
          title = _resources.default.getString("sleep_title");
        } else if (state.category === _consts.MINT_PRINTER_STATUS_CATEGORY.PRINTER_STATUS_CATEGORY_OFF) {
          title = _resources.default.getString("power_off_title");
        } else if (state.category === _consts.MINT_PRINTER_STATUS_CATEGORY.PRINTER_STATUS_CATEGORY_UPDATING) {
          title = _resources.default.getString("fw_title");
        }

        if (!title) {
          title = _resources.default.getString("unknown");
        }

        return title;
      }
    }, {
      key: "isCharging",
      value: function isCharging(battery) {
        if (battery == _consts.MINT_BATTERY.POWER_CAP_CHARGE_CRITICAL || battery == _consts.MINT_BATTERY.POWER_CAP_CHARGE_LOW || battery == _consts.MINT_BATTERY.POWER_CAP_CHARGE_MEDIAN || battery == _consts.MINT_BATTERY.POWER_CAP_CHARGE_HIGH || battery == _consts.MINT_BATTERY.POWER_CAP_CHARGE_FULL || battery == _consts.MINT_BATTERY.POWER_CAP_CHARGE_POWER_OFF) {
          return true;
        } else {
          return false;
        }
      }
    }, {
      key: "isUSBConnected",
      value: function isUSBConnected(sensor) {
        return (sensor & 8) != 0;
      }
    }, {
      key: "isPaperTrayColsed",
      value: function isPaperTrayColsed(sensor) {
        return (sensor & 4) != 0;
      }
    }, {
      key: "isTempError",
      value: function isTempError(temp) {
        if (temp == _consts.MINT_TEMP.MINT_TEMP_TO_LOW || temp == _consts.MINT_TEMP.MINT_TEMP_LOW || temp == _consts.MINT_TEMP.MINT_TEMP_HIGH || temp == _consts.MINT_TEMP.MINT_TEMP_TO_HIGH) {
          return true;
        } else {
          return false;
        }
      }
    }, {
      key: "isChargingError",
      value: function isChargingError(temp, battery, sensor) {
        this.log("isChargingError: temp = " + temp + " battery = " + battery + " sensor = " + sensor);
        var isUSBConnected = this.isUSBConnected(sensor);
        var isCharging = this.isCharging(battery);
        var isTempError = this.isTempError(temp);
        this.log("getNewBattery: isTempError = " + isTempError + " isUSBConnected = " + isUSBConnected + " isCharging = " + isCharging);

        if (isTempError && isUSBConnected && !isCharging) {
          return true;
        } else {
          return false;
        }
      }
    }, {
      key: "getNewBattery",
      value: function getNewBattery(temp, battery, sensor) {
        this.log("getNewBattery: temp = " + temp + " battery = " + battery + " sensor = " + sensor);
        var isUSBConnected = this.isUSBConnected(sensor);
        var isCharging = this.isCharging(battery);
        var isTempError = this.isTempError(temp);
        this.log("getNewBattery: isUSBConnected = " + isUSBConnected + " isCharging = " + isCharging);
        var newBattery = battery;

        if (isUSBConnected && !isCharging) {
          switch (battery) {
            case _consts.MINT_BATTERY.POWER_CAP_CRITICAL:
              newBattery = _consts.MINT_BATTERY.POWER_CAP_NO_CHARGE_CRITICAL;
              break;

            case _consts.MINT_BATTERY.POWER_CAP_LOW:
              newBattery = _consts.MINT_BATTERY.POWER_CAP_NO_CHARGE_LOW;
              break;

            case _consts.MINT_BATTERY.POWER_CAP_MEDIAN:
              newBattery = _consts.MINT_BATTERY.POWER_CAP_NO_CHARGE_MEDIAN;
              break;

            case _consts.MINT_BATTERY.POWER_CAP_HIGH:
              newBattery = _consts.MINT_BATTERY.POWER_CAP_NO_CHARGE_HIGH;
              break;

            case _consts.MINT_BATTERY.POWER_CAP_FULL:
              if (isTempError) {
                newBattery = _consts.MINT_BATTERY.POWER_CAP_NO_CHARGE_FULL_TEMP_ERROR;
              } else {
                newBattery = _consts.MINT_BATTERY.POWER_CAP_NO_CHARGE_FULL;
              }

              break;

            case _consts.MINT_BATTERY.POWER_CAP_POWER_OFF:
              newBattery = _consts.MINT_BATTERY.POWER_CAP_NO_CHARGE_POWER_OFF;
              break;
          }
        }

        return newBattery;
      }
    }, {
      key: "getRicottaErrorCategory",
      value: function getRicottaErrorCategory(error) {
        if (error == _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_PAPER_LOAD || error == _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_PAPER_JAM || error == _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_PAPER_LENGTH_ERROR || error == _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_PAPER_REMOVE || error == _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_PAPER_REMOVE_B || error == _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_PAPER_REMOVE_PRINTING || error == _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_PAPER_REMOVE_LOAD) {
          return _resources.default.getString("paper_error");
        } else if (error == _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_RIBBON_END || error == _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_NO_RIBBON_MARKER || error == _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_RIBBON_ERROR || error == _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_RIBBON_ERROR_2) {
          return _resources.default.getString("ribbon_error");
        } else if (error == _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_COVER_OPEN || error == _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_PRINTER_OVERHEAT || error == _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_PRINTER_OVERCOOL || error == _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_PRINTER_BATTERY_CRITICAL || error == _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_PRINTER_BATTERY_OFF) {
          return _resources.default.getString("device_error");
        } else {
          return "";
        }
      }
    }, {
      key: "getRicottaErrorTitle",
      value: function getRicottaErrorTitle(error) {
        if (error == _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_PAPER_LOAD) {
          return _resources.default.getString("ricotta_error_7103_title");
        } else if (error == _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_PAPER_JAM) {
          return _resources.default.getString("ricotta_error_7104_title");
        } else if (error == _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_PAPER_LENGTH_ERROR) {
          return _resources.default.getString("ricotta_error_7105_title");
        } else if (error == _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_PAPER_REMOVE) {
          return _resources.default.getString("ricotta_error_7110_title");
        } else if (error == _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_PAPER_REMOVE_B) {
          return _resources.default.getString("ricotta_error_7111_title");
        } else if (error == _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_PAPER_REMOVE_PRINTING) {
          return _resources.default.getString("ricotta_error_7112_title");
        } else if (error == _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_PAPER_REMOVE_LOAD) {
          return _resources.default.getString("ricotta_error_7114_title");
        } else if (error == _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_RIBBON_END) {
          return _resources.default.getString("ricotta_error_7201_title");
        } else if (error == _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_NO_RIBBON_MARKER) {
          return _resources.default.getString("ricotta_error_7204_title");
        } else if (error == _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_RIBBON_ERROR) {
          return _resources.default.getString("ricotta_error_7205_title");
        } else if (error == _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_RIBBON_ERROR_2) {
          return _resources.default.getString("ricotta_error_7208_title");
        } else if (error == _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_COVER_OPEN) {
          return _resources.default.getString("ricotta_error_7001_title");
        } else if (error == _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_PRINTER_OVERHEAT) {
          return _resources.default.getString("ricotta_error_7308_title");
        } else if (error == _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_PRINTER_OVERCOOL) {
          return _resources.default.getString("ricotta_error_7309_title");
        } else if (error == _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_PRINTER_BATTERY_CRITICAL) {
          return _resources.default.getString("ricotta_error_7310_title");
        } else if (error == _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_PRINTER_BATTERY_OFF) {
          return _resources.default.getString("ricotta_error_7311_title");
        } else {
          return "";
        }
      }
    }, {
      key: "log",
      value: function log(message) {
        _logUtils.default.reportLog(message);
      }
    }, {
      key: "reportLog",
      value: function reportLog(message) {
        _logUtils.default.reportLog(message);
      }
    }, {
      key: "getServer",
      value: function getServer() {
        this.log("getServer this.server = " + JSON.stringify(this.server));
        return this.server;
      }
    }, {
      key: "setServer",
      value: function setServer(server) {
        this.log("setServer server = " + JSON.stringify(server));
        this.server = server;
      }
    }, {
      key: "getBatteryNormal",
      value: function getBatteryNormal() {
        this.log("getBatteryNormal isBatteryNormal = " + this.isBatteryNormal);
        return this.isBatteryNormal;
      }
    }, {
      key: "setBatteryNormal",
      value: function setBatteryNormal(isNormal) {
        this.log("setBatteryNormal isNormal = " + isNormal);
        this.isBatteryNormal = isNormal;
      }
    }], [{
      key: "ShareInstance",
      value: function ShareInstance() {
        var singleton = new BluetoothPrintManager();
        return singleton;
      }
    }]);
    return BluetoothPrintManager;
  }();

  exports.default = BluetoothPrintManager;
},12311,[14314,14323,14329,14332,10074,12314,10033,12173,12335,12374,12377,12338,12194,12404,12317,12407,10077,10034,12371,12179,12197]);