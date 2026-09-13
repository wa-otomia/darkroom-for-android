__d(function (global, _$$_REQUIRE, _$$_IMPORT_DEFAULT, _$$_IMPORT_ALL, module, exports, _dependencyMap) {
  var _interopRequireWildcard = _$$_REQUIRE(_dependencyMap[0]);

  var _interopRequireDefault = _$$_REQUIRE(_dependencyMap[1]);

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  exports.default = undefined;

  var _extends2 = _interopRequireDefault(_$$_REQUIRE(_dependencyMap[2]));

  var _classCallCheck2 = _interopRequireDefault(_$$_REQUIRE(_dependencyMap[3]));

  var _possibleConstructorReturn2 = _interopRequireDefault(_$$_REQUIRE(_dependencyMap[4]));

  var _getPrototypeOf2 = _interopRequireDefault(_$$_REQUIRE(_dependencyMap[5]));

  var _assertThisInitialized2 = _interopRequireDefault(_$$_REQUIRE(_dependencyMap[6]));

  var _createClass2 = _interopRequireDefault(_$$_REQUIRE(_dependencyMap[7]));

  var _inherits2 = _interopRequireDefault(_$$_REQUIRE(_dependencyMap[8]));

  var _react = _interopRequireWildcard(_$$_REQUIRE(_dependencyMap[9]));

  var _reactNativeWebview = _$$_REQUIRE(_dependencyMap[10]);

  var _reactNative = _$$_REQUIRE(_dependencyMap[11]);

  var _consts = _$$_REQUIRE(_dependencyMap[12]);

  var _ThirdPartyHeadFile = _$$_REQUIRE(_dependencyMap[13]);

  var _reactNativeLinearGradient = _interopRequireDefault(_$$_REQUIRE(_dependencyMap[14]));

  var _resources = _interopRequireDefault(_$$_REQUIRE(_dependencyMap[15]));

  var _errorHandle = _interopRequireDefault(_$$_REQUIRE(_dependencyMap[16]));

  var _error_handle_dialog = _interopRequireDefault(_$$_REQUIRE(_dependencyMap[17]));

  var _logUtils = _interopRequireDefault(_$$_REQUIRE(_dependencyMap[18]));

  var _logKey = _$$_REQUIRE(_dependencyMap[19]);

  var _HtMessageDialog = _interopRequireDefault(_$$_REQUIRE(_dependencyMap[20]));

  var _reactNativeRootToast = _interopRequireDefault(_$$_REQUIRE(_dependencyMap[21]));

  var _printingSettingDataSource = _$$_REQUIRE(_dependencyMap[22]);

  var _ImgPrintingPreviewView = _$$_REQUIRE(_dependencyMap[23]);

  var _DocPrintingPreviewView = _$$_REQUIRE(_dependencyMap[24]);

  var _CommonHeadFile = _$$_REQUIRE(_dependencyMap[25]);

  var _ComponentsHeadFile = _$$_REQUIRE(_dependencyMap[26]);

  var _QualityHeadFile = _$$_REQUIRE(_dependencyMap[27]);

  var _bluetoothPrintManger = _interopRequireDefault(_$$_REQUIRE(_dependencyMap[28]));

  var _deviceConfig = _interopRequireDefault(_$$_REQUIRE(_dependencyMap[29]));

  var _ricotta_printing = _interopRequireDefault(_$$_REQUIRE(_dependencyMap[30]));

  var _en = _interopRequireDefault(_$$_REQUIRE(_dependencyMap[31]));

  var generateOnMessageFunction = function generateOnMessageFunction(data) {
    return "(function() {\n    document.dispatchEvent(new MessageEvent('message', {data: " + JSON.stringify(data) + "}));\n })()";
  };

  var calculateDimensions = function calculateDimensions() {
    var _Dimensions$get = _reactNative.Dimensions.get('window'),
        screenWidth = _Dimensions$get.width,
        screenHeight = _Dimensions$get.height;

    var cardWidth = _ThirdPartyHeadFile.Host.isPad ? screenWidth - 90 : _ThirdPartyHeadFile.ifIphoneX ? screenWidth - 60 : screenWidth - 120;
    var cardHeight = cardWidth / 2 * 3;
    var statusBarHeight = (0, _ThirdPartyHeadFile.getStatusBarHeight)(true);
    var webviewSafeWidth = _ThirdPartyHeadFile.Host.isPad ? screenWidth - 90 : _ThirdPartyHeadFile.ifIphoneX ? screenWidth - 110 : screenWidth - 140;
    var webviewViewHeight = screenHeight - (0, _ThirdPartyHeadFile.scale)(260);
    var webviewSafeHeight = webviewViewHeight - statusBarHeight - 50;
    var webviewWidth = 300;
    var webviewHeight = 450;

    if (webviewSafeWidth / webviewSafeHeight > 0.6666666666666666) {
      webviewHeight = Math.floor(webviewSafeHeight);
      webviewWidth = Math.floor(webviewHeight / 3 * 2);
    } else {
      webviewWidth = Math.floor(webviewSafeWidth);
      webviewHeight = Math.floor(webviewWidth / 2 * 3);
    }

    var IncrementValue = 30;
    var maxHeight = Math.ceil((screenHeight - 200 - 84) * 1.00 / IncrementValue) * IncrementValue;
    return {
      screenWidth: screenWidth,
      screenHeight: screenHeight,
      cardWidth: cardWidth,
      cardHeight: cardHeight,
      webviewSafeWidth: webviewSafeWidth,
      webviewViewHeight: webviewViewHeight,
      webviewSafeHeight: webviewSafeHeight,
      webviewWidth: webviewWidth,
      webviewHeight: webviewHeight,
      maxHeight: maxHeight
    };
  };

  var initialDimensions = calculateDimensions();
  var cardWidth = initialDimensions.cardWidth;
  var cardHeight = initialDimensions.cardHeight;
  var webviewSafeWidth = initialDimensions.webviewSafeWidth;
  var webviewViewHeight = initialDimensions.webviewViewHeight;
  var webviewSafeHeight = initialDimensions.webviewSafeHeight;
  var webviewWidth = initialDimensions.webviewWidth;
  var webviewHeight = initialDimensions.webviewHeight;
  var IncrementValue = 30;
  var maxHeight = initialDimensions.maxHeight;

  var SCREEN_WIDTH = _reactNative.Dimensions.get('window').width;

  var GESTURE_AREA_WIDTH = 35;
  var SWIPE_THRESHOLD = 80;

  var MintPrintWorking = function (_Component) {
    (0, _inherits2.default)(MintPrintWorking, _Component);
    (0, _createClass2.default)(MintPrintWorking, [{
      key: "onWebViewLoad",
      value: function onWebViewLoad() {
        this.webviewLoaded = true;
      }
    }, {
      key: "onMessage",
      value: function onMessage(e) {
        if (e.nativeEvent.data === 'load ok') {
          this.imageStatus = 'loaded';
        }
      }
    }, {
      key: "postMessage",
      value: function postMessage(data) {
        this.webview.injectJavaScript(generateOnMessageFunction(JSON.stringify(data)));
      }
    }]);

    function MintPrintWorking(props) {
      var _this;

      (0, _classCallCheck2.default)(this, MintPrintWorking);
      _this = (0, _possibleConstructorReturn2.default)(this, (0, _getPrototypeOf2.default)(MintPrintWorking).call(this, props));
      _this.MAX_FAILED_COUNT = 24;
      _this.currentFailedCount = 0;
      _this.MAX_JOB_FAILED_COUNT = 5;
      _this.currentJobFailedCount = 0;
      _this.imageData = _deviceConfig.default.isRicotta() || _deviceConfig.default.isRicottaG() ? _CommonHeadFile.HTImage.ricottaEnd : _CommonHeadFile.HTImage.ricottaPEnd;
      _this.webview = null;
      _this.imageStatus = 'wait';
      _this.webviewLoaded = false;
      _this.animationStatus = '';

      _this.updateDeviceVersion = function () {
        _QualityHeadFile.Spec.getDeviceStatus().then(function (result) {
          if (result.category.code === 40) {
            _this.Toast.show(_resources.default.getString("device_busy_update"), 1000);
          } else {
            _ThirdPartyHeadFile.Host.ui.openDeviceUpgradePage();
          }

          _this.setState({
            updateDeviceVisible: false
          });
        }).catch(function (error) {});
      };

      _this.checkDeviceVersion = function () {
        _this.props.navigation.navigate("mintCleanHome");
      };

      _this._startScanAnimation = function () {
        if (_this.state.isStopAnimation) {
          return;
        }

        if (!_this.state.scanAnimationVisible) {
          _this.setState({
            scanAnimationVisible: true
          });
        }

        _this.Animation = _reactNative.Animated.timing(_this.state.moveValue, {
          toValue: _this.state.animationViewLocation === "left" ? 1 : 0,
          duration: 1200,
          easing: _reactNative.Easing.linear
        }).start(function () {
          _this.setState({
            animationViewLocation: _this.state.animationViewLocation === "left" ? "right" : "left"
          });

          _this._startScanAnimation();
        });
      };

      _this._startPreViewAnimation = function () {
        if (_this.state.isStopAnimation) return;
        var addValue = 30 / _this.state.maxHeight;

        _this.state.preViewTopValue.setValue(0);

        _this.state.leaveAnimation.setValue(0);

        _this.startViewAnimationNext(Math.min(1, addValue));
      };

      _logUtils.default.reportLog("进入动画页面");

      _this.paperColorModeShow = (0, _printingSettingDataSource.PHOTO_COLOR_MODE_SHOW)();
      _this.handleBackPress = _this.handleBackPress.bind((0, _assertThisInitialized2.default)(_this));
      var initialDims = calculateDimensions();
      _this.state = {
        moveValue: new _reactNative.Animated.Value(0),
        animationViewLocation: 'left',
        scanAnimationVisible: false,
        preViewImageUrl: '',
        preViewTopValue: new _reactNative.Animated.Value(1),
        leaveAnimation: new _reactNative.Animated.Value(0),
        isStopAnimation: true,
        errorDetail: {},
        showErrorDialog: false,
        dialogVisible: false,
        alertMsg: '',
        finallyState: null,
        currentJob: null,
        currentUIStage: _consts.MINT_PRINTING_UI_STAGE.MINT_PRINTING_TRANSFERRING,
        stagePreparingText: _resources.default.getString('clean_step_preparation'),
        stagePrinterText: _resources.default.getString('button_printing'),
        progress: 0,
        rotation0: new _reactNative.Animated.Value(0),
        rotation1: new _reactNative.Animated.Value(0),
        rotation2: new _reactNative.Animated.Value(0),
        rotationIndex: _consts.MINT_PRINTING_UI_STAGE.MINT_PRINTING_TRANSFERRING,
        batteryWrongStatusDialogVisible: false,
        screenWidth: initialDims.screenWidth,
        screenHeight: initialDims.screenHeight,
        webviewWidth: initialDims.webviewWidth,
        webviewHeight: initialDims.webviewHeight,
        webviewViewHeight: initialDims.webviewViewHeight,
        maxHeight: initialDims.maxHeight
      };

      if (_reactNative.Platform.OS === 'ios') {
        _this._panResponder = _reactNative.PanResponder.create({
          onStartShouldSetPanResponder: function onStartShouldSetPanResponder() {
            return true;
          },
          onMoveShouldSetPanResponder: function onMoveShouldSetPanResponder(evt, gestureState) {
            var touchX = evt.nativeEvent.locationX;
            return _this.state.finallyState && touchX < GESTURE_AREA_WIDTH && gestureState.dx > 0;
          },
          onPanResponderMove: function onPanResponderMove(evt, gestureState) {},
          onPanResponderRelease: function onPanResponderRelease(evt, gestureState) {
            if (gestureState.dx > SWIPE_THRESHOLD) {
              _this.handleBackPress();
            }
          }
        });
      }

      return _this;
    }

    (0, _createClass2.default)(MintPrintWorking, [{
      key: "handleRotation",
      value: function handleRotation() {
        if (this.state.rotationIndex == _consts.MINT_PRINTING_UI_STAGE.MINT_PRINTING_TRANSFERRING) {
          this.state.rotation0.setValue(0);

          _reactNative.Animated.loop(_reactNative.Animated.timing(this.state.rotation0, {
            toValue: 1,
            duration: 2000,
            easing: _reactNative.Easing.linear,
            useNativeDriver: true
          })).start();

          this.state.rotation1.stopAnimation();
          this.state.rotation1.setValue(0);
          this.state.rotation2.stopAnimation();
          this.state.rotation2.setValue(0);
        } else if (this.state.rotationIndex == _consts.MINT_PRINTING_UI_STAGE.MINT_PRINTING_PREPARING) {
          this.state.rotation1.setValue(0);

          _reactNative.Animated.loop(_reactNative.Animated.timing(this.state.rotation1, {
            toValue: 1,
            duration: 2000,
            easing: _reactNative.Easing.linear,
            useNativeDriver: true
          })).start();

          this.state.rotation0.stopAnimation();
          this.state.rotation0.setValue(0);
          this.state.rotation2.stopAnimation();
          this.state.rotation2.setValue(0);
        } else if (this.state.rotationIndex == _consts.MINT_PRINTING_UI_STAGE.MINT_PRINTING_PRINTING) {
          this.state.rotation2.setValue(0);

          _reactNative.Animated.loop(_reactNative.Animated.timing(this.state.rotation2, {
            toValue: 1,
            duration: 2000,
            easing: _reactNative.Easing.linear,
            useNativeDriver: true
          })).start();

          this.state.rotation0.stopAnimation();
          this.state.rotation0.setValue(0);
          this.state.rotation1.stopAnimation();
          this.state.rotation1.setValue(0);
        }
      }
    }, {
      key: "restartAnimation",
      value: function restartAnimation() {
        var _this2 = this;

        if (this.state.isStopAnimation) {
          this.setState({
            isStopAnimation: false
          });
          setTimeout(function () {
            _this2._startPreViewAnimation();
          }, 100);
        }
      }
    }, {
      key: "getCurrentTime",
      value: function getCurrentTime() {
        return new Date().getTime();
      }
    }, {
      key: "componentDidMount",
      value: function componentDidMount() {
        var _this3 = this;

        _logUtils.default.reportEvent(_logKey.LOG_KEY.ROSEMARY_PAGE_EVENT_PRINTING_ANIMATION, _logKey.LOG_KEY.ROSEMARY_PAGE_EVENT_PRINTING_ANIMATION);

        this.listenerDarkMode = _reactNative.DeviceEventEmitter.addListener(_CommonHeadFile.NOTIFICATION_NAME.DarkModeSwitch, function (e) {
          _this3.setState({});
        });
        global.errorViewVisible = true;
        global.errorDialogVisible = true;
        this.dimensionsSubscription = _reactNative.Dimensions.addEventListener('change', function (_ref) {
          var window = _ref.window;

          _logUtils.default.log("\u5C4F\u5E55\u5C3A\u5BF8\u53D8\u5316: width=" + window.width + ", height=" + window.height);

          var newDims = calculateDimensions();

          _this3.setState({
            screenWidth: newDims.screenWidth,
            screenHeight: newDims.screenHeight,
            webviewWidth: newDims.webviewWidth,
            webviewHeight: newDims.webviewHeight,
            webviewViewHeight: newDims.webviewViewHeight,
            maxHeight: newDims.maxHeight
          }, function () {
            if (_this3.webview && _this3.webviewLoaded) {
              setTimeout(function () {
                _this3.postMessage({
                  action: 'resize',
                  width: newDims.webviewWidth,
                  height: newDims.webviewHeight
                });
              }, 100);
            }
          });
        });
        this.focusListener && this.focusListener.remove();
        this.focusListener = this.props.navigation.addListener('willFocus', function () {
          _logUtils.default.log("printing - willFocus");

          _this3._viewDidFocus();
        });

        this._viewDidFocus();

        this.blurListener && this.blurListener.remove();
        this.blurListener = this.props.navigation.addListener('willBlur', function () {
          _logUtils.default.log("printing - willBlur");

          _this3._viewDidUnFocus();
        });
        this.navigation = this.props.navigation;

        if (_reactNative.Platform.OS === 'android') {
          this.backHandler = _reactNative.BackHandler.addEventListener('hardwareBackPress', this.handleBackPress);
        }

        this.handleRotation();
      }
    }, {
      key: "removeJobTimeOutTask",
      value: function removeJobTimeOutTask() {
        this.jobRefreshInterval && clearInterval(this.jobRefreshInterval);
        this.jobRefreshInterval = null;
      }
    }, {
      key: "_viewDidFocus",
      value: function _viewDidFocus() {
        var _this4 = this;

        this.printerListener && this.printerListener.remove();
        this.jobListener && this.jobListener.remove();
        this.progressListener && this.progressListener.remove();
        this.printerListener = _reactNative.DeviceEventEmitter.addListener(_consts.PRINTER_STATE_CHANGE, function (res) {
          if (_this4.state.finallyState) {
            if (_bluetoothPrintManger.default.ShareInstance().getCurrentJob() && (!_this4.state.currentJob || _bluetoothPrintManger.default.ShareInstance().getCurrentJob().print_job_id != _this4.state.currentJob.getCurrentJob)) {
              _logUtils.default.reportLog("\u5F53\u524D\u6709\u65B0\u4EFB\u52A1\u6253\u5370\uFF0C\u91CD\u7F6E\u52A8\u753B\u9875\u9762\u72B6\u6001");

              _this4.setState({
                finallyState: null,
                currentUIStage: _consts.MINT_PRINTING_UI_STAGE.MINT_PRINTING_TRANSFERRING,
                stagePreparingText: _resources.default.getString('clean_step_preparation'),
                stagePrinterText: _resources.default.getString('button_printing'),
                progress: 0,
                rotation0: new _reactNative.Animated.Value(0),
                rotation1: new _reactNative.Animated.Value(0),
                rotation2: new _reactNative.Animated.Value(0),
                rotationIndex: _consts.MINT_PRINTING_UI_STAGE.MINT_PRINTING_TRANSFERRING,
                isStopAnimation: true,
                moveValue: new _reactNative.Animated.Value(0),
                animationViewLocation: 'right',
                preViewTopValue: new _reactNative.Animated.Value(1),
                leaveAnimation: new _reactNative.Animated.Value(0),
                preViewImageUrl: '',
                scanAnimationVisible: false
              });

              _this4.imageStatus = 'wait';

              _this4.handleRotation();
            } else {
              _logUtils.default.reportLog("\u5F53\u524D\u4EFB\u52A1\u5DF2\u7ED3\u675F\uFF0C\u4E0D\u518D\u5904\u7406\u540E\u7EED\u903B\u8F91");
            }
          } else {
            _this4.handleDeviceState(res);

            _this4.handleJobState(res, _bluetoothPrintManger.default.ShareInstance().getCurrentJob());
          }
        });
        this.jobListener = _reactNative.DeviceEventEmitter.addListener(_consts.PRINTERP_RINTING_JOB_STATE_CHANGE, function (res) {
          _this4.handleFinishJobState(res);
        });
        this.progressListener = _reactNative.DeviceEventEmitter.addListener(_consts.MINT_EVENT_TRANSFERRING_PROGRESS, function (res) {
          _this4.currentFailedCount = 0;

          _this4.setState({
            progress: res
          });
        });
        this.jobCreateListener && this.jobCreateListener.remove();
        this.jobCreateListener = _reactNative.DeviceEventEmitter.addListener(_consts.MINT_EVENT_CREATE_JOB_RESULT, function (e) {
          _this4.handleJobCreate(e);
        });
      }
    }, {
      key: "_viewDidUnFocus",
      value: function _viewDidUnFocus() {
        this.printerListener && this.printerListener.remove();
        this.jobListener && this.jobListener.remove();
        this.progressListener && this.progressListener.remove();
        this.removeJobTimeOutTask();
        this.state.isStopAnimation = true;
        this.jobCreateListener && this.jobCreateListener.remove();
      }
    }, {
      key: "componentWillUnmount",
      value: function componentWillUnmount() {
        this.focusListener && this.focusListener.remove();
        this.blurListener && this.blurListener.remove();
        this.willBlurSubscription && this.willBlurSubscription.remove();
        this.dimensionsSubscription && this.dimensionsSubscription.remove();

        if (_reactNative.Platform.OS === 'android' && this.backHandler) {
          this.backHandler.remove();
          this.backHandler = null;
        }

        this._viewDidUnFocus();

        this.listenerDarkMode && this.listenerDarkMode.remove();
      }
    }, {
      key: "handleDeviceState",
      value: function handleDeviceState(state) {
        var _this5 = this;

        this.jobRefreshTime = this.getCurrentTime();

        _logUtils.default.reportLog("printing-working: handleDeviceState - " + JSON.stringify(state));

        if (state.isSuccess) {
          this.currentFailedCount = 0;
          this.setState({
            bluetoothDisconnectVisible: false
          });
        } else {
          this.currentFailedCount++;

          if (this.currentFailedCount >= this.MAX_FAILED_COUNT) {
            this.setState({
              bluetoothDisconnectVisible: true
            });
          } else {
            this.setState({
              bluetoothDisconnectVisible: false
            });
          }
        }

        _errorHandle.default.handleDeviceState(this.props.navigation, state, false, {
          showErrorDialog: function showErrorDialog(result) {
            _this5.setState({
              errorDetail: result,
              showErrorDialog: true
            });
          },
          hideErrorDialog: function hideErrorDialog() {
            _this5.setState({
              showErrorDialog: false
            });
          },
          fromView: function fromView(data, navigation) {
            setTimeout(function () {
              _this5.props.navigation.replace("mintPrintQueue");
            }, 200);
          }
        });
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
      key: "handleFinishJobState",
      value: function handleFinishJobState(jobState) {
        _logUtils.default.reportLog("handleFinishJobState jobState:" + JSON.stringify(jobState));

        if (jobState.job_state == _consts.MINT_JOB_STATUS.PRINTER_JOB_STATUS_ABORTED || jobState.job_state == _consts.MINT_JOB_STATUS.PRINTER_JOB_STATUS_FINISHED || jobState.job_state == _consts.MINT_JOB_STATUS.PRINTER_JOB_STATUS_CANCELED) {
          this.setState({
            finallyState: jobState.job_state
          });
        }
      }
    }, {
      key: "handleJobState",
      value: function handleJobState(state, currentJob) {
        var _this6 = this;

        _logUtils.default.log("printing-working: handleJobState - " + JSON.stringify(state) + " currentJob = " + JSON.stringify(currentJob));

        this.jobRefreshTime = this.getCurrentTime();
        this.state.currentJob = currentJob;
        var tempCurrentUIstage = _consts.MINT_PRINTING_UI_STAGE.MINT_PRINTING_TRANSFERRING;

        var tempStagePrepareText = _resources.default.getString('clean_step_preparation');

        var tempStagePrinterText = _resources.default.getString('button_printing');

        if (state.category == _consts.MINT_PRINTER_STATUS_CATEGORY.PRINTER_STATUS_CATEGORY_PROCESSING) {
          this.currentJobFailedCount = 0;

          if (currentJob && currentJob.taskStatus == _consts.MINT_TASK_STATUS.MINT_TASK_STATUS_TRANSFERRING) {
            tempCurrentUIstage = _consts.MINT_PRINTING_UI_STAGE.MINT_PRINTING_TRANSFERRING;
          } else {
            switch (state.sub_category) {
              case _consts.MINT_PRINTER_SUB_CATEGORY.PRINTER_STATUS_SUB_CATEGORY_DECODING:
                tempCurrentUIstage = _consts.MINT_PRINTING_UI_STAGE.MINT_PRINTING_PREPARING;
                tempStagePrepareText = _resources.default.getString('clean_step_preparation_with_content').replace('%s', _resources.default.getString('printing_decoding_txt'));
                break;

              case _consts.MINT_PRINTER_SUB_CATEGORY.PRINTER_STATUS_SUB_CATEGORY_INIT:
                tempCurrentUIstage = _consts.MINT_PRINTING_UI_STAGE.MINT_PRINTING_PREPARING;
                tempStagePrepareText = _resources.default.getString('clean_step_preparation_with_content').replace('%s', _resources.default.getString('printing_prepatre_initialization_txt'));
                break;

              case _consts.MINT_PRINTER_SUB_CATEGORY.PRINTER_STATUS_SUB_CATEGORY_PRE_HEAT:
                tempCurrentUIstage = _consts.MINT_PRINTING_UI_STAGE.MINT_PRINTING_PREPARING;
                tempStagePrepareText = _resources.default.getString('clean_step_preparation_with_content').replace('%s', _resources.default.getString('printing_pre_heat_txt'));
                break;

              case _consts.MINT_PRINTER_SUB_CATEGORY.PRINTER_STATUS_SUB_CATEGORY_LOAD_PAPER:
                tempCurrentUIstage = _consts.MINT_PRINTING_UI_STAGE.MINT_PRINTING_PREPARING;
                tempStagePrepareText = _resources.default.getString('clean_step_preparation_with_content').replace('%s', _resources.default.getString('printing_load_paper_sub'));
                break;

              case _consts.MINT_PRINTER_SUB_CATEGORY.PRINTER_STATUS_SUB_CATEGORY_SMART_SHEET:
                tempCurrentUIstage = _consts.MINT_PRINTING_UI_STAGE.MINT_PRINTING_PREPARING;
                tempStagePrepareText = _resources.default.getString('clean_step_preparation_with_content').replace('%s', _resources.default.getString('printing_calibrate_txt'));
                break;

              case _consts.MINT_PRINTER_SUB_CATEGORY.PRINTER_STATUS_SUB_CATEGORY_PRINTING:
              case _consts.MINT_PRINTER_SUB_CATEGORY.PRINTER_STATUS_SUB_CATEGORY_PRINTING_Y:
                tempCurrentUIstage = _consts.MINT_PRINTING_UI_STAGE.MINT_PRINTING_PRINTING;

                if (this.animationStatus !== 'yellow' && this.webviewLoaded && this.imageStatus === 'loaded') {
                  this.animationStatus = 'yellow';
                  this.postMessage({
                    action: 'y'
                  });
                }

                break;

              case _consts.MINT_PRINTER_SUB_CATEGORY.PRINTER_STATUS_SUB_CATEGORY_PRINTING_M:
                tempCurrentUIstage = _consts.MINT_PRINTING_UI_STAGE.MINT_PRINTING_PRINTING;

                if (this.animationStatus !== 'magenta' && this.webviewLoaded && this.imageStatus === 'loaded') {
                  this.animationStatus = 'magenta';
                  this.postMessage({
                    action: 'm'
                  });
                }

                break;

              case _consts.MINT_PRINTER_SUB_CATEGORY.PRINTER_STATUS_SUB_CATEGORY_PRINTING_C:
                tempCurrentUIstage = _consts.MINT_PRINTING_UI_STAGE.MINT_PRINTING_PRINTING;

                if (this.animationStatus !== 'cyan' && this.webviewLoaded && this.imageStatus === 'loaded') {
                  this.animationStatus = 'cyan';
                  this.postMessage({
                    action: 'c'
                  });
                }

                break;

              case _consts.MINT_PRINTER_SUB_CATEGORY.PRINTER_STATUS_SUB_CATEGORY_PRINTING_OC:
                if (this.animationStatus !== 'oc' && this.webviewLoaded && this.imageStatus === 'loaded') {
                  this.animationStatus = 'oc';
                  this.postMessage({
                    action: 'oc'
                  });
                }

                tempCurrentUIstage = _consts.MINT_PRINTING_UI_STAGE.MINT_PRINTING_PRINTING;
                break;

              case _consts.MINT_PRINTER_SUB_CATEGORY.PRINTER_STATUS_SUB_CATEGORY_HOME_FEED:
                tempCurrentUIstage = _consts.MINT_PRINTING_UI_STAGE.MINT_PRINTING_PRINTING;
                break;

              case _consts.MINT_PRINTER_SUB_CATEGORY.PRINTER_STATUS_SUB_CATEGORY_EJECT:
                tempCurrentUIstage = _consts.MINT_PRINTING_UI_STAGE.MINT_PRINTING_PRINTING;
                break;

              case _consts.MINT_PRINTER_SUB_CATEGORY.PRINTER_STATUS_SUB_CATEGORY_COOL_DOWN:
                tempCurrentUIstage = _consts.MINT_PRINTING_UI_STAGE.MINT_PRINTING_PRINTING;
                tempStagePrinterText = _resources.default.getString('button_printing_with_content').replace('%s', _resources.default.getString('printing_cool_down_txt'));
                break;

              default:
                tempCurrentUIstage = _consts.MINT_PRINTING_UI_STAGE.MINT_PRINTING_TRANSFERRING;
                break;
            }
          }
        } else if (state.category == _consts.MINT_PRINTER_STATUS_CATEGORY.PRINTER_STATUS_CATEGORY_IDLE) {
          _logUtils.default.log("printing-working: \u5F53\u524D\u8BBE\u5907\u5904\u4E8Eidle\u72B6\u6001");

          this.currentJobFailedCount++;

          if (this.currentJobFailedCount > this.MAX_JOB_FAILED_COUNT) {
            this.setState({
              finallyJobState: _consts.MINT_JOB_STATUS.PRINTER_JOB_STATUS_ABORTED
            });
          }
        } else {
          _logUtils.default.log("printing-working: \u5F53\u524D\u8BBE\u5907\u4E0D\u5728\u6253\u5370\u72B6\u6001 - " + JSON.stringify(state.category));

          tempCurrentUIstage = _consts.MINT_PRINTING_UI_STAGE.MINT_PRINTING_TRANSFERRING;
        }

        _logUtils.default.log("printing-working: \u5F53\u524DUI\u72B6\u6001 tempCurrentUIstage - " + tempCurrentUIstage);

        if (tempCurrentUIstage < this.state.currentUIStage) {
          _logUtils.default.reportLog("\u83B7\u53D6\u7684\u4EFB\u52A1\u9636\u6BB5\u65E9\u4E8E\u5F53\u524D\u4EFB\u52A1\u9636\u6BB5\uFF0C\u4E0D\u66F4\u65B0UI");

          return;
        } else {
          if (tempCurrentUIstage == _consts.MINT_PRINTING_UI_STAGE.MINT_PRINTING_TRANSFERRING && this.state.rotationIndex != _consts.MINT_PRINTING_UI_STAGE.MINT_PRINTING_TRANSFERRING) {
            _logUtils.default.reportLog("\u5F00\u542F\u7B2C\u4E00\u4E2A\u52A8\u753B");

            this.setState({
              rotationIndex: _consts.MINT_PRINTING_UI_STAGE.MINT_PRINTING_TRANSFERRING
            });
            this.handleRotation();
          } else if (tempCurrentUIstage == _consts.MINT_PRINTING_UI_STAGE.MINT_PRINTING_PREPARING && this.state.rotationIndex != _consts.MINT_PRINTING_UI_STAGE.MINT_PRINTING_PREPARING) {
            _logUtils.default.reportLog("\u5F00\u542F\u7B2C\u4E8C\u4E2A\u52A8\u753B");

            this.setState({
              rotationIndex: _consts.MINT_PRINTING_UI_STAGE.MINT_PRINTING_PREPARING
            });
            this.handleRotation();
          } else if (tempCurrentUIstage == _consts.MINT_PRINTING_UI_STAGE.MINT_PRINTING_PRINTING && this.state.rotationIndex != _consts.MINT_PRINTING_UI_STAGE.MINT_PRINTING_PRINTING) {
            _logUtils.default.reportLog("\u5F00\u542F\u7B2C\u4E09\u4E2A\u52A8\u753B");

            this.restartAnimation();
            this.setState({
              rotationIndex: _consts.MINT_PRINTING_UI_STAGE.MINT_PRINTING_PRINTING
            });
            this.handleRotation();
          } else if (tempCurrentUIstage == _consts.MINT_PRINTING_UI_STAGE.MINT_PRINTING_PRINTING) {
            this.restartAnimation();
          }
        }

        var thisPreViewImageUrl = currentJob ? currentJob.imagePath : "";

        if (thisPreViewImageUrl === "") {
          this.imageStatus = 'wait';
        }

        if (thisPreViewImageUrl && this.webviewLoaded && this.imageStatus === 'wait') {
          this.imageStatus = 'loading';

          var getBasePath = function getBasePath() {
            return _ThirdPartyHeadFile.Host.isIOS ? _ThirdPartyHeadFile.Host.file.storageBasePath : "file://" + _ThirdPartyHeadFile.Host.file.storageBasePath;
          };

          _ThirdPartyHeadFile.Host.file.readFileToBase64(thisPreViewImageUrl.replace(getBasePath() + "/", '')).then(function (res) {
            _this6.postMessage({
              action: 'load',
              src: 'data:image/jpeg;base64,' + res
            });
          }).catch(function (err) {});
        }

        this.setState({
          currentUIStage: tempCurrentUIstage,
          stagePreparingText: tempStagePrepareText,
          preViewImageUrl: thisPreViewImageUrl,
          stagePrinterText: tempStagePrinterText
        });
      }
    }, {
      key: "handleJobCreate",
      value: function handleJobCreate(result) {
        _logUtils.default.log("\u52A8\u753B\u9875\u9762 - handleJobCreate result = " + JSON.stringify(result));

        if (!result.isSuccess) {
          if (result.errorCode == _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_FIND_NO_JOB || result.errorCode == _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_QUEUE_FULL || result.errorCode == _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_QUEUE_EMPTY || result.errorCode == _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_OVER_LIMITED_SIZE || result.errorCode == _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_TRANSFER_ERROR || result.errorCode == _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_LOW_BATTERY_ERROR || result.errorCode == _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_OVERHEAT || result.errorCode == _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_OVERCOOL) {
            this.showCreateJobError(result.errorCode);
          } else {}
        }
      }
    }, {
      key: "showCreateJobError",
      value: function showCreateJobError(code) {
        var _this7 = this;

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
            _this7.props.navigation.replace("mintPrintQueue");
          }
        }]);
      }
    }, {
      key: "_cancelJob",
      value: function _cancelJob() {
        _logUtils.default.reportEvent(_logKey.LOG_KEY.ROSEMARY_TAP_EVENT_PRINTING_CANCEL, _logKey.LOG_KEY.ROSEMARY_TAP_EVENT_PRINTING_CANCEL);

        this.setState({
          dialogVisible: true,
          alertMsg: _resources.default.getString("job_bt_cancel_txt")
        });
      }
    }, {
      key: "cancelJob",
      value: function cancelJob() {
        var _this8 = this;

        _bluetoothPrintManger.default.ShareInstance().cancelCurrentJob().then(function (result) {
          _this8.setState({
            finallyState: _consts.MINT_JOB_STATUS.PRINTER_JOB_STATUS_CANCELED
          });
        }).catch(function (err) {
          _reactNativeRootToast.default.show(_resources.default.getString("toast_cancel_job_fail"), {
            duration: _reactNativeRootToast.default.durations.SHORT,
            position: _reactNativeRootToast.default.positions.BOTTOM,
            shadow: true,
            animation: true,
            hideOnPress: true,
            delay: 0
          });
        });
      }
    }, {
      key: "printStateView",
      value: function printStateView() {
        var imgSource = null,
            title = "",
            subTitle = "";
        var hasCompleted = false;

        if (this.state.finallyState === _consts.MINT_JOB_STATUS.PRINTER_JOB_STATUS_FINISHED) {
          imgSource = _CommonHeadFile.HTImage.print_completed;
          title = _resources.default.getString("print_done_txt");

          if (_deviceConfig.default.isRicottaType()) {
            subTitle = _resources.default.getString("after_job_finish_tips");
          } else {
            subTitle = "";
          }

          hasCompleted = true;
        } else if (this.state.finallyState === _consts.MINT_JOB_STATUS.PRINTER_JOB_STATUS_ABORTED) {
          imgSource = _CommonHeadFile.HTImage.print_completed_error;
          title = _resources.default.getString("print_aborted_txt");
          subTitle = "";
        } else if (this.state.finallyState === _consts.MINT_JOB_STATUS.PRINTER_JOB_STATUS_CANCELED) {
          imgSource = _CommonHeadFile.HTImage.print_ic_cancel_sucess;
          title = _resources.default.getString("print_canceled_txt");
          subTitle = "";
        } else {
          return null;
        }

        var maxContentWidth = _ThirdPartyHeadFile.Host.isPad ? Math.min(this.state.screenWidth * 0.8, 600) : this.state.screenWidth;
        var imageSize = (0, _ThirdPartyHeadFile.scaleForPad)(64);
        var ricottaImageWidth = (0, _ThirdPartyHeadFile.scaleForPad)(316);
        var ricottaImageHeight = (0, _ThirdPartyHeadFile.scaleForPad)(187);
        return _react.default.createElement(_reactNative.View, {
          style: {
            top: 0,
            left: 0,
            right: 0,
            flex: 1,
            backgroundColor: _CommonHeadFile.HTColor.c_bg_general,
            alignItems: "center",
            justifyContent: "center",
            paddingHorizontal: _ThirdPartyHeadFile.Host.isPad ? (0, _ThirdPartyHeadFile.scale)(40) : 0
          }
        }, _react.default.createElement(_reactNative.View, {
          style: {
            alignItems: "center",
            width: maxContentWidth,
            maxWidth: '100%'
          }
        }, _react.default.createElement(_reactNative.Image, {
          style: {
            width: imageSize,
            height: imageSize
          },
          source: imgSource
        }), _react.default.createElement(_reactNative.Text, {
          style: {
            fontSize: (0, _ThirdPartyHeadFile.scaleForPad)(14),
            color: _CommonHeadFile.HTColor.c_font_999999,
            marginTop: (0, _ThirdPartyHeadFile.scaleForPad)(6),
            textAlign: "center",
            paddingHorizontal: (0, _ThirdPartyHeadFile.scale)(20)
          }
        }, title), hasCompleted && _deviceConfig.default.isRicottaType() && _react.default.createElement(_reactNative.View, {
          style: {
            marginTop: (0, _ThirdPartyHeadFile.scaleForPad)(54)
          }
        }, _react.default.createElement(_reactNative.Image, {
          style: {
            width: ricottaImageWidth,
            height: ricottaImageHeight,
            margin: 'auto'
          },
          source: this.imageData
        })), _react.default.createElement(_reactNative.Text, {
          style: {
            fontSize: (0, _ThirdPartyHeadFile.scaleForPad)(14),
            color: _CommonHeadFile.HTColor.c_font_999999,
            marginTop: (0, _ThirdPartyHeadFile.scaleForPad)(16),
            marginHorizontal: (0, _ThirdPartyHeadFile.scaleForPad)(25),
            textAlign: "center"
          }
        }, subTitle)), hasCompleted && _deviceConfig.default.isMint() ? _react.default.createElement(_reactNative.Text, {
          style: [styles.hintTextStyle, {
            color: _CommonHeadFile.HTColor.c_theme
          }],
          onPress: this.checkDeviceVersion
        }, _resources.default.getString("printed_bad_photo_tips")) : null);
      }
    }, {
      key: "startViewAnimationNext",
      value: function startViewAnimationNext(toValue) {
        var _this9 = this;

        if (this.state.isStopAnimation) return;
        var config = {
          toValue: toValue,
          bounciness: 3,
          speed: 3
        };

        _reactNative.Animated.spring(this.state.preViewTopValue, config).start(function () {
          if (toValue >= 1) {
            _this9._startPreViewAnimation();

            return;
          }

          var addValue = 30 / _this9.state.maxHeight;

          _this9.startViewAnimationNext(Math.min(1, toValue + addValue));
        });
      }
    }, {
      key: "_getScanAnimationView",
      value: function _getScanAnimationView() {
        if (!this.state.scanAnimationVisible) {
          return null;
        }

        var currentValueX = this.state.moveValue.interpolate({
          inputRange: [0, 1],
          outputRange: [0, this.state.screenWidth]
        });
        return _react.default.createElement(_reactNative.View, {
          style: [styles.animationContent, {
            width: this.state.screenWidth
          }]
        }, _react.default.createElement(_reactNative.Animated.View, {
          style: this.state.animationViewLocation === "left" ? [styles.animationLeft, {
            width: currentValueX
          }] : [styles.animationRight, {
            left: currentValueX
          }]
        }, _react.default.createElement(_reactNativeLinearGradient.default, {
          locations: [0, 0.5, 1.0],
          start: this.state.animationViewLocation === "left" ? {
            x: 0,
            y: 0
          } : {
            x: 1,
            y: 1
          },
          end: this.state.animationViewLocation === "left" ? {
            x: 1,
            y: 1
          } : {
            x: 0,
            y: 0
          },
          colors: ["rgba(11,132,255,0.03)", "rgba(11,132,255,0.12)", "rgba(11,132,255,0.3)"],
          style: {
            flex: 1
          }
        })), _react.default.createElement(_reactNative.Animated.View, {
          style: {
            left: currentValueX,
            width: (0, _ThirdPartyHeadFile.scale)(1),
            height: (0, _ThirdPartyHeadFile.scale)(110),
            backgroundColor: _CommonHeadFile.HTColor.c_theme,
            position: "absolute"
          }
        }));
      }
    }, {
      key: "_fileType",
      value: function _fileType() {
        return true;
      }
    }, {
      key: "getViewAnimationView",
      value: function getViewAnimationView() {
        _logUtils.default.log("getViewAnimationView this.state.preViewImageUrl = " + this.state.preViewImageUrl);

        var componentKey = "preview-" + this.state.screenWidth + "-" + this.state.screenHeight;

        if (this._fileType()) {
          if (this.state.currentJob && this.state.currentJob.job_type === _consts.JOB_TYPE.PRINTER_JOB_TYPE_CLEAN) {
            return _react.default.createElement(_ImgPrintingPreviewView.ImgPrintingPreviewView, {
              key: componentKey,
              isClean: true
            });
          }

          var imgEmpty = this.state.preViewImageUrl === undefined || this.state.preViewImageUrl === null || this.state.preViewImageUrl === "";

          if (imgEmpty) {
            return _react.default.createElement(_ImgPrintingPreviewView.ImgPrintingPreviewView, {
              key: componentKey
            });
          }

          return _react.default.createElement(_ImgPrintingPreviewView.ImgPrintingPreviewView, {
            key: componentKey,
            source: {
              uri: this.state.preViewImageUrl
            }
          });
        }

        return _react.default.createElement(_DocPrintingPreviewView.DocPrintingPreviewView, {
          key: componentKey
        });
      }
    }, {
      key: "_preViewAnimation",
      value: function _preViewAnimation() {
        var _this10 = this;

        var _this$state = this.state,
            webviewWidth = _this$state.webviewWidth,
            webviewHeight = _this$state.webviewHeight,
            webviewViewHeight = _this$state.webviewViewHeight,
            screenWidth = _this$state.screenWidth;
        return _react.default.createElement(_reactNative.View, {
          style: {
            width: screenWidth,
            height: webviewViewHeight,
            alignItems: "center",
            justifyContent: "center",
            paddingTop: (webviewViewHeight - webviewHeight) / 2 + 22
          }
        }, _react.default.createElement(_reactNativeWebview.WebView, {
          bounces: false,
          overScrollMode: "never",
          showsHorizontalScrollIndicator: false,
          showsVerticalScrollIndicator: false,
          scalesPageToFit: false,
          setBuiltInZoomControls: false,
          setDisplayZoomControls: false,
          source: {
            html: _ricotta_printing.default
          },
          style: {
            flex: 0,
            width: webviewWidth,
            height: webviewHeight,
            backgroundColor: _CommonHeadFile.HTColor.c_bg_general
          },
          scrollEnabled: false,
          ref: function ref(webview) {
            return _this10.webview = webview;
          },
          onMessage: this.onMessage.bind(this),
          onError: function onError() {},
          onLoad: function onLoad() {
            return _this10.onWebViewLoad();
          },
          javaScriptEnabled: true,
          injectedJavaScript: "\n            (function() {\n              window.postMessage = function(data) {\n              window.ReactNativeWebView.postMessage(data);\n              };\n\n              try {\n              // \u7981\u7528\u53CC\u6307\u7F29\u653E\uFF1AiOS \u9700\u8981 user-scalable=no + gesturestart \u62E6\u622A\uFF1BAndroid \u4E5F\u53EF\u907F\u514D web \u5185\u5BB9\u7F29\u653E\n              var meta = document.querySelector('meta[name=\"viewport\"]');\n              if (!meta) {\n                meta = document.createElement('meta');\n                meta.setAttribute('name', 'viewport');\n                document.head && document.head.appendChild(meta);\n              }\n              meta.setAttribute('content', 'width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no');\n              document.addEventListener('gesturestart', function(e) { e.preventDefault(); });\n              document.addEventListener('gesturechange', function(e) { e.preventDefault(); });\n              document.addEventListener('gestureend', function(e) { e.preventDefault(); });\n              document.addEventListener('touchmove', function(e) {\n                if (e && e.touches && e.touches.length > 1) {\n                  e.preventDefault();\n                }\n              }, { passive: false });\n            } catch (e) {}\n            })();\n          "
        }));
      }
    }, {
      key: "_bottomView",
      value: function _bottomView() {
        var _this11 = this;

        var enable = this.state.currentJob && !this.state.finallyState && this.state.currentUIStage > _consts.MINT_PRINTING_UI_STAGE.MINT_PRINTING_TRANSFERRING;
        var homeEnable = this.state.finallyState || this.state.currentUIStage > _consts.MINT_PRINTING_UI_STAGE.MINT_PRINTING_TRANSFERRING;
        return _react.default.createElement(_reactNative.View, {
          style: [styles.bottomView, {
            width: this.state.screenWidth
          }]
        }, _react.default.createElement(_reactNative.View, {
          style: [styles.bottomSubView, {
            width: this.state.screenWidth / 3
          }]
        }, _react.default.createElement(_reactNative.TouchableOpacity, {
          disabled: !enable,
          activeOpacity: 0.5,
          onPress: function onPress() {
            _this11._cancelJob();
          }
        }, _react.default.createElement(_reactNative.Image, {
          source: enable ? _CommonHeadFile.HTImage.print_img_cancel : _CommonHeadFile.HTImage.print_img_cancel_dis,
          style: [{
            width: (0, _ThirdPartyHeadFile.scale)(48),
            height: (0, _ThirdPartyHeadFile.scale)(48)
          }, {
            opacity: !enable && _CommonHeadFile.HTColor.isDarkMode ? 0.4 : 1
          }]
        })), _react.default.createElement(_reactNative.Text, {
          style: [styles.bottomText, {
            color: _CommonHeadFile.HTColor.c_font_000000,
            alignSelf: "center",
            opacity: enable ? 1 : 0.3
          }]
        }, _resources.default.getString("button_print_cancel"))), _react.default.createElement(_reactNative.View, {
          style: [styles.bottomSubView, {
            width: this.state.screenWidth / 3
          }]
        }, _react.default.createElement(_reactNative.TouchableOpacity, {
          activeOpacity: 0.5,
          disabled: !homeEnable,
          onPress: function onPress() {
            _logUtils.default.reportEvent(_logKey.LOG_KEY.ROSEMARY_TAP_EVENT_PRINTING_JOBQUEUE, _logKey.LOG_KEY.ROSEMARY_TAP_EVENT_PRINTING_JOBQUEUE);

            _this11.setState({
              isStopAnimation: true
            });

            _this11.props.navigation.replace("mintPrintQueue");
          }
        }, _react.default.createElement(_reactNative.Image, {
          source: _CommonHeadFile.HTImage.print_img_queue,
          style: {
            width: (0, _ThirdPartyHeadFile.scale)(48),
            height: (0, _ThirdPartyHeadFile.scale)(48),
            opacity: homeEnable ? 1 : 0.3
          }
        })), _react.default.createElement(_reactNative.Text, {
          style: [styles.bottomText, {
            color: _CommonHeadFile.HTColor.c_font_000000,
            alignSelf: "center",
            opacity: homeEnable ? 1 : 0.3
          }],
          ellipsizeMode: "tail"
        }, _resources.default.getString("button_job_list"))), _react.default.createElement(_reactNative.View, {
          style: [styles.bottomSubView, {
            width: this.state.screenWidth / 3
          }]
        }, _react.default.createElement(_reactNative.TouchableOpacity, {
          activeOpacity: 0.5,
          disabled: !homeEnable,
          onPress: function onPress() {
            _logUtils.default.reportEvent(_logKey.LOG_KEY.ROSEMARY_TAP_EVENT_PRINTING_HOME, _logKey.LOG_KEY.ROSEMARY_TAP_EVENT_PRINTING_HOME);

            _this11.setState({
              isStopAnimation: true
            });

            _this11.props.navigation.popToTop({
              animated: false
            });
          }
        }, _react.default.createElement(_reactNative.Image, {
          source: _CommonHeadFile.HTImage.print_img_home_mint,
          style: {
            width: (0, _ThirdPartyHeadFile.scale)(48),
            height: (0, _ThirdPartyHeadFile.scale)(48),
            opacity: homeEnable ? 1 : 0.3
          }
        })), _react.default.createElement(_reactNative.Text, {
          style: [styles.bottomText, {
            color: _CommonHeadFile.HTColor.c_font_000000,
            alignSelf: "center",
            opacity: homeEnable ? 1 : 0.3
          }]
        }, _resources.default.getString("button_home"))));
      }
    }, {
      key: "getPrintIcon",
      value: function getPrintIcon(stage, currentStage) {
        if (stage == currentStage) {
          return _CommonHeadFile.HTImage.printingDoing;
        } else if (stage < currentStage) {
          return _CommonHeadFile.HTImage.printingChecked;
        } else {
          return _CommonHeadFile.HTImage.printingTodo;
        }
      }
    }, {
      key: "getPrintTextStyle",
      value: function getPrintTextStyle(stage, currentStage) {
        if (stage == currentStage) {
          return styles.statusText;
        } else if (stage < currentStage) {
          return styles.statusTextDone;
        } else {
          return styles.statusTextTodo;
        }
      }
    }, {
      key: "getContentView",
      value: function getContentView() {
        var printStateView = this.printStateView();
        var spin1 = this.state.rotation0.interpolate({
          inputRange: [0, 1],
          outputRange: ['0deg', '360deg']
        });
        var spin2 = this.state.rotation1.interpolate({
          inputRange: [0, 1],
          outputRange: ['0deg', '360deg']
        });
        var spin3 = this.state.rotation2.interpolate({
          inputRange: [0, 1],
          outputRange: ['0deg', '360deg']
        });

        if (printStateView === null) {
          var text = _resources.default.getString('upload_progress_sub').replace('%s', this.state.progress).trim();

          if (this.state.progress == 0 || this.state.progress == 100) {
            text = _resources.default.getString('upload_progress_sub').replace('%s', '').replace('%', '').trim();
          }

          return _react.default.createElement(_reactNative.View, {
            style: {
              flex: 1,
              width: this.state.screenWidth,
              alignItems: "center",
              overflow: "hidden"
            }
          }, this._preViewAnimation(), _react.default.createElement(_reactNative.View, {
            style: [styles.info, {
              width: this.state.screenWidth - 60
            }]
          }, _react.default.createElement(_reactNative.View, {
            style: styles.statusItem
          }, _react.default.createElement(_reactNative.Animated.Image, {
            style: [styles.circle, {
              transform: [{
                rotate: spin1
              }]
            }],
            source: this.getPrintIcon(_consts.MINT_PRINTING_UI_STAGE.MINT_PRINTING_TRANSFERRING, this.state.currentUIStage)
          }), _react.default.createElement(_reactNative.Text, {
            style: this.getPrintTextStyle(_consts.MINT_PRINTING_UI_STAGE.MINT_PRINTING_TRANSFERRING, this.state.currentUIStage)
          }, text)), _react.default.createElement(_reactNative.View, {
            style: styles.line
          }), _react.default.createElement(_reactNative.View, {
            style: styles.statusItem
          }, _react.default.createElement(_reactNative.Animated.Image, {
            style: [styles.circle, {
              transform: [{
                rotate: spin2
              }]
            }],
            source: this.getPrintIcon(_consts.MINT_PRINTING_UI_STAGE.MINT_PRINTING_PREPARING, this.state.currentUIStage)
          }), _react.default.createElement(_reactNative.Text, {
            style: this.getPrintTextStyle(_consts.MINT_PRINTING_UI_STAGE.MINT_PRINTING_PREPARING, this.state.currentUIStage)
          }, this.state.stagePreparingText)), _react.default.createElement(_reactNative.View, {
            style: styles.line
          }), _react.default.createElement(_reactNative.View, {
            style: styles.statusItem
          }, _react.default.createElement(_reactNative.Animated.Image, {
            style: [styles.circle, {
              transform: [{
                rotate: spin3
              }]
            }],
            source: this.getPrintIcon(_consts.MINT_PRINTING_UI_STAGE.MINT_PRINTING_PRINTING, this.state.currentUIStage)
          }), _react.default.createElement(_reactNative.Text, {
            style: this.getPrintTextStyle(_consts.MINT_PRINTING_UI_STAGE.MINT_PRINTING_PRINTING, this.state.currentUIStage)
          }, this.state.stagePrinterText))));
        }

        this.removeJobTimeOutTask();
        this.webview = null;
        this.imageStatus = 'wait';
        this.webviewLoaded = false;
        this.animationStatus = '';
        return printStateView;
      }
    }, {
      key: "handleBackPress",
      value: function handleBackPress() {
        _logUtils.default.reportLog("返回手势被触发，当前状态：" + this.state.finallyState);

        if (this.state.finallyState) {
          this.props.navigation.popToTop({
            animated: true
          });
          return true;
        }

        return true;
      }
    }, {
      key: "render",
      value: function render() {
        var _this12 = this;

        return _react.default.createElement(_reactNative.View, {
          style: [styles.container, {
            backgroundColor: _CommonHeadFile.HTColor.c_bg_general,
            position: 'relative'
          }]
        }, _reactNative.Platform.OS === 'ios' && this.state.finallyState && _react.default.createElement(_reactNative.View, (0, _extends2.default)({
          style: styles.gestureArea
        }, this._panResponder.panHandlers)), this.getContentView(), this._bottomView(), this.state.showErrorDialog ? _react.default.createElement(_error_handle_dialog.default, {
          error: this.state.errorDetail
        }) : null, _react.default.createElement(_HtMessageDialog.default, {
          title: _resources.default.getString("default_alert_title"),
          visible: this.state.dialogVisible,
          message: this.state.alertMsg,
          buttons: [{
            text: _resources.default.getString("button_print_cancel"),
            onPress: function onPress() {
              _this12.setState({
                dialogVisible: false
              });

              _this12.cancelJob();
            }
          }, {
            text: _resources.default.getString("button_keep"),
            onPress: function onPress() {
              _this12.setState({
                dialogVisible: false
              });
            }
          }]
        }), _react.default.createElement(_HtMessageDialog.default, {
          title: _resources.default.getString("new_version_discovered"),
          visible: this.state.updateDeviceVisible,
          message: this.state.updateDeviceMsg,
          buttons: [{
            text: _resources.default.getString("moreLater"),
            onPress: function onPress() {
              _this12.props.navigation.popToTop();
            }
          }, {
            text: _resources.default.getString("updateNow"),
            onPress: function onPress() {
              _this12.updateDeviceVersion();
            }
          }]
        }), _react.default.createElement(_HtMessageDialog.default, {
          title: _resources.default.getString("default_alert_title"),
          visible: this.state.bluetoothDisconnectVisible,
          message: _resources.default.getString("bt_disconnect_txt"),
          buttons: [{
            text: _resources.default.getString("button_ok"),
            onPress: function onPress() {
              _this12.props.navigation.replace("mintPrintQueue");
            },
            textStyle: {
              color: _CommonHeadFile.HTColor.c_font_blueBtn,
              fontWeight: "600",
              fontSize: (0, _ThirdPartyHeadFile.scale)(16)
            },
            style: {
              flex: 1,
              height: (0, _ThirdPartyHeadFile.scale)(46),
              borderRadius: (0, _ThirdPartyHeadFile.scale)(23),
              justifyContent: 'center',
              alignItems: 'center',
              backgroundColor: _CommonHeadFile.HTColor.c_btn_blue
            }
          }]
        }), _react.default.createElement(_HtMessageDialog.default, {
          title: _resources.default.getString('default_alert_title'),
          visible: this.state.batteryWrongStatusDialogVisible,
          message: _resources.default.getString('battery_wrong_status_context'),
          buttons: [{
            text: _resources.default.getString('button_ok'),
            onPress: function onPress() {
              _this12.setState({
                batteryWrongStatusDialogVisible: false
              });
            }
          }]
        }), _react.default.createElement(_ComponentsHeadFile.HTMessageDialog, {
          ref: function ref(v) {
            return _this12.createJobErrorDialog = v;
          }
        }), _react.default.createElement(_ComponentsHeadFile.HTToast, {
          ref: function ref(t) {
            return _this12.Toast = t;
          }
        }));
      }
    }]);
    return MintPrintWorking;
  }(_react.Component);

  exports.default = MintPrintWorking;

  MintPrintWorking.navigationOptions = function (_ref2) {
    var navigation = _ref2.navigation;
    return {
      header: null,
      gesturesEnabled: false
    };
  };

  var styles = _reactNative.StyleSheet.create({
    container: {
      top: 0,
      flex: 1,
      alignItems: 'center'
    },
    previewView: {
      resizeMode: 'cover',
      width: cardWidth,
      height: cardHeight,
      shadowOffset: {
        width: 0,
        height: 4
      },
      shadowOpacity: 1,
      shadowRadius: 10
    },
    info: {
      bottom: (0, _ThirdPartyHeadFile.scale)(15),
      width: _consts.WIDTH - 60,
      alignItems: 'flex-start',
      position: 'absolute',
      backgroundColor: _CommonHeadFile.HTColor.c_bg_general
    },
    bottomView: {
      width: _consts.WIDTH,
      height: (0, _ThirdPartyHeadFile.scale)(108),
      paddingTop: (0, _ThirdPartyHeadFile.scale)(0),
      flexDirection: 'row',
      justifyContent: 'space-evenly',
      backgroundColor: _CommonHeadFile.HTColor.c_bg_general
    },
    bottomSubView: {
      width: _consts.WIDTH / 3,
      alignItems: "center"
    },
    bottomText: {
      top: (0, _ThirdPartyHeadFile.scale)(10),
      fontSize: (0, _ThirdPartyHeadFile.scale)(12),
      textAlign: 'center',
      flexWrap: 'wrap',
      flexDirection: 'row'
    },
    animationContent: {
      flexDirection: 'row',
      justifyContent: 'flex-end',
      position: 'absolute',
      width: _consts.WIDTH,
      height: (0, _ThirdPartyHeadFile.scale)(110)
    },
    animationLeft: {
      left: 0,
      height: (0, _ThirdPartyHeadFile.scale)(110),
      marginBottom: 5,
      backgroundColor: 'transparent',
      position: 'absolute'
    },
    animationRight: {
      right: 0,
      height: (0, _ThirdPartyHeadFile.scale)(110),
      marginBottom: 5,
      backgroundColor: 'transparent',
      position: 'absolute'
    },
    hintTextStyle: {
      textDecorationLine: "underline",
      textAlign: "center",
      marginBottom: (0, _ThirdPartyHeadFile.scale)(40),
      fontSize: (0, _ThirdPartyHeadFile.scale)(14),
      position: 'absolute',
      bottom: 0,
      paddingHorizontal: (0, _ThirdPartyHeadFile.scale)(20)
    },
    statusItem: {
      flexDirection: 'row',
      alignItems: 'center',
      marginTop: (0, _ThirdPartyHeadFile.scale)(5),
      marginBottom: (0, _ThirdPartyHeadFile.scale)(5)
    },
    line: {
      width: (0, _ThirdPartyHeadFile.scale)(1),
      height: (0, _ThirdPartyHeadFile.scale)(18),
      backgroundColor: '#D9D9D9',
      marginLeft: (0, _ThirdPartyHeadFile.scale)(10)
    },
    circle: {
      width: (0, _ThirdPartyHeadFile.scale)(20),
      height: (0, _ThirdPartyHeadFile.scale)(20)
    },
    statusText: {
      marginLeft: (0, _ThirdPartyHeadFile.scale)(10),
      fontSize: (0, _ThirdPartyHeadFile.scale)(15),
      color: _CommonHeadFile.HTColor.c_font_000000
    },
    statusTextDone: {
      marginLeft: (0, _ThirdPartyHeadFile.scale)(10),
      fontSize: (0, _ThirdPartyHeadFile.scale)(15),
      color: _CommonHeadFile.HTColor.c_theme
    },
    statusTextTodo: {
      marginLeft: (0, _ThirdPartyHeadFile.scale)(10),
      fontSize: (0, _ThirdPartyHeadFile.scale)(15),
      color: _CommonHeadFile.HTColor.c_font_black_40
    },
    gestureArea: {
      position: 'absolute',
      left: 0,
      top: 0,
      bottom: 0,
      width: GESTURE_AREA_WIDTH,
      zIndex: 999
    }
  });
},13208,[14317,14314,14353,14329,14383,14389,14386,14332,14398,10297,14728,10033,12173,10034,14308,10077,12332,12710,12194,12494,12518,12377,12497,13202,13205,10052,12416,13133,12311,12179,13211,10095]);