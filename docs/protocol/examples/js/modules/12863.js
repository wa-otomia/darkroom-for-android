__d(function (global, _$$_REQUIRE, _$$_IMPORT_DEFAULT, _$$_IMPORT_ALL, module, exports, _dependencyMap) {
  var _interopRequireWildcard = _$$_REQUIRE(_dependencyMap[0]);

  var _interopRequireDefault = _$$_REQUIRE(_dependencyMap[1]);

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  exports.default = undefined;

  var _toConsumableArray2 = _interopRequireDefault(_$$_REQUIRE(_dependencyMap[2]));

  var _regenerator = _interopRequireDefault(_$$_REQUIRE(_dependencyMap[3]));

  var _classCallCheck2 = _interopRequireDefault(_$$_REQUIRE(_dependencyMap[4]));

  var _createClass2 = _interopRequireDefault(_$$_REQUIRE(_dependencyMap[5]));

  var _possibleConstructorReturn2 = _interopRequireDefault(_$$_REQUIRE(_dependencyMap[6]));

  var _getPrototypeOf2 = _interopRequireDefault(_$$_REQUIRE(_dependencyMap[7]));

  var _assertThisInitialized2 = _interopRequireDefault(_$$_REQUIRE(_dependencyMap[8]));

  var _inherits2 = _interopRequireDefault(_$$_REQUIRE(_dependencyMap[9]));

  var _react = _interopRequireWildcard(_$$_REQUIRE(_dependencyMap[10]));

  var _reactNative = _$$_REQUIRE(_dependencyMap[11]);

  var _HomeCard = _interopRequireDefault(_$$_REQUIRE(_dependencyMap[12]));

  var _consts = _$$_REQUIRE(_dependencyMap[13]);

  var _request = _interopRequireDefault(_$$_REQUIRE(_dependencyMap[14]));

  var _LoadingDialog = _interopRequireDefault(_$$_REQUIRE(_dependencyMap[15]));

  var _logKey = _$$_REQUIRE(_dependencyMap[16]);

  var _bluetoothPrintManger = _interopRequireDefault(_$$_REQUIRE(_dependencyMap[17]));

  var _fileHelper = _interopRequireDefault(_$$_REQUIRE(_dependencyMap[18]));

  var _error_handle_dialog = _interopRequireDefault(_$$_REQUIRE(_dependencyMap[19]));

  var _resources = _interopRequireDefault(_$$_REQUIRE(_dependencyMap[20]));

  var _temp = _$$_REQUIRE(_dependencyMap[21]);

  var _commonUtils = _interopRequireDefault(_$$_REQUIRE(_dependencyMap[22]));

  var _logUtils = _interopRequireDefault(_$$_REQUIRE(_dependencyMap[23]));

  var _errorHandle = _interopRequireDefault(_$$_REQUIRE(_dependencyMap[24]));

  var _HtMessageDialog = _interopRequireDefault(_$$_REQUIRE(_dependencyMap[25]));

  var _ThirdPartyHeadFile = _$$_REQUIRE(_dependencyMap[26]);

  var _ServicesHeadFile = _$$_REQUIRE(_dependencyMap[27]);

  var _ComponentsHeadFile = _$$_REQUIRE(_dependencyMap[28]);

  var _CommonHeadFile = _$$_REQUIRE(_dependencyMap[29]);

  var _miot = _$$_REQUIRE(_dependencyMap[30]);

  var _index = _interopRequireWildcard(_$$_REQUIRE(_dependencyMap[31]));

  var _BaseDialog = _$$_REQUIRE(_dependencyMap[32]);

  var _index2 = _interopRequireDefault(_$$_REQUIRE(_dependencyMap[33]));

  var _lodash = _$$_REQUIRE(_dependencyMap[34]);

  var _deviceConfig = _interopRequireDefault(_$$_REQUIRE(_dependencyMap[35]));

  var _reactNativeLinearGradient = _interopRequireDefault(_$$_REQUIRE(_dependencyMap[36]));

  var _arUseHelp = _interopRequireDefault(_$$_REQUIRE(_dependencyMap[37]));

  var _DynamicUtil = _interopRequireDefault(_$$_REQUIRE(_dependencyMap[38]));

  var _ImageCacheManager = _interopRequireDefault(_$$_REQUIRE(_dependencyMap[39]));

  var _JumpUrlUtil = _interopRequireDefault(_$$_REQUIRE(_dependencyMap[40]));

  var _HomeDataService = _interopRequireDefault(_$$_REQUIRE(_dependencyMap[41]));

  var _TemplateVideoService = _interopRequireWildcard(_$$_REQUIRE(_dependencyMap[42]));

  var _GuideOverlay = _interopRequireDefault(_$$_REQUIRE(_dependencyMap[43]));

  var _GuideStorage = _interopRequireWildcard(_$$_REQUIRE(_dependencyMap[44]));

  var mainItemHeight = _consts.WIDTH / 360 * 122;
  var photoModule = _reactNative.NativeModules.HTRCTPhotoPickerModule;
  var imagePickerManager = _reactNative.NativeModules.ImagePickerManager;
  var scanModule = _reactNative.NativeModules.HTRCTScanModule;

  var _Dimensions$get = _reactNative.Dimensions.get('screen'),
      width = _Dimensions$get.width,
      height = _Dimensions$get.height;

  var BUTTON_WIDTH = _reactNative.Platform.select({
    android: (0, _ThirdPartyHeadFile.scale)(12),
    ios: (0, _ThirdPartyHeadFile.scale)(12)
  });

  var lastErrorCode = 0;
  var fwForceResult = {};
  var currentVersion;
  var arModule = _reactNative.NativeModules.HTRCTARModule;
  var cameraModule = _reactNative.NativeModules.HTRCTCamaraModule;
  var MAX_IOS_FAILED_COUNT = 3;
  var MINT_KEY_AR_SCAN_COLLECTION_IDS = 'MINT_KEY_AR_SCAN_COLLECTION_IDS';
  var AR_PHOTO_SCAN = 999;
  var RICOTTA_BANNER_URL_KEY = 'ricotta_banner_url';

  var rocittaHome = function (_Component) {
    (0, _inherits2.default)(rocittaHome, _Component);

    function rocittaHome(props) {
      var _this;

      (0, _classCallCheck2.default)(this, rocittaHome);
      _this = (0, _possibleConstructorReturn2.default)(this, (0, _getPrototypeOf2.default)(rocittaHome).call(this, props));
      _this.tipsDialogNeedShow = true;
      _this.functionType = _consts.JOB_TYPE.PHOTO_PRINT_JOB;
      _this.currentFWVersion = null;
      _this.lastVersion = null;
      _this.iosFailedCount = 0;
      _this._guidePhotoPrintRef = _react.default.createRef();
      _this._guideOverlayRef = _react.default.createRef();
      _this.hasGotForceUpgradeResult = false;
      _this.hasQueriedForceUpgrade = false;
      _this.needForceUpgrade = false;
      _this.iosFirstInDialogHaveShowed = false;
      _this._shareFileEnterHome = false;
      _this.pageRenderStart = 0;

      _this.showLoadingTips = function (tip) {
        _this.setState({
          showDialog: true,
          dialogTitle: tip
        });
      };

      _this.dismissTips = function () {
        _this.timerTips && clearTimeout(_this.timerTips);
        setTimeout(function () {
          _this.setState({
            showDialog: false,
            dialogTimeout: 0,
            dialogTitle: ''
          });
        }, 300);
      };

      _this.showFailTips = function (tip) {
        var timeout = arguments.length > 1 && arguments[1] !== undefined ? arguments[1] : 300;

        _this.setState({
          showDialog: true,
          dialogTimeout: timeout,
          dialogTitle: tip
        });

        _this.timerTips && clearTimeout(_this.timerTips);
        _this.timerTips = setTimeout(function () {
          _this.dismissTips();
        }, timeout);
      };

      _this.dataManager = new _HomeDataService.default();
      _this.state = {
        focus: false,
        shareActionFlag: false,
        categoryCode: 0,
        deviceStatus: {},
        categoryTitle: _resources.default.getString("get_status_title"),
        holdLangPress: 0,
        category: '',
        gifName: '',
        iconRotateValue: new _reactNative.Animated.Value(0),
        isIconRotateAnimating: false,
        sub_category: '',
        error: [],
        showDialog: false,
        dialogTimeout: 0,
        dialogTitle: '',
        msgSubscription: '',
        errorDetail: {},
        showErrorDialog: false,
        scanTipDialogVisible: false,
        scanTipMessage: "",
        fileSizeError: false,
        unboxingTipsVisible: false,
        unboxingTips: _resources.default.getString("need_oobe"),
        calibrationVisible: false,
        calibrationTips: _resources.default.getString("need_adjust"),
        cameraPermissionVisible: false,
        cameraPermissionTip: _resources.default.getString("camera_permission_txt"),
        enableErrorViewListener: true,
        continueAlignmentVisible: false,
        continueAlignmentMessage: _resources.default.getString('coutinueAlignment'),
        continueAlignmentTitle: _resources.default.getString('default_alert_title'),
        continueAlignmentBtn: _resources.default.getString('coutinueAlignmentBtn'),
        fwForceUpgradeVisible: false,
        fwForceUpgradeMessage: "",
        bluetoothConnectContainerVisible: false,
        battery: _consts.MINT_BATTERY.POWER_CAP_UNKNOWN,
        tipDiallogVisible: false,
        checkBoxChecked: true,
        hasNewFwToUpgrade: false,
        fwUpgradeDialogVisible: false,
        fwForceUpgradeDialogVisible: false,
        iOSBlueTipDialogVisble: false,
        homePageAvaliable: false,
        waterInfoSubtitle: '',
        connectionState: _consts.MINT_BLUETOOTH_STATE.DISCONNECTED,
        alertMessageVisible: true,
        alertMessage: _resources.default.getString("device_connecting_text"),
        alertSubMessage: _resources.default.getString("device_connecting_sub_text"),
        alertSubMessageVisible: false,
        alertMessageEnable: false,
        alertIcon: _CommonHeadFile.HTImage.homeIconConnecting,
        alertTextColor: _CommonHeadFile.HTColor.c_btn_blue,
        printerMainStatusText: _resources.default.getString("get_connect_statu_title"),
        printerSubStatusText: _resources.default.getString("get_connect_statu_sub"),
        btParseErrorDialogVisible: false,
        iosFailedDialogVisible: false,
        userActionScan: false,
        longTimeConnectFailedDialogVisible: false,
        alertButtonVisiable: false,
        showARUseHelp: false,
        arUseHelpConfig: {
          needNext: true,
          onContinue: null,
          onLater: null
        },
        longTimeConnectFailedDialogVisible: false,
        alertButtonVisiable: false,
        romPackageCheckDialogVisible: false,
        batteryWrongStatusDialogVisible: false,
        suportLivePhoto: false,
        bannerSource: {},
        bannerCachePath: null,
        showGuide: false
      };
      iosFailedCount = 0;
      _this.goFunction = _this.goFunction.bind((0, _assertThisInitialized2.default)(_this));
      _this.debouncedGoFunction = (0, _lodash.debounce)(_this.goFunction, 1000, {
        leading: true,
        trailing: false
      });
      _this.pageRenderStart = _commonUtils.default.getTimestamp();
      return _this;
    }

    (0, _createClass2.default)(rocittaHome, [{
      key: "componentDidMount",
      value: function componentDidMount() {
        var _this2 = this;

        _commonUtils.default.createScannedDir();

        global.enableLogInRelease = true;

        _logUtils.default.reportLog("\u5F53\u524D\u6D4B\u8BD5 Host.isDebug = " + _ThirdPartyHeadFile.Host.isDebug + " Package.buildType = " + _ThirdPartyHeadFile.Package.buildType + " Package.isDebug = " + _ThirdPartyHeadFile.Package.isDebug);

        _DynamicUtil.default.trackInit();

        _DynamicUtil.default.trackEvent(_consts.BIGDATA_KEYS.APP_LAUNCH_USG_LAUNCH, {});

        _DynamicUtil.default.setPluginStartTime();

        _DynamicUtil.default.trackEvent(_consts.BIGDATA_KEYS.APP_PERF_EVT_FIRSTPAGERENDER, {
          duration: _commonUtils.default.getTimestamp() - this.pageRenderStart
        });

        _DynamicUtil.default.trackEvent(_consts.BIGDATA_KEYS.APP_DEVICE_EVT_ISOWNER, {
          isOwner: _ThirdPartyHeadFile.Device.isOwner
        });

        this.focusListener && this.focusListener.remove();
        this.focusListener = this.props.navigation.addListener('willFocus', function () {
          _logUtils.default.reportLog("home - willFocus");

          _this2._viewDidFocus();
        });

        this._viewDidFocus();

        this.blurListener && this.blurListener.remove();
        this.blurListener = this.props.navigation.addListener('willBlur', function () {
          _logUtils.default.reportLog("home - willBlur");

          _this2._viewDidUnFocus();
        });

        _bluetoothPrintManger.default.ShareInstance().enablePrinterStatus();

        this.clearCache(_ServicesHeadFile.PATHS.TEMP_DIR);

        _ThirdPartyHeadFile.Service.getServerName().then(function (server) {
          _logUtils.default.reportLog("server1 = " + JSON.stringify(server));

          _bluetoothPrintManger.default.ShareInstance().setServer(server);
        });

        _logUtils.default.reportLog("language = " + _resources.default.getLanguage());

        this._packageAuthorizationAgreed = _ThirdPartyHeadFile.PackageEvent.packageAuthorizationAgreed.addListener(function () {
          _logUtils.default.reportLog("[PackageEvent.packageAuthorizationAgreed] info => \u7528\u6237\u70B9\u51FB\u540C\u610F\u5566!");

          _this2.startInit();
        });
        this._packageReceivedInformation = _ThirdPartyHeadFile.PackageEvent.packageReceivedInformation.addListener(function (message) {
          _logUtils.default.reportLog("\u6536\u5230\u901A\u77E5\u6570\u636E\uFF1A" + JSON.stringify(message));

          if (message.event === "share_print") {
            var value = message.value;

            _this2.handleSharePrintValue(value, "");
          }
        });

        if (_reactNative.Platform.OS === 'android') {
          try {
            _logUtils.default.reportLog("DarkMode.getColorScheme() = " + _miot.DarkMode.getColorScheme());

            _reactNative.NativeModules.HTRCTDarkModeModule.setDarkMode(_miot.DarkMode.getColorScheme() === 'dark');
          } catch (e) {
            _logUtils.default.reportLog("android nativemodules error info --> " + e);
          }
        }

        this.listenerDarkMode = _reactNative.DeviceEventEmitter.addListener(_CommonHeadFile.NOTIFICATION_NAME.DarkModeSwitch, function (e) {
          _this2.setState({});
        });
        this._cloudPrivacyEvent = _miot.PrivacyEvent.cloudPrivacyEvent.addListener(function (message) {
          _logUtils.default.reportLog("\u6536\u5230\u4E91\u7AEF\u9690\u79C1\u901A\u77E5\u6570\u636E\uFF1A" + JSON.stringify(message));

          if (!message) {
            _logUtils.default.reportLog("\u6536\u5230\u4E91\u7AEF\u9690\u79C1\u901A\u77E5\u6570\u636E\u4E3A\u7A7A");

            _this2.startInit();

            return;
          }

          switch (message.eventType) {
            case _miot.CLOUD_PRIVACY_EVENT_TYPES.AGREED:
              _this2.startInit();

              break;

            case _miot.CLOUD_PRIVACY_EVENT_TYPES.POP_DIALOG_SUCCESS:
              break;

            case _miot.CLOUD_PRIVACY_EVENT_TYPES.FAILED:
              _this2.startInit();

              break;

            default:
              _this2.startInit();

              break;
          }
        });

        if (!_index2.default.countryCode || _index2.default.countryCode.length === 0) {
          _reactNative.DeviceEventEmitter.addListener(_index.MINT_SERVICE_ENV_CHANGE, function (res) {
            if (_index2.default.countryCode === 'cn' && !_deviceConfig.default.isRicottaG()) {
              _logUtils.default.reportLog("\u56FD\u5185\u652F\u6301\u6B64\u529F\u80FD + " + _index2.default.countryCode + "  \u5F00\u59CB\u8C03\u7528\u83B7\u53D6banner\u6570\u636E");

              _this2.getBannerSource();

              _this2.setState({
                waterInfoSubtitle: '永远相信美好的事情即将发生！'
              });
            } else {
              var waterInfoSubs = ['Live your story', 'This moment is yours', 'Happiness in action', 'Joy in the making'];
              var randomIndex = Math.floor(Math.random() * waterInfoSubs.length);
              var randomSub = waterInfoSubs[randomIndex];

              _this2.setState({
                waterInfoSubtitle: randomSub
              });
            }
          });
        } else {
          if (_index2.default.countryCode === 'cn' && !_deviceConfig.default.isRicottaG()) {
            _logUtils.default.reportLog("\u56FD\u5185\u652F\u6301\u6B64\u529F\u80FD + " + _index2.default.countryCode + "  \u5F00\u59CB\u8C03\u7528\u83B7\u53D6banner\u6570\u636E");

            this.getBannerSource();
            this.setState({
              waterInfoSubtitle: '永远相信美好的事情即将发生！'
            });
          } else {
            var waterInfoSubs = ['Live your story', 'This moment is yours', 'Happiness in action', 'Joy in the making'];
            var randomIndex = Math.floor(Math.random() * waterInfoSubs.length);
            var randomSub = waterInfoSubs[randomIndex];
            this.setState({
              waterInfoSubtitle: randomSub
            });
          }
        }

        var supportLivePhoto = _ThirdPartyHeadFile.Host.getDeviceSupportLivePhoto;

        _logUtils.default.reportLog("\u7C73\u5BB6 supportLivePhoto \u8FD4\u56DE\u503C = " + _ThirdPartyHeadFile.Host.getDeviceSupportLivePhoto + " ---- \u7C7B\u578B " + typeof supportLivePhoto);

        var supportLivePhotoValue = false;

        if (_ThirdPartyHeadFile.Host.isAndroid) {
          supportLivePhotoValue = supportLivePhoto != null && supportLivePhoto != 'none' ? true : false;
        } else {
          supportLivePhotoValue = _deviceConfig.default.isRicottaP() ? false : _ThirdPartyHeadFile.Host.getDeviceSupportLivePhoto;
        }

        this.setState({
          suportLivePhoto: supportLivePhotoValue
        });

        _logUtils.default.reportLog("supportLivePhoto = " + supportLivePhotoValue);

        if (_deviceConfig.default.isRicotta()) {
          this.dataManager.initTemplate();
        }
      }
    }, {
      key: "getBannerSource",
      value: function getBannerSource() {
        var _this3 = this;

        var bannerSource = this.state.bannerSource;

        if (bannerSource) {
          this.loadCachedBannerImage(bannerSource == null ? undefined : bannerSource.img_url);
        }

        _request.default.getBannerSource().then(function (response) {
          _logUtils.default.reportLog("getBannerSource response = " + JSON.stringify(response));

          if (response.data && response.data.code == 0) {
            if (response.data.result && response.data.result.length > 0) {
              var bannerList = response.data.result;
              var firBannerData = bannerList[0];

              _ImageCacheManager.default.cacheImage(firBannerData.img_url);

              if (firBannerData.redirect_type) {
                _logUtils.default.reportLog("getBannerSource firBannerData0 = " + JSON.stringify(firBannerData));

                (0, _TemplateVideoService.parseBannerTemplateList)(firBannerData);

                _ThirdPartyHeadFile.Host.storage.set(RICOTTA_BANNER_URL_KEY, '');

                _this3.setState({
                  bannerSource: firBannerData
                }, function () {
                  if (_this3.state.showGuide) {
                    requestAnimationFrame(function () {
                      var _this3$_guideOverlayR;

                      (_this3$_guideOverlayR = _this3._guideOverlayRef.current) == null ? undefined : _this3$_guideOverlayR.remeasure();
                    });
                  }
                });

                return;
              }

              _logUtils.default.reportLog("getBannerSource firBannerData1 = " + JSON.stringify(firBannerData));

              _this3.setState({
                bannerSource: firBannerData
              }, function () {
                if (_this3.state.showGuide) {
                  requestAnimationFrame(function () {
                    var _this3$_guideOverlayR2;

                    (_this3$_guideOverlayR2 = _this3._guideOverlayRef.current) == null ? undefined : _this3$_guideOverlayR2.remeasure();
                  });
                }
              });

              _ThirdPartyHeadFile.Host.storage.set(RICOTTA_BANNER_URL_KEY, JSON.stringify(firBannerData));
            }
          } else {
            setTimeout(function () {
              _this3.getBannerSource();
            }, 5000);
          }
        }).catch(function (error) {
          _logUtils.default.reportLog("getBannerSource error = " + JSON.stringify(error));

          setTimeout(function () {
            _this3.getBannerSource();
          }, 5000);
        });
      }
    }, {
      key: "checkBluetoothOpen",
      value: function checkBluetoothOpen(isFirst) {
        var _this4 = this;

        _logUtils.default.reportLog("checkBluetoothOpen isFirst = " + isFirst);

        _miot.Bluetooth.checkBluetoothIsEnabled().then(function (result) {
          if (result) {
            _logUtils.default.reportLog("蓝牙已开启:", result);

            _bluetoothPrintManger.default.ShareInstance().setBluetoothReady(true);

            if (!isFirst) {
              _this4.startConnect();
            }
          } else {
            _logUtils.default.reportLog("蓝牙未开启，请检查开启蓝牙后再试");

            if (isFirst) {
              _bluetoothPrintManger.default.ShareInstance().init1();
            }

            if (_ThirdPartyHeadFile.Host.isAndroid) {
              _miot.Bluetooth.enableBluetoothForAndroid(true);
            } else {
              _ThirdPartyHeadFile.Host.ui.showBLESwitchGuide();
            }
          }
        });
      }
    }, {
      key: "checkBluetoothPermission",
      value: function checkBluetoothPermission(isFirst) {
        var _this5 = this;

        if (!isFirst) {
          _DynamicUtil.default.trackEvent(_consts.BIGDATA_KEYS.APP_PG_HOME_USG_CONNBTN, {});
        }

        _logUtils.default.reportLog("checkBluetoothPermission isFirst = " + isFirst);

        if (_reactNative.Platform.OS === 'android') {
          _miot.Bluetooth.checkBluetoothPermission().then(function (result) {
            _logUtils.default.reportLog("\u68C0\u67E5\u84DD\u7259\u6743\u9650\u6210\u529F\uFF1A " + JSON.stringify(result));

            if (result && result.data && result.data.checkBluetooth) {
              _this5.checkBluetoothOpen(isFirst);
            } else {
              if (isFirst) {
                _bluetoothPrintManger.default.ShareInstance().init1();
              }
            }
          }).catch(function (e) {
            _logUtils.default.reportLog("\u68C0\u67E5\u84DD\u7259\u6743\u9650\u5931\u8D25\uFF1A + " + JSON.stringify(e));

            if (isFirst) {
              _bluetoothPrintManger.default.ShareInstance().init1();
            }
          });
        } else {
          this.checkBluetoothOpen(isFirst);
        }
      }
    }, {
      key: "startInit",
      value: function startInit() {
        this.setState({
          homePageAvaliable: true
        });
        this.checkBluetoothPermission(true);
        this.checkSharePrint();
      }
    }, {
      key: "startConnect",
      value: function startConnect() {
        _logUtils.default.reportLog("startConnect bluetoothPrintManger.ShareInstance().getConnectStage() = " + _bluetoothPrintManger.default.ShareInstance().getConnectStage());

        if (_bluetoothPrintManger.default.ShareInstance().getConnectStage() == _consts.CONNECT_STAGE_RICOTTA.CONNECTING_BEFORE_3_MINS) {
          _logUtils.default.reportLog('CONNECTING_BEFORE_3_MINS 不需处理');
        } else if (_bluetoothPrintManger.default.ShareInstance().getConnectStage() == _consts.CONNECT_STAGE_RICOTTA.CONNECTING_BEFORE_10_MINS) {
          _logUtils.default.reportLog('CONNECTING_BEFORE_10_MINS');

          if (_ThirdPartyHeadFile.Host.isAndroid) {
            this.showFailTips(_resources.default.getString("home_bluetooth_connecting_tips"), 5000);
          } else {
            this.setState({
              iOSBlueTipDialogVisble: true
            });

            _DynamicUtil.default.trackEvent(_consts.BIGDATA_KEYS.APP_PG_HOME_EVT_POPIOSSET, {
              isFirst: false
            });
          }
        } else if (_bluetoothPrintManger.default.ShareInstance().getConnectStage() == _consts.CONNECT_STAGE_RICOTTA.CONNECTING_AFTER_10_MINS) {
          if (_ThirdPartyHeadFile.Host.isIOS && !_bluetoothPrintManger.default.ShareInstance().getMac()) {
            this.setState({
              iOSBlueTipDialogVisble: true
            });

            _DynamicUtil.default.trackEvent(_consts.BIGDATA_KEYS.APP_PG_HOME_EVT_POPIOSSET, {
              isFirst: false
            });
          } else {
            this.setState({
              longTimeConnectFailedDialogVisible: true
            });

            _DynamicUtil.default.trackEvent(_consts.BIGDATA_KEYS.APP_PG_HOME_EVT_POPLONGTIMECONNFAIL, {});
          }
        }
      }
    }, {
      key: "_viewDidFocus",
      value: function _viewDidFocus() {
        var _this6 = this;

        this.listener && this.listener.remove();
        this.listener = _reactNative.DeviceEventEmitter.addListener(_consts.PRINTER_STATE_CHANGE, function (e) {
          _logUtils.default.reportLog("home - handleDeviceState");

          _this6.handleDeviceState(e);
        });
        this.jobCreateListener && this.jobCreateListener.remove();
        this.jobCreateListener = _reactNative.DeviceEventEmitter.addListener(_consts.MINT_EVENT_CREATE_JOB_RESULT, function (e) {
          _this6.handleJobCreate(e);
        });

        this._checkAndShowGuide();
      }
    }, {
      key: "_isShareFileEntry",
      value: function _isShareFileEntry() {
        var _Package$entryInfo;

        if (this._shareFileEnterHome) {
          return true;
        }

        var value = (_Package$entryInfo = _ThirdPartyHeadFile.Package.entryInfo) == null ? undefined : _Package$entryInfo.value;
        var photos = value == null ? undefined : value.photos;
        return photos && photos.length === 1;
      }
    }, {
      key: "_checkAndShowGuide",
      value: function _checkAndShowGuide() {
        var _this7 = this;

        var should;
        return _regenerator.default.async(function _checkAndShowGuide$(_context) {
          while (1) {
            switch (_context.prev = _context.next) {
              case 0:
                _context.prev = 0;

                if (!this._isShareFileEntry()) {
                  _context.next = 4;
                  break;
                }

                _logUtils.default.reportLog('_checkAndShowGuide skip: share file entry');

                return _context.abrupt("return");

              case 4:
                _context.next = 6;
                return _regenerator.default.awrap(_GuideStorage.default.shouldShowGuide(_GuideStorage.GUIDE_KEYS.HOME_PHOTO_PRINT));

              case 6:
                should = _context.sent;

                if (!(!should || this._isShareFileEntry())) {
                  _context.next = 9;
                  break;
                }

                return _context.abrupt("return");

              case 9:
                this._guideTimer = setTimeout(function () {
                  if (_this7._isShareFileEntry()) {
                    return;
                  }

                  _GuideStorage.default.markGuideShown(_GuideStorage.GUIDE_KEYS.HOME_PHOTO_PRINT);

                  _this7.setState({
                    showGuide: true
                  });
                }, 300);
                _context.next = 15;
                break;

              case 12:
                _context.prev = 12;
                _context.t0 = _context["catch"](0);

                _logUtils.default.reportLog("_checkAndShowGuide error: " + _context.t0);

              case 15:
              case "end":
                return _context.stop();
            }
          }
        }, null, this, [[0, 12]]);
      }
    }, {
      key: "_viewDidUnFocus",
      value: function _viewDidUnFocus() {
        this.listener && this.listener.remove();
        this.jobCreateListener && this.jobCreateListener.remove();
      }
    }, {
      key: "componentWillUnmount",
      value: function componentWillUnmount() {
        this._guideTimer && clearTimeout(this._guideTimer);

        _logUtils.default.reportLog("home - componentWillUnmount");

        if (_DynamicUtil.default.getPluginStartTime() != 0) {
          var duration = _commonUtils.default.getTimestamp() - _DynamicUtil.default.getPluginStartTime();

          _ThirdPartyHeadFile.Host.storage.set(_consts.RICOTTA_KEY_PLUGIN_DURATION, duration);

          _logUtils.default.reportLog("home - componentWillUnmount duration = " + duration);
        }

        _bluetoothPrintManger.default.ShareInstance().disablePrinterStatus();

        this.listener && this.listener.remove();
        this.focusListener && this.focusListener.remove();
        this.blurListener && this.blurListener.remove();
        this.viewAppearListener && this.viewAppearListener.remove();
        this.viewDisappearListener && this.viewDisappearListener.remove();
        this._packageReceivedInformation && this._packageReceivedInformation.remove();
        this._packageWillExit && this._packageWillExit.remove();
        this._packageAuthorizationAgreed && this._packageAuthorizationAgreed.remove();
        this.listenerDarkMode && this.listenerDarkMode.remove();
        this.dataEventListener && this.dataEventListener.remove();
        this._cloudPrivacyEvent && this._cloudPrivacyEvent.remove();

        if (this.dataManager) {
          this.dataManager.cleanupTemplateInit();
        }

        _bluetoothPrintManger.default.ShareInstance().destory();
      }
    }, {
      key: "handleDeviceState",
      value: function handleDeviceState(state) {
        var _this8 = this;

        _logUtils.default.reportLog("handleDeviceState result = " + JSON.stringify(state));

        var title = '';
        var tempBattery = this.state.battery;
        var tempAlertMessageVisible = this.state.alertMessageVisible;
        var tempAlertMessage = this.state.alertMessage;
        var tempAlertSubMessage = this.state.alertSubMessage;
        var tempAlertSubMessageVisible = this.state.alertSubMessageVisible;
        var tempAlertIcon = this.state.alertIcon;
        var tempAlertTextColor = this.state.alertTextColor;
        var tempPrinterMainStatusText = this.state.printerMainStatusText;
        var tempPrinterSubStatusText = this.state.printerSubStatusText;
        var tempAlertButtonVisiable = this.state.alertButtonVisiable;

        if (state.isSuccess) {
          title = _bluetoothPrintManger.default.ShareInstance().getTitleFromState(state);

          if (state.category == "error") {
            tempAlertMessageVisible = true;
            tempAlertMessage = _bluetoothPrintManger.default.ShareInstance().getRicottaErrorCategory(state.error);
            tempAlertSubMessage = _bluetoothPrintManger.default.ShareInstance().getRicottaErrorTitle(state.error);
            tempAlertSubMessageVisible = true;
            tempAlertMessageEnable = true;
            tempAlertIcon = _CommonHeadFile.HTImage.homeIconAlert;
            tempAlertTextColor = _CommonHeadFile.HTColor.c_font_000000;
            tempPrinterMainStatusText = _bluetoothPrintManger.default.ShareInstance().getCurrentJob ? _resources.default.getString("print_job_pause") : _resources.default.getString("no_print_job_sub");
            tempPrinterSubStatusText = _resources.default.getString("printer_error");
            tempAlertButtonVisiable = false;
          } else {
            tempAlertMessageVisible = false;
            tempAlertButtonVisiable = false;

            _logUtils.default.reportLog("handleDeviceState result = " + JSON.stringify(state));

            tempPrinterMainStatusText = _bluetoothPrintManger.default.ShareInstance().getCurrentJob() ? _resources.default.getString("job_printing") : _resources.default.getString("no_print_job_sub");

            if (state.category == _consts.MINT_PRINTER_STATUS_CATEGORY.PRINTER_STATUS_CATEGORY_IDLE) {
              tempPrinterSubStatusText = _resources.default.getString("printer_idle");

              if (_bluetoothPrintManger.default.ShareInstance().getLoaclJobs().length > 0 || _bluetoothPrintManger.default.ShareInstance().getErrorJobs().length > 0) {
                tempPrinterSubStatusText = _resources.default.getString("jobToBePrint").replace('%s', _bluetoothPrintManger.default.ShareInstance().getLoaclJobs().length) + " " + _resources.default.getString("jobInErrorState").replace('%s', _bluetoothPrintManger.default.ShareInstance().getErrorJobs().length);
              }
            } else if (state.category == _consts.MINT_PRINTER_STATUS_CATEGORY.PRINTER_STATUS_CATEGORY_PROCESSING) {
              tempPrinterSubStatusText = _resources.default.getString("job_printing");
              tempPrinterMainStatusText = _resources.default.getString("job_printing");
            } else if (state.category == _consts.MINT_PRINTER_STATUS_CATEGORY.PRINTER_STATUS_CATEGORY_SLEEP) {
              tempPrinterSubStatusText = _resources.default.getString("printer_sleep");
            } else if (state.category == _consts.MINT_PRINTER_STATUS_CATEGORY.PRINTER_STATUS_CATEGORY_OFF) {
              tempPrinterSubStatusText = _resources.default.getString("printer_off");
            } else if (state.category == _consts.MINT_PRINTER_STATUS_CATEGORY.PRINTER_STATUS_CATEGORY_UPDATING) {
              tempPrinterSubStatusText = _resources.default.getString("printer_updating");
            } else {
              tempPrinterSubStatusText = _resources.default.getString("toast_bt_get_status");
            }

            if (_bluetoothPrintManger.default.ShareInstance().isChargingError(state["battery-temp"], state.battery, state.sensor)) {
              tempAlertMessageVisible = true;
              tempAlertMessage = _resources.default.getString("charge_pause_title");
              tempAlertSubMessage = _resources.default.getString("charge_pause_context");
              tempAlertSubMessageVisible = true;
              tempAlertButtonVisiable = false;
              tempAlertMessageEnable = false;
              tempAlertIcon = _CommonHeadFile.HTImage.homeIconAlert;
              tempAlertTextColor = _CommonHeadFile.HTColor.c_font_000000;
            }
          }

          var newBattery = _bluetoothPrintManger.default.ShareInstance().getNewBattery(state["battery-temp"], state.battery, state.sensor);

          _logUtils.default.reportLog("handleDeviceState newBattery = " + newBattery);

          this.setState({
            categoryCode: state.category,
            deviceStatus: state,
            categoryTitle: title,
            error: state.error,
            bluetoothConnectContainerVisible: false,
            alertMessageEnable: tempAlertMessageEnable,
            battery: newBattery,
            iOSBlueTipDialogVisble: false,
            connectionState: state.connectionState,
            alertMessageVisible: tempAlertMessageVisible,
            alertMessage: tempAlertMessage,
            alertSubMessage: tempAlertSubMessage,
            alertSubMessageVisible: tempAlertSubMessageVisible,
            alertIcon: tempAlertIcon,
            alertTextColor: tempAlertTextColor,
            printerMainStatusText: tempPrinterMainStatusText,
            printerSubStatusText: tempPrinterSubStatusText,
            alertButtonVisiable: tempAlertButtonVisiable
          });
          this.checkFwUpgrade();
        } else {
          if (state.connectionState === _consts.MINT_BLUETOOTH_STATE.DISCONNECTED || state.connectionState === _consts.MINT_BLUETOOTH_STATE.AUTH_FAILED) {
            tempBattery = _consts.MINT_BATTERY.DEVICE_NOT_CONNECTED;
            tempAlertMessageVisible = true;
            tempAlertMessage = _resources.default.getString("home_bluetooth_disconnected");
            tempAlertSubMessage = _resources.default.getString("home_bluetooth_reconnect_tips");
            tempAlertSubMessageVisible = true;
            tempAlertMessageEnable = false;
            tempAlertIcon = _CommonHeadFile.HTImage.homeIconNoConnected;
            tempAlertTextColor = _CommonHeadFile.HTColor.c_abnormal;
            tempPrinterMainStatusText = _resources.default.getString("get_connect_statu_title");
            tempPrinterSubStatusText = _resources.default.getString("get_connect_statu_sub");
            tempAlertButtonVisiable = true;

            if (_ThirdPartyHeadFile.Host.isIOS && !_bluetoothPrintManger.default.ShareInstance().getMac() && !this.iosFirstInDialogHaveShowed) {
              this.iosFirstInDialogHaveShowed = true;
              this.setState({
                iOSBlueTipDialogVisble: true
              });

              _DynamicUtil.default.trackEvent(_consts.BIGDATA_KEYS.APP_PG_HOME_EVT_POPIOSSET, {
                isFirst: true
              });
            }
          } else if (state.connectionState === _consts.MINT_BLUETOOTH_STATE.CONNECTING) {
            tempBattery = _consts.MINT_BATTERY.DEVICE_CONNECTING;
            tempAlertMessageVisible = true;
            tempAlertMessage = _resources.default.getString("device_connecting_text");
            tempAlertSubMessage = _resources.default.getString("device_connecting_sub_text");
            tempAlertSubMessageVisible = true;
            tempAlertMessageEnable = false;
            tempAlertIcon = _CommonHeadFile.HTImage.homeIconConnecting;
            tempAlertTextColor = _CommonHeadFile.HTColor.c_btn_blue;
            tempPrinterMainStatusText = _resources.default.getString("get_connect_statu_title");
            tempPrinterSubStatusText = _resources.default.getString("get_connect_statu_sub");
            tempAlertButtonVisiable = false;
          } else if (state.connectionState === _consts.MINT_BLUETOOTH_STATE.CONNECTING_NEED_ALERT) {
            tempBattery = _consts.MINT_BATTERY.DEVICE_CONNECTING;
            tempAlertMessageVisible = true;
            tempAlertMessage = _resources.default.getString("device_connecting_text");
            tempAlertSubMessage = _resources.default.getString("device_connecting_sub_text");
            tempAlertSubMessageVisible = true;
            tempAlertMessageEnable = false;
            tempAlertIcon = _CommonHeadFile.HTImage.homeIconConnecting;
            tempAlertTextColor = _CommonHeadFile.HTColor.c_btn_blue;
            tempPrinterMainStatusText = _resources.default.getString("get_connect_statu_title");
            tempPrinterSubStatusText = _resources.default.getString("get_connect_statu_sub");
            tempAlertButtonVisiable = true;
          } else if (state.connectionState === _consts.MINT_BLUETOOTH_STATE.AUTH) {
            tempAlertMessageVisible = false;
            tempPrinterSubStatusText = _resources.default.getString("get_status_title");
          }

          this.setState({
            categoryCode: _consts.MINT_PRINTER_STATUS_CATEGORY.PRINTER_STATUS_CATEGORY_OFF,
            categoryTitle: state.reason,
            deviceStatus: state,
            connectionState: state.connectionState,
            bluetoothConnectContainerVisible: state.connectionState === _consts.MINT_BLUETOOTH_STATE.DISCONNECTED || state.connectionState === _consts.MINT_BLUETOOTH_STATE.AUTH_FAILED,
            battery: tempBattery,
            alertMessageVisible: tempAlertMessageVisible,
            alertMessage: tempAlertMessage,
            alertSubMessage: tempAlertSubMessage,
            alertMessageEnable: tempAlertMessageEnable,
            alertSubMessageVisible: tempAlertSubMessageVisible,
            alertIcon: tempAlertIcon,
            alertTextColor: tempAlertTextColor,
            printerMainStatusText: tempPrinterMainStatusText,
            printerSubStatusText: tempPrinterSubStatusText,
            btParseErrorDialogVisible: this.state.btParseErrorDialogVisible == true ? this.state.btParseErrorDialogVisible : state.code == _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_RPT_ERROR,
            alertButtonVisiable: tempAlertButtonVisiable
          });

          if (state.code == _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_RPT_ERROR) {
            _DynamicUtil.default.trackEvent(_consts.BIGDATA_KEYS.APP_PG_HOME_EVT_POPRPT, {});
          }
        }

        this.startIconRotation();

        _logUtils.default.reportLog("this.state.enableErrorViewListener =" + this.state.enableErrorViewListener);

        if (this.state.enableErrorViewListener) {
          _errorHandle.default.handleDeviceState(this.props.navigation, state, false, {
            showErrorDialog: function showErrorDialog(result) {
              _this8.setState({
                errorDetail: result,
                showErrorDialog: true
              });
            },
            hideErrorDialog: function hideErrorDialog() {
              _this8.setState({
                showErrorDialog: false
              });
            },
            fromView: function fromView(data, navigation) {
              _logUtils.default.reportLog('fromView home');
            }
          });
        }
      }
    }, {
      key: "handleJobCreate",
      value: function handleJobCreate(result) {
        _logUtils.default.reportLog("\u9996\u9875\u9875\u9762 - handleJobCreate result = " + JSON.stringify(result));

        if (!result.isSuccess) {
          if (result.errorCode == _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_FIND_NO_JOB || result.errorCode == _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_QUEUE_FULL || result.errorCode == _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_QUEUE_EMPTY || result.errorCode == _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_OVER_LIMITED_SIZE || result.errorCode == _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_TRANSFER_ERROR || result.errorCode == _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_LOW_BATTERY_ERROR || result.errorCode == _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_OVERHEAT || result.errorCode == _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_OVERCOOL) {
            this.showCreateJobError(result.errorCode);
          } else {}
        }
      }
    }, {
      key: "showCreateJobError",
      value: function showCreateJobError(code) {
        var _this9 = this;

        _logUtils.default.reportLog("showCreateJobError code = " + code);

        var message = '';

        switch (code) {
          case _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_FIND_NO_JOB:
          case _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_TRANSFER_ERROR:
            message = _resources.default.getString('transfer_fail_txt');
            break;

          case _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_QUEUE_FULL:
          case _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_QUEUE_EMPTY:
            message = _resources.default.getString('printer_buzy_zink_txt');
            break;

          case _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_OVER_LIMITED_SIZE:
            message = _resources.default.getString('file_size_zink_txt');
            break;

          case _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_LOW_BATTERY_ERROR:
            message = _resources.default.getString('battery_empty_txt');
            break;

          case _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_OVERHEAT:
          case _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_OVERCOOL:
            message = _resources.default.getString('temperature_tips_before_print');
            break;
        }

        this.createJobErrorDialog.show(_resources.default.getString('default_alert_title'), message, [{
          text: _resources.default.getString('button_ok'),
          onPress: function onPress() {
            _this9.props.navigation.navigate("mintPrintQueue");
          }
        }]);
      }
    }, {
      key: "detailButtonAction",
      value: function detailButtonAction() {
        _DynamicUtil.default.trackEvent(_consts.BIGDATA_KEYS.APP_PG_HOME_USG_QUEUE, {});

        if (this.state.homePageAvaliable) {
          this.navigateTo("mintPrintQueue");
        } else {
          _logUtils.default.reportLog("detailButtonAction false");
        }
      }
    }, {
      key: "goClean",
      value: function goClean() {
        _logUtils.default.reportLog("首页goClean");

        this.navigateTo("mintCleanHome");
      }
    }, {
      key: "clearCache",
      value: function clearCache(path) {
        var _this10 = this;

        _ThirdPartyHeadFile.Host.file.isFileExists(_ServicesHeadFile.PATHS.TEMP_DIR).then(function (res) {
          if (!res) {
            var params3 = {
              dirPath: _ServicesHeadFile.PATHS.TEMP_DIR,
              recursive: true
            };

            _ThirdPartyHeadFile.Host.file.mkdir(params3);
          }
        });

        setTimeout(function () {
          var currentTimeInSecond = _commonUtils.default.getTimestampInSecond();

          _ThirdPartyHeadFile.Host.file.readFileList(path).then(function (result) {
            for (var i = 0; i < result.length; i++) {
              if (result[i]['size'] == -1) {
                _this10.clearCache(path + "/" + result[i]['name']);

                continue;
              } else {
                if (currentTimeInSecond - result[i]['modifyTime'] > 86400) {
                  _ThirdPartyHeadFile.Host.file.deleteFile(path + "/" + result[i]['name']).then(function (res) {}).catch(function (err) {});
                }
              }
            }
          }).catch(function (err) {});
        }, 100);
      }
    }, {
      key: "hasNewFwToUpgrade",
      value: function hasNewFwToUpgrade(lastVersion, currentFWVersion) {
        try {
          var lastFourDigits = lastVersion.split('_')[1];
          var lastVersionNum = parseInt(lastFourDigits, 10);
          var currentFourDigits = currentFWVersion.split('_')[1];
          var currentVersionNum = parseInt(currentFourDigits, 10);

          _logUtils.default.reportLog("\u6700\u65B0\u7248\u672C\uFF1A" + lastVersionNum + " \u5F53\u524D\u7248\u672C\uFF1A" + currentVersionNum);

          return lastVersionNum > currentVersionNum;
        } catch (e) {
          _logUtils.default.reportLog("\u68C0\u67E5\u56FA\u4EF6\u5347\u7EA7\u5F02\u5E38 e = " + e);

          return false;
        }
      }
    }, {
      key: "checkBatteryStatus",
      value: function checkBatteryStatus(battery, sensor) {
        _logUtils.default.reportLog("ricotta home - checkBatteryStatus battery = " + battery + " sensor = " + sensor);

        if (battery && sensor) {
          var isCharging = _bluetoothPrintManger.default.ShareInstance().isCharging(battery);

          var isUSBConnected = _bluetoothPrintManger.default.ShareInstance().isUSBConnected(sensor);

          var isPaperTrayColsed = _bluetoothPrintManger.default.ShareInstance().isPaperTrayColsed(sensor);

          _logUtils.default.reportLog("checkBatteryStatus isCharging = " + isCharging + " isUSBConnected = " + isUSBConnected + " isPaperTrayColsed = " + isPaperTrayColsed);

          if (battery == _consts.MINT_BATTERY.POWER_CAP_FULL || isUSBConnected && isCharging || !isUSBConnected && !isCharging) {
            _bluetoothPrintManger.default.ShareInstance().setBatteryNormal(true);
          } else if (isUSBConnected && !isCharging) {
            if (_bluetoothPrintManger.default.ShareInstance().getBatteryNormal()) {
              _bluetoothPrintManger.default.ShareInstance().setBatteryNormal(false);

              this.setState({
                batteryWrongStatusDialogVisible: true
              });
            }
          }
        }
      }
    }, {
      key: "checkFwUpgrade",
      value: function checkFwUpgrade() {
        var _this11 = this;

        _logUtils.default.reportLog("mint home - checkFwUpgrade");

        this.currentFWVersion = _bluetoothPrintManger.default.ShareInstance().getFwVersion();

        _logUtils.default.reportLog("this.currentFWVersion = " + this.currentFWVersion);

        if (this.currentFWVersion) {
          if (!this.hasQueriedForceUpgrade) {
            _logUtils.default.reportLog("\u8BF7\u6C42\u662F\u5426\u5F3A\u5236\u5347\u7EA7\u7ED3\u679C");

            this.hasQueriedForceUpgrade = true;

            _request.default.checkFwForceUpdate(this.currentFWVersion).then(function (response) {
              _logUtils.default.reportLog("checkFwForceUpdate response = " + JSON.stringify(response));

              if (response && response.data && response.data.code == 0 && response.data.result && response.data.result.upgrade) {
                _this11.needForceUpgrade = response.data.result.must;
              } else {
                _this11.needForceUpgrade = false;
              }

              _this11.hasGotForceUpgradeResult = true;
            }).catch(function (error) {
              _logUtils.default.reportLog("checkFwForceUpdate error = " + JSON.stringify(error));

              _this11.needForceUpgrade = false;
              _this11.hasGotForceUpgradeResult = true;
            });
          } else if (this.hasGotForceUpgradeResult) {
            if (!this.lastVersion) {
              _ThirdPartyHeadFile.Service.smarthome.getLatestVersionV2(_ThirdPartyHeadFile.Device.deviceID).then(function (response) {
                _logUtils.default.reportLog("latest version = " + JSON.stringify(response));

                _this11.lastVersion = response;

                if (_this11.lastVersion && _this11.lastVersion.version && _this11.currentFWVersion && _this11.lastVersion.version != _this11.currentFWVersion && _this11.hasNewFwToUpgrade(_this11.lastVersion.version, _this11.currentFWVersion)) {
                  _this11.setState({
                    hasNewFwToUpgrade: true
                  });

                  if (_this11.needForceUpgrade) {
                    _this11.setState({
                      fwForceUpgradeDialogVisible: true
                    });

                    _DynamicUtil.default.trackEvent(_consts.BIGDATA_KEYS.APP_PG_HOME_EVT_POPFWFORCE, {
                      current: _this11.currentFWVersion
                    });
                  } else {
                    _ThirdPartyHeadFile.Host.storage.get(_consts.MINT_KEY_FW_UPGRADE_TIME_KEY).then(function (res) {
                      _logUtils.default.reportLog("getLastFwUpgradeTime = " + res);

                      var now = Date.now();

                      if (!res || now - parseInt(res, 10) > 172800000) {
                        _ThirdPartyHeadFile.Host.storage.set(_consts.MINT_KEY_FW_UPGRADE_TIME_KEY, now.toString());

                        _this11.setState({
                          fwUpgradeDialogVisible: true
                        });
                      } else {
                        _logUtils.default.reportLog("mint home - checkFwUpgrade 时间间隔过短，不提示");
                      }
                    }).catch(function (error) {
                      _logUtils.default.reportLog("getLastFwUpgradeTime error = " + error);
                    });
                  }
                } else {
                  _this11.setState({
                    hasNewFwToUpgrade: false
                  });
                }
              }).catch(function (error) {
                _this11.setState({
                  hasNewFwToUpgrade: false
                });

                _logUtils.default.reportLog("getLatestVersionV2 error = " + error);
              });
            } else {
              _logUtils.default.reportLog("mint home - checkFwUpgrade lastVersion = " + JSON.stringify(this.lastVersion));

              if (this.lastVersion && this.lastVersion.version && this.currentFWVersion && this.lastVersion.version != this.currentFWVersion && this.hasNewFwToUpgrade(this.lastVersion.version, this.currentFWVersion)) {
                this.setState({
                  hasNewFwToUpgrade: true
                });

                if (this.needForceUpgrade) {
                  this.setState({
                    fwForceUpgradeDialogVisible: true
                  });

                  _DynamicUtil.default.trackEvent(_consts.BIGDATA_KEYS.APP_PG_HOME_EVT_POPFWFORCE, {
                    current: this.currentFWVersion
                  });
                }
              } else {
                this.setState({
                  hasNewFwToUpgrade: false
                });
              }
            }
          }
        } else {
          this.setState({
            hasNewFwToUpgrade: false
          });
        }
      }
    }, {
      key: "checkSharePrint",
      value: function checkSharePrint() {
        _logUtils.default.reportLog("Home--> checkSharePrint");

        if (this.state.shareActionFlag == true) {
          _logUtils.default.reportLog("home - checkSharePrint - this.state.shareActionFlag == true");

          return;
        }

        this.state.shareActionFlag = true;

        if (_ThirdPartyHeadFile.Package.entryInfo) {
          _logUtils.default.reportLog("home - checkSharePrint - Package.entryInfo");

          var value = _ThirdPartyHeadFile.Package.entryInfo.value;

          _logUtils.default.reportLog("home - checkSharePrint - Package.entryInfo - " + value);

          if (value == undefined) {
            return;
          }

          var mimeType = "";

          if (value.mimeType != undefined) {
            mimeType = value.mimeType;
          }

          this.handleSharePrintValue(value, mimeType);
        } else {}
      }
    }, {
      key: "handleSharePrintValue",
      value: function handleSharePrintValue(value, mimeType) {
        var _value$photos,
            _this12 = this;

        _logUtils.default.reportLog("Home--> handleSharePrintValue value = " + JSON.stringify(value) + " mineType = " + JSON.stringify(mimeType));

        if ((value == null ? undefined : (_value$photos = value.photos) == null ? undefined : _value$photos.length) === 1) {
          this._shareFileEnterHome = true;
        }

        if (value) {
          var photos = value.photos;

          if (photos && photos.length == 1) {
            var filePath = photos[0];

            _logUtils.default.reportLog("Home--> checkSharePrint - filePath \uFF1A" + filePath);

            if (_fileHelper.default.fileTypeIsPhoto(filePath) == true || _fileHelper.default.mimeTypeTypeIsPhoto(mimeType) == true) {
                var filePathInSandbox = filePath;

                _logUtils.default.reportLog("----------------------------------------" + filePath);

                _fileHelper.default.copyFileToTMP(filePath, mimeType).then(function (result) {
                  filePathInSandbox = result;

                  _logUtils.default.reportLog("home - copyFileToTMP " + JSON.stringify(filePathInSandbox));

                  if (filePathInSandbox) {
                    _ThirdPartyHeadFile.Host.file.readFileSegmentToBase64(filePathInSandbox, 0, 1).then(function (result) {
                      var fileSize = result.totalLength / 1024 / 1024;

                      _logUtils.default.reportLog("Home--> checkSharePrint - fileSize success \uFF1A" + fileSize);

                      _this12.handleSharePrindFile(filePathInSandbox);
                    }).catch(function (error) {
                      _CommonHeadFile.HTSingleton.toast.show(_resources.default.getString('toast_process_fail'));

                      _logUtils.default.reportLog("Home--> checkSharePrint - fileSize failed \uFF1A" + JSON.stringify(error));
                    });
                  } else {
                    _CommonHeadFile.HTSingleton.toast.show(_resources.default.getString('toast_process_fail'));
                  }
                }).catch(function (err) {
                  _CommonHeadFile.HTSingleton.toast.show('拷贝文件出错');

                  _this12.reportLog("home - checkSharePrint - DcopyFileToTMP === failed : " + JSON.stringify(err));
                });
              } else {
              _CommonHeadFile.HTSingleton.toast.show(_resources.default.getString("fileform_error_toast"));
            }
          } else {
            _CommonHeadFile.HTSingleton.toast.show(_resources.default.getString('toast_process_fail'));
          }
        }
      }
    }, {
      key: "handleSharePrindFile",
      value: function handleSharePrindFile(filePath) {
        var _this13 = this;

        if (_fileHelper.default.fileTypeIsPhoto(filePath)) {
          var allPath = _ThirdPartyHeadFile.Host.file.storageBasePath + "/" + filePath;
          this.showLoadingTips(_resources.default.getString("toast_loading"));

          if (_ThirdPartyHeadFile.Host.isIOS) {
            try {
              _reactNative.NativeModules.HTRCTImageFactoryModule.getImageInfo(allPath, function (result) {
                _this13.dismissTips();

                _logUtils.default.reportLog("home - handleSharePrindFile - getImageInfo success \uFF1A" + JSON.stringify(_resources.default));

                if (result && result.code === 0) {
                  var _width = result.data.width;
                  var _height = result.data.height;
                  var radio = _width / _height;
                  var newWidth = 0;
                  var newHeight = 0;

                  if (_width > 1040 && _height > 1560) {
                    if (_width > _height) {
                      newHeight = 1560;
                      newWidth = 1560 * radio;
                    } else {
                      newWidth = 1040;
                      newHeight = 1040 / radio;
                    }
                  } else {
                    newWidth = _width;
                    newHeight = _height;
                  }

                  _reactNative.NativeModules.HTRCTPhotoQualityModule.imageSizeCompress({
                    sandBoxFolder: _ThirdPartyHeadFile.Host.file.storageBasePath + "/" + _ServicesHeadFile.PATHS.TEMP_DIR,
                    imageName: new Date().getTime().toString(),
                    imagePath: allPath,
                    maxSize: 3072,
                    imageWidth: newWidth,
                    imageHeight: newHeight
                  }, function (result) {
                    var filePath = result['data']['filePath'];
                    (0, _temp.setImageTempData)([filePath]);

                    _this13.props.navigation.navigate('ricottaPreviewImage', {
                      fileFrom: _ServicesHeadFile.PATHS.SHARE,
                      waterInfoSubtitle: _this13.state.waterInfoSubtitle
                    });
                  });
                } else {
                  _this13.dismissTips();

                  _CommonHeadFile.HTSingleton.toast.show(_resources.default.getString("fileread_error_toast"));
                }
              });
            } catch (err) {
              this.dismissTips();

              _CommonHeadFile.HTSingleton.toast.show(_resources.default.getString("fileread_error_toast"));

              _logUtils.default.reportLog("home - handleSharePrindFile - getImageInfo failed \uFF1A" + JSON.stringify(err));
            }

            return;
          }

          _ServicesHeadFile.ImageExtension.compressImageWithPaths([allPath], function (sucess, paths) {
            _this13.dismissTips();

            if (sucess) {
              (0, _temp.setImageTempData)(paths);

              _this13.props.navigation.navigate('ricottaPreviewImage', {
                fileFrom: _ServicesHeadFile.PATHS.SHARE,
                waterInfoSubtitle: _this13.state.waterInfoSubtitle
              });
            } else {
              _this13.showFailTips('数据处理失败');
            }
          }).catch(function (err) {
            _this13.dismissTips();

            _CommonHeadFile.HTSingleton.toast.show(_resources.default.getString("fileread_error_toast"));
          });
        } else if (_fileHelper.default.fileTypeIsOfficeFile(filePath)) {
          var scanedIdCardFile = filePath.indexOf(_ServicesHeadFile.PATHS.SCANNED_IDCARD) != -1 || filePath.indexOf(_ServicesHeadFile.PATHS.SCANNED_CARD_ID) != -1 || filePath.indexOf(_ServicesHeadFile.PATHS.SCANNED_CARD_RESIDENCE) != -1 || filePath.indexOf(_ServicesHeadFile.PATHS.SCANNED_CARD_PASSPORT) != -1;
          var pageName = scanedIdCardFile ? 'scanIDCardPreviewDoc' : 'previewDoc2';
          this.props.navigation.navigate(pageName, {
            path: filePath,
            pageCount: 0
          });
        } else {
          _logUtils.default.reportLog("选择的文件类型为：其他");

          _CommonHeadFile.HTSingleton.toast.show(_resources.default.getString("fileform_error_toast"));
        }
      }
    }, {
      key: "navigateTo",
      value: function navigateTo(path) {
        _logUtils.default.reportLog("home navigateTo", path);

        this.props.navigation.navigate(path);
      }
    }, {
      key: "longPressAction",
      value: function longPressAction(index) {
        if (index == 2) {
          this.setState({
            holdLangPress: 2
          });
        } else if (index == 1 && this.state.holdLangPress == 2) {
          this.setState({
            holdLangPress: this.state.holdLangPress + index
          });
        } else if (index == 3 && this.state.holdLangPress == 3) {
          this.setState({
            holdLangPress: this.state.holdLangPress + index
          });
        } else {
          this.setState({
            holdLangPress: 0
          });
        }

        if (this.state.holdLangPress == 6) {
          this.props.navigation.navigate('commonRequest');
        }
      }
    }, {
      key: "buyButtonPressAction",
      value: function buyButtonPressAction() {
        var _this14 = this;

        this.showLoadingTips(_resources.default.getString("toast_loading"));

        _request.default.getBuyLink("ca").then(function (response) {
          _this14.dismissTips();

          _logUtils.default.reportLog("buyButtonPressAction response = " + JSON.stringify(response));

          var code = response.data.code;

          if (code == 0) {
            var url = response.data.result.url;

            if (url) {
              _ThirdPartyHeadFile.Host.ui.openWebPage(url);
            } else {
              _this14.showFailTips(_resources.default.getString('not_ready'));
            }
          } else {
            var errorMessage = response.data.message;

            _this14.showFailTips(errorMessage);
          }
        }).catch(function (error) {
          _this14.dismissTips();

          _logUtils.default.reportLog("buyButtonPressAction error = " + JSON.stringify(error));
        });
      }
    }, {
      key: "getIcon",
      value: function getIcon(state_code) {
        switch (state_code) {
          case _consts.MINT_PRINTER_STATUS_CATEGORY.PRINTER_STATUS_CATEGORY_IDLE:
            return _CommonHeadFile.HTImage.homegif_Home_idle;

          case _consts.MINT_PRINTER_STATUS_CATEGORY.PRINTER_STATUS_CATEGORY_SLEEP:
            return _CommonHeadFile.HTImage.homegif_Home_sleep;

          case _consts.MINT_PRINTER_STATUS_CATEGORY.PRINTER_STATUS_CATEGORY_PROCESSING:
          case _consts.MINT_PRINTER_STATUS_CATEGORY.PRINTER_STATUS_CATEGORY_UPDATING:
            return _CommonHeadFile.HTImage.homegif_Home_progress;

          case _consts.MINT_PRINTER_STATUS_CATEGORY.PRINTER_STATUS_CATEGORY_OFF:
            return _CommonHeadFile.HTImage.homegif_Home_off;

          case _consts.MINT_PRINTER_STATUS_CATEGORY.PRINTER_STATUS_CATEGORY_ERROR:
            return _CommonHeadFile.HTImage.homegif_Home_error;

          default:
            return _CommonHeadFile.HTImage.homegif_Home_idle;
        }
      }
    }, {
      key: "_openPhotoPickerView",
      value: function _openPhotoPickerView() {
        return _regenerator.default.async(function _openPhotoPickerView$(_context2) {
          while (1) {
            switch (_context2.prev = _context2.next) {
              case 0:
                this.openPhotoPickerView();

              case 1:
              case "end":
                return _context2.stop();
            }
          }
        }, null, this);
      }
    }, {
      key: "openPhotoPickerView",
      value: function openPhotoPickerView() {
        var _this15 = this;

        var requestParam = {
          sandBoxFolder: _ThirdPartyHeadFile.Host.file.storageBasePath + "/" + _ServicesHeadFile.PATHS.TEMP_DIR,
          w: 1040,
          h: 1560,
          maxNumber: 1,
          minNumber: 1,
          isSingle: true,
          isSupportCamera: _reactNative.Platform.OS === 'ios' && _ThirdPartyHeadFile.Host.isPad ? false : true,
          pushAnimated: false,
          targetWidth: 1040,
          targetHeight: 1560,
          maxSize: 3072
        };
        photoModule.launchImageLibrary(requestParam, function (result) {
          _logUtils.default.reportLog("openPhotoPickerView result = " + JSON.stringify(result));

          if (result.code == 0) {
            (0, _temp.setImageTempData)(result.data.paths);
            setTimeout(function () {
              _this15.props.navigation.navigate('ricottaPreviewImage', {
                fileFrom: _ServicesHeadFile.PATHS.HOME,
                extra: result.data.exif,
                waterInfoSubtitle: _this15.state.waterInfoSubtitle,
                openPhotoPicker: function openPhotoPicker() {
                  _this15._openPhotoPickerView();
                }
              });
            }, 750);
          } else {
            _logUtils.default.reportLog("mint home launchImageLibrary fail: " + JSON.stringify(result));
          }
        });
      }
    }, {
      key: "imagePuzzle",
      value: function imagePuzzle() {
        var _this16 = this;

        var requestParam = {
          sandBoxFolder: _ThirdPartyHeadFile.Host.file.storageBasePath + "/" + _ServicesHeadFile.PATHS.TEMP_DIR,
          w: 1040,
          h: 1560,
          maxNumber: 4,
          minNumber: 2,
          isSingle: false,
          isSupportCamera: false,
          pushAnimated: false,
          targetWidth: 520,
          targetHeight: 780,
          maxSize: 1536
        };
        photoModule.launchImageLibrary(requestParam, function (result) {
          if (result.code == 0 && result.data && result.data.paths && result.data.paths.length > 1) {
            _this16.props.navigation.navigate("PuzzlezzIndex", {
              imgs: result["data"]["paths"],
              openPhotoPicker: function openPhotoPicker() {
                _this16.imagePuzzle();
              }
            });
          } else if (result.code == 0 && result.data && result.data.paths && result.data.paths.length <= 1) {
            _CommonHeadFile.HTSingleton.toast.show(_resources.default.getString('picture_need_more', 2));
          } else {
            _CommonHeadFile.HTSingleton.toast.show(_resources.default.getString('error_unenumerated'));
          }
        });
      }
    }, {
      key: "imageSplit",
      value: function imageSplit() {
        var _this17 = this;

        var requestParam = {
          sandBoxFolder: _ThirdPartyHeadFile.Host.file.storageBasePath + "/" + _ServicesHeadFile.PATHS.TEMP_DIR,
          w: 1040,
          h: 1560,
          maxNumber: 1,
          minNumber: 1,
          isSingle: true,
          isSupportCamera: _reactNative.Platform.OS === 'ios' && _ThirdPartyHeadFile.Host.isPad ? false : true,
          pushAnimated: false,
          targetWidth: 1040,
          targetHeight: 1560,
          maxSize: 3072
        };
        photoModule.launchImageLibrary(requestParam, function (result) {
          if (result.code == 0) {
            if (result.data.paths && result.data.paths.length > 0) {
              _this17.props.navigation.navigate("SplitIndex", {
                imgs: result.data.paths,
                openPhotoPicker: function openPhotoPicker() {
                  _this17.imageSplit();
                }
              });
            } else {
              _this17.showFailTips(_resources.default.getString('toast_process_fail'));
            }
          } else {
            _CommonHeadFile.HTSingleton.toast.show(_resources.default.getString('error_unenumerated'));
          }
        });
      }
    }, {
      key: "requireCamPerssion",
      value: function requireCamPerssion() {
        this.setState({
          cameraPermissionVisible: true,
          cameraPermissionTip: _resources.default.getString("camera_permission_txt")
        });
      }
    }, {
      key: "exitApp",
      value: function exitApp() {
        global.errorViewVisible = true;
        global.errorDialogVisible = true;

        _ThirdPartyHeadFile.Package.exit();
      }
    }, {
      key: "renderTitlebar",
      value: function renderTitlebar() {
        var _this18 = this;

        return _react.default.createElement(_ComponentsHeadFile.HTNavigationBar, {
          backgroundColor: 'transparent',
          title: _ThirdPartyHeadFile.Device.name,
          titleStyle: {
            color: _CommonHeadFile.HTColor.c_font_000000,
            fontSize: (0, _ThirdPartyHeadFile.scale)(15),
            marginTop: (0, _ThirdPartyHeadFile.scale)(8)
          },
          left: [{
            icon: _CommonHeadFile.HTImage.nav_back,
            onPress: function onPress() {
              _logUtils.default.reportEvent(_logKey.LOG_KEY.ROSEMARY_TAP_EVENT_MAIN_CLOSE_PLUGIN, _logKey.LOG_KEY.ROSEMARY_TAP_EVENT_MAIN_CLOSE_PLUGIN);

              _this18.exitApp();
            }
          }],
          right: [{
            icon: this.state.hasNewFwToUpgrade ? _CommonHeadFile.HTImage.nav_more_hint : _CommonHeadFile.HTImage.nav_more,
            onPress: function onPress() {
              _DynamicUtil.default.trackEvent(_consts.BIGDATA_KEYS.APP_PG_HOME_USG_SETTING, {});

              _logUtils.default.reportEvent(_logKey.LOG_KEY.ROSEMARY_TAP_EVENT_MAIN_SETTING, _logKey.LOG_KEY.ROSEMARY_TAP_EVENT_MAIN_SETTING);

              if (_this18.state.homePageAvaliable) {
                _this18.props.navigation.navigate('mintSetting', {
                  hasNewFwToUpgrade: _this18.state.hasNewFwToUpgrade
                });
              }
            }
          }],
          mintBattery: this.state.battery,
          style: styles.navigationBar
        });
      }
    }, {
      key: "arVideoEdit",
      value: function arVideoEdit() {
        var _this19 = this;

        _ThirdPartyHeadFile.Host.storage.get('AR_VIDEO_HELP_VIEWED').then(function (viewed) {
          if (!viewed) {
            _this19.setState({
              showARUseHelp: true,
              arUseHelpConfig: {
                needNext: true,
                onContinue: function onContinue() {
                  _this19.setState({
                    showARUseHelp: false
                  }, function () {
                    _this19.startARVideoEdit();
                  });
                }
              }
            });
          } else {
            _this19.startARVideoEdit();
          }
        }).catch(function (error) {
          _this19.setState({
            showARUseHelp: true,
            arUseHelpConfig: {
              needNext: true,
              onContinue: function onContinue() {
                _this19.setState({
                  showARUseHelp: false
                }, function () {
                  _this19.startARVideoEdit();
                });
              }
            }
          });
        });
      }
    }, {
      key: "startARVideoEdit",
      value: function startARVideoEdit() {
        var _this20 = this;

        var writePermissionsRes, access, data;
        return _regenerator.default.async(function startARVideoEdit$(_context3) {
          while (1) {
            switch (_context3.prev = _context3.next) {
              case 0:
                if (!_ThirdPartyHeadFile.Host.isAndroid) {
                  _context3.next = 9;
                  break;
                }

                _context3.next = 3;
                return _regenerator.default.awrap(this.checkWritePermissions());

              case 3:
                writePermissionsRes = _context3.sent;

                _CommonHeadFile.LogUtils.reportLog("****startARVideoEdit>>>writePermissionsRes:" + writePermissionsRes);

                if (writePermissionsRes) {
                  _context3.next = 7;
                  break;
                }

                return _context3.abrupt("return");

              case 7:
                _context3.next = 15;
                break;

              case 9:
                if (!_ThirdPartyHeadFile.Host.isIOS) {
                  _context3.next = 15;
                  break;
                }

                _context3.next = 12;
                return _regenerator.default.awrap(this.checkIOSSystermPermisssions('5'));

              case 12:
                access = _context3.sent;

                if (access) {
                  _context3.next = 15;
                  break;
                }

                return _context3.abrupt("return");

              case 15:
                _CommonHeadFile.LogUtils.log("****startARVideoEdit>>>ENTER");

                data = {
                  ar_type: 1,
                  picSize: {
                    width: 1040,
                    height: 1560
                  },
                  maxSize: 1536,
                  sandBoxFolder: _ThirdPartyHeadFile.Host.file.storageBasePath + "/" + _ServicesHeadFile.PATHS.TEMP_DIR
                };
                arModule.ar_edit_choose(data, function (result) {
                  _CommonHeadFile.LogUtils.reportLog('选择AR视频后的回调' + JSON.stringify(result));

                  _this20.arPicEdit(result, true);
                });

              case 18:
              case "end":
                return _context3.stop();
            }
          }
        }, null, this);
      }
    }, {
      key: "arsoundEdit",
      value: function arsoundEdit() {
        var _this21 = this;

        var writePermissionsRes, data;
        return _regenerator.default.async(function arsoundEdit$(_context4) {
          while (1) {
            switch (_context4.prev = _context4.next) {
              case 0:
                if (!_ThirdPartyHeadFile.Host.isAndroid) {
                  _context4.next = 7;
                  break;
                }

                _context4.next = 3;
                return _regenerator.default.awrap(this.checkWritePermissions());

              case 3:
                writePermissionsRes = _context4.sent;

                _CommonHeadFile.LogUtils.reportLog("****arsoundEdit>>>writePermissionsRes:" + writePermissionsRes);

                if (writePermissionsRes) {
                  _context4.next = 7;
                  break;
                }

                return _context4.abrupt("return");

              case 7:
                _CommonHeadFile.LogUtils.log("****arsoundEdit>>>ENTER");

                data = {
                  ar_type: 2,
                  picSize: {
                    width: 1040,
                    height: 1560
                  },
                  maxSize: 1536,
                  sandBoxFolder: _ThirdPartyHeadFile.Host.file.storageBasePath + "/" + _ServicesHeadFile.PATHS.TEMP_DIR
                };
                arModule.ar_edit_choose(data, function (result) {
                  _this21.arPicEdit(result, false);
                });

              case 10:
              case "end":
                return _context4.stop();
            }
          }
        }, null, this);
      }
    }, {
      key: "arPicEdit",
      value: function arPicEdit(data, isVideo) {
        var _this22 = this;

        if (data.code != 0) {
          return;
        }

        var picPaths = data.data.paths;
        var objectType = 4;

        if (isVideo) {
          objectType = 3;
        }

        _logUtils.default.reportLog("ricotta home compressImageWithPaths: " + JSON.stringify(data));

        this.showLoadingTips(_resources.default.getString("toast_loading"));

        _ServicesHeadFile.ImageExtension.compressImageWithPaths(picPaths, function (sucess, paths) {
          _this22.dismissTips();

          _logUtils.default.reportLog("ricotta home compressImageWithPaths: " + JSON.stringify(paths));

          if (sucess) {
            if (paths && paths.length > 0) {
              (0, _temp.setImageTempData)(paths);
              setTimeout(function () {
                _this22.props.navigation.navigate('ricottaPreviewImage', {
                  fileFrom: _ServicesHeadFile.PATHS.HOME,
                  extra: data.data.exif,
                  videoInfo: data.data.avInfo,
                  objectType: objectType,
                  waterInfoSubtitle: _this22.state.waterInfoSubtitle,
                  openPhotoPicker: function openPhotoPicker() {}
                });
              }, 750);
            } else {
              _this22.showFailTips(_resources.default.getString('toast_process_fail'));
            }
          } else {
            _this22.showFailTips(_resources.default.getString('toast_process_fail'));
          }
        }, 1536);
      }
    }, {
      key: "startARScan",
      value: function startARScan(data) {
        var _this23 = this;

        return _regenerator.default.async(function startARScan$(_context5) {
          while (1) {
            switch (_context5.prev = _context5.next) {
              case 0:
                _CommonHeadFile.LogUtils.reportLog("-startARScan--");

                _ThirdPartyHeadFile.Host.storage.get('AR_VIDEO_HELP_VIEWED').then(function (viewed) {
                  if (!viewed) {
                    _this23.setState({
                      showARUseHelp: true,
                      arUseHelpConfig: {
                        needNext: true,
                        onContinue: function onContinue() {
                          _this23.setState({
                            showARUseHelp: false
                          }, function () {
                            _this23.startARVideoEdit();
                          });
                        },
                        onLater: function onLater() {
                          _this23.setState({
                            showARUseHelp: false
                          }, function () {
                            if (_ThirdPartyHeadFile.Host.isAndroid) {
                              _this23.checkCameraPermissions().then(function (res) {
                                _CommonHeadFile.LogUtils.reportLog("-checkCameraPermissions--" + res);

                                if (res) {
                                  arModule.ar_scan(data);
                                }
                              }).catch(function (error) {
                                _CommonHeadFile.LogUtils.reportLog("-checkCameraPermissions--error-" + error);
                              });
                            } else {
                              _this23.checkIOSSystermPermisssions('9').then(function (res) {
                                if (res) {
                                  arModule.ar_scan(data);
                                }
                              }).catch(function (error) {
                                _CommonHeadFile.LogUtils.reportLog("-checkIOSSystermPermisssions--error-" + JSON.stringify(error));
                              });
                            }
                          });
                        }
                      }
                    });
                  } else {
                    if (_ThirdPartyHeadFile.Host.isAndroid) {
                      _this23.checkCameraPermissions().then(function (res) {
                        _CommonHeadFile.LogUtils.reportLog("-checkCameraPermissions--" + res);

                        if (res) {
                          arModule.ar_scan(data);
                        }
                      }).catch(function (error) {
                        _CommonHeadFile.LogUtils.reportLog("-checkCameraPermissions--error-" + error);
                      });
                    } else {
                      _this23.checkIOSSystermPermisssions('9').then(function (res) {
                        if (res) {
                          arModule.ar_scan(data);
                        }
                      }).catch(function (error) {
                        _CommonHeadFile.LogUtils.reportLog("-checkIOSSystermPermisssions--error-" + JSON.stringify(error));
                      });
                    }
                  }
                }).catch(function (error) {
                  _this23.setState({
                    showARUseHelp: true,
                    arUseHelpConfig: {
                      needNext: true,
                      onContinue: function onContinue() {
                        _this23.setState({
                          showARUseHelp: false
                        }, function () {
                          if (_ThirdPartyHeadFile.Host.isAndroid) {
                            _this23.checkCameraPermissions().then(function (res) {
                              _CommonHeadFile.LogUtils.reportLog("-checkCameraPermissions--" + res);

                              if (res) {
                                arModule.ar_scan(data);
                              }
                            }).catch(function (error) {
                              _CommonHeadFile.LogUtils.reportLog("-checkCameraPermissions--error-" + error);
                            });
                          } else {
                            arModule.ar_scan(data);
                          }
                        });
                      }
                    }
                  });
                });

              case 2:
              case "end":
                return _context5.stop();
            }
          }
        });
      }
    }, {
      key: "arScan",
      value: function arScan() {
        var _this24 = this;

        if (this.state.userActionScan) {
          _logUtils.default.reportLog(">>>\u70B9\u51FB\u626B\u4E00\u626Breturn>>userActionScan:" + this.state.userActionScan);

          return;
        }

        this.setState({
          userActionScan: true
        });

        _request.default.arRrefreshToken().then(function (response) {
          _logUtils.default.reportLog('刷新token的回调' + JSON.stringify(response));

          var allClctIds = [].concat((0, _toConsumableArray2.default)(response.data.result.pub_clct_ids), (0, _toConsumableArray2.default)(response.data.result.pvt_clct_ids), (0, _toConsumableArray2.default)(response.data.result.shr_clct_ids));
          var allClctIdStr = '';

          if (allClctIds.length > 1) {
            allClctIdStr = allClctIds.join(',');
          } else if (allClctIds.length === 1) {
            allClctIdStr = allClctIds[0];
          } else {
            allClctIdStr = '';
          }

          _ThirdPartyHeadFile.Host.storage.get(MINT_KEY_AR_SCAN_COLLECTION_IDS).then(function (lastCollectionIds) {
            if (lastCollectionIds && lastCollectionIds.length > 0) {
              var lastCollectionIdsArray = lastCollectionIds.split(',');
              var currentCollectionIdsArray = allClctIdStr.split(',');

              for (var i = 0; i < lastCollectionIdsArray.length; i++) {
                if (!currentCollectionIdsArray.includes(lastCollectionIdsArray[i])) {
                  if (_ThirdPartyHeadFile.Host.isIOS) {
                    arModule.ar_cleanARSDKCache({});
                  } else {
                    arModule.ar_cleanARSDKCache();
                  }

                  _CommonHeadFile.LogUtils.reportLog("ricotta home arScan cleanARSDKCache");
                }
              }
            }

            _ThirdPartyHeadFile.Host.storage.set('MINT_KEY_AR_SCAN_COLLECTION_IDS', allClctIdStr);

            _CommonHeadFile.LogUtils.reportLog("ricotta home arScan allClctIdStr = " + allClctIdStr);

            _CommonHeadFile.LogUtils.reportLog("ricotta home arScan lastCollectionIds = " + lastCollectionIds);

            var token = response.data.result.token;
            var data = {
              collectionIds: allClctIdStr,
              token: token,
              arUrl: _index2.default.hiarUrl,
              apiKey: _index2.default.hanntoHiarKey,
              apiSecret: _index2.default.hanntoHiarStr
            };

            _logUtils.default.reportLog('刷新token的回调2' + JSON.stringify(data));

            _this24.startARScan(data);

            _this24.setState({
              userActionScan: false
            });
          });
        }).catch(function (error) {
          _logUtils.default.reportLog('刷新token的失败回调catch:' + JSON.stringify(error));

          _CommonHeadFile.HTSingleton.toast.show(_resources.default.getString('network_error_toast'));

          _this24.setState({
            userActionScan: false
          });
        });
      }
    }, {
      key: "myARPhotos",
      value: function myARPhotos() {
        var _this25 = this;

        _ThirdPartyHeadFile.Host.storage.get('AR_VIDEO_HELP_VIEWED').then(function (viewed) {
          if (!viewed) {
            _this25.setState({
              showARUseHelp: true,
              arUseHelpConfig: {
                needNext: true,
                onContinue: function onContinue() {
                  _this25.setState({
                    showARUseHelp: false
                  }, function () {
                    _this25.startARVideoEdit();
                  });
                },
                onLater: function onLater() {
                  _this25.setState({
                    showARUseHelp: false
                  }, function () {
                    _this25.props.navigation.navigate("ARPhotoList");
                  });
                }
              }
            });
          } else {
            _this25.props.navigation.navigate("ARPhotoList");
          }
        });
      }
    }, {
      key: "launchCamaraImage",
      value: function launchCamaraImage() {
        this.navigateTo('PhotoBooth');
      }
    }, {
      key: "chooseLivePhoto",
      value: function chooseLivePhoto() {
        this.functionType = _consts.JOB_TYPE.LIVE_PHOTO;
        this.checkTipsDialog();
      }
    }, {
      key: "checkTipsDialog",
      value: function checkTipsDialog() {
        if (this.state.homePageAvaliable) {
          this.debouncedGoFunction();
        } else {
          _logUtils.default.reportLog("设备未授权");
        }
      }
    }, {
      key: "checkWritePermissions",
      value: function checkWritePermissions() {
        var permissionRes, writePermissionGranted, writePermissionResult;
        return _regenerator.default.async(function checkWritePermissions$(_context6) {
          while (1) {
            switch (_context6.prev = _context6.next) {
              case 0:
                permissionRes = false;
                _context6.next = 3;
                return _regenerator.default.awrap(_reactNative.PermissionsAndroid.check(_reactNative.PermissionsAndroid.PERMISSIONS.WRITE_EXTERNAL_STORAGE));

              case 3:
                writePermissionGranted = _context6.sent;

                if (writePermissionGranted) {
                  _context6.next = 11;
                  break;
                }

                _context6.next = 7;
                return _regenerator.default.awrap(_reactNative.PermissionsAndroid.request(_reactNative.PermissionsAndroid.PERMISSIONS.WRITE_EXTERNAL_STORAGE));

              case 7:
                writePermissionResult = _context6.sent;

                if (writePermissionResult !== _reactNative.PermissionsAndroid.RESULTS.GRANTED) {
                  if (writePermissionResult === _reactNative.PermissionsAndroid.RESULTS.DENIED) {
                    _DynamicUtil.default.trackEvent(_consts.BIGDATA_KEYS.APP_PERF_EVENT_PERMISSIONDENIED, {
                      type: 0
                    });

                    _logUtils.default.reportLog("Home--> 获取读写权限失败");
                  } else if (writePermissionResult === _reactNative.PermissionsAndroid.RESULTS.NEVER_ASK_AGAIN) {
                    _DynamicUtil.default.trackEvent(_consts.BIGDATA_KEYS.APP_PERF_EVENT_PERMISSIONDENIED, {
                      type: 0
                    });

                    _logUtils.default.reportLog("Home--> 获取读写权限失败1");
                  }

                  permissionRes = false;
                } else {
                  _logUtils.default.reportLog("Home--> 你已获取了读写权限1");

                  permissionRes = true;
                }

                _context6.next = 13;
                break;

              case 11:
                _logUtils.default.reportLog("Home--> 你已获取了读写权限2");

                permissionRes = true;

              case 13:
                return _context6.abrupt("return", permissionRes);

              case 14:
              case "end":
                return _context6.stop();
            }
          }
        });
      }
    }, {
      key: "checkCameraPermissions",
      value: function checkCameraPermissions() {
        var permissionRes, cameraPermissionGranted, cameraPermissionResult;
        return _regenerator.default.async(function checkCameraPermissions$(_context7) {
          while (1) {
            switch (_context7.prev = _context7.next) {
              case 0:
                permissionRes = false;
                _context7.next = 3;
                return _regenerator.default.awrap(_reactNative.PermissionsAndroid.check(_reactNative.PermissionsAndroid.PERMISSIONS.CAMERA));

              case 3:
                cameraPermissionGranted = _context7.sent;

                if (cameraPermissionGranted) {
                  _context7.next = 11;
                  break;
                }

                _context7.next = 7;
                return _regenerator.default.awrap(_reactNative.PermissionsAndroid.request(_reactNative.PermissionsAndroid.PERMISSIONS.CAMERA));

              case 7:
                cameraPermissionResult = _context7.sent;

                if (cameraPermissionResult !== _reactNative.PermissionsAndroid.RESULTS.GRANTED) {
                  if (cameraPermissionResult === _reactNative.PermissionsAndroid.RESULTS.DENIED) {
                    _DynamicUtil.default.trackEvent(_consts.BIGDATA_KEYS.APP_PERF_EVENT_PERMISSIONDENIED, {
                      type: 1
                    });

                    _logUtils.default.reportLog("Home--> 获取相机权限失败");
                  } else if (cameraPermissionResult === _reactNative.PermissionsAndroid.RESULTS.NEVER_ASK_AGAIN) {
                    _DynamicUtil.default.trackEvent(_consts.BIGDATA_KEYS.APP_PERF_EVENT_PERMISSIONDENIED, {
                      type: 1
                    });

                    _logUtils.default.reportLog("Home--> 获取相机权限失败1");
                  }

                  permissionRes = false;
                } else {
                  _logUtils.default.reportLog("Home--> 你已获取了相机权限1");

                  permissionRes = true;
                }

                _context7.next = 13;
                break;

              case 11:
                _logUtils.default.reportLog("Home--> 你已获取了相机权限2");

                permissionRes = true;

              case 13:
                return _context7.abrupt("return", permissionRes);

              case 14:
              case "end":
                return _context7.stop();
            }
          }
        });
      }
    }, {
      key: "checkIOSSystermPermisssions",
      value: function checkIOSSystermPermisssions(accessType) {
        return _regenerator.default.async(function checkIOSSystermPermisssions$(_context8) {
          while (1) {
            switch (_context8.prev = _context8.next) {
              case 0:
                return _context8.abrupt("return", new Promise(function (resolve, reject) {
                  var param = {
                    accessType: accessType
                  };

                  try {
                    cameraModule.checkIOSAccessForType(param, function (result) {
                      if (result.code == 0) {
                        resolve(true);
                      } else {
                        resolve(false);
                      }
                    });
                  } catch (error) {
                    reject(error);
                  }
                }));

              case 1:
              case "end":
                return _context8.stop();
            }
          }
        });
      }
    }, {
      key: "goFunction",
      value: function goFunction() {
        var writePermissionGranted, writePermissionResult;
        return _regenerator.default.async(function goFunction$(_context9) {
          while (1) {
            switch (_context9.prev = _context9.next) {
              case 0:
                _logUtils.default.reportLog("Home--> goFunction this.functionType = " + this.functionType);

                if (!_ThirdPartyHeadFile.Host.isAndroid) {
                  _context9.next = 19;
                  break;
                }

                if (!(this.functionType === _consts.JOB_TYPE.PHOTO_PRINT_JOB || this.functionType === _consts.JOB_TYPE.JIGSAW_JOB || this.functionType === _consts.JOB_TYPE.SPLIT_JOB)) {
                  _context9.next = 19;
                  break;
                }

                _context9.next = 5;
                return _regenerator.default.awrap(_reactNative.PermissionsAndroid.check(_reactNative.PermissionsAndroid.PERMISSIONS.WRITE_EXTERNAL_STORAGE));

              case 5:
                writePermissionGranted = _context9.sent;

                if (writePermissionGranted) {
                  _context9.next = 18;
                  break;
                }

                _context9.next = 9;
                return _regenerator.default.awrap(_reactNative.PermissionsAndroid.request(_reactNative.PermissionsAndroid.PERMISSIONS.WRITE_EXTERNAL_STORAGE));

              case 9:
                writePermissionResult = _context9.sent;

                if (!(writePermissionResult !== _reactNative.PermissionsAndroid.RESULTS.GRANTED)) {
                  _context9.next = 15;
                  break;
                }

                if (writePermissionResult === _reactNative.PermissionsAndroid.RESULTS.DENIED) {
                  _DynamicUtil.default.trackEvent(_consts.BIGDATA_KEYS.APP_PERF_EVENT_PERMISSIONDENIED, {
                    type: 0
                  });

                  _logUtils.default.reportLog("Home--> 获取读写权限失败");
                } else if (writePermissionResult === _reactNative.PermissionsAndroid.RESULTS.NEVER_ASK_AGAIN) {
                  _DynamicUtil.default.trackEvent(_consts.BIGDATA_KEYS.APP_PERF_EVENT_PERMISSIONDENIED, {
                    type: 0
                  });

                  _logUtils.default.reportLog("Home--> 获取读写权限失败1");
                }

                return _context9.abrupt("return");

              case 15:
                _logUtils.default.reportLog("Home--> 你已获取了读写权限1");

              case 16:
                _context9.next = 19;
                break;

              case 18:
                _logUtils.default.reportLog("Home--> 你已获取了读写权限2");

              case 19:
                this.jumpToFunction();

              case 20:
              case "end":
                return _context9.stop();
            }
          }
        }, null, this);
      }
    }, {
      key: "jumpToFunction",
      value: function jumpToFunction() {
        if (this.functionType === _consts.JOB_TYPE.PHOTO_PRINT_JOB) {
          this._openPhotoPickerView();
        } else if (this.functionType === _consts.JOB_TYPE.JIGSAW_JOB) {
          this.imagePuzzle();
        } else if (this.functionType === _consts.JOB_TYPE.SPLIT_JOB) {
          this.imageSplit();
        } else if (this.functionType === _consts.JOB_TYPE.HEADSHOT_JOB) {
          this.props.navigation.navigate("HeadshotIndex");
        } else if (this.functionType === _consts.JOB_TYPE.PHOTO_BOOTH) {
          this.launchCamaraImage();
        } else if (this.functionType === AR_PHOTO_SCAN) {
          if (!this.checkRomPackage()) {
            this.arScan();
          }
        } else if (this.functionType === _consts.JOB_TYPE.PHONOGRAPH_JOB) {
          if (!this.checkRomPackage()) {
            this.arsoundEdit();
          }
        } else if (this.functionType === _consts.JOB_TYPE.AR_PHOTO_JOB) {
          if (!this.checkRomPackage()) {
            this.arVideoEdit();
          }
        } else if (this.functionType === _consts.JOB_TYPE.LIVE_PHOTO) {
          this.checkLivePhotoMessage();
        } else if (this.functionType == _consts.JOB_TYPE.PHOTO_TEMPLATE) {
          this.openTemplateSelectPage();
        }
      }
    }, {
      key: "openTemplateSelectPage",
      value: function openTemplateSelectPage() {
        this.props.navigation.navigate('TemplateSelectPage', {
          waterInfoSubtitle: this.state.waterInfoSubtitle
        });
      }
    }, {
      key: "checkLivePhotoMessage",
      value: function checkLivePhotoMessage() {
        var _this26 = this;

        if (_ThirdPartyHeadFile.Host.isAndroid) {
          this.startLivePhoto();
          return;
        }

        var param = {
          accessType: '5'
        };
        cameraModule.checkIOSAccessForType(param, function (result) {
          if (result.code == 0) {
            _this26.startLivePhoto();
          } else {}
        });
      }
    }, {
      key: "startLivePhoto",
      value: function startLivePhoto() {
        var _this27 = this;

        _ThirdPartyHeadFile.Host.storage.get(_consts.RICOTTA_KEY_LIVEPHOTO_FIRST_CHECK).then(function (result) {
          if (result == null || result == '' || result == undefined || result == false) {
            _this27.props.navigation.navigate('LivePhotoMessage', {
              waterInfoSubtitle: _this27.state.waterInfoSubtitle
            });
          } else {
            _this27.openLivePhotoPage();
          }
        }).catch(function (err) {
          _logUtils.default.reportLog(" checkLivePhotoMessage " + err + "}");

          _this27.openLivePhotoPage();
        });
      }
    }, {
      key: "openLivePhotoPage",
      value: function openLivePhotoPage() {
        var _this28 = this;

        var writePermissionsRes, param;
        return _regenerator.default.async(function openLivePhotoPage$(_context10) {
          while (1) {
            switch (_context10.prev = _context10.next) {
              case 0:
                if (!_ThirdPartyHeadFile.Host.isAndroid) {
                  _context10.next = 7;
                  break;
                }

                _context10.next = 3;
                return _regenerator.default.awrap(this.checkWritePermissions());

              case 3:
                writePermissionsRes = _context10.sent;

                _CommonHeadFile.LogUtils.reportLog("****LivePhoto>>>writePermissionsRes:" + writePermissionsRes);

                if (writePermissionsRes) {
                  _context10.next = 7;
                  break;
                }

                return _context10.abrupt("return");

              case 7:
                _CommonHeadFile.LogUtils.log("****openLivePhotoPage>>>ENTER");

                param = {
                  sandBoxFolder: _ThirdPartyHeadFile.Host.file.storageBasePath + "/" + _ServicesHeadFile.PATHS.TEMP_DIR
                };

                _ThirdPartyHeadFile.Host.ui.openPickLivePhotoPage(param).then(function (result) {
                  _logUtils.default.reportLog("openPickLivePhotoPage result: " + JSON.stringify(result));

                  _this28.showLoadingTips(_resources.default.getString("toast_loading"));

                  _logUtils.default.reportLog("livePhoto callbackResult: " + JSON.stringify(result));

                  var resultData = JSON.parse(result.data.key_result_info);
                  var data = resultData.data;

                  if (resultData.code == 0) {
                    var picPath = data.path;

                    _ServicesHeadFile.ImageExtension.compressImageWithPaths([picPath], function (sucess, paths) {
                      _logUtils.default.reportLog("livephoto compressImageWithPaths: " + JSON.stringify(paths));

                      if (sucess && paths && paths.length > 0) {
                        (0, _temp.setImageTempData)(paths);
                        setTimeout(function () {
                          _this28.dismissTips();

                          _this28.props.navigation.navigate('ricottaPreviewImage', {
                            fileFrom: _ServicesHeadFile.PATHS.HOME,
                            extra: data.exif,
                            videoInfo: data.avInfo,
                            objectType: 3,
                            waterInfoSubtitle: _this28.state.waterInfoSubtitle,
                            isLivephotoPrint: true,
                            openPhotoPicker: function openPhotoPicker() {
                              _this28.openLivePhotoPage();
                            }
                          });
                        }, 750);
                      } else {
                        _this28.dismissTips();

                        _CommonHeadFile.HTSingleton.toast.show(_resources.default.getString('toast_process_fail'));
                      }
                    }, 1536);
                  } else {
                    _this28.dismissTips();

                    _CommonHeadFile.HTSingleton.toast.show(_resources.default.getString('toast_process_fail'));
                  }
                }).catch(function (error) {
                  _logUtils.default.reportLog("openPickLivePhotoPage result: " + JSON.stringify(error));
                });

              case 10:
              case "end":
                return _context10.stop();
            }
          }
        }, null, this);
      }
    }, {
      key: "checkRomPackage",
      value: function checkRomPackage() {
        if (_ThirdPartyHeadFile.Host.isAndroid && _ThirdPartyHeadFile.Host.isMiuiChannel) {
          this.setState({
            romPackageCheckDialogVisible: true
          });

          _DynamicUtil.default.trackEvent(_consts.BIGDATA_KEYS.APP_PG_HOME_EVT_POPROMCHECK, {});

          return true;
        } else {
          return false;
        }
      }
    }, {
      key: "handleAlert",
      value: function handleAlert() {
        var _this29 = this;

        if (this.state.connectionState == _consts.MINT_BLUETOOTH_STATE.DISCONNECTED || this.state.connectionState == _consts.MINT_BLUETOOTH_STATE.AUTH_FAILED) {} else if (this.state.deviceStatus.category == "error") {
          _errorHandle.default.handleDeviceState(this.props.navigation, this.state.deviceStatus, true, {
            showErrorDialog: function showErrorDialog(result) {
              _this29.setState({
                errorDetail: result,
                showErrorDialog: true
              });
            },
            hideErrorDialog: function hideErrorDialog() {
              _this29.setState({
                showErrorDialog: false
              });
            },
            fromView: function fromView(data, navigation) {}
          });
        } else {}
      }
    }, {
      key: "showHomeTipsDialog",
      value: function showHomeTipsDialog() {
        var _this30 = this;

        return _react.default.createElement(_reactNative.View, null, _react.default.createElement(_BaseDialog.BaseDialog, {
          visible: this.state.tipDiallogVisible,
          title: _ThirdPartyHeadFile.language.getString('home_space_help_title'),
          onDismiss: function onDismiss() {
            _this30.setState({
              tipDiallogVisible: false
            });
          },
          btnContainer: {
            marginTop: (0, _ThirdPartyHeadFile.scale)(13)
          },
          buttons: [{
            text: _ThirdPartyHeadFile.language.getString('button_ok'),
            style: {
              width: _consts.WIDTH - (0, _ThirdPartyHeadFile.scale)(54),
              height: (0, _ThirdPartyHeadFile.scale)(46),
              flex: 1,
              height: (0, _ThirdPartyHeadFile.scale)(46),
              borderRadius: (0, _ThirdPartyHeadFile.scale)(16),
              justifyContent: 'center',
              alignItems: 'center',
              backgroundColor: _CommonHeadFile.HTColor.c_btn_blue
            },
            textStyle: {
              color: _CommonHeadFile.HTColor.c_font_blueBtn,
              fontWeight: 'bold',
              fontSize: (0, _ThirdPartyHeadFile.scaleForPad)(16)
            },
            onPress: function onPress() {
              if (_this30.state.checkBoxChecked) {
                _this30.tipsDialogNeedShow = false;
              }

              _this30.setState({
                tipDiallogVisible: false
              });

              _this30.debouncedGoFunction();
            }
          }]
        }, _react.default.createElement(_reactNative.View, {
          style: {
            flexDirection: 'column',
            flex: 1,
            paddingBottom: 27
          }
        }, _react.default.createElement(_reactNative.Text, {
          style: {
            width: _consts.WIDTH - (0, _ThirdPartyHeadFile.scale)(80),
            left: (0, _ThirdPartyHeadFile.scale)(40),
            fontSize: (0, _ThirdPartyHeadFile.scale)(15),
            color: _CommonHeadFile.HTColor.c_font_000000
          }
        }, _ThirdPartyHeadFile.language.getString('zink_use_txt')), _react.default.createElement(_reactNative.View, {
          style: {
            flexDirection: 'row',
            left: (0, _ThirdPartyHeadFile.scale)(40),
            top: (0, _ThirdPartyHeadFile.scale)(17),
            alignContent: 'center',
            alignItems: 'center'
          }
        }, _react.default.createElement(_ComponentsHeadFile.HTButton, {
          children: _react.default.createElement(_reactNative.Image, {
            style: {
              width: 22,
              height: 22,
              borderRadius: 11
            },
            source: this.state.checkBoxChecked ? _CommonHeadFile.HTImage.print_icon_selected : _CommonHeadFile.HTImage.print_icon_unselected
          }),
          onPress: function onPress(_) {
            var checkBoxChecked = _this30.state.checkBoxChecked;
            checkBoxChecked = !checkBoxChecked;

            _this30.setState({
              checkBoxChecked: checkBoxChecked
            });
          }
        }), _react.default.createElement(_reactNative.Text, {
          style: {
            left: 10,
            fontSize: (0, _ThirdPartyHeadFile.scale)(14),
            alignContent: 'center',
            color: _CommonHeadFile.HTColor.c_font_000000
          }
        }, _ThirdPartyHeadFile.language.getString('no_longer_remind_txt'))))));
      }
    }, {
      key: "componentDidUpdate",
      value: function componentDidUpdate(prevProps, prevState) {
        var _this31 = this;

        if (this.state.alertSubMessageVisible && !prevState.alertSubMessageVisible) {
          this.startIconRotation();
        } else if (!this.state.alertSubMessageVisible && prevState.alertSubMessageVisible) {
          this.state.iconRotateValue.setValue(0);
        }

        if (prevState.alertMessageVisible !== this.state.alertMessageVisible && this.state.showGuide) {
          requestAnimationFrame(function () {
            var _this31$_guideOverlay;

            (_this31$_guideOverlay = _this31._guideOverlayRef.current) == null ? undefined : _this31$_guideOverlay.remeasure();
          });
        }
      }
    }, {
      key: "startIconRotation",
      value: function startIconRotation() {
        if (this.state.alertSubMessageVisible && (this.state.connectionState === _consts.MINT_BLUETOOTH_STATE.CONNECTING || this.state.connectionState === _consts.MINT_BLUETOOTH_STATE.CONNECTING_NEED_ALERT)) {
          if (!this.state.isIconRotateAnimating) {
            this.setState({
              isIconRotateAnimating: true
            });
            this.state.iconRotateValue.setValue(0);

            _reactNative.Animated.loop(_reactNative.Animated.timing(this.state.iconRotateValue, {
              toValue: 1,
              duration: 1300,
              easing: _reactNative.Easing.sin,
              useNativeDriver: true
            })).start();
          } else {}
        } else {
          if (this.state.isIconRotateAnimating) {
            this.setState({
              isIconRotateAnimating: false
            });
            this.state.iconRotateValue.stopAnimation();
            this.state.iconRotateValue.setValue(0);
          } else {}
        }
      }
    }, {
      key: "renderPhotoBooth",
      value: function renderPhotoBooth() {
        var _this32 = this;

        return _react.default.createElement(_HomeCard.default, {
          onPress: function onPress() {
            _DynamicUtil.default.trackEvent(_consts.BIGDATA_KEYS.APP_PG_HOME_USG_PHOTOBOOTH, {});

            _this32.functionType = _consts.JOB_TYPE.PHOTO_BOOTH;

            _this32.checkTipsDialog();
          },
          title: _resources.default.getString('photo_booth'),
          subTitle: _resources.default.getString('photo_booth_text'),
          hideArrow: true,
          isMini: true,
          textStyle: styles.whiteColor,
          subTitleStyle: styles.subTitleStyle,
          icon: _CommonHeadFile.HTImage.homeIconPhotoBooth
        });
      }
    }, {
      key: "renderTemplate",
      value: function renderTemplate() {
        var _this33 = this;

        return _react.default.createElement(_HomeCard.default, {
          onPress: function onPress() {
            _DynamicUtil.default.trackEvent(_consts.BIGDATA_KEYS.APP_PG_HOME_USG_TEMPLATE, {});

            _this33.functionType = _consts.JOB_TYPE.PHOTO_TEMPLATE;

            _this33.checkTipsDialog();
          },
          title: _resources.default.getString('photo_template'),
          subTitle: _resources.default.getString('photo_template_text'),
          hideArrow: true,
          isMini: true,
          textStyle: styles.whiteColor,
          subTitleStyle: styles.subTitleStyle,
          icon: _CommonHeadFile.HTImage.homeIconPhotoTemplate
        });
      }
    }, {
      key: "loadCachedBannerImage",
      value: function loadCachedBannerImage(url) {
        var cachedPath;
        return _regenerator.default.async(function loadCachedBannerImage$(_context11) {
          while (1) {
            switch (_context11.prev = _context11.next) {
              case 0:
                _context11.prev = 0;
                _context11.next = 3;
                return _regenerator.default.awrap(_ImageCacheManager.default.getImage(url));

              case 3:
                cachedPath = _context11.sent;

                if (cachedPath) {
                  this.setState({
                    bannerCachePath: cachedPath
                  });
                }

                _context11.next = 10;
                break;

              case 7:
                _context11.prev = 7;
                _context11.t0 = _context11["catch"](0);

                _logUtils.default.reportLog("\u52A0\u8F7Dbanner\u7F13\u5B58\u56FE\u7247\u5931\u8D25: " + url, _context11.t0);

              case 10:
              case "end":
                return _context11.stop();
            }
          }
        }, null, this, [[0, 7]]);
      }
    }, {
      key: "render",
      value: function render() {
        var _this34 = this,
            _this$state$bannerSou;

        var _this$state = this.state,
            showDialog = _this$state.showDialog,
            dialogTimeout = _this$state.dialogTimeout,
            dialogTitle = _this$state.dialogTitle,
            bannerSource = _this$state.bannerSource;
        var rotate = this.state.iconRotateValue.interpolate({
          inputRange: [0, 1],
          outputRange: ['0deg', '720deg']
        });
        return _react.default.createElement(_reactNative.View, {
          style: {
            flex: 1,
            backgroundColor: _CommonHeadFile.HTColor.c_bg_general
          }
        }, _react.default.createElement(_reactNativeLinearGradient.default, {
          colors: _CommonHeadFile.HTColor.isDarkMode ? ['#39465A', '#000000'] : ['#CEE1FF', '#F7F7F7'],
          style: styles.gradientBackground
        }), this.renderTitlebar(), this.state.alertMessageVisible && _react.default.createElement(_reactNative.TouchableOpacity, {
          style: [styles.userContainer, {
            marginTop: (0, _ThirdPartyHeadFile.scale)(10)
          }],
          disable: !this.state.alertMessageEnable,
          onPress: function onPress() {
            _this34.handleAlert();
          }
        }, this.state.alertSubMessageVisible ? _react.default.createElement(_reactNative.Animated.Image, {
          source: this.state.alertIcon,
          style: [styles.avatar, {
            height: (0, _ThirdPartyHeadFile.scale)(30),
            width: (0, _ThirdPartyHeadFile.scale)(30),
            marginLeft: (0, _ThirdPartyHeadFile.scale)(22),
            transform: [{
              rotate: rotate
            }]
          }]
        }) : _react.default.createElement(_reactNative.Image, {
          source: this.state.alertIcon,
          style: [styles.avatar, {
            height: (0, _ThirdPartyHeadFile.scale)(30),
            width: (0, _ThirdPartyHeadFile.scale)(30),
            marginLeft: (0, _ThirdPartyHeadFile.scale)(22)
          }]
        }), _react.default.createElement(_reactNative.View, {
          style: styles.infoContainer
        }, _react.default.createElement(_reactNative.View, {
          style: styles.infoContainer1
        }, _react.default.createElement(_reactNative.Text, {
          style: [styles.name, {
            color: this.state.alertTextColor
          }]
        }, this.state.alertMessage), this.state.alertSubMessageVisible && _react.default.createElement(_reactNative.Text, {
          style: [styles.subName]
        }, this.state.alertSubMessage))), this.state.alertMessageEnable && _react.default.createElement(_reactNative.Image, {
          source: _CommonHeadFile.HTImage.print_ic_arrow,
          style: styles._icon,
          resizeMode: "contain"
        }), this.state.alertButtonVisiable && _react.default.createElement(_reactNative.TouchableOpacity, {
          style: [styles.clickButtonStyle, {
            backgroundColor: _CommonHeadFile.HTColor.c_alphaColor(!this.state.alertSubMessageVisible ? '#FF9900' : '#0D84FF', 0.1)
          }],
          disable: !this.state.alertMessageEnable,
          onPress: function onPress() {
            _this34.checkBluetoothPermission(false);
          }
        }, _react.default.createElement(_reactNative.Text, {
          style: [styles.clickButtonContentStyle, {
            color: !this.state.alertSubMessageVisible ? '#FF9900' : '#0D84FF',
            fontSize: _ThirdPartyHeadFile.language.getLanguage() == "zh" ? 13 : 12
          }],
          numberOfLines: 2
        }, _resources.default.getString("button_bt_connect")))), _react.default.createElement(_reactNative.TouchableOpacity, {
          style: [styles.userContainer, {
            marginTop: (0, _ThirdPartyHeadFile.scale)(10),
            backgroundColor: '#3899ff'
          }],
          onPress: function onPress() {
            _this34.detailButtonAction();
          }
        }, _react.default.createElement(_reactNative.Image, {
          source: _CommonHeadFile.HTImage.homeIconSstatus,
          style: [styles.avatar, {
            height: (0, _ThirdPartyHeadFile.scale)(30),
            width: (0, _ThirdPartyHeadFile.scale)(30),
            marginLeft: (0, _ThirdPartyHeadFile.scale)(22)
          }]
        }), _react.default.createElement(_reactNative.View, {
          style: styles.infoContainer
        }, _react.default.createElement(_reactNative.View, {
          style: styles.infoContainer1
        }, _react.default.createElement(_reactNative.Text, {
          style: [styles.stateName, {
            color: '#ffffff'
          }]
        }, this.state.printerMainStatusText)), _react.default.createElement(_reactNative.Text, {
          style: [styles.stateSubName, {
            color: _CommonHeadFile.HTColor.c_alphaColor('#ffffff', 0.6)
          }]
        }, this.state.printerSubStatusText)), _react.default.createElement(_reactNative.Image, {
          source: _CommonHeadFile.HTImage.ic_arrow,
          style: styles._icon,
          resizeMode: "contain"
        })), _react.default.createElement(_reactNative.ScrollView, {
          style: styles.container,
          contentContainerStyle: {
            paddingBottom: (0, _ThirdPartyHeadFile.getBottomSpace)() + (0, _ThirdPartyHeadFile.scale)(50)
          },
          showsHorizontalScrollIndicator: false,
          showsVerticalScrollIndicator: false
        }, bannerSource && typeof bannerSource === 'object' && Object.keys(bannerSource).length > 0 && _react.default.createElement(_reactNative.TouchableOpacity, {
          onPress: function onPress() {
            _DynamicUtil.default.trackEvent(_consts.BIGDATA_KEYS.APP_PG_HOME_USG_HELP, {});

            if (_deviceConfig.default.isRicottaG()) {
              _ThirdPartyHeadFile.Host.ui.openWebPage(_JumpUrlUtil.default.ricottaGuideUrl());

              return;
            }

            if (bannerSource.redirect_type === 2) {
              _DynamicUtil.default.trackEvent(_consts.BIGDATA_KEYS.APP_PG_HOME_EVT_ARTEMPLATE, {});

              _this34.props.navigation.navigate('TemplateVideoPage', {
                bannerData: bannerSource,
                waterInfoSubtitle: _this34.state.waterInfoSubtitle
              });
            } else {
              var _bannerSource$link_ur;

              if ((bannerSource == null ? undefined : bannerSource.link_url) && (bannerSource == null ? undefined : (_bannerSource$link_ur = bannerSource.link_url) == null ? undefined : _bannerSource$link_ur.length) > 0) {
                _ThirdPartyHeadFile.Host.ui.openWebPage(bannerSource == null ? undefined : bannerSource.link_url);
              }
            }
          }
        }, _deviceConfig.default.isRicottaG() && _react.default.createElement(_reactNative.View, null, _react.default.createElement(_reactNative.Image, {
          style: styles.img,
          source: _CommonHeadFile.HTImage.ricottaGHomeImage,
          resizeMode: 'cover'
        }), _react.default.createElement(_reactNative.View, {
          style: styles.bannerTextView
        }, _react.default.createElement(_reactNative.Text, {
          style: [styles.bannerText]
        }, _deviceConfig.default.getDeviceName()), _react.default.createElement(_reactNative.Text, {
          style: [styles.bannerText, {
            marginTop: (0, _ThirdPartyHeadFile.scale)(4),
            color: '#000000',
            lineHeight: (0, _ThirdPartyHeadFile.scale)(18)
          }],
          numberOfLines: 4
        }, _resources.default.getString('beginner_guide_video')))), (_deviceConfig.default.isRicotta() || _deviceConfig.default.isRicottaP()) && _react.default.createElement(_reactNative.Image, {
          style: styles.img,
          source: this.state.bannerCachePath ? {
            uri: this.state.bannerCachePath
          } : {
            uri: (_this$state$bannerSou = this.state.bannerSource) == null ? undefined : _this$state$bannerSou.img_url
          },
          resizeMode: 'cover',
          onError: function onError(e) {
            var _this34$state$bannerS;

            _logUtils.default.reportLog('Banner image load error:', e.nativeEvent.error + ' ' + ((_this34$state$bannerS = _this34.state.bannerSource) == null ? undefined : _this34$state$bannerS.img_url));
          }
        })), _react.default.createElement(_reactNative.View, {
          style: [styles.moreTitleWrap, {
            height: (0, _ThirdPartyHeadFile.scale)(22),
            marginTop: (0, _ThirdPartyHeadFile.scale)(20)
          }]
        }, _react.default.createElement(_reactNative.Text, {
          style: styles.titleText
        }, _resources.default.getString('home_navi_classic_title'))), _react.default.createElement(_reactNative.View, {
          style: [styles.mainCardWrapItem, styles.doubleCardWrapItem]
        }, _react.default.createElement(_reactNative.View, {
          ref: this._guidePhotoPrintRef,
          collapsable: false,
          style: {
            alignSelf: 'flex-start'
          },
          onLayout: function onLayout() {
            if (_this34.state.showGuide) {
              var _this34$_guideOverlay;

              (_this34$_guideOverlay = _this34._guideOverlayRef.current) == null ? undefined : _this34$_guideOverlay.remeasure();
            }
          }
        }, _react.default.createElement(_HomeCard.default, {
          onPress: function onPress() {
            _DynamicUtil.default.trackEvent(_consts.BIGDATA_KEYS.APP_PG_HOME_USG_PHOTOPRINT, {});

            _this34.functionType = _consts.JOB_TYPE.PHOTO_PRINT_JOB;

            _this34.checkTipsDialog();
          },
          title: _resources.default.getString('home_navi_photo_title'),
          subTitle: _resources.default.getString('home_navi_photo_sub'),
          hideArrow: true,
          isMini: true,
          textStyle: styles.whiteColor,
          subTitleStyle: styles.subTitleStyle,
          icon: _CommonHeadFile.HTImage.homeIconPhoto
        })), this.state.suportLivePhoto && _react.default.createElement(_HomeCard.default, {
          onPress: function onPress() {
            _DynamicUtil.default.trackEvent(_consts.BIGDATA_KEYS.APP_PG_HOME_USG_LIVEPHOTO, {});

            _this34.functionType = _consts.JOB_TYPE.LIVE_PHOTO;

            _this34.checkTipsDialog();
          },
          title: _resources.default.getString('livephoto_title'),
          subTitle: _resources.default.getString('livephoto_text'),
          hideArrow: true,
          isMini: true,
          textStyle: styles.whiteColor,
          subTitleStyle: styles.subTitleStyle,
          icon: _CommonHeadFile.HTImage.livephoto_icon
        }), !this.state.suportLivePhoto && this.renderPhotoBooth()), (this.state.suportLivePhoto || _deviceConfig.default.isRicotta()) && _react.default.createElement(_reactNative.View, {
          style: [styles.doubleCardWrapItem, {
            marginTop: (0, _ThirdPartyHeadFile.scale)(10),
            height: (0, _ThirdPartyHeadFile.scale)(120)
          }]
        }, this.state.suportLivePhoto && this.renderPhotoBooth(), _deviceConfig.default.isRicotta() ? this.renderTemplate() : null), _react.default.createElement(_reactNative.View, {
          style: [styles.moreTitleWrap, {
            height: (0, _ThirdPartyHeadFile.scale)(22)
          }]
        }, _react.default.createElement(_reactNative.Text, {
          style: styles.titleText
        }, _resources.default.getString('home_navi_ar_title'))), _react.default.createElement(_reactNative.View, {
          style: [styles.mainCardWrapItem, styles.doubleCardWrapItem]
        }, _react.default.createElement(_HomeCard.default, {
          style: {
            width: (_consts.WIDTH - (0, _ThirdPartyHeadFile.scale)(32)) / 2
          },
          onPress: function onPress() {
            _DynamicUtil.default.trackEvent(_consts.BIGDATA_KEYS.APP_PG_HOME_USG_ARPHOTO, {});

            _this34.functionType = _consts.JOB_TYPE.AR_PHOTO_JOB;

            _this34.checkTipsDialog();
          },
          onLongPress: this.longPressAction.bind(this, 3),
          title: _resources.default.getString('home_navi_ar_title'),
          subTitle: _resources.default.getString('home_navi_ar_sub'),
          hideArrow: true,
          isMini: true,
          textStyle: styles.whiteColor,
          subTitleStyle: styles.subTitleStyle,
          icon: _CommonHeadFile.HTImage.homeIconArPhoto
        }), _react.default.createElement(_HomeCard.default, {
          style: {
            width: (_consts.WIDTH - (0, _ThirdPartyHeadFile.scale)(32)) / 2
          },
          onPress: function onPress() {
            _DynamicUtil.default.trackEvent(_consts.BIGDATA_KEYS.APP_PG_HOME_USG_AUDIOPHOTO, {});

            _this34.functionType = _consts.JOB_TYPE.PHONOGRAPH_JOB;

            _this34.checkTipsDialog();
          },
          title: _resources.default.getString('home_navi_music_title'),
          subTitle: _resources.default.getString('home_navi_music_sub'),
          hideArrow: true,
          isMini: true,
          textStyle: styles.whiteColor,
          subTitleStyle: styles.subTitleStyle,
          icon: _CommonHeadFile.HTImage.homeIconArAudio
        })), _react.default.createElement(_reactNative.View, {
          style: [styles.doubleCardWrapItem, {
            marginTop: (0, _ThirdPartyHeadFile.scale)(10),
            height: (0, _ThirdPartyHeadFile.scale)(120)
          }]
        }, _react.default.createElement(_HomeCard.default, {
          onPress: function onPress() {
            _DynamicUtil.default.trackEvent(_consts.BIGDATA_KEYS.APP_PG_HOME_USG_MYAR, {});

            _this34.myARPhotos();
          },
          onLongPress: this.longPressAction.bind(this, 3),
          title: _resources.default.getString('home_navi_ar_photo_title'),
          subTitle: _resources.default.getString('home_navi_scan_scaner_sub'),
          hideArrow: true,
          isMini: true,
          textStyle: styles.whiteColor,
          subTitleStyle: styles.subTitleStyle,
          icon: _CommonHeadFile.HTImage.homeIconMyAr
        }), _react.default.createElement(_HomeCard.default, {
          onPress: function onPress() {
            _DynamicUtil.default.trackEvent(_consts.BIGDATA_KEYS.APP_PG_HOME_USG_ARSCAN, {});

            _this34.functionType = AR_PHOTO_SCAN;

            _this34.checkTipsDialog();
          },
          title: _resources.default.getString('scan_ar_title'),
          subTitle: _resources.default.getString('home_navi_scan_phone_sub'),
          hideArrow: true,
          isMini: true,
          textStyle: styles.whiteColor,
          subTitleStyle: styles.subTitleStyle,
          icon: _CommonHeadFile.HTImage.homeIconArScan
        })), _react.default.createElement(_reactNative.View, {
          style: [styles.moreTitleWrap, {
            height: (0, _ThirdPartyHeadFile.scale)(22)
          }]
        }, _react.default.createElement(_reactNative.Text, {
          style: styles.titleText
        }, _resources.default.getString('home_navi_fun_title'))), _react.default.createElement(_reactNative.View, {
          style: [styles.funTemplateContainer]
        }, _react.default.createElement(_HomeCard.default, {
          onPress: function onPress() {
            _DynamicUtil.default.trackEvent(_consts.BIGDATA_KEYS.APP_PG_HOME_USG_PUZZLEPHOTO, {});

            _logUtils.default.reportLog("点击了拼图打印");

            _this34.functionType = _consts.JOB_TYPE.JIGSAW_JOB;

            _this34.checkTipsDialog();
          },
          title: _resources.default.getString('home_navi_splice_title'),
          subTitle: _resources.default.getString("home_navi_splice_sub"),
          hideArrow: false,
          isMini: false,
          textStyle: styles.name,
          subTitleStyle: styles.userId,
          icon: _CommonHeadFile.HTImage.homeIconPuzzle,
          height: (0, _ThirdPartyHeadFile.scale)(70)
        }), _react.default.createElement(_HomeCard.default, {
          onPress: function onPress() {
            _DynamicUtil.default.trackEvent(_consts.BIGDATA_KEYS.APP_PG_HOME_USG_SPLITPHOTO, {});

            _logUtils.default.reportLog("点击了拆图打印");

            _this34.functionType = _consts.JOB_TYPE.SPLIT_JOB;

            _this34.checkTipsDialog();
          },
          title: _resources.default.getString('home_navi_split_title'),
          subTitle: _resources.default.getString("home_navi_split_sub"),
          hideArrow: false,
          isMini: false,
          textStyle: styles.name,
          subTitleStyle: styles.userId,
          icon: _CommonHeadFile.HTImage.homeIconSplit,
          height: (0, _ThirdPartyHeadFile.scale)(70)
        }), _react.default.createElement(_HomeCard.default, {
          onPress: function onPress() {
            _DynamicUtil.default.trackEvent(_consts.BIGDATA_KEYS.APP_PG_HOME_USG_HEADSHOTPHOTO, {});

            _logUtils.default.reportLog("点击了大头照");

            _this34.functionType = _consts.JOB_TYPE.HEADSHOT_JOB;

            _this34.checkTipsDialog();
          },
          title: _resources.default.getString('home_navi_club_title'),
          subTitle: _resources.default.getString("home_navi_club_sub"),
          hideArrow: false,
          isMini: false,
          textStyle: styles.name,
          subTitleStyle: styles.userId,
          icon: _CommonHeadFile.HTImage.homeIconHead,
          height: (0, _ThirdPartyHeadFile.scale)(70)
        })), _react.default.createElement(_reactNative.View, {
          style: [styles.moreTitleWrap, {
            height: (0, _ThirdPartyHeadFile.scale)(22)
          }]
        }, _react.default.createElement(_reactNative.Text, {
          style: styles.titleText
        }, _resources.default.getString('home_navi_other_title'))), _react.default.createElement(_reactNative.View, {
          style: [styles.otherContainer]
        }, _ThirdPartyHeadFile.Device.isOwner && _react.default.createElement(_HomeCard.default, {
          style: {
            borderRadius: (0, _ThirdPartyHeadFile.scale)(15)
          },
          title: _resources.default.getString('home_navi_deviec_share_title'),
          hideArrow: false,
          isMini: false,
          textStyle: styles.name,
          subTitleStyle: styles.userId,
          icon: _CommonHeadFile.HTImage.homeIconDeviceShare,
          height: (0, _ThirdPartyHeadFile.scale)(70),
          onPress: function onPress() {
            _DynamicUtil.default.trackEvent(_consts.BIGDATA_KEYS.APP_PG_HOME_USG_DEVICESHARE, {});

            _logUtils.default.reportLog("设备分享");

            _ThirdPartyHeadFile.Host.storage.get(_consts.RICOTTA_KEY_DEVICE_SHARE_INTRODUCE).then(function (value) {
              if (value) {
                _ThirdPartyHeadFile.Host.ui.openShareDevicePage();
              } else {
                _this34.props.navigation.navigate("DeviceShareIntroduce");
              }
            }).catch(function (error) {
              _this34.props.navigation.navigate("DeviceShareIntroduce");
            });
          }
        }), !_deviceConfig.default.isRicottaG() && _react.default.createElement(_HomeCard.default, {
          style: {
            borderRadius: (0, _ThirdPartyHeadFile.scale)(15)
          },
          onPress: function onPress() {
            _DynamicUtil.default.trackEvent(_consts.BIGDATA_KEYS.APP_PG_HOME_USG_BUYLINK, {});

            _logUtils.default.reportLog("购买相纸");

            _this34.buyButtonPressAction();
          },
          title: _resources.default.getString('home_navi_buy_paper_title'),
          subTitle: _resources.default.getString('home_navi_buy_paper_sub'),
          hideArrow: false,
          isMini: false,
          icon: _CommonHeadFile.HTImage.homeIconBuyPhotoPaper,
          height: (0, _ThirdPartyHeadFile.scale)(70)
        })), this.state.showErrorDialog ? _react.default.createElement(_error_handle_dialog.default, {
          error: this.state.errorDetail
        }) : null), _react.default.createElement(_LoadingDialog.default, {
          visible: showDialog,
          text: dialogTitle,
          timeout: dialogTimeout
        }), _react.default.createElement(_HtMessageDialog.default, {
          title: _resources.default.getString('default_alert_title'),
          visible: this.state.fileSizeError,
          message: _resources.default.getString('doc_large_limit_txt', 20),
          buttons: [{
            text: _resources.default.getString('button_ok'),
            onPress: function onPress() {
              _this34.setState({
                fileSizeError: false
              });
            },
            textStyle: {
              color: '#ffffff',
              fontWeight: '600',
              fontSize: (0, _ThirdPartyHeadFile.scale)(16)
            },
            style: {
              flex: 1,
              height: (0, _ThirdPartyHeadFile.scale)(46),
              borderRadius: (0, _ThirdPartyHeadFile.scale)(16),
              justifyContent: 'center',
              alignItems: 'center',
              backgroundColor: '#0B84FF'
            }
          }]
        }), _react.default.createElement(_HtMessageDialog.default, {
          title: _resources.default.getString('default_alert_title'),
          visible: this.state.btParseErrorDialogVisible,
          message: _resources.default.getString('bt_parse_error_txt'),
          messageLineHeight: 24,
          buttons: [{
            text: _resources.default.getString('button_out'),
            onPress: function onPress() {
              _this34.setState({
                btParseErrorDialogVisible: false
              });

              _this34.exitApp();
            },
            textStyle: {
              color: '#ffffff',
              fontWeight: '600',
              fontSize: (0, _ThirdPartyHeadFile.scale)(16)
            },
            style: {
              flex: 1,
              height: (0, _ThirdPartyHeadFile.scale)(46),
              borderRadius: (0, _ThirdPartyHeadFile.scale)(16),
              justifyContent: 'center',
              alignItems: 'center',
              backgroundColor: '#0B84FF'
            }
          }]
        }), _react.default.createElement(_HtMessageDialog.default, {
          title: _resources.default.getString('important_alert_title'),
          visible: this.state.fwForceUpgradeVisible,
          message: this.state.fwForceUpgradeMessage,
          buttons: [{
            text: _resources.default.getString('update_later'),
            onPress: function onPress() {
              _this34.setState({
                fwForceUpgradeVisible: false
              });

              _this34.exitApp();
            }
          }, {
            text: _resources.default.getString('updateNow'),
            onPress: function onPress() {
              _this34.setState({
                fwForceUpgradeVisible: false
              });

              _this34.props.navigation.navigate('fwForceUpgrade', {
                fwForceInfo: fwForceResult,
                version: currentVersion
              });
            },
            style: {
              flex: 1,
              height: (0, _ThirdPartyHeadFile.scale)(46),
              width: (width - (0, _ThirdPartyHeadFile.scale)(27) * 2 - BUTTON_WIDTH) / 2,
              borderRadius: (0, _ThirdPartyHeadFile.scale)(16),
              justifyContent: 'center',
              alignItems: 'center',
              backgroundColor: _CommonHeadFile.HTColor.mi_blue,
              marginLeft: (0, _ThirdPartyHeadFile.scale)(12)
            }
          }]
        }), _react.default.createElement(_HtMessageDialog.default, {
          title: _resources.default.getString('default_alert_title'),
          visible: this.state.cameraPermissionVisible,
          message: this.state.cameraPermissionTip,
          buttons: [{
            text: _resources.default.getString('try_later'),
            onPress: function onPress() {
              _this34.setState({
                cameraPermissionVisible: false
              });
            }
          }, {
            text: _resources.default.getString('button_confirm'),
            onPress: function onPress() {
              _this34.setState({
                cameraPermissionVisible: false
              });

              _ThirdPartyHeadFile.Host.ui.openTerminalDeviceSettingPage(1);
            }
          }]
        }), _react.default.createElement(_HtMessageDialog.default, {
          title: _resources.default.getString('default_alert_title'),
          visible: this.state.fwUpgradeDialogVisible,
          message: _resources.default.getString('fw_update_txt'),
          buttons: [{
            text: _resources.default.getString('button_cancel'),
            onPress: function onPress() {
              _this34.setState({
                fwUpgradeDialogVisible: false
              });
            }
          }, {
            text: _resources.default.getString('button_update'),
            onPress: function onPress() {
              _this34.setState({
                fwUpgradeDialogVisible: false
              });

              _this34.props.navigation.navigate("mintFwUpgrade");
            }
          }]
        }), _react.default.createElement(_HtMessageDialog.default, {
          title: _resources.default.getString('default_alert_title'),
          visible: this.state.fwForceUpgradeDialogVisible,
          message: _resources.default.getString('fw_update_txt'),
          buttons: [{
            text: _resources.default.getString('button_update'),
            onPress: function onPress() {
              _this34.props.navigation.navigate("mintFwUpgrade");
            }
          }]
        }), _react.default.createElement(_HtMessageDialog.default, {
          title: _resources.default.getString('default_alert_title'),
          visible: this.state.romPackageCheckDialogVisible,
          message: _resources.default.getString('rom_package_check'),
          buttons: [{
            text: _resources.default.getString('button_ok'),
            onPress: function onPress() {
              _this34.setState({
                romPackageCheckDialogVisible: false
              });
            }
          }]
        }), _react.default.createElement(_HtMessageDialog.default, {
          title: _resources.default.getString('default_alert_title'),
          visible: this.state.batteryWrongStatusDialogVisible,
          message: _resources.default.getString('battery_wrong_status_context'),
          buttons: [{
            text: _resources.default.getString('button_ok'),
            onPress: function onPress() {
              _this34.setState({
                batteryWrongStatusDialogVisible: false
              });
            }
          }]
        }), _react.default.createElement(_ComponentsHeadFile.HTMessageDialog, {
          ref: function ref(v) {
            return _this34.createJobErrorDialog = v;
          }
        }), this.showHomeTipsDialog(), _react.default.createElement(_GuideOverlay.default, {
          ref: this._guideOverlayRef,
          visible: this.state.showGuide,
          steps: [{
            ref: this._guidePhotoPrintRef,
            tipText: _resources.default.getString('guide_photo_print_txt'),
            tipPosition: 'bottom',
            borderRadius: (0, _ThirdPartyHeadFile.scale)(15),
            padding: 0
          }],
          onFinish: function onFinish() {
            _this34.setState({
              showGuide: false
            });

            _GuideStorage.default.onFinish();
          },
          onSkip: function onSkip() {
            _this34.setState({
              showGuide: false
            });

            _GuideStorage.default.onSkip().catch(function () {});
          }
        }), _react.default.createElement(_HtMessageDialog.default, {
          title: _resources.default.getString('default_alert_title'),
          visible: this.state.iOSBlueTipDialogVisble,
          message: _resources.default.getString('bt_manual_connect_txt').replace('%s', _deviceConfig.default.getDeviceBluetoothName()),
          containerStyle: this.state.showGuide ? styles.dialogAboveGuide : undefined,
          buttons: [{
            text: _resources.default.getString('button_cancel'),
            onPress: function onPress() {
              _this34.setState({
                iOSBlueTipDialogVisble: false
              });
            }
          }, {
            text: _resources.default.getString('button_set'),
            onPress: function onPress() {
              _this34.setState({
                iOSBlueTipDialogVisble: false
              });

              _ThirdPartyHeadFile.Host.ui.openTerminalDeviceSettingPage(2);
            }
          }]
        }), _react.default.createElement(_HtMessageDialog.default, {
          title: _resources.default.getString('bluetooth_not_connected_title'),
          visible: this.state.longTimeConnectFailedDialogVisible,
          message: _resources.default.getString('bluetooth_not_connected_context'),
          leftAlign: true,
          containerStyle: this.state.showGuide ? styles.dialogAboveGuide : undefined,
          buttons: [{
            text: _resources.default.getString('button_check_faq'),
            onPress: function onPress() {
              _this34.setState({
                longTimeConnectFailedDialogVisible: false
              });

              _ThirdPartyHeadFile.Host.ui.openHelpPage();

              _bluetoothPrintManger.default.ShareInstance().connectAction();
            },
            textStyle: {
              fontSize: _ThirdPartyHeadFile.language.getLanguage() == 'ru' ? (0, _ThirdPartyHeadFile.scale)(15) : (0, _ThirdPartyHeadFile.scale)(16)
            }
          }, {
            text: _resources.default.getString('bluetooth_manuel_connect'),
            onPress: function onPress() {
              _this34.setState({
                longTimeConnectFailedDialogVisible: false
              });

              _bluetoothPrintManger.default.ShareInstance().connectAction();

              if (_ThirdPartyHeadFile.Host.isIOS) {
                _ThirdPartyHeadFile.Host.ui.openTerminalDeviceSettingPage(2);
              }
            }
          }]
        }), _react.default.createElement(_reactNative.Modal, {
          visible: this.state.showARUseHelp,
          transparent: false,
          animationType: "none",
          statusBarTranslucent: true,
          onRequestClose: function onRequestClose() {
            _this34.setState({
              showARUseHelp: false
            });
          }
        }, _react.default.createElement(_arUseHelp.default, {
          navigation: {
            state: {
              params: this.state.arUseHelpConfig
            },
            goBack: function goBack() {
              _this34.setState({
                showARUseHelp: false
              });
            }
          }
        })));
      }
    }]);
    return rocittaHome;
  }(_react.Component);

  exports.default = rocittaHome;

  rocittaHome.navigationOptions = function (_ref) {
    var navigation = _ref.navigation;
    return {
      header: null
    };
  };

  var styles = _reactNative.StyleSheet.create({
    container: {
      flex: 1,
      marginTop: (0, _ThirdPartyHeadFile.scale)(12)
    },
    statusWrap: {
      alignItems: 'center',
      height: (0, _ThirdPartyHeadFile.scale)(172)
    },
    statusGifWrap: {
      marginVertical: (0, _ThirdPartyHeadFile.scale)(0),
      width: 120,
      height: 64
    },
    statusTextWrap: {
      fontSize: (0, _ThirdPartyHeadFile.scale)(14),
      marginTop: 5,
      fontFamily: _CommonHeadFile.FONT_FAMILY.MILanPro_Light
    },
    mainCardWrap: {},
    mainCardWrapItem: {
      height: (0, _ThirdPartyHeadFile.scale)(120)
    },
    doubleCardWrapItem: {
      flexDirection: 'row',
      paddingHorizontal: (0, _ThirdPartyHeadFile.scale)(12),
      width: _consts.WIDTH,
      justifyContent: 'space-between'
    },
    moreTitleWrap: {
      paddingHorizontal: (0, _ThirdPartyHeadFile.scale)(16),
      justifyContent: 'center',
      marginTop: (0, _ThirdPartyHeadFile.scale)(14)
    },
    titleText: {
      color: '#8c93b0',
      fontSize: (0, _ThirdPartyHeadFile.scale)(13),
      marginLeft: (0, _ThirdPartyHeadFile.scale)(16),
      marginBottom: (0, _ThirdPartyHeadFile.scale)(6)
    },
    whiteColor: {
      color: _CommonHeadFile.HTColor.c_font_000000,
      fontSize: (0, _ThirdPartyHeadFile.scale)(15),
      marginTop: -(0, _ThirdPartyHeadFile.scale)(1)
    },
    subTitleStyle: {
      color: _CommonHeadFile.HTColor.c_font_000000_60,
      fontSize: (0, _ThirdPartyHeadFile.scale)(12)
    },
    bluetoothContainer: {
      width: _consts.WIDTH,
      height: _consts.WIDTH / 3240 * 738
    },
    normalCardBack: {
      width: _consts.WIDTH,
      height: _consts.WIDTH / 3240 * 738,
      paddingLeft: (0, _ThirdPartyHeadFile.scale)(78),
      paddingRight: (0, _ThirdPartyHeadFile.scale)(35),
      flexDirection: 'row'
    },
    bluetoothText: {
      fontSize: 16
    },
    connectButton: {
      padding: 8,
      borderRadius: 4,
      backgroundColor: '#4A90E2'
    },
    connectButtonText: {
      color: '#FFF'
    },
    warningContainer: {
      width: '80%',
      flexDirection: "row",
      padding: (0, _ThirdPartyHeadFile.scale)(12),
      backgroundColor: _CommonHeadFile.HTColor.c_home_warning_bg,
      marginHorizontal: (0, _ThirdPartyHeadFile.scale)(24),
      borderRadius: (0, _ThirdPartyHeadFile.scale)(16),
      position: "absolute",
      top: 0,
      justifyContent: 'space-between',
      alignItems: 'center'
    },
    warningText: {
      color: _CommonHeadFile.HTColor.c_font_warning,
      fontSize: (0, _ThirdPartyHeadFile.scale)(14)
    },
    warningIcon: {
      width: (0, _ThirdPartyHeadFile.scale)(6),
      height: (0, _ThirdPartyHeadFile.scale)(10)
    },
    userContainer: {
      flexDirection: 'row',
      backgroundColor: _CommonHeadFile.HTColor.c_card,
      marginHorizontal: (0, _ThirdPartyHeadFile.scale)(12),
      minHeight: (0, _ThirdPartyHeadFile.scale)(70),
      alignItems: 'center',
      borderRadius: (0, _ThirdPartyHeadFile.scale)(15)
    },
    modelContainer: {
      flexDirection: 'row',
      backgroundColor: _CommonHeadFile.HTColor.c_card,
      height: (0, _ThirdPartyHeadFile.scale)(70),
      alignItems: 'center'
    },
    avatar: {
      width: (0, _ThirdPartyHeadFile.scale)(28),
      height: (0, _ThirdPartyHeadFile.scale)(28),
      marginHorizontal: (0, _ThirdPartyHeadFile.scale)(10),
      marginLeft: (0, _ThirdPartyHeadFile.scale)(20)
    },
    infoContainer: {
      flex: 1,
      marginVertical: (0, _ThirdPartyHeadFile.scale)(15)
    },
    infoContainer1: {
      flexDirection: 'column',
      alignItems: 'flex-start'
    },
    _icon: {
      width: (0, _ThirdPartyHeadFile.scale)(22),
      height: (0, _ThirdPartyHeadFile.scale)(22),
      marginHorizontal: (0, _ThirdPartyHeadFile.scale)(20)
    },
    name: {
      fontSize: (0, _ThirdPartyHeadFile.scale)(16),
      fontWeight: 'bold',
      color: _CommonHeadFile.HTColor.c_font_000000
    },
    subName: {
      fontSize: (0, _ThirdPartyHeadFile.scale)(13),
      color: _CommonHeadFile.HTColor.c_font_000000_60,
      marginTop: (0, _ThirdPartyHeadFile.scale)(2),
      lineHeight: (0, _ThirdPartyHeadFile.scale)(18)
    },
    stateName: {
      fontSize: (0, _ThirdPartyHeadFile.scale)(15),
      fontWeight: 'bold',
      color: _CommonHeadFile.HTColor.c_font_FFFFFF
    },
    stateSubName: {
      fontSize: (0, _ThirdPartyHeadFile.scale)(12),
      color: _CommonHeadFile.HTColor.c_font_FFFFFF,
      marginTop: (0, _ThirdPartyHeadFile.scale)(2)
    },
    notShared: {
      color: _CommonHeadFile.HTColor.c_font_999999,
      backgroundColor: _CommonHeadFile.HTColor.c_btn_gray,
      fontSize: (0, _ThirdPartyHeadFile.scale)(10),
      fontWeight: 'bold',
      padding: (0, _ThirdPartyHeadFile.scale)(4),
      borderRadius: (0, _ThirdPartyHeadFile.scale)(4),
      marginLeft: (0, _ThirdPartyHeadFile.scale)(6)
    },
    userId: {
      marginTop: (0, _ThirdPartyHeadFile.scale)(3),
      fontSize: (0, _ThirdPartyHeadFile.scale)(12),
      color: _CommonHeadFile.HTColor.c_font_000000_60
    },
    img: {
      width: _consts.WIDTH - (0, _ThirdPartyHeadFile.scale)(12) * 2,
      height: (_consts.WIDTH - (0, _ThirdPartyHeadFile.scale)(12) * 2) * 339 / 1008,
      alignSelf: 'center',
      borderRadius: (0, _ThirdPartyHeadFile.scale)(16),
      marginHorizontal: (0, _ThirdPartyHeadFile.scale)(12),
      backgroundColor: _CommonHeadFile.HTColor.c_home_banner_bg
    },
    bannerTextView: {
      position: 'absolute',
      width: (_consts.WIDTH - (0, _ThirdPartyHeadFile.scale)(12) * 2) * 0.6,
      height: (_consts.WIDTH - (0, _ThirdPartyHeadFile.scale)(12) * 2) * 339 / 1008,
      marginLeft: (0, _ThirdPartyHeadFile.scale)(20),
      flex: 1,
      justifyContent: 'center'
    },
    bannerText: {
      fontSize: (0, _ThirdPartyHeadFile.scale)(12),
      color: '#000000',
      overflow: 'hidden'
    },
    gradientBackground: {
      position: 'absolute',
      top: 0,
      left: 0,
      right: 0,
      width: _consts.WIDTH,
      height: (0, _ThirdPartyHeadFile.scale)(191),
      zIndex: 0
    },
    navigationBar: {
      backgroundColor: 'transparent',
      elevation: 0,
      shadowOpacity: 0,
      borderBottomWidth: 0
    },
    clickButtonStyle: {
      minWidth: 68,
      maxWidth: 138,
      height: 36,
      marginRight: (0, _ThirdPartyHeadFile.scale)(8),
      justifyContent: 'center',
      borderRadius: 18
    },
    funTemplateContainer: {
      marginHorizontal: (0, _ThirdPartyHeadFile.scale)(12),
      backgroundColor: _CommonHeadFile.HTColor.c_card,
      borderRadius: (0, _ThirdPartyHeadFile.scale)(15),
      overflow: 'hidden'
    },
    otherContainer: {
      marginHorizontal: (0, _ThirdPartyHeadFile.scale)(12),
      backgroundColor: 'transparent',
      borderRadius: (0, _ThirdPartyHeadFile.scale)(15),
      overflow: 'hidden',
      paddingBottom: 10,
      flexDirection: 'column',
      justifyContent: 'space-between'
    },
    firstModelItem: {
      borderTopLeftRadius: (0, _ThirdPartyHeadFile.scale)(15),
      borderTopRightRadius: (0, _ThirdPartyHeadFile.scale)(15)
    },
    lastModelItem: {
      borderBottomLeftRadius: (0, _ThirdPartyHeadFile.scale)(15),
      borderBottomRightRadius: (0, _ThirdPartyHeadFile.scale)(15)
    },
    clickButtonContentStyle: {
      textAlign: 'center',
      marginHorizontal: 12,
      textAlignVertical: 'center'
    },
    dialogAboveGuide: {
      zIndex: 10001,
      elevation: 10001
    }
  });
},12863,[14317,14314,14368,14683,14329,14332,14383,14389,14386,14398,10297,10033,12866,12173,12203,12521,12494,12311,12374,12710,10077,12665,12167,12194,12332,12518,10034,12170,12416,10052,10074,12296,12452,12296,11518,12179,14308,12869,12197,12872,12884,12887,12905,12908,12911]);