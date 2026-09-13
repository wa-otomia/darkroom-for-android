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

  var _possibleConstructorReturn2 = _interopRequireDefault(_$$_REQUIRE(_dependencyMap[5]));

  var _getPrototypeOf2 = _interopRequireDefault(_$$_REQUIRE(_dependencyMap[6]));

  var _inherits2 = _interopRequireDefault(_$$_REQUIRE(_dependencyMap[7]));

  var _resources = _interopRequireWildcard(_$$_REQUIRE(_dependencyMap[8]));

  var _CommonSetting = _$$_REQUIRE(_dependencyMap[9]);

  var _ListItem = _$$_REQUIRE(_dependencyMap[10]);

  var _Separator = _interopRequireDefault(_$$_REQUIRE(_dependencyMap[11]));

  var _miot = _$$_REQUIRE(_dependencyMap[12]);

  var _NavigationBar = _interopRequireDefault(_$$_REQUIRE(_dependencyMap[13]));

  var _react = _interopRequireWildcard(_$$_REQUIRE(_dependencyMap[14]));

  var _temp = _$$_REQUIRE(_dependencyMap[15]);

  var _reactNative = _$$_REQUIRE(_dependencyMap[16]);

  var _spec = _interopRequireDefault(_$$_REQUIRE(_dependencyMap[17]));

  var _consts = _$$_REQUIRE(_dependencyMap[18]);

  var _socketConnectManger = _interopRequireDefault(_$$_REQUIRE(_dependencyMap[19]));

  var _mhuiRn = _$$_REQUIRE(_dependencyMap[20]);

  var _fileHelper = _interopRequireDefault(_$$_REQUIRE(_dependencyMap[21]));

  var _request = _interopRequireDefault(_$$_REQUIRE(_dependencyMap[22]));

  var _logUtils = _interopRequireDefault(_$$_REQUIRE(_dependencyMap[23]));

  var _JsonUtil = _interopRequireDefault(_$$_REQUIRE(_dependencyMap[24]));

  var _farAndNearFieldHelper = _interopRequireDefault(_$$_REQUIRE(_dependencyMap[25]));

  var _ht_crypto = _interopRequireDefault(_$$_REQUIRE(_dependencyMap[26]));

  var _aesJs = _interopRequireDefault(_$$_REQUIRE(_dependencyMap[27]));

  var _jsBase = _$$_REQUIRE(_dependencyMap[28]);

  var _ht_utils = _interopRequireDefault(_$$_REQUIRE(_dependencyMap[29]));

  var _deviceDataFormat = _interopRequireDefault(_$$_REQUIRE(_dependencyMap[30]));

  var _CoverLayer = _interopRequireDefault(_$$_REQUIRE(_dependencyMap[31]));

  var _HtMessageDialogV = _interopRequireDefault(_$$_REQUIRE(_dependencyMap[32]));

  var _HTClassicBluetooth = _interopRequireDefault(_$$_REQUIRE(_dependencyMap[33]));

  var _ServicesHeadFile = _$$_REQUIRE(_dependencyMap[34]);

  var _errorHandle = _interopRequireDefault(_$$_REQUIRE(_dependencyMap[35]));

  var _errorHandle2 = _interopRequireDefault(_$$_REQUIRE(_dependencyMap[36]));

  var _ComponentsHeadFile = _$$_REQUIRE(_dependencyMap[37]);

  var _ThirdPartyHeadFile = _$$_REQUIRE(_dependencyMap[38]);

  var _UtilsHeadFile = _$$_REQUIRE(_dependencyMap[39]);

  var startSystemTime = '';
  var currentTime = '';
  var tempJobId;
  var startPrintSystemTime;
  var tempTime;
  var currentPrintTime;
  var testPath = _consts.PATHS.PDF_DIR + "/test1.pdf";
  var bodyData;
  var subData;
  var lowData;
  var photoModule = _reactNative.NativeModules.HTRCTPhotoPickerModule;

  var bt = _miot.Device.getBluetoothLE();

  var UUID_SERVICE = '00000100-0065-6C62-2E74-6F696D2E696D';
  var UUID_LED_READ_WRITE = '00000101-0065-6C62-2E74-6F696D2E696D';
  var UUID_BUTTON_READ_WRITE_NOTIFY = '00000102-0065-6C62-2E74-6F696D2E696D';

  var commonRequest = function (_Component) {
    (0, _inherits2.default)(commonRequest, _Component);

    function commonRequest(props, context) {
      var _this;

      (0, _classCallCheck2.default)(this, commonRequest);
      _this = (0, _possibleConstructorReturn2.default)(this, (0, _getPrototypeOf2.default)(commonRequest).call(this, props, context));
      _this.htClassicBluetooth = _HTClassicBluetooth.default.getInstance();
      _this.jobId = 1;
      _this.originImagePath = '';
      _this.imagePath = '';
      _this.fwPath = '';
      _this.mint_uuid = "";
      _this.state = {
        sliderValue: 25,
        switchValue: false,
        result: '',
        downLoadPath: '',
        jobId: 0,
        visible: false,
        dialogTitle: _ThirdPartyHeadFile.language.getString('toast_loading'),
        farAndNearMode: _consts.FAR_AND_NEAR_CONFIG.AUTO,
        encryptData: '',
        lastClickTime: 0,
        afteSaleSum: 0,
        version: 0,
        connectState: '未连接',
        btConnect: false,
        blueConnecting: false,
        scType: 2,
        chars: {}
      };
      return _this;
    }

    (0, _createClass2.default)(commonRequest, [{
      key: "navigateTo",
      value: function navigateTo(path) {
        this.props.navigation.navigate(path);
      }
    }, {
      key: "renderTitleBar",
      value: function renderTitleBar() {
        var _this2 = this;

        return _react.default.createElement(_ComponentsHeadFile.HTNavigationBar, {
          title: "经典蓝牙测试",
          hiddenLine: true,
          left: [{
            icon: _UtilsHeadFile.HTImage.nav_back,
            onPress: function onPress() {
              return _this2.props.navigation.goBack();
            }
          }]
        });
      }
    }, {
      key: "render",
      value: function render() {
        var _this3 = this;

        return _react.default.createElement(_reactNative.View, {
          style: styles.container
        }, this.renderTitleBar(), _react.default.createElement(_Separator.default, null), _react.default.createElement(_reactNative.ScrollView, {
          showsVerticalScrollIndicator: false
        }, _react.default.createElement(_reactNative.View, {
          style: [styles.blank, {
            borderTopWidth: 0
          }]
        }), _react.default.createElement(_reactNative.View, {
          style: styles.featureSetting
        }, _react.default.createElement(_reactNative.View, {
          style: styles.titleContainer
        }, _react.default.createElement(_reactNative.Text, {
          style: styles.title
        }, _resources.strings.featureSetting + " mi version:" + _miot.Host.version + " apilevel:" + _miot.Host.apiLevel + " package:" + _miot.Package.version), _react.default.createElement(_reactNative.Text, {
          style: styles.title
        }, _resources.strings.featureSetting + " Native Version:[" + this.state.version + "]  Platform:[" + _reactNative.Platform.OS + "]")), _react.default.createElement(_Separator.default, {
          style: {
            marginLeft: _resources.Styles.common.padding
          }
        }), _react.default.createElement(_ListItem.ListItem, {
          title: "\u8BBE\u5907\u57FA\u672C\u4FE1\u606F",
          subtitle: "\u8BBE\u5907\u57FA\u672C\u4FE1\u606F",
          onPress: function onPress() {
            _this3.setState({
              result: "Device.deviceID = " + _miot.Device.deviceID + "\n                  Device.model = " + _miot.Device.model + "\n                  Device.name = " + _miot.Device.name + "\n                  Device.mac = " + _miot.Device.mac + "\n                  Device.lastVersion = " + _miot.Device.lastVersion + "\n                  Device.type = " + _miot.Device.type + "\n                  Device.isOwner = " + _miot.Device.isOwner + "\n                  Device.pd_id = " + _miot.Device.pd_id + "\n                  Host.isPad = " + _miot.Host.isPad + "\n                  Host.isMiuiChannel = " + _miot.Host.isMiuiChannel + "\n                  language = " + _resources.default.getLanguage()
            });
          }
        }), _react.default.createElement(_ListItem.ListItem, {
          title: "\u8BBE\u5907\u72B6\u6001\u662F\u5426\u4E00\u81F4",
          subtitle: "\u8BBE\u5907\u72B6\u6001\u662F\u5426\u4E00\u81F4",
          onPress: function onPress() {
            var state1 = {
              "category": "idle",
              "sub_category": "init",
              "error": 0,
              "battery": 4,
              "battery-level": 94,
              "battery-temp": 2,
              "time_left": 568292,
              "clean_remain": 0,
              "sensor": 13,
              "isSuccess": true,
              "connectionState": 5
            };
            var state2 = {
              "category": "processing",
              "sub_category": "printing_M",
              "error": 0,
              "battery": 20,
              "battery-level": 96,
              "battery-temp": 2,
              "time_left": 547340,
              "clean_remain": 0,
              "job_type": 0,
              "job_id": 37,
              "prt_copies": 1,
              "sensor": 12,
              "isSuccess": true,
              "connectionState": 5
            };

            var result = _JsonUtil.default.areObjectsEqualExcludingTimeLeft(state1, state2);

            _this3.setState({
              result: "state1 state2 is same: " + result
            });
          }
        }), _react.default.createElement(_ListItem.ListItem, {
          title: "AR\u83B7\u53D6\u88AB\u5206\u4EAB\u5217\u8868",
          subtitle: "\u83B7\u53D6\u88AB\u5206\u4EAB\u5217\u8868",
          onPress: function onPress() {
            _this3.testARShareList();
          }
        }), _react.default.createElement(_ListItem.ListItem, {
          title: "AR\u5206\u4EAB\u6743\u9650",
          subtitle: "\u5206\u4EAB\u6743\u9650",
          onPress: function onPress() {
            _this3.testARShare(true);
          }
        }), _react.default.createElement(_ListItem.ListItem, {
          title: "AR\u53D6\u6D88\u5206\u4EAB\u6743\u9650",
          subtitle: "\u53D6\u6D88\u5206\u4EAB\u6743\u9650",
          onPress: function onPress() {
            _this3.testARShare(false);
          }
        }), _react.default.createElement(_ListItem.ListItem, {
          title: "\u7C73\u5BB6\u83B7\u53D6\u88AB\u5206\u4EAB\u7528\u6237\u63A5\u53E3\u9A8C\u8BC1",
          subtitle: "\u7C73\u5BB6\u83B7\u53D6\u88AB\u5206\u4EAB\u7528\u6237\u63A5\u53E3\u9A8C\u8BC1",
          onPress: function onPress() {
            _this3.testShare();
          }
        }), _react.default.createElement(_ListItem.ListItem, {
          title: "\u7C73\u5BB6\u5206\u4EAB\u8BBE\u5907\u63A5\u53E3\u9A8C\u8BC1",
          subtitle: "\u7C73\u5BB6\u5206\u4EAB\u8BBE\u5907\u63A5\u53E3\u9A8C\u8BC1",
          onPress: function onPress() {
            _this3.testShare1();
          }
        }), _react.default.createElement(_ListItem.ListItem, {
          title: "BLE",
          subtitle: "BLE",
          onPress: function onPress() {
            var isBLE = bt.isBLE;
            var mac = bt.mac;
            var UUID = bt.UUID;
            var isConnected = bt.isConnected;
            var isConnecting = bt.isConnecting;
            var doingOTA = bt.doingOTA;

            _this3.setState({
              result: "isBLE = " + isBLE + " \n                  mac = " + mac + " \n                  UUID = " + UUID + " \n                  isConnected = " + isConnected + " \n                  isConnecting = " + isConnecting + " \n                  doingOTA = " + doingOTA
            });
          }
        }), _react.default.createElement(_ListItem.ListItem, {
          title: "BLE connect",
          subtitle: "BLE connect",
          onPress: function onPress() {
            _this3.connect();
          }
        }), _react.default.createElement(_ListItem.ListItem, {
          title: "\u53D1\u8D77\u53CD\u8FDE",
          subtitle: "\u53D1\u8D77\u53CD\u8FDE\uFF0C\u53D1\u9001\u6307\u4EE4\u540E\u65AD\u8FDE",
          onPress: function onPress() {
            _this3.reverseConnect();
          }
        }), _react.default.createElement(_ListItem.ListItem, {
          title: "\u53D1\u8D77\u53CD\u8FDE",
          subtitle: "\u53D1\u8D77\u53CD\u8FDE\uFF0C\u53D1\u9001\u6307\u4EE4\u540E\u4E0D\u65AD\u8FDE",
          onPress: function onPress() {
            _this3.reverseConnect2();
          }
        }), _react.default.createElement(_ListItem.ListItem, {
          title: "\u662F\u5426\u517C\u5BB9\u4E0D\u52A0\u5BC6\u56FA\u4EF6",
          subtitle: "\u662F\u5426\u517C\u5BB9\u4E0D\u52A0\u5BC6\u56FA\u4EF6",
          onPress: function onPress() {
            _this3.changeCompatible();
          }
        }), _react.default.createElement(_ListItem.ListItem, {
          title: "BLE write",
          subtitle: "BLE write \u4E0D\u52A0\u5BC6",
          onPress: function onPress() {
            _this3.bleWrite();
          }
        }), _react.default.createElement(_ListItem.ListItem, {
          title: "BLE write",
          subtitle: "BLE write \u52A0\u5BC61",
          onPress: function onPress() {
            _this3.bleWrite1();
          }
        }), _react.default.createElement(_ListItem.ListItem, {
          title: "BLE write",
          subtitle: "BLE write \u52A0\u5BC62",
          onPress: function onPress() {
            _this3.bleWrite2();
          }
        }), _react.default.createElement(_ListItem.ListItem, {
          title: "SayHello",
          subtitle: "\u5F00\u59CB\u63E1\u624B",
          onPress: function onPress() {
            _this3.htClassicBluetooth.sayHello();
          }
        }), _react.default.createElement(_ListItem.ListItem, {
          title: "\u8BBE\u5907create",
          subtitle: "create",
          onPress: function onPress() {
            _this3.htClassicBluetooth.create().then(function (res) {
              _this3.setState({
                result: "create success " + res
              });
            }).catch(function (error) {
              _this3.setState({
                result: "create failed " + error
              });
            });
          }
        }), _react.default.createElement(_ListItem.ListItem, {
          title: "\u8BBE\u5907connect",
          subtitle: "connect",
          onPress: function onPress() {
            _this3.htClassicBluetooth.connect().then(function (res) {
              _this3.setState({
                result: "connectSocket success " + res
              });
            }).catch(function (error) {
              _this3.setState({
                result: "connectSocket failed " + error
              });
            });
          }
        }), _react.default.createElement(_ListItem.ListItem, {
          title: "\u8BBE\u5907disconnect",
          subtitle: "disconnect",
          onPress: function onPress() {
            _this3.htClassicBluetooth.disconnect().then(function (res) {
              _this3.setState({
                result: "disconnectSocket success " + res
              });
            }).catch(function (error) {
              _this3.setState({
                result: "disconnectSocket failed " + error
              });
            });
          }
        }), _react.default.createElement(_ListItem.ListItem, {
          title: "\u8BBE\u5907destroy",
          subtitle: "destroy",
          onPress: function onPress() {
            _this3.htClassicBluetooth.destroy().then(function (res) {
              _this3.setState({
                result: "destroy success " + res
              });
            }).catch(function (error) {
              _this3.setState({
                result: "destroy failed " + error
              });
            });
          }
        }), _react.default.createElement(_ListItem.ListItem, {
          title: "getMixStatus",
          subtitle: "getMixStatus",
          onPress: function onPress() {
            _this3.htClassicBluetooth.getMixStatus().then(function (res) {
              _this3.setState({
                result: "getMixStatus success " + JSON.stringify(res)
              });
            }).catch(function (error) {
              _this3.setState({
                result: "getMixStatus failed " + error
              });
            });
          }
        }), _react.default.createElement(_ListItem.ListItem, {
          title: "getDeviceInfo",
          subtitle: "getDeviceInfo",
          onPress: function onPress() {
            _this3.htClassicBluetooth.getDeviceInfo().then(function (res) {
              _this3.setState({
                result: "getDeviceInfo success " + JSON.stringify(res)
              });
            }).catch(function (error) {
              _this3.setState({
                result: "getDeviceInfo failed " + error
              });
            });
          }
        }), _react.default.createElement(_ListItem.ListItem, {
          title: "getBigData",
          subtitle: "getBigData",
          onPress: function onPress() {
            _this3.htClassicBluetooth.getBigData().then(function (res) {
              _this3.setState({
                result: "getBigData success " + JSON.stringify(res)
              });
            }).catch(function (error) {
              _this3.setState({
                result: "getBigData failed " + error
              });
            });
          }
        }), _react.default.createElement(_ListItem.ListItem, {
          title: "printJob",
          subtitle: "printJob",
          onPress: function onPress() {
            _this3.htClassicBluetooth.printJob().then(function (res) {
              _this3.setState({
                result: "printJob success " + JSON.stringify(res)
              });

              _this3.jobId = res.result.job_id;
            }).catch(function (error) {
              _this3.setState({
                result: "printJob failed " + error
              });
            });
          }
        }), _react.default.createElement(_ListItem.ListItem, {
          title: "printJob2",
          subtitle: "\u521B\u5EFA\u4EFB\u52A1\u5B8C\u6574\u6D41\u7A0B",
          onPress: function onPress() {
            _this3.htClassicBluetooth.printJob2(_this3.imagePath, 1, 0, {
              onCreatJobSuccess: function onCreatJobSuccess(jobId) {
                _this3.setState({
                  result: "onCreatJobSuccess " + JSON.stringify(jobId)
                });

                _this3.jobId = jobId;
              },
              onCreatJobFailed: function onCreatJobFailed(error, jobId) {
                _this3.setState({
                  result: _this3.state.result + " onCreatJobFailed " + error + " jobId:" + jobId
                });
              },
              onProgress: function onProgress(send, total, jobId) {
                _this3.setState({
                  result: _this3.state.result + " onProgress send = " + send + " total = " + total + " jobId:" + jobId
                });
              },
              onTransferSuccess: function onTransferSuccess(jobId) {
                _this3.setState({
                  result: _this3.state.result + " onTransferSuccess " + jobId
                });
              },
              onTransferFailed: function onTransferFailed(error, jobId) {
                _this3.setState({
                  result: _this3.state.result + " onTransferFailed " + error + " jobId:" + jobId
                });
              },
              onConfirmJobSuccess: function onConfirmJobSuccess(jobId) {
                _this3.setState({
                  result: _this3.state.result + " onConfirmJobSuccess " + jobId
                });
              },
              onConfirmJobFailed: function onConfirmJobFailed(error, jobId) {
                _this3.setState({
                  result: _this3.state.result + " onConfirmJobFailed " + error + " jobId:" + jobId
                });
              },
              onFailed: function onFailed(error, jobId) {
                _this3.setState({
                  result: _this3.state.result + " onFailed " + error + " jobId:" + jobId
                });
              }
            });
          }
        }), _react.default.createElement(_ListItem.ListItem, {
          title: "getJobInfo",
          subtitle: "getJobInfo",
          onPress: function onPress() {
            _this3.htClassicBluetooth.getJobInfo(_this3.jobId).then(function (res) {
              _this3.setState({
                result: "getJobInfo success " + JSON.stringify(res)
              });
            }).catch(function (error) {
              _this3.setState({
                result: "getJobInfo failed " + error
              });
            });
          }
        }), _react.default.createElement(_ListItem.ListItem, {
          title: "confirmJob",
          subtitle: "confirmJob",
          onPress: function onPress() {
            _this3.htClassicBluetooth.confirmJob(_this3.jobId).then(function (res) {
              _this3.setState({
                result: "confirmJob success " + JSON.stringify(res)
              });
            }).catch(function (error) {
              _this3.setState({
                result: "confirmJob failed " + error
              });
            });
          }
        }), _react.default.createElement(_ListItem.ListItem, {
          title: "cancelJob",
          subtitle: "cancelJob",
          onPress: function onPress() {
            _this3.htClassicBluetooth.cancelJob(_this3.jobId).then(function (res) {
              _this3.setState({
                result: "cancelJob success " + JSON.stringify(res)
              });
            }).catch(function (error) {
              _this3.setState({
                result: "cancelJob failed " + error
              });
            });
          }
        }), _react.default.createElement(_ListItem.ListItem, {
          title: "resumePrinter",
          subtitle: "resumePrinter",
          onPress: function onPress() {
            _this3.htClassicBluetooth.resumePrinter().then(function (res) {
              _this3.setState({
                result: "resumePrinter success " + JSON.stringify(res)
              });
            }).catch(function (error) {
              _this3.setState({
                result: "resumePrinter failed " + error
              });
            });
          }
        }), _react.default.createElement(_ListItem.ListItem, {
          title: "getFwVersion",
          subtitle: "getFwVersion",
          onPress: function onPress() {
            _this3.htClassicBluetooth.getFwVersion().then(function (res) {
              _this3.setState({
                result: "getFwVersion success " + JSON.stringify(res)
              });
            }).catch(function (error) {
              _this3.setState({
                result: "getFwVersion failed " + error
              });
            });
          }
        }), _react.default.createElement(_ListItem.ListItem, {
          title: "clean",
          subtitle: "clean",
          onPress: function onPress() {
            _this3.htClassicBluetooth.clean().then(function (res) {
              _this3.setState({
                result: "clean success " + JSON.stringify(res)
              });
            }).catch(function (error) {
              _this3.setState({
                result: "clean failed " + error
              });
            });
          }
        }), _react.default.createElement(_ListItem.ListItem, {
          title: "cleanData",
          subtitle: "cleanData",
          onPress: function onPress() {
            _this3.htClassicBluetooth.cleanData().then(function (res) {
              _this3.setState({
                result: "cleanData success " + JSON.stringify(res)
              });
            }).catch(function (error) {
              _this3.setState({
                result: "cleanData failed " + error
              });
            });
          }
        }), _react.default.createElement(_ListItem.ListItem, {
          title: "\u9009\u62E9\u56FE\u7247",
          subtitle: "\u9009\u62E9\u56FE\u7247",
          onPress: function onPress() {
            _this3._openPhotoPickerView();
          }
        }), _react.default.createElement(_ListItem.ListItem, {
          title: "\u4E0B\u8F7D\u56FA\u4EF6",
          subtitle: "\u4E0B\u8F7D\u56FA\u4EF6",
          onPress: function onPress() {
            _this3.downloadFW();
          }
        }), _react.default.createElement(_ListItem.ListItem, {
          title: "\u83B7\u53D6\u6700\u65B0\u56FA\u4EF6\u4FE1\u606F",
          subtitle: "\u83B7\u53D6\u6700\u65B0\u56FA\u4EF6\u4FE1\u606F",
          onPress: function onPress() {
            _this3.getLastFwInfo();
          }
        }), _react.default.createElement(_ListItem.ListItem, {
          title: "\u56FA\u4EF6\u5347\u7EA7",
          subtitle: "\u56FA\u4EF6\u5347\u7EA7",
          onPress: function onPress() {
            _this3.htClassicBluetooth.fwUpgrade(_this3.fwPath, {
              onCreatJobSuccess: function onCreatJobSuccess(jobId) {
                _this3.setState({
                  result: "onCreatJobSuccess " + jobId
                });

                _this3.jobId = jobId;
              },
              onCreatJobFailed: function onCreatJobFailed(error, jobId) {
                _this3.setState({
                  result: _this3.state.result + " onCreatJobFailed " + error + " jobId:" + jobId
                });
              },
              onProgress: function onProgress(send, total, jobId) {
                _this3.setState({
                  result: _this3.state.result + " onProgress send = " + send + " total = " + total + " jobId:" + jobId
                });
              },
              onTransferSuccess: function onTransferSuccess(jobId) {
                _this3.setState({
                  result: _this3.state.result + " onTransferSuccess " + jobId
                });
              },
              onTransferFailed: function onTransferFailed(error, jobId) {
                _this3.setState({
                  result: _this3.state.result + " onTransferFailed " + error + " jobId:" + jobId
                });
              },
              onConfirmJobSuccess: function onConfirmJobSuccess(jobId) {
                _this3.setState({
                  result: _this3.state.result + " onConfirmJobSuccess " + jobId
                });
              },
              onConfirmJobFailed: function onConfirmJobFailed(error, jobId) {
                _this3.setState({
                  result: _this3.state.result + " onConfirmJobFailed " + error + " jobId:" + jobId
                });
              },
              onFailed: function onFailed(error, jobId) {
                _this3.setState({
                  result: _this3.state.result + " onFailed " + error + " jobId:" + jobId
                });
              }
            });
          }
        }), _react.default.createElement(_ListItem.ListItem, {
          title: "\u5B57\u7B26\u4E32\u5230\u5B57\u8282\u6570\u7EC4",
          subtitle: "\u5B57\u7B26\u4E32\u5230\u5B57\u8282\u6570\u7EC4",
          onPress: function onPress() {
            var hexString = "48656c6c6f";

            var byteArray = _this3.htClassicBluetooth.hexStringToByteArray(hexString);

            _this3.setState({
              result: "hexString 48656c6c6f byteArray " + byteArray
            });
          }
        }), _react.default.createElement(_ListItem.ListItem, {
          title: "\u5B57\u8282\u6570\u7EC4\u5230\u5B57\u7B26\u4E32",
          subtitle: "\u5B57\u8282\u6570\u7EC4\u5230\u5B57\u7B26\u4E32",
          onPress: function onPress() {
            var byteArray = new Uint8Array([72, 101, 108, 108, 111]);

            var hexString = _this3.htClassicBluetooth.byteArrayToHexString(byteArray);

            _this3.setState({
              result: "byteArray " + byteArray + " hexString " + hexString
            });
          }
        }), _react.default.createElement(_ListItem.ListItem, {
          title: "Mint\u9519\u8BEF\u5904\u7406",
          subtitle: "PRINTER_DEVICE_ERROR_OUT_OF_MEMORY",
          onPress: function onPress() {
            var deviceStatus = {
              "category": _consts.MINT_PRINTER_STATUS_CATEGORY.PRINTER_STATUS_CATEGORY_ERROR,
              "sub_category": "printing",
              "error": _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_OUT_OF_MEMORY,
              "battery": 19,
              "battery_pct": 100,
              "clean_remain": 0,
              "job_type": 0,
              "job_id": 96,
              "prt_copies": 1,
              "isSuccess": true,
              "connectionState": 2
            };

            _errorHandle.default.handleDeviceState(_this3.props.navigation, deviceStatus, true, {
              showErrorDialog: function showErrorDialog(result) {},
              hideErrorDialog: function hideErrorDialog() {},
              fromView: function fromView(data, navigation) {}
            });
          }
        }), _react.default.createElement(_ListItem.ListItem, {
          title: "Mint\u9519\u8BEF\u5904\u7406",
          subtitle: "PRINTER_DEVICE_ERROR_SYSTEM_ERROR",
          onPress: function onPress() {
            var deviceStatus = {
              "category": _consts.MINT_PRINTER_STATUS_CATEGORY.PRINTER_STATUS_CATEGORY_ERROR,
              "sub_category": "printing",
              "error": _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_SYSTEM_ERROR,
              "battery": 19,
              "battery_pct": 100,
              "clean_remain": 0,
              "job_type": 0,
              "job_id": 96,
              "prt_copies": 1,
              "isSuccess": true,
              "connectionState": 2
            };

            _errorHandle.default.handleDeviceState(_this3.props.navigation, deviceStatus, true, {
              showErrorDialog: function showErrorDialog(result) {},
              hideErrorDialog: function hideErrorDialog() {},
              fromView: function fromView(data, navigation) {}
            });
          }
        }), _react.default.createElement(_ListItem.ListItem, {
          title: "Mint\u9519\u8BEF\u5904\u7406",
          subtitle: "PRINTER_DEVICE_ERROR_DECODE_ERROR",
          onPress: function onPress() {
            var deviceStatus = {
              "category": _consts.MINT_PRINTER_STATUS_CATEGORY.PRINTER_STATUS_CATEGORY_ERROR,
              "sub_category": "printing",
              "error": _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_DECODE_ERROR,
              "battery": 19,
              "battery_pct": 100,
              "clean_remain": 0,
              "job_type": 0,
              "job_id": 96,
              "prt_copies": 1,
              "isSuccess": true,
              "connectionState": 2
            };

            _errorHandle.default.handleDeviceState(_this3.props.navigation, deviceStatus, true, {
              showErrorDialog: function showErrorDialog(result) {},
              hideErrorDialog: function hideErrorDialog() {},
              fromView: function fromView(data, navigation) {}
            });
          }
        }), _react.default.createElement(_ListItem.ListItem, {
          title: "Mint\u9519\u8BEF\u5904\u7406",
          subtitle: "PRINTER_DEVICE_ERROR_COVER_OPEN",
          onPress: function onPress() {
            var deviceStatus = {
              "category": _consts.MINT_PRINTER_STATUS_CATEGORY.PRINTER_STATUS_CATEGORY_ERROR,
              "sub_category": "printing",
              "error": _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_COVER_OPEN,
              "battery": 19,
              "battery_pct": 100,
              "clean_remain": 0,
              "job_type": 0,
              "job_id": 96,
              "prt_copies": 1,
              "isSuccess": true,
              "connectionState": 2
            };

            _errorHandle.default.handleDeviceState(_this3.props.navigation, deviceStatus, true, {
              showErrorDialog: function showErrorDialog(result) {},
              hideErrorDialog: function hideErrorDialog() {},
              fromView: function fromView(data, navigation) {}
            });
          }
        }), _react.default.createElement(_ListItem.ListItem, {
          title: "Mint\u9519\u8BEF\u5904\u7406",
          subtitle: "PRINTER_DEVICE_ERROR_HEAD_OVER_HEAT",
          onPress: function onPress() {
            var deviceStatus = {
              "category": _consts.MINT_PRINTER_STATUS_CATEGORY.PRINTER_STATUS_CATEGORY_ERROR,
              "sub_category": "printing",
              "error": _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_HEAD_OVER_HEAT,
              "battery": 19,
              "battery_pct": 100,
              "clean_remain": 0,
              "job_type": 0,
              "job_id": 96,
              "prt_copies": 1,
              "isSuccess": true,
              "connectionState": 2
            };

            _errorHandle.default.handleDeviceState(_this3.props.navigation, deviceStatus, true, {
              showErrorDialog: function showErrorDialog(result) {},
              hideErrorDialog: function hideErrorDialog() {},
              fromView: function fromView(data, navigation) {}
            });
          }
        }), _react.default.createElement(_ListItem.ListItem, {
          title: "Mint\u9519\u8BEF\u5904\u7406",
          subtitle: "PRINTER_DEVICE_ERROR_PAPER_EMPTY",
          onPress: function onPress() {
            var deviceStatus = {
              "category": _consts.MINT_PRINTER_STATUS_CATEGORY.PRINTER_STATUS_CATEGORY_ERROR,
              "sub_category": "printing",
              "error": _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_PAPER_EMPTY,
              "battery": 19,
              "battery_pct": 100,
              "clean_remain": 0,
              "job_type": 0,
              "job_id": 96,
              "prt_copies": 1,
              "isSuccess": true,
              "connectionState": 2
            };

            _errorHandle.default.handleDeviceState(_this3.props.navigation, deviceStatus, true, {
              showErrorDialog: function showErrorDialog(result) {},
              hideErrorDialog: function hideErrorDialog() {},
              fromView: function fromView(data, navigation) {}
            });
          }
        }), _react.default.createElement(_ListItem.ListItem, {
          title: "Mint\u9519\u8BEF\u5904\u7406",
          subtitle: "PRINTER_DEVICE_ERROR_PAPER_MISMATCH",
          onPress: function onPress() {
            var deviceStatus = {
              "category": _consts.MINT_PRINTER_STATUS_CATEGORY.PRINTER_STATUS_CATEGORY_ERROR,
              "sub_category": "printing",
              "error": _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_PAPER_MISMATCH,
              "battery": 19,
              "battery_pct": 100,
              "clean_remain": 0,
              "job_type": 0,
              "job_id": 96,
              "prt_copies": 1,
              "isSuccess": true,
              "connectionState": 2
            };

            _errorHandle.default.handleDeviceState(_this3.props.navigation, deviceStatus, true, {
              showErrorDialog: function showErrorDialog(result) {},
              hideErrorDialog: function hideErrorDialog() {},
              fromView: function fromView(data, navigation) {}
            });
          }
        }), _react.default.createElement(_ListItem.ListItem, {
          title: "Mint\u9519\u8BEF\u5904\u7406",
          subtitle: "PRINTER_DEVICE_ERROR_PAPER_LOAD",
          onPress: function onPress() {
            var deviceStatus = {
              "category": _consts.MINT_PRINTER_STATUS_CATEGORY.PRINTER_STATUS_CATEGORY_ERROR,
              "sub_category": "printing",
              "error": _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_PAPER_LOAD,
              "battery": 19,
              "battery_pct": 100,
              "clean_remain": 0,
              "job_type": 0,
              "job_id": 96,
              "prt_copies": 1,
              "isSuccess": true,
              "connectionState": 2
            };

            _errorHandle.default.handleDeviceState(_this3.props.navigation, deviceStatus, true, {
              showErrorDialog: function showErrorDialog(result) {},
              hideErrorDialog: function hideErrorDialog() {},
              fromView: function fromView(data, navigation) {}
            });
          }
        }), _react.default.createElement(_ListItem.ListItem, {
          title: "Mint\u9519\u8BEF\u5904\u7406",
          subtitle: "PRINTER_DEVICE_ERROR_PAPER_JAM",
          onPress: function onPress() {
            var deviceStatus = {
              "category": _consts.MINT_PRINTER_STATUS_CATEGORY.PRINTER_STATUS_CATEGORY_ERROR,
              "sub_category": "printing",
              "error": _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_PAPER_JAM,
              "battery": 19,
              "battery_pct": 100,
              "clean_remain": 0,
              "job_type": 0,
              "job_id": 96,
              "prt_copies": 1,
              "isSuccess": true,
              "connectionState": 2
            };

            _errorHandle.default.handleDeviceState(_this3.props.navigation, deviceStatus, true, {
              showErrorDialog: function showErrorDialog(result) {},
              hideErrorDialog: function hideErrorDialog() {},
              fromView: function fromView(data, navigation) {}
            });
          }
        }), _react.default.createElement(_ListItem.ListItem, {
          title: "Mint\u9519\u8BEF\u5904\u7406",
          subtitle: "PRINTER_DEVICE_ERROR_NO_SMARTSHEET",
          onPress: function onPress() {
            var deviceStatus = {
              "category": _consts.MINT_PRINTER_STATUS_CATEGORY.PRINTER_STATUS_CATEGORY_ERROR,
              "sub_category": "printing",
              "error": _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_NO_SMARTSHEET,
              "battery": 19,
              "battery_pct": 100,
              "clean_remain": 0,
              "job_type": 0,
              "job_id": 96,
              "prt_copies": 1,
              "isSuccess": true,
              "connectionState": 2
            };

            _errorHandle.default.handleDeviceState(_this3.props.navigation, deviceStatus, true, {
              showErrorDialog: function showErrorDialog(result) {},
              hideErrorDialog: function hideErrorDialog() {},
              fromView: function fromView(data, navigation) {}
            });
          }
        }), _react.default.createElement(_ListItem.ListItem, {
          title: "Mint\u9519\u8BEF\u5904\u7406",
          subtitle: "PRINTER_DEVICE_ERROR_HW_ERROR",
          onPress: function onPress() {
            var deviceStatus = {
              "category": _consts.MINT_PRINTER_STATUS_CATEGORY.PRINTER_STATUS_CATEGORY_ERROR,
              "sub_category": "printing",
              "error": _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_HW_ERROR,
              "battery": 19,
              "battery_pct": 100,
              "clean_remain": 0,
              "job_type": 0,
              "job_id": 96,
              "prt_copies": 1,
              "isSuccess": true,
              "connectionState": 2
            };

            _errorHandle.default.handleDeviceState(_this3.props.navigation, deviceStatus, true, {
              showErrorDialog: function showErrorDialog(result) {},
              hideErrorDialog: function hideErrorDialog() {},
              fromView: function fromView(data, navigation) {}
            });
          }
        }), _react.default.createElement(_ListItem.ListItem, {
          title: "Ricotta\u9519\u8BEF\u5904\u7406",
          subtitle: "7001",
          onPress: function onPress() {
            var deviceStatus = {
              "category": _consts.MINT_PRINTER_STATUS_CATEGORY.PRINTER_STATUS_CATEGORY_ERROR,
              "sub_category": "printing",
              "error": _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_COVER_OPEN,
              "battery": 19,
              "battery_pct": 100,
              "clean_remain": 0,
              "job_type": 0,
              "job_id": 96,
              "prt_copies": 1,
              "isSuccess": true,
              "connectionState": 2
            };

            _errorHandle2.default.handleDeviceState(_this3.props.navigation, deviceStatus, true, {
              showErrorDialog: function showErrorDialog(result) {},
              hideErrorDialog: function hideErrorDialog() {},
              fromView: function fromView(data, navigation) {}
            });
          }
        }), _react.default.createElement(_ListItem.ListItem, {
          title: "Ricotta\u9519\u8BEF\u5904\u7406",
          subtitle: "7103",
          onPress: function onPress() {
            var deviceStatus = {
              "category": _consts.MINT_PRINTER_STATUS_CATEGORY.PRINTER_STATUS_CATEGORY_ERROR,
              "sub_category": "printing",
              "error": _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_PAPER_LOAD,
              "battery": 19,
              "battery_pct": 100,
              "clean_remain": 0,
              "job_type": 0,
              "job_id": 96,
              "prt_copies": 1,
              "isSuccess": true,
              "connectionState": 2
            };

            _errorHandle2.default.handleDeviceState(_this3.props.navigation, deviceStatus, true, {
              showErrorDialog: function showErrorDialog(result) {},
              hideErrorDialog: function hideErrorDialog() {},
              fromView: function fromView(data, navigation) {}
            });
          }
        }), _react.default.createElement(_ListItem.ListItem, {
          title: "Ricotta\u9519\u8BEF\u5904\u7406",
          subtitle: "7104",
          onPress: function onPress() {
            var deviceStatus = {
              "category": _consts.MINT_PRINTER_STATUS_CATEGORY.PRINTER_STATUS_CATEGORY_ERROR,
              "sub_category": "printing",
              "error": _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_PAPER_JAM,
              "battery": 19,
              "battery_pct": 100,
              "clean_remain": 0,
              "job_type": 0,
              "job_id": 96,
              "prt_copies": 1,
              "isSuccess": true,
              "connectionState": 2
            };

            _errorHandle2.default.handleDeviceState(_this3.props.navigation, deviceStatus, true, {
              showErrorDialog: function showErrorDialog(result) {},
              hideErrorDialog: function hideErrorDialog() {},
              fromView: function fromView(data, navigation) {}
            });
          }
        }), _react.default.createElement(_ListItem.ListItem, {
          title: "Ricotta\u9519\u8BEF\u5904\u7406",
          subtitle: "7105",
          onPress: function onPress() {
            var deviceStatus = {
              "category": _consts.MINT_PRINTER_STATUS_CATEGORY.PRINTER_STATUS_CATEGORY_ERROR,
              "sub_category": "printing",
              "error": _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_PAPER_LENGTH_ERROR,
              "battery": 19,
              "battery_pct": 100,
              "clean_remain": 0,
              "job_type": 0,
              "job_id": 96,
              "prt_copies": 1,
              "isSuccess": true,
              "connectionState": 2
            };

            _errorHandle2.default.handleDeviceState(_this3.props.navigation, deviceStatus, true, {
              showErrorDialog: function showErrorDialog(result) {},
              hideErrorDialog: function hideErrorDialog() {},
              fromView: function fromView(data, navigation) {}
            });
          }
        }), _react.default.createElement(_ListItem.ListItem, {
          title: "Ricotta\u9519\u8BEF\u5904\u7406",
          subtitle: "7110",
          onPress: function onPress() {
            var deviceStatus = {
              "category": _consts.MINT_PRINTER_STATUS_CATEGORY.PRINTER_STATUS_CATEGORY_ERROR,
              "sub_category": "printing",
              "error": _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_PAPER_REMOVE,
              "battery": 19,
              "battery_pct": 100,
              "clean_remain": 0,
              "job_type": 0,
              "job_id": 96,
              "prt_copies": 1,
              "isSuccess": true,
              "connectionState": 2
            };

            _errorHandle2.default.handleDeviceState(_this3.props.navigation, deviceStatus, true, {
              showErrorDialog: function showErrorDialog(result) {},
              hideErrorDialog: function hideErrorDialog() {},
              fromView: function fromView(data, navigation) {}
            });
          }
        }), _react.default.createElement(_ListItem.ListItem, {
          title: "Ricotta\u9519\u8BEF\u5904\u7406",
          subtitle: "7111",
          onPress: function onPress() {
            var deviceStatus = {
              "category": _consts.MINT_PRINTER_STATUS_CATEGORY.PRINTER_STATUS_CATEGORY_ERROR,
              "sub_category": "printing",
              "error": _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_PAPER_REMOVE_B,
              "battery": 19,
              "battery_pct": 100,
              "clean_remain": 0,
              "job_type": 0,
              "job_id": 96,
              "prt_copies": 1,
              "isSuccess": true,
              "connectionState": 2
            };

            _errorHandle2.default.handleDeviceState(_this3.props.navigation, deviceStatus, true, {
              showErrorDialog: function showErrorDialog(result) {},
              hideErrorDialog: function hideErrorDialog() {},
              fromView: function fromView(data, navigation) {}
            });
          }
        }), _react.default.createElement(_ListItem.ListItem, {
          title: "Ricotta\u9519\u8BEF\u5904\u7406",
          subtitle: "7112",
          onPress: function onPress() {
            var deviceStatus = {
              "category": _consts.MINT_PRINTER_STATUS_CATEGORY.PRINTER_STATUS_CATEGORY_ERROR,
              "sub_category": "printing",
              "error": _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_PAPER_REMOVE_PRINTING,
              "battery": 19,
              "battery_pct": 100,
              "clean_remain": 0,
              "job_type": 0,
              "job_id": 96,
              "prt_copies": 1,
              "isSuccess": true,
              "connectionState": 2
            };

            _errorHandle2.default.handleDeviceState(_this3.props.navigation, deviceStatus, true, {
              showErrorDialog: function showErrorDialog(result) {},
              hideErrorDialog: function hideErrorDialog() {},
              fromView: function fromView(data, navigation) {}
            });
          }
        }), _react.default.createElement(_ListItem.ListItem, {
          title: "Ricotta\u9519\u8BEF\u5904\u7406",
          subtitle: "7114",
          onPress: function onPress() {
            var deviceStatus = {
              "category": _consts.MINT_PRINTER_STATUS_CATEGORY.PRINTER_STATUS_CATEGORY_ERROR,
              "sub_category": "printing",
              "error": _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_PAPER_REMOVE_LOAD,
              "battery": 19,
              "battery_pct": 100,
              "clean_remain": 0,
              "job_type": 0,
              "job_id": 96,
              "prt_copies": 1,
              "isSuccess": true,
              "connectionState": 2
            };

            _errorHandle2.default.handleDeviceState(_this3.props.navigation, deviceStatus, true, {
              showErrorDialog: function showErrorDialog(result) {},
              hideErrorDialog: function hideErrorDialog() {},
              fromView: function fromView(data, navigation) {}
            });
          }
        }), _react.default.createElement(_ListItem.ListItem, {
          title: "Ricotta\u9519\u8BEF\u5904\u7406",
          subtitle: "7201",
          onPress: function onPress() {
            var deviceStatus = {
              "category": _consts.MINT_PRINTER_STATUS_CATEGORY.PRINTER_STATUS_CATEGORY_ERROR,
              "sub_category": "printing",
              "error": _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_RIBBON_END,
              "battery": 19,
              "battery_pct": 100,
              "clean_remain": 0,
              "job_type": 0,
              "job_id": 96,
              "prt_copies": 1,
              "isSuccess": true,
              "connectionState": 2
            };

            _errorHandle2.default.handleDeviceState(_this3.props.navigation, deviceStatus, true, {
              showErrorDialog: function showErrorDialog(result) {},
              hideErrorDialog: function hideErrorDialog() {},
              fromView: function fromView(data, navigation) {}
            });
          }
        }), _react.default.createElement(_ListItem.ListItem, {
          title: "Ricotta\u9519\u8BEF\u5904\u7406",
          subtitle: "7204",
          onPress: function onPress() {
            var deviceStatus = {
              "category": _consts.MINT_PRINTER_STATUS_CATEGORY.PRINTER_STATUS_CATEGORY_ERROR,
              "sub_category": "printing",
              "error": _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_NO_RIBBON_MARKER,
              "battery": 19,
              "battery_pct": 100,
              "clean_remain": 0,
              "job_type": 0,
              "job_id": 96,
              "prt_copies": 1,
              "isSuccess": true,
              "connectionState": 2
            };

            _errorHandle2.default.handleDeviceState(_this3.props.navigation, deviceStatus, true, {
              showErrorDialog: function showErrorDialog(result) {},
              hideErrorDialog: function hideErrorDialog() {},
              fromView: function fromView(data, navigation) {}
            });
          }
        }), _react.default.createElement(_ListItem.ListItem, {
          title: "Ricotta\u9519\u8BEF\u5904\u7406",
          subtitle: "7205",
          onPress: function onPress() {
            var deviceStatus = {
              "category": _consts.MINT_PRINTER_STATUS_CATEGORY.PRINTER_STATUS_CATEGORY_ERROR,
              "sub_category": "printing",
              "error": _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_RIBBON_ERROR,
              "battery": 19,
              "battery_pct": 100,
              "clean_remain": 0,
              "job_type": 0,
              "job_id": 96,
              "prt_copies": 1,
              "isSuccess": true,
              "connectionState": 2
            };

            _errorHandle2.default.handleDeviceState(_this3.props.navigation, deviceStatus, true, {
              showErrorDialog: function showErrorDialog(result) {},
              hideErrorDialog: function hideErrorDialog() {},
              fromView: function fromView(data, navigation) {}
            });
          }
        }), _react.default.createElement(_ListItem.ListItem, {
          title: "Ricotta\u9519\u8BEF\u5904\u7406",
          subtitle: "7208",
          onPress: function onPress() {
            var deviceStatus = {
              "category": _consts.MINT_PRINTER_STATUS_CATEGORY.PRINTER_STATUS_CATEGORY_ERROR,
              "sub_category": "printing",
              "error": _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_RIBBON_ERROR_2,
              "battery": 19,
              "battery_pct": 100,
              "clean_remain": 0,
              "job_type": 0,
              "job_id": 96,
              "prt_copies": 1,
              "isSuccess": true,
              "connectionState": 2
            };

            _errorHandle2.default.handleDeviceState(_this3.props.navigation, deviceStatus, true, {
              showErrorDialog: function showErrorDialog(result) {},
              hideErrorDialog: function hideErrorDialog() {},
              fromView: function fromView(data, navigation) {}
            });
          }
        }), _react.default.createElement(_ListItem.ListItem, {
          title: "Ricotta\u9519\u8BEF\u5904\u7406",
          subtitle: "7308",
          onPress: function onPress() {
            var deviceStatus = {
              "category": _consts.MINT_PRINTER_STATUS_CATEGORY.PRINTER_STATUS_CATEGORY_ERROR,
              "sub_category": "printing",
              "error": _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_PRINTER_OVERHEAT,
              "battery": 19,
              "battery_pct": 100,
              "clean_remain": 0,
              "job_type": 0,
              "job_id": 96,
              "prt_copies": 1,
              "isSuccess": true,
              "connectionState": 2
            };

            _errorHandle2.default.handleDeviceState(_this3.props.navigation, deviceStatus, true, {
              showErrorDialog: function showErrorDialog(result) {},
              hideErrorDialog: function hideErrorDialog() {},
              fromView: function fromView(data, navigation) {}
            });
          }
        }), _react.default.createElement(_ListItem.ListItem, {
          title: "Ricotta\u9519\u8BEF\u5904\u7406",
          subtitle: "7309",
          onPress: function onPress() {
            var deviceStatus = {
              "category": _consts.MINT_PRINTER_STATUS_CATEGORY.PRINTER_STATUS_CATEGORY_ERROR,
              "sub_category": "printing",
              "error": _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_PRINTER_OVERCOOL,
              "battery": 19,
              "battery_pct": 100,
              "clean_remain": 0,
              "job_type": 0,
              "job_id": 96,
              "prt_copies": 1,
              "isSuccess": true,
              "connectionState": 2
            };

            _errorHandle2.default.handleDeviceState(_this3.props.navigation, deviceStatus, true, {
              showErrorDialog: function showErrorDialog(result) {},
              hideErrorDialog: function hideErrorDialog() {},
              fromView: function fromView(data, navigation) {}
            });
          }
        }), _react.default.createElement(_ListItem.ListItem, {
          title: "Ricotta\u9519\u8BEF\u5904\u7406",
          subtitle: "7310",
          onPress: function onPress() {
            var deviceStatus = {
              "category": _consts.MINT_PRINTER_STATUS_CATEGORY.PRINTER_STATUS_CATEGORY_ERROR,
              "sub_category": "printing",
              "error": _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_PRINTER_BATTERY_CRITICAL,
              "battery": 19,
              "battery_pct": 100,
              "clean_remain": 0,
              "job_type": 0,
              "job_id": 96,
              "prt_copies": 1,
              "isSuccess": true,
              "connectionState": 2
            };

            _errorHandle2.default.handleDeviceState(_this3.props.navigation, deviceStatus, true, {
              showErrorDialog: function showErrorDialog(result) {},
              hideErrorDialog: function hideErrorDialog() {},
              fromView: function fromView(data, navigation) {}
            });
          }
        }), _react.default.createElement(_ListItem.ListItem, {
          title: "Ricotta\u9519\u8BEF\u5904\u7406",
          subtitle: "7311",
          onPress: function onPress() {
            var deviceStatus = {
              "category": _consts.MINT_PRINTER_STATUS_CATEGORY.PRINTER_STATUS_CATEGORY_ERROR,
              "sub_category": "printing",
              "error": _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_PRINTER_BATTERY_OFF,
              "battery": 19,
              "battery_pct": 100,
              "clean_remain": 0,
              "job_type": 0,
              "job_id": 96,
              "prt_copies": 1,
              "isSuccess": true,
              "connectionState": 2
            };

            _errorHandle2.default.handleDeviceState(_this3.props.navigation, deviceStatus, true, {
              showErrorDialog: function showErrorDialog(result) {},
              hideErrorDialog: function hideErrorDialog() {},
              fromView: function fromView(data, navigation) {}
            });
          }
        }))), _react.default.createElement(_reactNative.View, null, _react.default.createElement(_reactNative.Text, null, "    \u54CD\u5E94\u5185\u5BB9\u663E\u793A"), _react.default.createElement(_reactNative.TextInput, {
          editable: false,
          style: {
            height: 300,
            margin: 10,
            borderColor: '#eee',
            borderWidth: 0.5,
            backgroundColor: '#ccc'
          },
          multiline: true,
          numberOfLines: 0,
          value: JSON.stringify(this.state.result)
        })), _react.default.createElement(_mhuiRn.LoadingDialog, {
          visible: this.state.visible,
          message: this.state.dialogTitle
        }), _react.default.createElement(_CoverLayer.default, {
          ref: function ref(_ref) {
            return _this3.coverLayer = _ref;
          }
        }));
      }
    }, {
      key: "componentDidMount",
      value: function componentDidMount() {
        var _this4 = this;

        this._s1 = _miot.BluetoothEvent.bluetoothSeviceDiscovered.addListener(function (blut, services) {
          if (services.length <= 0) {
            return;
          }

          var s = services.map(function (s) {
            return {
              uuid: s.UUID,
              char: []
            };
          });

          _this4.setState({
            services: s
          });

          if (bt.isConnected) {
            services.forEach(function (s) {
              _this4.state.services[s.UUID] = s;
              s.startDiscoverCharacteristics();
            });
          }

          _miot.Device.getBluetoothLE().getVersion(true, true).then(function (version) {}).catch(function (err) {});
        });
        this._s2 = _miot.BluetoothEvent.bluetoothCharacteristicDiscovered.addListener(function (bluetooth, service, characters) {
          var services = _this4.state.services;
          services.forEach(function (s) {
            if (s.uuid === service.UUID) {
              s.char = characters.map(function (s) {
                return s.UUID;
              });
            }
          });

          _this4.setState({
            services: services
          });

          if (bt.isConnected) {
            characters.forEach(function (c) {
              _this4.state.chars[c.UUID] = c;
            });
          }
        });
        this._s3 = _miot.BluetoothEvent.bluetoothCharacteristicValueChanged.addListener(function (bluetooth, service, character, value) {
          if (service.UUID.indexOf('ffd5') > 0) {}

          if (character.UUID.toUpperCase() === UUID_BUTTON_READ_WRITE_NOTIFY) {
            bt.securityLock.decryptMessage(value).then(function (res) {});
          }
        });
        this._s4 = _miot.BluetoothEvent.bluetoothSeviceDiscoverFailed.addListener(function (blut, data) {});
        this._s5 = _miot.BluetoothEvent.bluetoothCharacteristicDiscoverFailed.addListener(function (blut, data) {});
        this._s6 = _miot.BluetoothEvent.bluetoothConnectionStatusChanged.addListener(function (blut, isConnect) {
          if (bt.mac === blut.mac) {
            _this4.setState({
              connectState: isConnect ? '已连接' : '未连接'
            });
          }
        });
        this._s7 = _miot.BluetoothEvent.bluetoothDeviceDiscovered.addListener(function (result) {
          if (result.mac === bt.mac) {
            _logUtils.default.reportLog("\u53D1\u73B0\u5F53\u524D\u8BBE\u5907" + JSON.stringify(result));

            _this4.setState({
              result: "\u53D1\u73B0\u5F53\u524D\u8BBE\u5907" + JSON.stringify(result)
            });

            _miot.Bluetooth.stopScan();

            _this4.connect(result.mac);
          } else {
            _logUtils.default.reportLog("\u53D1\u73B0\u5176\u4ED6\u8BBE\u5907" + JSON.stringify(result));
          }
        });
        this._s8 = _miot.DeviceEvent.bleDeviceFirmwareNeedUpgrade.addListener(function (device) {});
        this._s9 = _miot.BluetoothEvent.bluetoothStatusChanged.addListener(function (isOn) {
          if (!isOn) {
            _this4.setState({
              connectState: '未连接',
              testCharNotify: false,
              btConnect: false,
              chars: {},
              services: []
            });
          }
        });
      }
    }, {
      key: "componentWillUnmount",
      value: function componentWillUnmount() {
        this._s1.remove();

        this._s2.remove();

        this._s3.remove();

        this._s4.remove();

        this._s5.remove();

        this._s6.remove();

        this._s7.remove();

        this._s8.remove();

        this._s9.remove();
      }
    }, {
      key: "downloadFW",
      value: function downloadFW() {
        var _this5 = this;

        this.fwPath = _consts.PATHS.TEMP_DIR + "/" + Date.now() + ".bin";

        _logUtils.default.reportLog("\u4E0B\u8F7D\u8DEF\u5F84 " + this.fwPath);

        _miot.Service.smarthome.getLatestVersionV2(_miot.Device.deviceID).then(function (response) {
          _this5.setState({
            result: "latest version = " + JSON.stringify(response)
          });

          var dest = response.url;

          if (!dest) {
            _this5.setState({
              result: "getLatestVersionV2 failed dest = " + dest
            });
          } else {
            _miot.Host.file.downloadFile(dest, _this5.fwPath).then(function (res) {
              _this5.setState({
                result: "downloadFile success"
              });
            }).catch(function (error) {
              _this5.setState({
                result: "downloadFile failed " + error
              });
            });
          }
        }).catch(function (error) {
          _this5.setState({
            result: "getLatestVersionV2 failed " + error
          });
        });
      }
    }, {
      key: "getLastFwInfo",
      value: function getLastFwInfo() {
        var _this6 = this;

        _miot.Service.smarthome.getLatestVersionV2(_miot.Device.deviceID).then(function (response) {
          _this6.setState({
            result: "latest version = " + JSON.stringify(response)
          });
        });
      }
    }, {
      key: "testARShareList",
      value: function testARShareList() {
        var _this7 = this;

        _request.default.arGetShareList().then(function (response) {
          if (response.data && response.data.code == 0) {
            _this7.setState({
              result: "result = " + JSON.stringify(response.data.result)
            });
          } else {
            _this7.setState({
              result: "" + JSON.stringify(response)
            });
          }
        }).catch(function (error) {});
      }
    }, {
      key: "testARShare",
      value: function testARShare(enable) {
        _request.default.arShare("1", "TEST_NICK_NAME", enable).then(function (response) {}).catch(function (error) {});
      }
    }, {
      key: "testShare",
      value: function testShare() {
        var _this8 = this;

        var cmdObj = {};
        cmdObj['pid'] = _miot.Device.type;
        cmdObj['did'] = _miot.Device.deviceID;

        _miot.Service.callSmartHomeAPI("/share/get_share_user", cmdObj).then(function (response) {
          _this8.setState({
            result: "response = " + JSON.stringify(response)
          });
        }).catch(function (error) {
          _this8.setState({
            result: "error = " + JSON.stringify(error)
          });
        });
      }
    }, {
      key: "testShare1",
      value: function testShare1() {
        var _this9 = this;

        var cmdObj = {};
        cmdObj['command'] = "share_request";
        cmdObj['did'] = _miot.Device.deviceID;
        cmdObj['userid'] = "988658820";

        _miot.Service.callSmartHomeAPI("/share/share_request", cmdObj).then(function (response) {
          _this9.setState({
            result: "response = " + JSON.stringify(response)
          });
        }).catch(function (error) {
          _this9.setState({
            result: "error = " + JSON.stringify(error)
          });
        });
      }
    }, {
      key: "_openPhotoPickerView",
      value: function _openPhotoPickerView() {
        var granted;
        return _regenerator.default.async(function _openPhotoPickerView$(_context) {
          while (1) {
            switch (_context.prev = _context.next) {
              case 0:
                if (!_miot.Host.isAndroid) {
                  _context.next = 14;
                  break;
                }

                _context.next = 3;
                return _regenerator.default.awrap(_reactNative.PermissionsAndroid.check(_reactNative.PermissionsAndroid.PERMISSIONS.WRITE_EXTERNAL_STORAGE));

              case 3:
                if (_context.sent) {
                  _context.next = 11;
                  break;
                }

                _context.next = 6;
                return _regenerator.default.awrap(_reactNative.PermissionsAndroid.request(_reactNative.PermissionsAndroid.PERMISSIONS.WRITE_EXTERNAL_STORAGE));

              case 6:
                granted = _context.sent;

                if (granted === _reactNative.PermissionsAndroid.RESULTS.GRANTED) {
                  _logUtils.default.reportLog("Home--> 你已获取了读写权限");
                } else if (granted === _reactNative.PermissionsAndroid.RESULTS.DENIED) {
                  _logUtils.default.reportLog("Home--> 获取读写权限失败");
                } else if (granted === _reactNative.PermissionsAndroid.RESULTS.NEVER_ASK_AGAIN) {
                  _UtilsHeadFile.HTSingleton.toast.show(_resources.default.getString('read_refuse_toast'));
                }

                return _context.abrupt("return");

              case 11:
                this.openPhotoPickerView();

              case 12:
                _context.next = 15;
                break;

              case 14:
                this.openPhotoPickerView();

              case 15:
              case "end":
                return _context.stop();
            }
          }
        }, null, this);
      }
    }, {
      key: "openPhotoPickerView",
      value: function openPhotoPickerView() {
        var _this10 = this;

        var requestParam = {
          sandBoxFolder: _miot.Host.file.storageBasePath + "/" + _consts.PATHS.PIC_SCAN_CACHE,
          w: 1200,
          h: 1800,
          maxNumber: 1,
          minNumber: 1,
          isSingle: true,
          isSupportCamera: false,
          pushAnimated: false
        };
        photoModule.launchImageLibrary(requestParam, function (result) {
          if (result.code == 0) {
            _ServicesHeadFile.ImageExtension.compressImageWithPaths(result["data"]["paths"], function (sucess, paths) {
              _logUtils.default.reportLog("mintg debug compressImageWithPaths: " + JSON.stringify(result["data"]["paths"]) + " to " + JSON.stringify(paths));

              if (sucess) {
                _this10.setState({
                  result: "\u9009\u62E9\u56FE\u7247\u6210\u529F " + JSON.stringify(paths)
                });

                _this10.imagePath = paths[0];
                _this10.originImagePath = result["data"]["paths"][0];
              } else {
                _this10.setState({
                  result: "\u9009\u62E9\u56FE\u7247\u5931\u8D25 \u538B\u7F29\u5931\u8D25"
                });
              }
            }, 512);
          } else {
            _this10.setState({
              result: "\u9009\u62E9\u56FE\u7247\u5931\u8D25"
            });
          }
        });
      }
    }, {
      key: "connect",
      value: function connect() {
        var _this11 = this;

        var mac = arguments.length > 0 && arguments[0] !== undefined ? arguments[0] : undefined;
        var disconnectOntimeOut = arguments.length > 1 && arguments[1] !== undefined ? arguments[1] : true;

        if (_miot.Host.isAndroid) {
          _miot.Bluetooth.stopScan(15000);
        }

        this.setState({
          blueConnecting: true,
          connectState: '连接中。。。'
        });

        _logUtils.default.reportLog('准备开始蓝牙连接');

        if (bt.isConnected) {
          _logUtils.default.reportLog('蓝牙设备已经连接');

          _logUtils.default.reportLog('开始发先服务');

          this.setState({
            blueConnecting: false,
            connectState: '已连接',
            btConnect: true
          });
          bt.startDiscoverServices();
        } else if (bt.isConnecting) {
          _logUtils.default.reportLog('蓝牙正处于连接中，请等待连接结果后再试');
        } else {
          var that = this;

          _logUtils.default.reportLog("" + _miot.Host.isAndroid);

          bt.connect(-1).then(function (data) {
            _logUtils.default.reportLog("ble connect \u6210\u529F: " + JSON.stringify(data));

            _this11.mint_uuid = data.uuid;

            _this11.setState({
              blueConnecting: false,
              connectState: '已连接',
              btConnect: true
            });

            _this11.setState({
              result: "\u5DF2\u8FDE\u63A5\u8BBE\u5907\u6210\u529F\uFF0Cdata = " + JSON.stringify(data)
            });
          }).catch(function (data) {
            _this11.setState({
              blueConnecting: false,
              connectState: '连接失败',
              btConnect: false
            });

            _logUtils.default.reportLog("ble connect failed: " + JSON.stringify(data));

            _this11.setState({
              result: "\u8FDE\u63A5\u8BBE\u5907\u5931\u8D25\uFF0Cdata = " + JSON.stringify(data)
            });
          });
        }
      }
    }, {
      key: "reverseConnect",
      value: function reverseConnect() {
        var _this12 = this;

        _miot.Host.storage.get(_consts.MY_DEVICE_MAC_KEY).then(function (res) {
          var mac = res;
          bt.connect(-1).then(function (res) {
            _logUtils.default.reportLog("ble connect \u6210\u529F: " + JSON.stringify(res));

            _this12.setState({
              result: "\u5DF2\u8FDE\u63A5\u8BBE\u5907BLE\u6210\u529F\uFF0Cres = " + JSON.stringify(res)
            });

            var data = {
              objects: [{
                siid: 6,
                piid: 2,
                value: mac,
                type: 10
              }]
            };
            data = JSON.stringify(data);

            _logUtils.default.reportLog("bleWrite data: " + data);

            _miot.Bluetooth.spec.setPropertiesValue(_this12.mint_uuid, data).then(function (res) {
              _logUtils.default.reportLog("bleWrite \u6210\u529F: " + JSON.stringify(res));

              _this12.setState({
                result: "bleWrite \u6210\u529F: " + JSON.stringify(res)
              });

              if (bt.isConnected) {
                bt.disconnect();
              }
            }).catch(function (err) {
              _logUtils.default.reportLog(JSON.stringify(err));

              _this12.setState({
                result: "bleWrite \u5931\u8D25: " + JSON.stringify(err)
              });

              if (bt.isConnected) {
                bt.disconnect();
              }
            });
          }).catch(function (error) {
            _logUtils.default.reportLog("ble connect failed: " + JSON.stringify(error));

            _this12.setState({
              result: "\u8FDE\u63A5\u8BBE\u5907BLE\u5931\u8D25\uFF0Cerror = " + JSON.stringify(error)
            });
          });
        }).catch(function (error) {
          _this12.setState({
            result: "\u83B7\u53D6MAC\u5931\u8D25: " + JSON.stringify(error)
          });
        });
      }
    }, {
      key: "reverseConnect2",
      value: function reverseConnect2() {
        var _this13 = this;

        _miot.Host.storage.get(_consts.MY_DEVICE_MAC_KEY).then(function (res) {
          var mac = res;
          bt.connect(-1).then(function (res) {
            _logUtils.default.reportLog("ble connect \u6210\u529F: " + JSON.stringify(res));

            _this13.setState({
              result: "\u5DF2\u8FDE\u63A5\u8BBE\u5907BLE\u6210\u529F\uFF0Cres = " + JSON.stringify(res)
            });

            var data = {
              objects: [{
                siid: 6,
                piid: 2,
                value: mac,
                type: 10
              }]
            };
            data = JSON.stringify(data);

            _logUtils.default.reportLog("bleWrite data: " + data);

            _miot.Bluetooth.spec.setPropertiesValue(_this13.mint_uuid, data).then(function (res) {
              _logUtils.default.reportLog("bleWrite \u6210\u529F: " + JSON.stringify(res));

              _this13.setState({
                result: "bleWrite \u6210\u529F: " + JSON.stringify(res)
              });
            }).catch(function (err) {
              _logUtils.default.reportLog(JSON.stringify(err));

              _this13.setState({
                result: "bleWrite \u5931\u8D25: " + JSON.stringify(err)
              });
            });
          }).catch(function (error) {
            _logUtils.default.reportLog("ble connect failed: " + JSON.stringify(error));

            _this13.setState({
              result: "\u8FDE\u63A5\u8BBE\u5907BLE\u5931\u8D25\uFF0Cerror = " + JSON.stringify(error)
            });
          });
        }).catch(function (error) {
          _this13.setState({
            result: "\u83B7\u53D6MAC\u5931\u8D25: " + JSON.stringify(error)
          });
        });
      }
    }, {
      key: "bleWrite",
      value: function bleWrite() {
        var _this14 = this;

        var mac = bt.mac;
        var data = {
          objects: [{
            siid: 6,
            piid: 2,
            value: mac,
            type: 10
          }]
        };
        data = JSON.stringify(data);

        _logUtils.default.reportLog("bleWrite data: " + data);

        _miot.Bluetooth.spec.setPropertiesValue(this.mint_uuid, data).then(function (res) {
          _logUtils.default.reportLog(JSON.stringify(res));

          _this14.setState({
            result: "bleWrite \u6210\u529F: " + JSON.stringify(res)
          });
        }).catch(function (err) {
          _logUtils.default.reportLog(JSON.stringify(err));

          _this14.setState({
            result: "bleWrite \u5931\u8D25: " + JSON.stringify(err)
          });
        });
      }
    }, {
      key: "bleWrite1",
      value: function bleWrite1() {
        var _this15 = this;

        var mac = bt.mac;

        _miot.Device.getBluetoothLE().securityLock.encryptMessageWithToken(mac).then(function (res) {
          _logUtils.default.reportLog("bleWrite1 res = " + JSON.stringify(res));

          var data = {
            objects: [{
              siid: 6,
              piid: 2,
              value: res.result,
              type: 10
            }]
          };
          data = JSON.stringify(data);

          _logUtils.default.reportLog("bleWrite1 data: " + data);

          _miot.Bluetooth.spec.setPropertiesValue(_this15.mint_uuid, data).then(function (res) {
            _logUtils.default.reportLog(JSON.stringify(res));

            _this15.setState({
              result: "bleWrite1 \u6210\u529F: " + JSON.stringify(res)
            });
          }).catch(function (err) {
            _logUtils.default.reportLog(JSON.stringify(err));

            _this15.setState({
              result: "bleWrite1 \u5931\u8D25: " + JSON.stringify(err)
            });
          });
        }).catch(function (err) {
          _logUtils.default.reportLog(err);

          _this15.setState({
            result: "bleWrite1 \u5931\u8D25: " + JSON.stringify(err)
          });
        });
      }
    }, {
      key: "bleWrite2",
      value: function bleWrite2() {
        var _this16 = this;

        var mac = bt.mac;

        _miot.Device.getBluetoothLE().securityLock.encryptMessage(mac).then(function (res) {
          _logUtils.default.reportLog("bleWrite2 res = " + JSON.stringify(res));

          var data = {
            objects: [{
              siid: 6,
              piid: 2,
              value: res,
              type: 10
            }]
          };
          data = JSON.stringify(data);

          _logUtils.default.reportLog("bleWrite2 data: " + data);

          _miot.Bluetooth.spec.setPropertiesValue(_this16.mint_uuid, data).then(function (res) {
            _logUtils.default.reportLog(JSON.stringify(res));

            _this16.setState({
              result: "bleWrite2 \u6210\u529F: " + JSON.stringify(res)
            });
          }).catch(function (err) {
            _logUtils.default.reportLog(JSON.stringify(err));

            _this16.setState({
              result: "bleWrite2 \u5931\u8D25: " + JSON.stringify(err)
            });
          });
        }).catch(function (err) {
          _logUtils.default.reportLog(err);

          _this16.setState({
            result: "bleWrite1 \u5931\u8D25: " + JSON.stringify(err)
          });
        });
      }
    }, {
      key: "disconnect",
      value: function disconnect() {
        this.setState({
          connectState: '断开连接中。。。'
        });
        bt.disconnect();
      }
    }, {
      key: "changeCompatible",
      value: function changeCompatible() {
        var _this17 = this;

        _miot.Host.storage.get(_consts.MINT_KEY_COMPATIBLE_WITH_LOW_FW_VERSION).then(function (res) {
          if (res) {
            _miot.Host.storage.set(_consts.MINT_KEY_COMPATIBLE_WITH_LOW_FW_VERSION, false);

            _this17.setState({
              result: "\u4E4B\u524D\u662F\u5426\u517C\u5BB9\uFF1A\u662F\u3002\u73B0\u5728\u662F\u5426\u517C\u5BB9\uFF1A\u5426"
            });
          } else {
            _miot.Host.storage.set(_consts.MINT_KEY_COMPATIBLE_WITH_LOW_FW_VERSION, true);

            _this17.setState({
              result: "\u4E4B\u524D\u662F\u5426\u517C\u5BB9\uFF1A\u5426\u3002\u73B0\u5728\u662F\u5426\u517C\u5BB9\uFF1A\u662F"
            });
          }
        }).catch(function (err) {
          _logUtils.default.reportLog("changeCompatible = " + err);
        });
      }
    }]);
    return commonRequest;
  }(_react.Component);

  exports.default = commonRequest;

  commonRequest.navigationOptions = function (_ref2) {
    var navigation = _ref2.navigation;
    return {
      header: null
    };
  };

  var styles = _reactNative.StyleSheet.create({
    container: {
      backgroundColor: _resources.Styles.common.backgroundColor,
      flex: 1
    },
    featureSetting: {
      backgroundColor: '#fff'
    },
    blank: {
      height: 8,
      backgroundColor: _resources.Styles.common.backgroundColor,
      borderTopColor: _resources.Styles.common.hairlineColor,
      borderTopWidth: _reactNative.StyleSheet.hairlineWidth,
      borderBottomColor: _resources.Styles.common.hairlineColor,
      borderBottomWidth: _reactNative.StyleSheet.hairlineWidth
    },
    titleContainer: {
      height: 32,
      backgroundColor: '#fff',
      justifyContent: 'center',
      paddingLeft: _resources.Styles.common.padding
    },
    title: {
      fontSize: 11,
      color: 'rgba(0,0,0,0.5)',
      lineHeight: 14
    }
  });
},13166,[14317,14314,14683,14329,14332,14383,14389,14398,10077,10353,10338,10332,10074,10719,10297,12665,10033,12314,12173,12338,22414,12374,12203,12194,12404,12335,12365,12308,12293,12359,12317,13040,13163,12407,12170,12329,12332,12416,10034,10055]);