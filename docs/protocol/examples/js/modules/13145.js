__d(function (global, _$$_REQUIRE, _$$_IMPORT_DEFAULT, _$$_IMPORT_ALL, module, exports, _dependencyMap) {
  var _interopRequireDefault = _$$_REQUIRE(_dependencyMap[0]);

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  exports.default = undefined;

  var _classCallCheck2 = _interopRequireDefault(_$$_REQUIRE(_dependencyMap[1]));

  var _createClass2 = _interopRequireDefault(_$$_REQUIRE(_dependencyMap[2]));

  var _possibleConstructorReturn2 = _interopRequireDefault(_$$_REQUIRE(_dependencyMap[3]));

  var _getPrototypeOf2 = _interopRequireDefault(_$$_REQUIRE(_dependencyMap[4]));

  var _inherits2 = _interopRequireDefault(_$$_REQUIRE(_dependencyMap[5]));

  var _react = _interopRequireDefault(_$$_REQUIRE(_dependencyMap[6]));

  var _reactNative = _$$_REQUIRE(_dependencyMap[7]);

  var _consts = _$$_REQUIRE(_dependencyMap[8]);

  var _errorView = _interopRequireDefault(_$$_REQUIRE(_dependencyMap[9]));

  var _ErrorHeadFile = _$$_REQUIRE(_dependencyMap[10]);

  var _logKey = _$$_REQUIRE(_dependencyMap[11]);

  var _CommonHeadFile = _$$_REQUIRE(_dependencyMap[12]);

  var _HTClassicBluetooth = _interopRequireDefault(_$$_REQUIRE(_dependencyMap[13]));

  var _miot = _$$_REQUIRE(_dependencyMap[14]);

  var _deviceConfig = _interopRequireDefault(_$$_REQUIRE(_dependencyMap[15]));

  var _DynamicUtil = _interopRequireDefault(_$$_REQUIRE(_dependencyMap[16]));

  var errorCode = 0;
  var errorBgColor = _consts.ERR_BG.ERR_BG_RED;
  var errImagePath = _CommonHeadFile.HTImage.error_device_error;
  var paperSize = 'A4';

  var Error = function (_React$Component) {
    (0, _inherits2.default)(Error, _React$Component);

    function Error(props) {
      var _this;

      (0, _classCallCheck2.default)(this, Error);
      _this = (0, _possibleConstructorReturn2.default)(this, (0, _getPrototypeOf2.default)(Error).call(this, props));

      _this.showLoadingTips = function (tip) {
        _this.setState({
          showLoading: true,
          loadingTitle: tip
        });
      };

      _this.dismissTips = function () {
        _this.timerTips && clearTimeout(_this.timerTips);
        setTimeout(function () {
          _this.setState({
            showLoading: false,
            loadingTimeout: 0,
            loadingTitle: ''
          });
        }, 300);
      };

      _this.showFailTips = function (tip) {
        _this.setState({
          showLoading: true,
          loadingTimeout: 300,
          loadingTitle: tip
        });

        _this.timerTips && clearTimeout(_this.timerTips);
        _this.timerTips = setTimeout(function () {
          _this.dismissTips();
        }, 300);
      };

      var params = _this.navigateParams();

      _this.setContent(params.error.code, params.error.isCleaning);

      var subtitle = params.error.code == _consts.PRINTER_STATE_ALERTS.ERR_PFS_PAPER_MISMATCH ? params.error.subStatus.replace('%s', paperSize) : params.error.subStatus;
      _this.state = {
        title: params.error.status,
        errorCode: params.error.code,
        subtitle: subtitle,
        errorTitleStyle: _this.setErrorContentTextStyle(params.error.code),
        description: params.error.description,
        bgcolor: errorBgColor,
        errorIon: errImagePath,
        isShowhelpButton: params.error.isHelp,
        isShowhandleButton: params.error.isResume,
        isShowdontHandleButton: true,
        errorHelpName: params.error.helpButtonName,
        error: params.error,
        showLoading: false,
        loadingTitle: '',
        showDialog: false,
        dialogTitle: _ErrorHeadFile.Language.getString('default_alert_title'),
        dialogMessage: _ErrorHeadFile.Language.getString('button_contact'),
        dailogCancelmessage: _ErrorHeadFile.Language.getString('button_cancel'),
        dialogConfirmmessage: _ErrorHeadFile.Language.getString('button_confirm')
      };
      return _this;
    }

    (0, _createClass2.default)(Error, [{
      key: "setErrorContentTextStyle",
      value: function setErrorContentTextStyle(code) {
        if (code == _consts.PRINTER_STATE_ALERTS.ERR_CARRIER_MOTOR_STALL || code == _consts.PRINTER_STATE_ALERTS.ERR_CARRIER_MOTOR_LOCK || code >= _consts.PRINTER_STATE_ALERTS.ERR_IDS_CARTRIDGE_BLACK_DEFECTIVE && code <= _consts.PRINTER_STATE_ALERTS.ERR_IDS_CARTRIDGE_BOTH_INCORRECT || code == _consts.PRINTER_STATE_ALERTS.ERR_IDS_CARTRIDGE_CONTACT_FAILURE || code >= _consts.PRINTER_STATE_ALERTS.ERR_IDS_CARTRIDGE_BLACK_INCOMPITABLE && code <= _consts.PRINTER_STATE_ALERTS.ERR_IDS_CARTRIDGE_BOTH_INCOMPITABLE) {
          return {
            textAlign: 'auto'
          };
        } else {
          return {};
        }
      }
    }, {
      key: "componentDidMount",
      value: function componentDidMount() {
        var _this2 = this;

        _ErrorHeadFile.LogUtil.reportEvent(_logKey.LOG_KEY.ROSEMARY_PAGE_EVENT_ERROR, _logKey.LOG_KEY.ROSEMARY_PAGE_EVENT_ERROR);

        _DynamicUtil.default.trackEvent(_consts.BIGDATA_KEYS.APP_PG_ERR_EVT_START, {
          code: this.state.errorCode
        });

        _ErrorHeadFile.LogUtil.reportLog('------------------错误界面进入---------------------');

        this.listener && this.listener.remove();
        this.listener = _reactNative.DeviceEventEmitter.addListener(_consts.PRINTER_STATE_CHANGE, function (e) {
          _this2.handleDeviceState(e);
        });
        this.jobListener && this.jobListener.remove();
        this.jobListener = _reactNative.DeviceEventEmitter.addListener(_consts.PRINTERP_RINTING_JOB_STATE_CHANGE, function (res) {
          _this2.handleJobState(res);
        });
        this.backHandler = _reactNative.BackHandler.addEventListener('hardwareBackPress', function () {
          return true;
        });
        this.listenerDarkMode = _reactNative.DeviceEventEmitter.addListener(_CommonHeadFile.NOTIFICATION_NAME.DarkModeSwitch, function (e) {
          _this2.setState({});
        });
      }
    }, {
      key: "componentWillUnmount",
      value: function componentWillUnmount() {
        _ErrorHeadFile.LogUtil.reportLog('------------------错误界面离开---------------------');

        this.listener && this.listener.remove();
        this.jobListener && this.jobListener.remove();
        this.backHandler && this.backHandler.remove();
        this.listenerDarkMode && this.listenerDarkMode.remove();
      }
    }, {
      key: "handleDeviceState",
      value: function handleDeviceState(result) {
        if (this.isPhotoPrinter()) {
          this.mintHandleDeviceState(result);
          return;
        }

        if (result.category.code < _consts.PRINTER_STATE.ERROR && result.category.code > 0) {
          _ErrorHeadFile.LogUtil.reportLog('error: 错误已回复');

          if (this.state.showDialog) {
            _ErrorHeadFile.Toast.show(_ErrorHeadFile.Language.getString('toast_process_success'), {
              duration: _ErrorHeadFile.Toast.durations.SHORT,
              position: _ErrorHeadFile.Toast.positions.BOTTOM,
              shadow: true,
              animation: true,
              hideOnPress: true,
              delay: 0
            });

            this.dismissTips();
          }

          global.errorViewVisible = true;
          this.props.navigation.goBack();
        } else {
          if (errorCode == 0 || errorCode != result.errors[0].code) {
            errorCode = result.errors[0].code;

            _ErrorHeadFile.LogUtil.reportLog("error: " + JSON.stringify(result));
          }

          if (result.category.code > 0) {
            this.setContent(result.errors[0].code);
            var subtitle = result.errors[0].code == _consts.PRINTER_STATE_ALERTS.ERR_PFS_PAPER_MISMATCH ? result.errors[0].subStatus.replace('%s', paperSize) : result.errors[0].subStatus;
            this.setState({
              error: result.errors[0],
              title: result.errors[0].status,
              errorCode: result.errors[0].code,
              errorTitleStyle: this.setErrorContentTextStyle(result.errors[0].code),
              subtitle: subtitle,
              description: result.errors[0].description,
              bgcolor: errorBgColor,
              errorIon: errImagePath,
              isShowhandleButton: result.errors[0].isResume,
              isShowhelpButton: result.errors[0].isHelp,
              errorHelpName: result.errors[0].helpButtonName
            });
          } else {
            this.setState({
              error: {},
              title: _ErrorHeadFile.Language.getString('get_status_title'),
              subtitle: _ErrorHeadFile.Language.getString('get_status_title'),
              description: _ErrorHeadFile.Language.getString('get_status_title'),
              bgcolor: _consts.ERR_BG.ERR_BG_RED,
              errorIon: _CommonHeadFile.HTImage.error_device_error,
              isShowhandleButton: false,
              isShowhelpButton: false
            });
          }
        }
      }
    }, {
      key: "mintHandleDeviceState",
      value: function mintHandleDeviceState(result) {
        if (result.category != _consts.MINT_PRINTER_STATUS_CATEGORY.PRINTER_STATUS_CATEGORY_ERROR) {
          _ErrorHeadFile.LogUtil.reportLog('error: 错误已恢复');

          if (this.state.showDialog) {
            _ErrorHeadFile.Toast.show(_ErrorHeadFile.Language.getString('toast_process_success'), {
              duration: _ErrorHeadFile.Toast.durations.SHORT,
              position: _ErrorHeadFile.Toast.positions.BOTTOM,
              shadow: true,
              animation: true,
              hideOnPress: true,
              delay: 0
            });

            this.dismissTips();
          }

          global.errorViewVisible = true;
          this.props.navigation.goBack();
        } else if (result.error != this.state.errorCode) {
          global.errorViewVisible = true;
          this.props.navigation.goBack();
        }
      }
    }, {
      key: "handleJobState",
      value: function handleJobState(result) {
        if (result == null) {
          return;
        }

        _ErrorHeadFile.LogUtil.reportLog('error: 打印任务状态信息->' + JSON.stringify(result));

        paperSize = _ErrorHeadFile.Language.getString('set_region_a4_sub');

        if (result != null && result.mediaSize != null) {
          switch (result.mediaSize) {
            case _consts.MEDIA_SIZE.a4:
              paperSize = _ErrorHeadFile.Language.getString('set_region_a4_sub');
              break;

            case _consts.MEDIA_SIZE.a5:
              paperSize = _ErrorHeadFile.Language.getString('set_region_a5_sub');
              break;

            case _consts.MEDIA_SIZE.index_4x6:
              paperSize = _ErrorHeadFile.Language.getString('set_region_6inch_sub');
              break;

            case _consts.MEDIA_SIZE.na_5x7:
              paperSize = _ErrorHeadFile.Language.getString('set_region_7inch_sub');
              break;

            case _consts.MEDIA_SIZE.b5:
              paperSize = _ErrorHeadFile.Language.getString('set_region_b5_sub');
              break;

            default:
              paperSize = _ErrorHeadFile.Language.getString('set_region_a4_sub');
              break;
          }
        }
      }
    }, {
      key: "progressButtonAction",
      value: function progressButtonAction() {
        var _this3 = this;

        _ErrorHeadFile.LogUtil.reportEvent(_logKey.LOG_KEY.ROSEMARY_TAP_EVENT_ERROR_PROGRESS, _logKey.LOG_KEY.ROSEMARY_TAP_EVENT_ERROR_PROGRESS);

        _ErrorHeadFile.LogUtil.reportLog('error: 点击了已处理');

        this.showLoadingTips(_ErrorHeadFile.Language.getString('toast_processing'));

        if (this.isPhotoPrinter()) {
          _HTClassicBluetooth.default.getInstance().resumePrinter().then(function () {
            _ErrorHeadFile.LogUtil.reportLog('error: 处理成功');

            _this3.dismissTips();

            setTimeout(function () {
              global.errorViewVisible = true;
            }, 1500);

            _this3.props.navigation.goBack();
          }).catch(function (err) {
            _ErrorHeadFile.LogUtil.reportLog("error: \u5904\u7406\u5931\u8D25" + JSON.stringify(err));

            _this3.dismissTips();

            _ErrorHeadFile.Toast.show(_ErrorHeadFile.Language.getString('toast_process_fail'), {
              duration: _ErrorHeadFile.Toast.durations.SHORT,
              position: _ErrorHeadFile.Toast.positions.BOTTOM,
              shadow: true,
              animation: true,
              hideOnPress: true,
              delay: 0
            });
          });
        } else {
          _ErrorHeadFile.Spec.resumePrinter().then(function () {
            _ErrorHeadFile.LogUtil.reportLog('error: 处理成功');
          }).catch(function (err) {
            _ErrorHeadFile.LogUtil.reportLog("error: \u5904\u7406\u5931\u8D25" + JSON.stringify(err));

            _this3.dismissTips();

            _ErrorHeadFile.Toast.show(_ErrorHeadFile.Language.getString('toast_process_fail'), {
              duration: _ErrorHeadFile.Toast.durations.SHORT,
              position: _ErrorHeadFile.Toast.positions.BOTTOM,
              shadow: true,
              animation: true,
              hideOnPress: true,
              delay: 0
            });
          });
        }
      }
    }, {
      key: "releaseButtonAction",
      value: function releaseButtonAction() {
        _ErrorHeadFile.LogUtil.reportEvent(_logKey.LOG_KEY.ROSEMARY_TAP_EVENT_ERROR_RELEASE, _logKey.LOG_KEY.ROSEMARY_TAP_EVENT_ERROR_RELEASE);

        if (this.isPhotoPrinter() && this.state.errorCode == _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_NO_SMARTSHEET) {
          this.progressButtonAction();
        } else {
          if (this.isEcnError(this.state.errorCode)) {
            this.cancelJob();
          } else if (!this.state.isShowhandleButton) {
            if (this.state.error.code >= 7001 && this.state.error.code <= 7022) {
              _ErrorHeadFile.Spec.cancelAlignment(2);
            } else {
              _ErrorHeadFile.Spec.resumePrinter();
            }
          }

          _ErrorHeadFile.LogUtil.reportLog('error: 点击了暂不处理');

          global.errorViewVisible = false;
          this.navigateGoBack();
        }
      }
    }, {
      key: "cancelJob",
      value: function cancelJob() {
        _ErrorHeadFile.Spec.cancelPrintJob().then(function () {
          _ErrorHeadFile.LogUtil.reportLog('error: 取消打印成功');
        }).catch(function (err) {
          _ErrorHeadFile.LogUtil.reportLog("error: \u53D6\u6D88\u6253\u5370\u5931\u8D25" + JSON.stringify(err));
        });
      }
    }, {
      key: "errorHelpAction",
      value: function errorHelpAction() {
        var _this4 = this;

        if (this.state.errorHelpName == _ErrorHeadFile.Language.getString('button_video')) {
          _ErrorHeadFile.LogUtil.reportEvent(_logKey.LOG_KEY.ROSEMARY_TAP_EVENT_ERROR_HELP, _logKey.LOG_KEY.ROSEMARY_TAP_EVENT_ERROR_HELP);

          this.props.navigation.navigate('errorHelp', {
            error: this.state.error,
            callback: function callback(isHandled) {
              global.errorViewVisible = isHandled ? isHandled : false;

              _this4.navigateGoBack();
            }
          });
        } else {
          this.setState({
            showDialog: true
          });
        }
      }
    }, {
      key: "isEcnError",
      value: function isEcnError(code) {
        if (code == _consts.PRINTER_STATE_ALERTS.ERROR_SELF_CLEARING_ERR || code == _consts.PRINTER_STATE_ALERTS.ERROR_APPLY_AI_URL_ERR || code == _consts.PRINTER_STATE_ALERTS.ERROR_APPLY_AI_PROCESS_ERR || code == _consts.PRINTER_STATE_ALERTS.ERROR_CLOUD_AI_PROCESS_ERR || code == _consts.PRINTER_STATE_ALERTS.ERROR_FAIL_TO_CORRECTLY_RECOGNIZE_TEXTBOOK) {
          return true;
        } else {
          return false;
        }
      }
    }, {
      key: "setContent",
      value: function setContent(code, isCleaning) {
        if (_deviceConfig.default.isRicottaType()) {
          switch (code) {
            case _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_COVER_OPEN:
              errImagePath = _deviceConfig.default.isRicotta() || _deviceConfig.default.isRicottaG() ? _CommonHeadFile.HTImage.ricottaErrorCoverImage : _CommonHeadFile.HTImage.ricottaPErrorCoverImage;
              break;

            case _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_PAPER_LOAD:
              errImagePath = _deviceConfig.default.isRicotta() || _deviceConfig.default.isRicottaG() ? _CommonHeadFile.HTImage.ricottaErrorRibbonTray : _CommonHeadFile.HTImage.ricottaPErrorRibbonTray;
              break;

            case _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_PAPER_JAM:
            case _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_PAPER_LENGTH_ERROR:
            case _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_PAPER_REMOVE_B:
            case _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_PAPER_REMOVE_PRINTING:
            case _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_PAPER_REMOVE_LOAD:
              errImagePath = _deviceConfig.default.isRicotta() || _deviceConfig.default.isRicottaG() ? _CommonHeadFile.HTImage.ricottaErrorPaperImage : _CommonHeadFile.HTImage.ricottaPErrorPaperImage;
              break;

            case _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_PAPER_REMOVE:
              errImagePath = _deviceConfig.default.isRicotta() || _deviceConfig.default.isRicottaG() ? _CommonHeadFile.HTImage.ricottaErrorPaperOutImage : _CommonHeadFile.HTImage.ricottaPErrorPaperOutImage;
              break;

            case _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_RIBBON_END:
            case _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_NO_RIBBON_MARKER:
            case _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_RIBBON_ERROR:
            case _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_RIBBON_ERROR_2:
              errImagePath = _deviceConfig.default.isRicotta() || _deviceConfig.default.isRicottaG() ? _CommonHeadFile.HTImage.ricottaErrorRibbonImage : _CommonHeadFile.HTImage.ricottaPErrorRibbonImage;
              break;

            case _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_PRINTER_OVERHEAT:
              errImagePath = _CommonHeadFile.HTImage.ricottaErrorOverheat;
              break;

            case _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_PRINTER_OVERCOOL:
              errImagePath = _CommonHeadFile.HTImage.ricottaErrorOvercool;
              break;

            case _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_PRINTER_BATTERY_CRITICAL:
              errImagePath = _CommonHeadFile.HTImage.ricottaErrorBatteryCritical;
              break;

            case _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_PRINTER_BATTERY_OFF:
              errImagePath = _CommonHeadFile.HTImage.ricottaErrorBatteryOff;
              break;
          }
        } else {
          switch (code) {
            case _consts.PRINTER_STATE_ALERTS.ERR_PFS_PAPER_JAM:
              errorBgColor = _consts.ERR_BG.ERR_BG_RED;
              errImagePath = _CommonHeadFile.HTImage.error_paper_jam;
              break;

            case _consts.PRINTER_STATE_ALERTS.ERR_TOP_COVER_OPEN_DURING_PROCESSING:
              errImagePath = _CommonHeadFile.HTImage.error_door_open;
              errorBgColor = _consts.ERR_BG.ERROR_BG_ORANGE;
              break;

            case _consts.PRINTER_STATE_ALERTS.ERR_PFS_PAPER_OUT:
            case _consts.PRINTER_STATE_ALERTS.ERR_PFS_NO_PICK:
              errImagePath = _CommonHeadFile.HTImage.error_paper_empty;
              errorBgColor = _consts.ERR_BG.ERROR_BG_ORANGE;
              break;

            case _consts.PRINTER_STATE_ALERTS.ERR_PFS_PAPER_MISMATCH:
            case _consts.PRINTER_STATE_ALERTS.ERR_PFS_INVALID_ALIGNMENT_101:
            case _consts.PRINTER_STATE_ALERTS.ERR_PFS_INVALID_ALIGNMENT_1X0:
            case _consts.PRINTER_STATE_ALERTS.ERR_PFS_INVALID_ALIGNMENT_010:
            case _consts.PRINTER_STATE_ALERTS.ERR_PFS_PAPER_SHORT:
              errImagePath = _CommonHeadFile.HTImage.error_paper_error;
              errorBgColor = _consts.ERR_BG.ERROR_BG_ORANGE;
              break;

            case _consts.PRINTER_STATE_ALERTS.ERR_IDS_INK_BLACK_EMPTY:
            case _consts.PRINTER_STATE_ALERTS.ERR_IDS_INK_CYAN_EMPTY:
            case _consts.PRINTER_STATE_ALERTS.ERR_IDS_INK_KC_EMPTY:
            case _consts.PRINTER_STATE_ALERTS.ERR_IDS_INK_CM_EMPTY:
            case _consts.PRINTER_STATE_ALERTS.ERR_IDS_INK_MAGENTA_EMPTY:
            case _consts.PRINTER_STATE_ALERTS.ERR_IDS_INK_KM_EMPTY:
            case _consts.PRINTER_STATE_ALERTS.ERR_IDS_INK_KCM_EMPTY:
            case _consts.PRINTER_STATE_ALERTS.ERR_IDS_INK_YELLOW_EMPTY:
            case _consts.PRINTER_STATE_ALERTS.ERR_IDS_INK_KY_EMPTY:
            case _consts.PRINTER_STATE_ALERTS.ERR_IDS_INK_CY_EMPTY:
            case _consts.PRINTER_STATE_ALERTS.ERR_IDS_INK_KCY_EMPTY:
            case _consts.PRINTER_STATE_ALERTS.ERR_IDS_INK_MY_EMPTY:
            case _consts.PRINTER_STATE_ALERTS.ERR_IDS_INK_KMY_EMPTY:
            case _consts.PRINTER_STATE_ALERTS.ERR_IDS_INK_CMY_EMPTY:
            case _consts.PRINTER_STATE_ALERTS.ERR_IDS_INK_KCMY_EMPTY:
            case _consts.PRINTER_STATE_ALERTS.ERR_IDS_LOI_SENSOR_ERROR:
              errImagePath = _CommonHeadFile.HTImage.error_ink_empty;
              errorBgColor = _consts.ERR_BG.ERROR_BG_ORANGE;
              break;

            case _consts.PRINTER_STATE_ALERTS.ERR_IDS_LOI_SENSOR_ERROR:
              errImagePath = _CommonHeadFile.HTImage.error_ink_empty;
              errorBgColor = _consts.ERR_BG.ERR_BG_RED;
              break;

            case _consts.PRINTER_STATE_ALERTS.ERR_IDS_CARTRIDGE_BLACK_DEFECTIVE:
            case _consts.PRINTER_STATE_ALERTS.ERR_IDS_CARTRIDGE_BLACK_INCORRECT:
            case _consts.PRINTER_STATE_ALERTS.ERR_IDS_CARTRIDGE_BLACK_ERROR:
            case _consts.PRINTER_STATE_ALERTS.ERR_IDS_CARTRIDGE_BLACK_INCOMPITABLE:
              errImagePath = _CommonHeadFile.HTImage.error_cartridge_black_error;
              errorBgColor = _consts.ERR_BG.ERR_BG_RED;
              break;

            case _consts.PRINTER_STATE_ALERTS.ERR_IDS_CARTRIDGE_BLACK_HIGH_TEMP:
              errImagePath = _CommonHeadFile.HTImage.error_cartridge_black_error;
              errorBgColor = _consts.ERR_BG.ERR_BG_RED;
              break;

            case _consts.PRINTER_STATE_ALERTS.ERR_IDS_CARTRIDGE_CMY_HIGH_TEMP:
              errImagePath = _CommonHeadFile.HTImage.error_cartridge_cmy_error;
              errorBgColor = _consts.ERR_BG.ERR_BG_RED;
              break;

            case _consts.PRINTER_STATE_ALERTS.ERR_IDS_CARTRIDGE_BOTH_HIGH_TEMP:
              errImagePath = _CommonHeadFile.HTImage.error_cartridge_both_error;
              errorBgColor = _consts.ERR_BG.ERR_BG_RED;
              break;

            case _consts.PRINTER_STATE_ALERTS.ERR_CARTRIDGE_CMY_DEFECTIVE:
            case _consts.PRINTER_STATE_ALERTS.ERR_IDS_CARTRIDGE_CMY_INCORRECT:
            case _consts.PRINTER_STATE_ALERTS.ERR_IDS_CARTRIDGE_CMY_ERROR:
            case _consts.PRINTER_STATE_ALERTS.ERR_IDS_CARTRIDGE_CMY_INCOMPITABLE:
              errImagePath = _CommonHeadFile.HTImage.error_cartridge_cmy_error;
              errorBgColor = _consts.ERR_BG.ERR_BG_RED;
              break;

            case _consts.PRINTER_STATE_ALERTS.ERR_IDS_CARTRIDGE_DEFECTIVE:
            case _consts.PRINTER_STATE_ALERTS.ERR_IDS_CARTRIDGE_BOTH_INCORRECT:
            case _consts.PRINTER_STATE_ALERTS.ERR_IDS_CARTRIDGE_BOTH_ERROR:
            case _consts.PRINTER_STATE_ALERTS.ERR_IDS_CARTRIDGE_BOTH_INCOMPITABLE:
            case _consts.PRINTER_STATE_ALERTS.ERR_IDS_CARTRIDGE_CONTACT_FAILURE:
              errImagePath = _CommonHeadFile.HTImage.error_cartridge_both_error;
              errorBgColor = _consts.ERR_BG.ERR_BG_RED;
              break;

            case _consts.PRINTER_STATE_ALERTS.ERR_IDS_CARTRIDGE_BLACK_ABSENT:
              errImagePath = _CommonHeadFile.HTImage.error_cartridge_black_absent;
              errorBgColor = _consts.ERR_BG.ERR_BG_RED;
              break;

            case _consts.PRINTER_STATE_ALERTS.ERR_IDS_CARTRIDGE_CMY_ABSENT:
              errImagePath = _CommonHeadFile.HTImage.error_cartridge_cmy_absent;
              errorBgColor = _consts.ERR_BG.ERR_BG_RED;
              break;

            case _consts.PRINTER_STATE_ALERTS.ERR_IDS_CARTRIDGE_BOTH_ABSENT:
              errImagePath = _CommonHeadFile.HTImage.error_cartridge_both_absent;
              errorBgColor = _consts.ERR_BG.ERR_BG_RED;
              break;

            case _consts.PRINTER_STATE_ALERTS.ERROR_SELF_CLEARING_ERR:
            case _consts.PRINTER_STATE_ALERTS.ERROR_APPLY_AI_URL_ERR:
            case _consts.PRINTER_STATE_ALERTS.ERROR_APPLY_AI_PROCESS_ERR:
            case _consts.PRINTER_STATE_ALERTS.ERROR_CLOUD_AI_PROCESS_ERR:
            case _consts.PRINTER_STATE_ALERTS.ERROR_FAIL_TO_CORRECTLY_RECOGNIZE_TEXTBOOK:
              errImagePath = _CommonHeadFile.HTImage.error_device_error;
              errorBgColor = _consts.ERR_BG.ERROR_BG_ORANGE;
              break;

            case _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_OUT_OF_MEMORY:
              errImagePath = _CommonHeadFile.HTImage.errorMintNormal;
              errorBgColor = _consts.ERR_BG.ERROR_BG_ORANGE;
              break;

            case _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_SYSTEM_ERROR:
              errImagePath = _CommonHeadFile.HTImage.errorMintNormal;
              errorBgColor = _consts.ERR_BG.ERROR_BG_ORANGE;
              break;

            case _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_DECODE_ERROR:
              errImagePath = _CommonHeadFile.HTImage.errorMintNormal;
              errorBgColor = _consts.ERR_BG.ERROR_BG_ORANGE;
              break;

            case _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_COVER_OPEN:
              if (isCleaning) {
                errImagePath = _CommonHeadFile.HTImage.errorMintSmart;
              } else {
                errImagePath = _CommonHeadFile.HTImage.errorMintOpen;
              }

              errorBgColor = _consts.ERR_BG.ERROR_BG_ORANGE;
              break;

            case _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_HEAD_OVER_HEAT:
              errImagePath = _CommonHeadFile.HTImage.errorMintNormal;
              errorBgColor = _consts.ERR_BG.ERROR_BG_ORANGE;
              break;

            case _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_PAPER_EMPTY:
              if (isCleaning) {
                errImagePath = _CommonHeadFile.HTImage.errorMintSmart;
              } else {
                errImagePath = _CommonHeadFile.HTImage.errorMintOpen;
              }

              errorBgColor = _consts.ERR_BG.ERROR_BG_ORANGE;
              break;

            case _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_PAPER_MISMATCH:
              if (isCleaning) {
                errImagePath = _CommonHeadFile.HTImage.errorMintSmart;
              } else {
                errImagePath = _CommonHeadFile.HTImage.errorMintOpen;
              }

              errorBgColor = _consts.ERR_BG.ERROR_BG_ORANGE;
              break;

            case _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_PAPER_LOAD:
              if (isCleaning) {
                errImagePath = _CommonHeadFile.HTImage.errorMintSmart;
              } else {
                errImagePath = _CommonHeadFile.HTImage.errorMintOpen;
              }

              errorBgColor = _consts.ERR_BG.ERROR_BG_ORANGE;
              break;

            case _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_PAPER_JAM:
              errImagePath = _CommonHeadFile.HTImage.errorMintButton;
              errorBgColor = _consts.ERR_BG.ERROR_BG_ORANGE;
              break;

            case _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_NO_SMARTSHEET:
              errImagePath = _CommonHeadFile.HTImage.errorMintSmart;
              errorBgColor = _consts.ERR_BG.ERROR_BG_ORANGE;
              break;

            case _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_HW_ERROR:
              errImagePath = _CommonHeadFile.HTImage.errorMintButton;
              errorBgColor = _consts.ERR_BG.ERROR_BG_ORANGE;
              break;

            case _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_PAPER_LENGTH_ERROR:
            case _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_PAPER_EJECT_ERROR:
            case _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_PAPER_ERROR:
            case _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_PAPER_REMOVE:
            case _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_RIBBON_END:
            case _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_NO_RIBBON:
            case _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_NO_RIBBON_MARKER:
            case _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_RIBBON_ERROR:
              errImagePath = _CommonHeadFile.HTImage.errorMintNormal;
              errorBgColor = _consts.ERR_BG.ERROR_BG_ORANGE;
              break;

            default:
              errorBgColor = _consts.ERR_BG.ERR_BG_RED;
              errImagePath = _CommonHeadFile.HTImage.error_device_error;
              break;
          }
        }
      }
    }, {
      key: "navigateTo",
      value: function navigateTo(path) {
        this.props.navigation.navigate(path);
      }
    }, {
      key: "navigateGoBack",
      value: function navigateGoBack() {
        this.props.navigation.goBack();

        if (this.props.navigation.state.params.callback != null) {
          this.listener && this.listener.remove();
          this.props.navigation.state.params.callback(_consts.VIEW_NAME.ERROR_VIEW, this.props.navigation);
        } else {}
      }
    }, {
      key: "navigateParams",
      value: function navigateParams() {
        var params = this.props.navigation.state.params;
        return params;
      }
    }, {
      key: "cancelAction",
      value: function cancelAction() {
        this.setState({
          showDialog: false
        });
      }
    }, {
      key: "confirmAction",
      value: function confirmAction() {
        _ErrorHeadFile.LogUtil.reportEvent(_logKey.LOG_KEY.ROSEMARY_TAP_EVENT_ERROR_CALL, _logKey.LOG_KEY.ROSEMARY_TAP_EVENT_ERROR_CALL);

        this.setState({
          showDialog: false
        });
        var url = "tel:4001005678";

        _reactNative.Linking.canOpenURL(url).then(function (supported) {
          if (!supported) {
            _ErrorHeadFile.LogUtil.reportLog('error: 不支持打电话功能');
          } else {
            _ErrorHeadFile.LogUtil.reportLog("error: \u652F\u6301\u6253\u7535\u8BDD\u529F\u80FD--\u300Btel:4001005678");

            return _reactNative.Linking.openURL(url);
          }
        }).catch(function (err) {
          return _ErrorHeadFile.LogUtil.reportLog("error\uFF1A An error occurred--\u300B" + err);
        });
      }
    }, {
      key: "dismissAction",
      value: function dismissAction() {
        this.setState({
          showDialog: false
        });
      }
    }, {
      key: "getReleaseButtonName",
      value: function getReleaseButtonName() {
        var result = '';

        if (this.isPhotoPrinter() && this.state.errorCode == _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_NO_SMARTSHEET) {
          result = this.navigateParams().error.releaseButtonName;
        } else if (this.isEcnError(this.state.errorCode)) {
          result = _ErrorHeadFile.Language.getString('ecn_error_button');
        } else if (this.state.isShowhandleButton) {
          result = _ErrorHeadFile.Language.getString('button_skip');
        } else {
          result = _ErrorHeadFile.Language.getString('button_ok');
        }

        return result;
      }
    }, {
      key: "isPhotoPrinter",
      value: function isPhotoPrinter() {
        return _deviceConfig.default.isPhotoPrinter();
      }
    }, {
      key: "getTitleTextColor",
      value: function getTitleTextColor() {
        if (_deviceConfig.default.isRicottaType()) {
          return _CommonHeadFile.HTColor.c_font_000000;
        } else {
          return _CommonHeadFile.HTColor.c_alphaColor('#FFFFFF', 0.9);
        }
      }
    }, {
      key: "getErrorCodeTextColor",
      value: function getErrorCodeTextColor() {
        if (_deviceConfig.default.isRicottaType()) {
          return _CommonHeadFile.HTColor.c_alphaColor(_CommonHeadFile.HTColor.c_font_000000, 0.5);
        } else {
          return _CommonHeadFile.HTColor.c_alphaColor('#FFFFFF', 0.5);
        }
      }
    }, {
      key: "getErrorTitleStyle",
      value: function getErrorTitleStyle() {
        if (_deviceConfig.default.isRicottaType()) {
          return {
            color: _CommonHeadFile.HTColor.c_font_000000,
            textAlign: 'left'
          };
        } else {
          return this.state.errorTitleStyle;
        }
      }
    }, {
      key: "getDescriptionStyle",
      value: function getDescriptionStyle() {
        if (_deviceConfig.default.isRicottaType()) {
          return {
            color: _CommonHeadFile.HTColor.c_font_black_40,
            textAlign: 'left'
          };
        } else {
          return {};
        }
      }
    }, {
      key: "get2_3ButtonTextColor",
      value: function get2_3ButtonTextColor() {
        if (_deviceConfig.default.isRicottaType()) {
          return _CommonHeadFile.HTColor.c_font_000000;
        } else {
          return _CommonHeadFile.HTColor.c_alphaColor('#FFFFFF', 0.9);
        }
      }
    }, {
      key: "get2_3ButtonBackgroundColor",
      value: function get2_3ButtonBackgroundColor() {
        if (_deviceConfig.default.isRicottaType()) {
          return _CommonHeadFile.HTColor.c_btn_gray;
        } else {
          return _CommonHeadFile.HTColor.c_alphaColor('#FFFFFF', 0.9);
        }
      }
    }, {
      key: "getReleaseButtonBackgroundColor",
      value: function getReleaseButtonBackgroundColor() {
        if (_deviceConfig.default.isRicottaType()) {
          return "#0C80FF";
        } else {
          return null;
        }
      }
    }, {
      key: "getBgResources",
      value: function getBgResources() {
        if (_deviceConfig.default.isRicottaType()) {
          return null;
        } else {
          return this.state.bgcolor;
        }
      }
    }, {
      key: "getBgColor",
      value: function getBgColor() {
        if (_deviceConfig.default.isRicottaType()) {
          return _CommonHeadFile.HTColor.c_bg_FFFFFF;
        } else {
          return null;
        }
      }
    }, {
      key: "render",
      value: function render() {
        return _react.default.createElement(_errorView.default, {
          title: this.state.title,
          titleTextColor: this.getTitleTextColor(),
          errorCode: this.state.errorCode,
          titleErrorCodeColor: this.getErrorCodeTextColor(),
          subtitle: this.state.subtitle,
          errorTitleStyle: this.getErrorTitleStyle(),
          description: this.state.description,
          descriptionStyle: this.getDescriptionStyle(),
          bgResources: this.getBgResources(),
          imagepath: this.state.errorIon,
          releaseButtonName: this.getReleaseButtonName(),
          errorHelpName: this.state.errorHelpName,
          progressButtonAction: this.progressButtonAction.bind(this),
          releaseButtonAction: this.releaseButtonAction.bind(this),
          errorHelpAction: this.errorHelpAction.bind(this),
          isShowhelpButton: this.state.isShowhelpButton,
          isShowhandleButton: this.state.isShowhandleButton,
          isShowdontHandleButton: this.state.isShowdontHandleButton,
          showDialog: this.state.showLoading,
          dialogTitle: this.state.loadingTitle,
          dialogDismiss: this.dismissTips.bind(this),
          showMessageDialog: this.state.showDialog,
          MDTitle: this.state.dialogTitle,
          MDContent: this.state.dialogMessage,
          MDCancelMessage: this.state.dailogCancelmessage,
          MDConfirmMessage: this.state.dialogConfirmmessage,
          MDCancelAction: this.cancelAction.bind(this),
          MDConfirmAction: this.confirmAction.bind(this),
          handleButtonTextColor: this.get2_3ButtonTextColor(),
          handleButtonBackgroundColor: this.get2_3ButtonBackgroundColor(),
          releaseButtonBackgroundColor: this.getReleaseButtonBackgroundColor(),
          bgColor: this.getBgColor()
        });
      }
    }]);
    return Error;
  }(_react.default.Component);

  exports.default = Error;

  Error.navigationOptions = function (_ref) {
    var navigation = _ref.navigation;
    return {
      header: null,
      gesturesEnabled: false
    };
  };
},13145,[14314,14329,14332,14383,14389,14398,10297,10033,12173,13148,13139,12494,10052,12407,10074,12179,12197]);