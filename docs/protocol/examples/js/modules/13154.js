__d(function (global, _$$_REQUIRE, _$$_IMPORT_DEFAULT, _$$_IMPORT_ALL, module, exports, _dependencyMap) {
  var _interopRequireWildcard = _$$_REQUIRE(_dependencyMap[0]);

  var _interopRequireDefault = _$$_REQUIRE(_dependencyMap[1]);

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  exports.default = undefined;

  var _classCallCheck2 = _interopRequireDefault(_$$_REQUIRE(_dependencyMap[2]));

  var _possibleConstructorReturn2 = _interopRequireDefault(_$$_REQUIRE(_dependencyMap[3]));

  var _getPrototypeOf2 = _interopRequireDefault(_$$_REQUIRE(_dependencyMap[4]));

  var _createClass2 = _interopRequireDefault(_$$_REQUIRE(_dependencyMap[5]));

  var _inherits2 = _interopRequireDefault(_$$_REQUIRE(_dependencyMap[6]));

  var _react = _interopRequireWildcard(_$$_REQUIRE(_dependencyMap[7]));

  var _reactNative = _$$_REQUIRE(_dependencyMap[8]);

  var _consts = _$$_REQUIRE(_dependencyMap[9]);

  var _errorHelpView = _interopRequireDefault(_$$_REQUIRE(_dependencyMap[10]));

  var _ErrorHeadFile = _$$_REQUIRE(_dependencyMap[11]);

  var _logKey = _$$_REQUIRE(_dependencyMap[12]);

  var _HTClassicBluetooth = _interopRequireDefault(_$$_REQUIRE(_dependencyMap[13]));

  var _miot = _$$_REQUIRE(_dependencyMap[14]);

  var _deviceConfig = _interopRequireDefault(_$$_REQUIRE(_dependencyMap[15]));

  var _ComponentsHeadFile = _$$_REQUIRE(_dependencyMap[16]);

  var _UtilsHeadFile = _$$_REQUIRE(_dependencyMap[17]);

  var _ThirdPartyHeadFile = _$$_REQUIRE(_dependencyMap[18]);

  var _bluetoothPrintManger = _interopRequireDefault(_$$_REQUIRE(_dependencyMap[19]));

  var _logUtils = _interopRequireDefault(_$$_REQUIRE(_dependencyMap[20]));

  var errorCode = 0;
  var paperSize = 'A4';
  var videoPath = '';

  var ErrorHelp = function (_Component) {
    (0, _inherits2.default)(ErrorHelp, _Component);
    (0, _createClass2.default)(ErrorHelp, [{
      key: "navigateTo",
      value: function navigateTo(path) {
        this.props.navigation.navigate(path);
      }
    }]);

    function ErrorHelp(props) {
      var _this;

      (0, _classCallCheck2.default)(this, ErrorHelp);
      _this = (0, _possibleConstructorReturn2.default)(this, (0, _getPrototypeOf2.default)(ErrorHelp).call(this, props));

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
        _this.setState({
          showDialog: true,
          dialogTimeout: 300,
          dialogTitle: tip
        });

        _this.timerTips && clearTimeout(_this.timerTips);
        _this.timerTips = setTimeout(function () {
          _this.dismissTips();
        }, 300);
      };

      var params = _this.props.navigation.state.params;

      _this.setContent(params.error.code);

      _this.state = {
        title: params.error.helpTitle ? params.error.helpTitle : params.error.status,
        subtitle: params.error.subStatus,
        helpDescription: params.error.helpDesription,
        description: params.error.description,
        error: {},
        isShowhandleButton: params.error.isResume,
        showDialog: false,
        dialogTimeout: 0,
        dialogTitle: '',
        videoPath: videoPath,
        errorCode: params.error.code
      };
      return _this;
    }

    (0, _createClass2.default)(ErrorHelp, [{
      key: "componentDidMount",
      value: function componentDidMount() {
        var _this2 = this;

        _ErrorHeadFile.LogUtil.reportEvent(_logKey.LOG_KEY.ROSEMARY_PAGE_EVENT_ERROR_HELP, _logKey.LOG_KEY.ROSEMARY_PAGE_EVENT_ERROR_HELP);

        _ErrorHeadFile.LogUtil.reportLog('------------------错误帮助界面进入---------------------');

        this.listener = _reactNative.DeviceEventEmitter.addListener(_consts.PRINTER_STATE_CHANGE, function (e) {
          _this2.handleDeviceState(e);
        });
        this.jobListener = _reactNative.DeviceEventEmitter.addListener(_consts.PRINTERP_RINTING_JOB_STATE_CHANGE, function (res) {
          _this2.handleJobState(res);
        });
        this.backHandler = _reactNative.BackHandler.addEventListener('hardwareBackPress', function () {
          return true;
        });
        this.listenerDarkMode = _reactNative.DeviceEventEmitter.addListener(_UtilsHeadFile.NOTIFICATION_NAME.DarkModeSwitch, function (e) {
          _this2.setState({});
        });
      }
    }, {
      key: "componentWillUnmount",
      value: function componentWillUnmount() {
        _ErrorHeadFile.LogUtil.reportLog('------------------错误帮助界面离开---------------------');

        this.setState({
          isPaused: true
        });
        this.listener.remove();
        this.timer && clearTimeout(this.timer);
        this.backHandler && this.backHandler.remove();
        this.listenerDarkMode && this.listenerDarkMode.remove();
      }
    }, {
      key: "setContent",
      value: function setContent(code) {
        var isCleaning = this.props.navigation.state.params.error.isCleaning;

        switch (code) {
          case _consts.PRINTER_STATE_ALERTS.ERR_PFS_PAPER_JAM:
            videoPath = 'https://cdn.cnbj1.fds.api.mi-img.com/hantu/static/rosemary/video/matter_remove.mp4';
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
            videoPath = 'https://cdn.cnbj1.fds.api.mi-img.com/hantu/static/rosemary/video/ink_install.mp4';
            break;

          case _consts.PRINTER_STATE_ALERTS.ERR_IDS_CARTRIDGE_BLACK_ABSENT:
          case _consts.PRINTER_STATE_ALERTS.ERR_IDS_CARTRIDGE_CMY_ABSENT:
          case _consts.PRINTER_STATE_ALERTS.ERR_IDS_CARTRIDGE_BOTH_ABSENT:
          case _consts.PRINTER_STATE_ALERTS.ERR_IDS_CARTRIDGE_BLACK_GAS_GAUGE_END:
          case _consts.PRINTER_STATE_ALERTS.ERR_IDS_CARTRIDGE_CMY_GAS_GAUGE_END:
          case _consts.PRINTER_STATE_ALERTS.ERR_IDS_CARTRIDGE_BOTH_GAS_GAUGE_END:
          case _consts.PRINTER_STATE_ALERTS.ERR_IDS_CARTRIDGE_BLACK_DETACH_END:
          case _consts.PRINTER_STATE_ALERTS.ERR_IDS_CARTRIDGE_CMY_DETACH_END:
          case _consts.PRINTER_STATE_ALERTS.ERR_IDS_CARTRIDGE_BOTH_DETACH_END:
          case _consts.PRINTER_STATE_ALERTS.ERR_IDS_CARTRIDGE_BLACK_PRIMING_END:
          case _consts.PRINTER_STATE_ALERTS.ERR_IDS_CARTRIDGE_CMY_PRIMING_END:
          case _consts.PRINTER_STATE_ALERTS.ERR_IDS_CARTRIDGE_BOTH_PRIMING_END:
            videoPath = 'https://cdn.cnbj1.fds.api.mi-img.com/hantu/static/rosemary/video/printhead_install.mp4';
            break;

          case _consts.PRINTER_STATE_ALERTS.ERR_IDS_CARTRIDGE_DEFECTIVE:
          case _consts.PRINTER_STATE_ALERTS.ERR_IDS_CARTRIDGE_BLACK_HIGH_TEMP:
          case _consts.PRINTER_STATE_ALERTS.ERR_IDS_CARTRIDGE_CMY_HIGH_TEMP:
          case _consts.PRINTER_STATE_ALERTS.ERR_IDS_CARTRIDGE_BOTH_HIGH_TEMP:
          case _consts.PRINTER_STATE_ALERTS.ERR_IDS_CARTRIDGE_BLACK_INCORRECT:
          case _consts.PRINTER_STATE_ALERTS.ERR_IDS_CARTRIDGE_CMY_INCORRECT:
          case _consts.PRINTER_STATE_ALERTS.ERR_IDS_CARTRIDGE_BOTH_INCORRECT:
          case _consts.PRINTER_STATE_ALERTS.ERR_IDS_CARTRIDGE_CONTACT_FAILURE:
          case _consts.PRINTER_STATE_ALERTS.ERR_IDS_CARTRIDGE_BLACK_INCOMPITABLE:
          case _consts.PRINTER_STATE_ALERTS.ERR_IDS_CARTRIDGE_CMY_INCOMPITABLE:
          case _consts.PRINTER_STATE_ALERTS.ERR_IDS_CARTRIDGE_BOTH_INCOMPITABLE:
            videoPath = "https://cdn.eco.mi.com/hantu/static/rosemary/video/RM_Russia_print_head_handling.mp4";
            break;

          case _consts.PRINTER_STATE_ALERTS.ERROR_SELF_CLEARING_ERR:
          case _consts.PRINTER_STATE_ALERTS.ERROR_APPLY_AI_URL_ERR:
          case _consts.PRINTER_STATE_ALERTS.ERROR_APPLY_AI_PROCESS_ERR:
          case _consts.PRINTER_STATE_ALERTS.ERROR_CLOUD_AI_PROCESS_ERR:
          case _consts.PRINTER_STATE_ALERTS.ERROR_FAIL_TO_CORRECTLY_RECOGNIZE_TEXTBOOK:
            var jobType = this.props.navigation.state.params.error.jobType;

            if (jobType == _consts.JOB_TYPE.AI_EXAM_PAPER_REMOVE_HANDWEITTEN_JOB) {
              videoPath = "https://cdn.eco.mi.com/hantu/static/rosemary/video/RM_APP_Error_Exam_Restore.mp4";
            } else if (jobType == _consts.JOB_TYPE.AI_TEXT_PREVIEW_JOB) {
              videoPath = "https://cdn.eco.mi.com/hantu/static/rosemary/video/RM_APP_Error_Class_Preview.mp4";
            }

            break;

          case _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_PAPER_EMPTY:
          case _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_NO_SMARTSHEET:
            if (isCleaning) {
              videoPath = this.getMintOutPaperClean();
            } else {
              videoPath = this.getMintOutPaperPrint();
              ;
            }

            break;

          case _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_PAPER_LOAD:
            if (this.isRicottaType()) {
              videoPath = this.getRicottaPaperLoadError();
            } else {
              if (isCleaning) {
                videoPath = this.getMintPaperLoadErrorClean();
              } else {
                videoPath = this.getMintPaperLoadErrorPrint();
              }
            }

            break;

          case _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_PAPER_LENGTH_ERROR:
            videoPath = this.getRicottaPaperLengthError();
            break;

          case _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_NO_RIBBON_MARKER:
            videoPath = this.getRicottaNoRibbonError();
            break;

          case _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_RIBBON_ERROR:
            videoPath = this.getRicottaRibbonError();
            break;

          case _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_PAPER_REMOVE_B:
            videoPath = this.getRicottaPaperDectectBError();
            break;

          case _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_PAPER_REMOVE_LOAD:
            videoPath = this.getRicottaPickupPaperJamError();
            break;

          case _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_RIBBON_ERROR_2:
            videoPath = this.getRicottaSecondRibbonError();
            break;

          case _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_PAPER_JAM:
            videoPath = this.getPaperJamError();
            break;
        }
      }
    }, {
      key: "getMintOutPaperClean",
      value: function getMintOutPaperClean() {
        var server = _bluetoothPrintManger.default.ShareInstance().getServer();

        if (server) {
          if (server.serverCode.toLowerCase() == 'sg') {
            return _consts.MINT_ERROR_HELP_URL.MINT_OUT_PAPER_CLEAN_SG;
          } else if (server.serverCode.toLowerCase() == 'us') {
            return _consts.MINT_ERROR_HELP_URL.MINT_OUT_PAPER_CLEAN_OR;
          } else if (server.serverCode.toLowerCase() == 'de') {
            return _consts.MINT_ERROR_HELP_URL.MINT_OUT_PAPER_CLEAN_EU;
          } else if (server.serverCode.toLowerCase() == 'ru') {
            return _consts.MINT_ERROR_HELP_URL.MINT_OUT_PAPER_CLEAN_RU;
          } else {
            return _consts.MINT_ERROR_HELP_URL.MINT_OUT_PAPER_CLEAN_SG;
          }
        } else {
          return _consts.MINT_ERROR_HELP_URL.MINT_OUT_PAPER_CLEAN_SG;
        }
      }
    }, {
      key: "getMintOutPaperPrint",
      value: function getMintOutPaperPrint() {
        var server = _bluetoothPrintManger.default.ShareInstance().getServer();

        if (server) {
          if (server.serverCode.toLowerCase() == 'sg') {
            return _consts.MINT_ERROR_HELP_URL.MINT_OUT_PAPER_PRINT_SG;
          } else if (server.serverCode.toLowerCase() == 'us') {
            return _consts.MINT_ERROR_HELP_URL.MINT_OUT_PAPER_PRINT_OR;
          } else if (server.serverCode.toLowerCase() == 'de') {
            return _consts.MINT_ERROR_HELP_URL.MINT_OUT_PAPER_PRINT_EU;
          } else if (server.serverCode.toLowerCase() == 'ru') {
            return _consts.MINT_ERROR_HELP_URL.MINT_OUT_PAPER_PRINT_RU;
          } else {
            return _consts.MINT_ERROR_HELP_URL.MINT_OUT_PAPER_PRINT_SG;
          }
        } else {
          return _consts.MINT_ERROR_HELP_URL.MINT_OUT_PAPER_PRINT_SG;
        }
      }
    }, {
      key: "getMintPaperLoadErrorClean",
      value: function getMintPaperLoadErrorClean() {
        var server = _bluetoothPrintManger.default.ShareInstance().getServer();

        if (server) {
          if (server.serverCode.toLowerCase() == 'sg') {
            return _consts.MINT_ERROR_HELP_URL.MINT_PAPER_LOAD_ERROR_CLEAN_SG;
          } else if (server.serverCode.toLowerCase() == 'us') {
            return _consts.MINT_ERROR_HELP_URL.MINT_PAPER_LOAD_ERROR_CLEAN_OR;
          } else if (server.serverCode.toLowerCase() == 'de') {
            return _consts.MINT_ERROR_HELP_URL.MINT_PAPER_LOAD_ERROR_CLEAN_EU;
          } else if (server.serverCode.toLowerCase() == 'ru') {
            return _consts.MINT_ERROR_HELP_URL.MINT_PAPER_LOAD_ERROR_CLEAN_RU;
          } else {
            return _consts.MINT_ERROR_HELP_URL.MINT_PAPER_LOAD_ERROR_CLEAN_SG;
          }
        } else {
          return _consts.MINT_ERROR_HELP_URL.MINT_PAPER_LOAD_ERROR_CLEAN_SG;
        }
      }
    }, {
      key: "getMintPaperLoadErrorPrint",
      value: function getMintPaperLoadErrorPrint() {
        var server = _bluetoothPrintManger.default.ShareInstance().getServer();

        if (server) {
          if (server.serverCode.toLowerCase() == 'sg') {
            return _consts.MINT_ERROR_HELP_URL.MINT_PAPER_LOAD_ERROR_PRINT_SG;
          } else if (server.serverCode.toLowerCase() == 'us') {
            return _consts.MINT_ERROR_HELP_URL.MINT_PAPER_LOAD_ERROR_PRINT_OR;
          } else if (server.serverCode.toLowerCase() == 'de') {
            return _consts.MINT_ERROR_HELP_URL.MINT_PAPER_LOAD_ERROR_PRINT_EU;
          } else if (server.serverCode.toLowerCase() == 'ru') {
            return _consts.MINT_ERROR_HELP_URL.MINT_PAPER_LOAD_ERROR_PRINT_RU;
          } else {
            return _consts.MINT_ERROR_HELP_URL.MINT_PAPER_LOAD_ERROR_PRINT_SG;
          }
        } else {
          return _consts.MINT_ERROR_HELP_URL.MINT_PAPER_LOAD_ERROR_PRINT_SG;
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
          this.props.navigation.goBack(this.props.navigation.state.params.screen_key);
        } else if (result.error != this.state.errorCode) {
          global.errorViewVisible = true;
          this.props.navigation.goBack(this.props.navigation.state.params.screen_key);
        }
      }
    }, {
      key: "getRicottaPaperLoadError",
      value: function getRicottaPaperLoadError() {
        var server = _bluetoothPrintManger.default.ShareInstance().getServer();

        if (_deviceConfig.default.isRicotta()) {
          return _consts.RICOTTA_ERROR_HELP_URL.RICOTTA_PAPER_LOAD_URL;
        } else if (_deviceConfig.default.isRicottaP()) {
          return _consts.RICOTTA_ERROR_HELP_URL.RICOTTA_P_PAPER_LOAD_URL;
        } else if (_deviceConfig.default.isRicottaG()) {
          if (server) {
            if (server.serverCode.toLowerCase() == 'sg') {
              return _consts.RICOTTA_ERROR_HELP_URL.RICOTTA_OVERSEAS_PAPER_LOAD_URL_SG;
            } else if (server.serverCode.toLowerCase() == 'us') {
              return _consts.RICOTTA_ERROR_HELP_URL.RICOTTA_OVERSEAS_PAPER_LOAD_URL_OR;
            } else if (server.serverCode.toLowerCase() == 'de') {
              return _consts.RICOTTA_ERROR_HELP_URL.RICOTTA_OVERSEAS_PAPER_LOAD_URL_EU;
            } else if (server.serverCode.toLowerCase() == 'ru') {
              return _consts.RICOTTA_ERROR_HELP_URL.RICOTTA_OVERSEAS_PAPER_LOAD_URL_RU;
            } else {
              return _consts.RICOTTA_ERROR_HELP_URL.RICOTTA_OVERSEAS_PAPER_LOAD_URL_SG;
            }
          } else {
            return _consts.RICOTTA_ERROR_HELP_URL.RICOTTA_OVERSEAS_PAPER_LOAD_URL_SG;
          }
        } else {
          return _consts.RICOTTA_ERROR_HELP_URL.RICOTTA_PAPER_LOAD_URL;
        }
      }
    }, {
      key: "getRicottaPickupPaperJamError",
      value: function getRicottaPickupPaperJamError() {
        var server = _bluetoothPrintManger.default.ShareInstance().getServer();

        if (_deviceConfig.default.isRicotta()) {
          return _consts.RICOTTA_ERROR_HELP_URL.RICOTTA_PICKUP_PAPER_JAM_URL;
        } else if (_deviceConfig.default.isRicottaP()) {
          return _consts.RICOTTA_ERROR_HELP_URL.RICOTTA_P_PICKUP_PAPER_JAM_URL;
        } else if (_deviceConfig.default.isRicottaG()) {
          if (server) {
            if (server.serverCode.toLowerCase() == 'sg') {
              return _consts.RICOTTA_ERROR_HELP_URL.RICOTTA_OVERSEAS_PICKUP_PAPER_JAM_URL_SG;
            } else if (server.serverCode.toLowerCase() == 'us') {
              return _consts.RICOTTA_ERROR_HELP_URL.RICOTTA_OVERSEAS_PICKUP_PAPER_JAM_URL_OR;
            } else if (server.serverCode.toLowerCase() == 'de') {
              return _consts.RICOTTA_ERROR_HELP_URL.RICOTTA_OVERSEAS_PICKUP_PAPER_JAM_URL_EU;
            } else if (server.serverCode.toLowerCase() == 'ru') {
              return _consts.RICOTTA_ERROR_HELP_URL.RICOTTA_OVERSEAS_PICKUP_PAPER_JAM_URL_RU;
            } else {
              return _consts.RICOTTA_ERROR_HELP_URL.RICOTTA_OVERSEAS_PICKUP_PAPER_JAM_URL_SG;
            }
          } else {
            return _consts.RICOTTA_ERROR_HELP_URL.RICOTTA_OVERSEAS_PICKUP_PAPER_JAM_URL_SG;
          }
        } else {
          return _consts.RICOTTA_ERROR_HELP_URL.RICOTTA_PICKUP_PAPER_JAM_URL;
        }
      }
    }, {
      key: "getRicottaPaperDectectBError",
      value: function getRicottaPaperDectectBError() {
        var server = _bluetoothPrintManger.default.ShareInstance().getServer();

        if (_deviceConfig.default.isRicotta()) {
          return _consts.RICOTTA_ERROR_HELP_URL.RICOTTA_PAPER_DETECT_B_URL;
        } else if (_deviceConfig.default.isRicottaP()) {
          return _consts.RICOTTA_ERROR_HELP_URL.RICOTTA_P_PAPER_DETECT_B_URL;
        } else if (_deviceConfig.default.isRicottaG()) {
          if (server) {
            if (server.serverCode.toLowerCase() == 'sg') {
              return _consts.RICOTTA_ERROR_HELP_URL.RICOTTA_OVERSEAS_PAPER_DETECT_B_URL_SG;
            } else if (server.serverCode.toLowerCase() == 'us') {
              return _consts.RICOTTA_ERROR_HELP_URL.RICOTTA_OVERSEAS_PAPER_DETECT_B_URL_OR;
            } else if (server.serverCode.toLowerCase() == 'de') {
              return _consts.RICOTTA_ERROR_HELP_URL.RICOTTA_OVERSEAS_PAPER_DETECT_B_URL_EU;
            } else if (server.serverCode.toLowerCase() == 'ru') {
              return _consts.RICOTTA_ERROR_HELP_URL.RICOTTA_OVERSEAS_PAPER_DETECT_B_URL_RU;
            } else {
              return _consts.RICOTTA_ERROR_HELP_URL.RICOTTA_OVERSEAS_PAPER_DETECT_B_URL_SG;
            }
          } else {
            return _consts.RICOTTA_ERROR_HELP_URL.RICOTTA_OVERSEAS_PAPER_DETECT_B_URL_SG;
          }
        } else {
          return _consts.RICOTTA_ERROR_HELP_URL.RICOTTA_PAPER_DETECT_B_URL;
        }
      }
    }, {
      key: "getRicottaNoRibbonError",
      value: function getRicottaNoRibbonError() {
        var server = _bluetoothPrintManger.default.ShareInstance().getServer();

        if (_deviceConfig.default.isRicotta()) {
          return _consts.RICOTTA_ERROR_HELP_URL.RICOTTA_NO_RIBBON_URL;
        } else if (_deviceConfig.default.isRicottaP()) {
          return _consts.RICOTTA_ERROR_HELP_URL.RICOTTA_P_NO_RIBBON_URL;
        } else if (_deviceConfig.default.isRicottaG()) {
          if (server) {
            if (server.serverCode.toLowerCase() == 'sg') {
              return _consts.RICOTTA_ERROR_HELP_URL.RICOTTA_OVERSEAS_NO_RIBBON_URL_SG;
            } else if (server.serverCode.toLowerCase() == 'us') {
              return _consts.RICOTTA_ERROR_HELP_URL.RICOTTA_OVERSEAS_NO_RIBBON_URL_OR;
            } else if (server.serverCode.toLowerCase() == 'de') {
              return _consts.RICOTTA_ERROR_HELP_URL.RICOTTA_OVERSEAS_NO_RIBBON_URL_EU;
            } else if (server.serverCode.toLowerCase() == 'ru') {
              return _consts.RICOTTA_ERROR_HELP_URL.RICOTTA_OVERSEAS_NO_RIBBON_URL_RU;
            } else {
              return _consts.RICOTTA_ERROR_HELP_URL.RICOTTA_OVERSEAS_NO_RIBBON_URL_SG;
            }
          } else {
            return _consts.RICOTTA_ERROR_HELP_URL.RICOTTA_OVERSEAS_NO_RIBBON_URL_SG;
          }
        } else {
          return _consts.RICOTTA_ERROR_HELP_URL.RICOTTA_NO_RIBBON_URL;
        }
      }
    }, {
      key: "getRicottaPaperLengthError",
      value: function getRicottaPaperLengthError() {
        var server = _bluetoothPrintManger.default.ShareInstance().getServer();

        if (_deviceConfig.default.isRicotta()) {
          return _consts.RICOTTA_ERROR_HELP_URL.RICOTTA_PAPER_LENGTH_URL;
        } else if (_deviceConfig.default.isRicottaP()) {
          return _consts.RICOTTA_ERROR_HELP_URL.RICOTTA_P_PAPER_LENGTH_URL;
        } else if (_deviceConfig.default.isRicottaG()) {
          if (server) {
            if (server.serverCode.toLowerCase() == 'sg') {
              return _consts.RICOTTA_ERROR_HELP_URL.RICOTTA_OVERSEAS_PAPER_LENGTH_URL_SG;
            } else if (server.serverCode.toLowerCase() == 'us') {
              return _consts.RICOTTA_ERROR_HELP_URL.RICOTTA_OVERSEAS_PAPER_LENGTH_URL_OR;
            } else if (server.serverCode.toLowerCase() == 'de') {
              return _consts.RICOTTA_ERROR_HELP_URL.RICOTTA_OVERSEAS_PAPER_LENGTH_URL_EU;
            } else if (server.serverCode.toLowerCase() == 'ru') {
              return _consts.RICOTTA_ERROR_HELP_URL.RICOTTA_OVERSEAS_PAPER_LENGTH_URL_RU;
            } else {
              return _consts.RICOTTA_ERROR_HELP_URL.RICOTTA_OVERSEAS_PAPER_LENGTH_URL_SG;
            }
          } else {
            return _consts.RICOTTA_ERROR_HELP_URL.RICOTTA_OVERSEAS_PAPER_LENGTH_URL_SG;
          }
        } else {
          return _consts.RICOTTA_ERROR_HELP_URL.RICOTTA_PAPER_LENGTH_URL;
        }
      }
    }, {
      key: "getRicottaRibbonError",
      value: function getRicottaRibbonError() {
        var server = _bluetoothPrintManger.default.ShareInstance().getServer();

        if (_deviceConfig.default.isRicotta()) {
          return _consts.RICOTTA_ERROR_HELP_URL.RICOTTA_RIBBON_ERROR_URL;
        } else if (_deviceConfig.default.isRicottaP()) {
          return _consts.RICOTTA_ERROR_HELP_URL.RICOTTA_P_RIBBON_ERROR_URL;
        } else if (_deviceConfig.default.isRicottaG()) {
          if (server) {
            if (server.serverCode.toLowerCase() == 'sg') {
              return _consts.RICOTTA_ERROR_HELP_URL.RICOTTA_OVERSEAS_RIBBON_ERROR_URL_SG;
            } else if (server.serverCode.toLowerCase() == 'us') {
              return _consts.RICOTTA_ERROR_HELP_URL.RICOTTA_OVERSEAS_RIBBON_ERROR_URL_OR;
            } else if (server.serverCode.toLowerCase() == 'de') {
              return _consts.RICOTTA_ERROR_HELP_URL.RICOTTA_OVERSEAS_RIBBON_ERROR_URL_EU;
            } else if (server.serverCode.toLowerCase() == 'ru') {
              return _consts.RICOTTA_ERROR_HELP_URL.RICOTTA_OVERSEAS_RIBBON_ERROR_URL_RU;
            } else {
              return _consts.RICOTTA_ERROR_HELP_URL.RICOTTA_OVERSEAS_RIBBON_ERROR_URL_SG;
            }
          } else {
            return _consts.RICOTTA_ERROR_HELP_URL.RICOTTA_OVERSEAS_RIBBON_ERROR_URL_SG;
          }
        } else {
          return _consts.RICOTTA_ERROR_HELP_URL.RICOTTA_RIBBON_ERROR_URL;
        }
      }
    }, {
      key: "getRicottaSecondRibbonError",
      value: function getRicottaSecondRibbonError() {
        var server = _bluetoothPrintManger.default.ShareInstance().getServer();

        if (_deviceConfig.default.isRicotta()) {
          return _consts.RICOTTA_ERROR_HELP_URL.RICOTTA_SECOND_RIBBON_ERROR_URL;
        } else if (_deviceConfig.default.isRicottaP()) {
          return _consts.RICOTTA_ERROR_HELP_URL.RICOTTA_P_SECOND_RIBBON_ERROR_URL;
        } else if (_deviceConfig.default.isRicottaG()) {
          if (server) {
            if (server.serverCode.toLowerCase() == 'sg') {
              return _consts.RICOTTA_ERROR_HELP_URL.RICOTTA_OVERSEAS_SECOND_RIBBON_ERROR_URL_SG;
            } else if (server.serverCode.toLowerCase() == 'us') {
              return _consts.RICOTTA_ERROR_HELP_URL.RICOTTA_OVERSEAS_SECOND_RIBBON_ERROR_URL_OR;
            } else if (server.serverCode.toLowerCase() == 'de') {
              return _consts.RICOTTA_ERROR_HELP_URL.RICOTTA_OVERSEAS_SECOND_RIBBON_ERROR_URL_EU;
            } else if (server.serverCode.toLowerCase() == 'ru') {
              return _consts.RICOTTA_ERROR_HELP_URL.RICOTTA_OVERSEAS_SECOND_RIBBON_ERROR_URL_RU;
            } else {
              return _consts.RICOTTA_ERROR_HELP_URL.RICOTTA_OVERSEAS_SECOND_RIBBON_ERROR_URL_SG;
            }
          } else {
            return _consts.RICOTTA_ERROR_HELP_URL.RICOTTA_OVERSEAS_SECOND_RIBBON_ERROR_URL_SG;
          }
        } else {
          return _consts.RICOTTA_ERROR_HELP_URL.RICOTTA_SECOND_RIBBON_ERROR_URL;
        }
      }
    }, {
      key: "getPaperJamError",
      value: function getPaperJamError() {
        var server = _bluetoothPrintManger.default.ShareInstance().getServer();

        if (_deviceConfig.default.isRicotta()) {
          return _consts.RICOTTA_ERROR_HELP_URL.RICOTTA_PAPER_JAM_URL;
        } else if (_deviceConfig.default.isRicottaP()) {
          return _consts.RICOTTA_ERROR_HELP_URL.RICOTTA_P_PAPER_JAM_URL;
        } else if (_deviceConfig.default.isRicottaG()) {
          if (server) {
            if (server.serverCode.toLowerCase() == 'sg') {
              return _consts.RICOTTA_ERROR_HELP_URL.RICOTTA_OVERSEAS_PAPER_JAM_URL_SG;
            } else if (server.serverCode.toLowerCase() == 'us') {
              return _consts.RICOTTA_ERROR_HELP_URL.RICOTTA_OVERSEAS_PAPER_JAM_URL_OR;
            } else if (server.serverCode.toLowerCase() == 'de') {
              return _consts.RICOTTA_ERROR_HELP_URL.RICOTTA_OVERSEAS_PAPER_JAM_URL_EU;
            } else if (server.serverCode.toLowerCase() == 'ru') {
              return _consts.RICOTTA_ERROR_HELP_URL.RICOTTA_OVERSEAS_PAPER_JAM_URL_RU;
            } else {
              return _consts.RICOTTA_ERROR_HELP_URL.RICOTTA_OVERSEAS_PAPER_JAM_URL_SG;
            }
          } else {
            return _consts.RICOTTA_ERROR_HELP_URL.RICOTTA_OVERSEAS_PAPER_JAM_URL_SG;
          }
        } else {
          return _consts.RICOTTA_ERROR_HELP_URL.RICOTTA_PAPER_JAM_URL;
        }
      }
    }, {
      key: "handleDeviceState",
      value: function handleDeviceState(result) {
        if (this.isPhotoPrinter()) {
          this.mintHandleDeviceState(result);
          return;
        }

        if (result.category.code < _consts.PRINTER_STATE.ERROR && result.category.code > 0) {
          _ErrorHeadFile.LogUtil.reportLog('error help: 错误已回复');

          if (this.state.showDialog) {
            this.dismissTips();

            _ErrorHeadFile.Toast.show(_ErrorHeadFile.Language.getString('toast_process_success'), {
              duration: _ErrorHeadFile.Toast.durations.SHORT,
              position: _ErrorHeadFile.Toast.positions.BOTTOM,
              shadow: true,
              animation: true,
              hideOnPress: true,
              delay: 0
            });
          }

          global.errorViewVisible = true;
          this.props.navigation.goBack(this.props.navigation.state.params.screen_key);
        } else {
          _ErrorHeadFile.LogUtil.reportLog("error help: " + JSON.stringify(result));

          if (result.category.code > 0) {
            this.setContent(result.errors[0].code);
            this.setState({
              error: result.errors[0],
              title: result.errors[0].status,
              subtitle: result.errors[0].code == _consts.PRINTER_STATE_ALERTS.ERR_PFS_PAPER_MISMATCH ? result.errors[0].subStatus.replace('%s', paperSize) : result.errors[0].subStatus,
              helpDescription: result.errors[0].helpDesription,
              description: result.errors[0].description,
              isShowhandleButton: result.errors[0].isResume
            });
          } else {
            this.setState({
              error: {},
              title: _ErrorHeadFile.Language.getString('get_status_title'),
              subtitle: _ErrorHeadFile.Language.getString('get_status_title'),
              description: _ErrorHeadFile.Language.getString('get_status_title'),
              helpDescription: _ErrorHeadFile.Language.getString('get_status_title'),
              isShowhandleButton: false
            });
          }
        }
      }
    }, {
      key: "handleJobState",
      value: function handleJobState(result) {
        if (result == null) {
          return;
        }

        paperSize = _ErrorHeadFile.Language.getString('set_region_a4_sub');

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
    }, {
      key: "progressButtonAction",
      value: function progressButtonAction() {
        var _this3 = this;

        _ErrorHeadFile.LogUtil.reportEvent(_logKey.LOG_KEY.ROSEMARY_TAP_EVENT_ERROR_HELP_PROGRESS, _logKey.LOG_KEY.ROSEMARY_TAP_EVENT_ERROR_HELP_PROGRESS);

        _ErrorHeadFile.LogUtil.reportLog('error-help: 点击了已处理');

        this.showLoadingTips(_ErrorHeadFile.Language.getString('toast_processing'));
        this.setState({
          isPaused: true
        });

        if (this.isPhotoPrinter()) {
          _HTClassicBluetooth.default.getInstance().resumePrinter().then(function () {
            _ErrorHeadFile.LogUtil.reportLog('error-help: 处理成功');

            _this3.dismissTips();

            if (_this3.props.navigation.state.params.callback != null) {
              _this3.props.navigation.state.params.callback(true);
            }

            _this3.props.navigation.goBack(_this3.props.navigation.state.params.screen_key);
          }).catch(function (err) {
            _ErrorHeadFile.LogUtil.reportLog("error-help: \u5904\u7406\u5931\u8D25" + JSON.stringify(err));

            _this3.dismissTips();

            _this3.setState({
              isPaused: false
            });

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
          _ErrorHeadFile.Spec.resumePrinter().then(function (res) {
            _ErrorHeadFile.LogUtil.reportLog('error-help: 处理成功');
          }).catch(function (err) {
            _ErrorHeadFile.LogUtil.reportLog('error-help: 处理失败');

            _this3.dismissTips();

            _this3.setState({
              isPaused: false
            });

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
        _ErrorHeadFile.LogUtil.reportEvent(_logKey.LOG_KEY.ROSEMARY_TAP_EVENT_ERROR_HELP_RELEASE, _logKey.LOG_KEY.ROSEMARY_TAP_EVENT_ERROR_HELP_RELEASE);

        _ErrorHeadFile.LogUtil.reportLog('error-help: 点击了暂不处理');

        if (this.isPhotoPrinter() && this.state.errorCode == _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_NO_SMARTSHEET) {
          this.progressButtonAction();
        } else {
          if (this.isEcnError(this.state.errorCode)) {
            this.cancelJob();
          } else if (!this.state.isShowhandleButton) {
            _ErrorHeadFile.Spec.resumePrinter();
          }

          this.player.setPaused();
          global.errorViewVisible = false;

          if (this.props.navigation.state.params.callback != null) {
            this.props.navigation.state.params.callback();
          }

          this.props.navigation.goBack(this.props.navigation.state.params.screen_key);
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
      key: "cancelJob",
      value: function cancelJob() {
        _ErrorHeadFile.Spec.cancelPrintJob().then(function () {
          _ErrorHeadFile.LogUtil.reportLog('error: 取消打印成功');
        }).catch(function (err) {
          _ErrorHeadFile.LogUtil.reportLog("error: \u53D6\u6D88\u6253\u5370\u5931\u8D25" + JSON.stringify(err));
        });
      }
    }, {
      key: "getReleaseButtonName",
      value: function getReleaseButtonName() {
        var result = '';

        if (this.isPhotoPrinter() && this.state.errorCode == _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_NO_SMARTSHEET) {
          result = this.props.navigation.state.params.error.releaseButtonName;
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
      key: "isRicottaType",
      value: function isRicottaType() {
        return _deviceConfig.default.isRicottaType();
      }
    }, {
      key: "renderTitleBar",
      value: function renderTitleBar() {
        var _this4 = this;

        return _react.default.createElement(_ComponentsHeadFile.HTNavigationBar, {
          title: this.isPhotoPrinter() ? this.state.title : _ThirdPartyHeadFile.language.getString("error_help"),
          hiddenLine: true,
          left: [{
            icon: _UtilsHeadFile.HTImage.nav_back,
            onPress: function onPress() {
              return _this4.props.navigation.goBack();
            }
          }]
        });
      }
    }, {
      key: "render",
      value: function render() {
        var _this5 = this;

        return _react.default.createElement(_reactNative.View, {
          style: {
            flex: 1
          }
        }, this.renderTitleBar(), _react.default.createElement(_errorHelpView.default, {
          title: this.isPhotoPrinter() ? "" : this.state.title,
          subtitle: this.state.helpDescription,
          ref: function ref(_ref) {
            return _this5.player = _ref;
          },
          videoPath: this.state.videoPath,
          progressButtonAction: this.progressButtonAction.bind(this),
          releaseButtonAction: this.releaseButtonAction.bind(this),
          isShowhandleButton: this.state.isShowhandleButton,
          isShowdontHandleButton: true,
          releaseName: this.getReleaseButtonName(),
          showDialog: this.state.showDialog,
          dialogTitle: this.state.dialogTitle,
          dialogDismiss: this.dismissTips.bind(this)
        }));
      }
    }]);
    return ErrorHelp;
  }(_react.Component);

  exports.default = ErrorHelp;

  ErrorHelp.navigationOptions = function (_ref2) {
    var navigation = _ref2.navigation;
    return {
      header: null,
      gesturesEnabled: false
    };
  };
},13154,[14317,14314,14329,14383,14389,14332,14398,10297,10033,12173,13157,13139,12494,12407,10074,12179,12416,10055,10034,12311,12194]);