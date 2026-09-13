__d(function (global, _$$_REQUIRE, _$$_IMPORT_DEFAULT, _$$_IMPORT_ALL, module, exports, _dependencyMap) {
  var _interopRequireWildcard = _$$_REQUIRE(_dependencyMap[0]);

  var _interopRequireDefault = _$$_REQUIRE(_dependencyMap[1]);

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  exports.default = undefined;

  var _objectSpread2 = _interopRequireDefault(_$$_REQUIRE(_dependencyMap[2]));

  var _classCallCheck2 = _interopRequireDefault(_$$_REQUIRE(_dependencyMap[3]));

  var _createClass2 = _interopRequireDefault(_$$_REQUIRE(_dependencyMap[4]));

  var _possibleConstructorReturn2 = _interopRequireDefault(_$$_REQUIRE(_dependencyMap[5]));

  var _getPrototypeOf2 = _interopRequireDefault(_$$_REQUIRE(_dependencyMap[6]));

  var _inherits2 = _interopRequireDefault(_$$_REQUIRE(_dependencyMap[7]));

  var _propTypes = _interopRequireDefault(_$$_REQUIRE(_dependencyMap[8]));

  var _react = _interopRequireWildcard(_$$_REQUIRE(_dependencyMap[9]));

  var _reactNative = _$$_REQUIRE(_dependencyMap[10]);

  var _ThirdPartyHeadFile = _$$_REQUIRE(_dependencyMap[11]);

  var _UtilsHeadFile = _$$_REQUIRE(_dependencyMap[12]);

  var _screenAdapter = _$$_REQUIRE(_dependencyMap[13]);

  var _consts = _$$_REQUIRE(_dependencyMap[14]);

  var _resources = _interopRequireDefault(_$$_REQUIRE(_dependencyMap[15]));

  var WIDTH = _reactNative.Dimensions.get("window").width;

  var titleHeight = (0, _screenAdapter.scale)(53);
  var iconSize = (0, _screenAdapter.scale)(40);

  var HTNavigationBar = function (_Component) {
    (0, _inherits2.default)(HTNavigationBar, _Component);

    function HTNavigationBar() {
      (0, _classCallCheck2.default)(this, HTNavigationBar);
      return (0, _possibleConstructorReturn2.default)(this, (0, _getPrototypeOf2.default)(HTNavigationBar).apply(this, arguments));
    }

    (0, _createClass2.default)(HTNavigationBar, [{
      key: "render",
      value: function render() {
        var _this$props = this.props,
            containComponent = _this$props.containComponent,
            left = _this$props.left,
            right = _this$props.right,
            style = _this$props.style,
            backgroundColor = _this$props.backgroundColor;

        _reactNative.StatusBar.setBarStyle(_UtilsHeadFile.HTColor.isDarkMode ? 'light-content' : 'dark-content');

        if (_reactNative.Platform.OS == 'android') {
          _reactNative.StatusBar.setTranslucent(true);

          _reactNative.StatusBar.setBackgroundColor('transparent');
        }

        var paddingTop = _reactNative.Platform.select({
          ios: (0, _ThirdPartyHeadFile.ifIphoneX)(44, 20),
          android: _reactNative.StatusBar.currentHeight,
          default: 0
        });

        if (containComponent) {
          return _react.default.createElement(_reactNative.View, {
            style: {
              paddingTop: paddingTop,
              backgroundColor: backgroundColor ? backgroundColor : _UtilsHeadFile.HTColor.c_bg_general
            }
          }, _react.default.createElement(_reactNative.View, {
            style: [styles.container, {
              paddingHorizontal: 0
            }, style]
          }, containComponent));
        }

        left.length < right.length && left.push({});
        left.length > right.length && right.unshift({});
        var containerStyle = [styles.container];
        if (!this.props.hiddenLine) containerStyle.push([styles.hiddenLine, {
          borderColor: _UtilsHeadFile.HTColor.c_divider
        }]);
        if (this.props.style) containerStyle.push(this.props.style);
        return _react.default.createElement(_reactNative.View, {
          style: {
            paddingTop: paddingTop,
            backgroundColor: backgroundColor ? backgroundColor : _UtilsHeadFile.HTColor.c_bg_general
          }
        }, _react.default.createElement(_reactNative.View, {
          style: containerStyle
        }, this.renderIcons(left, 'left'), this.renderTitle(), this.renderIcons(right, 'right')));
      }
    }, {
      key: "renderIcons",
      value: function renderIcons(arr, key) {
        var icons = (arr || []).slice(0, 2);
        return icons.map(function (item, i) {
          if (!item.icon) {
            return _react.default.createElement(_reactNative.View, {
              key: key + i,
              style: {
                width: iconSize
              }
            });
          }

          return _react.default.createElement(_reactNative.View, {
            key: key + i,
            ref: item.ref,
            collapsable: false,
            style: (0, _objectSpread2.default)({
              width: iconSize,
              height: iconSize
            }, item.style)
          }, _react.default.createElement(_reactNative.TouchableWithoutFeedback, {
            disabled: item.disable,
            onPress: item.onPress
          }, _react.default.createElement(_reactNative.Image, {
            style: [styles.icon, item.iconStyle],
            resizeMode: "contain",
            source: item.disable ? item.icon.disable : item.icon.normal
          })));
        });
      }
    }, {
      key: "renderTitle",
      value: function renderTitle() {
        var _this$props2 = this.props,
            mintBattery = _this$props2.mintBattery,
            title = _this$props2.title,
            onPressTitle = _this$props2.onPressTitle;
        return _react.default.createElement(_reactNative.View, {
          style: [styles.titleContainer]
        }, _react.default.isValidElement(title) ? _react.default.createElement(_reactNative.View, {
          numberOfLines: 1,
          style: [styles.titleView, {
            color: _UtilsHeadFile.HTColor.c_font_000000
          }, this.props.titleStyle],
          onPress: onPressTitle
        }, title || '') : !this.isBatteryLegal(mintBattery) ? _react.default.createElement(_reactNative.Text, {
          numberOfLines: 1,
          style: [styles.title, {
            color: _UtilsHeadFile.HTColor.c_font_000000
          }, this.props.titleStyle],
          onPress: onPressTitle
        }, title || '') : _react.default.createElement(_reactNative.View, {
          style: styles.titleContainer1
        }, _react.default.createElement(_reactNative.Text, {
          numberOfLines: 1,
          style: [styles.title, {
            color: _UtilsHeadFile.HTColor.c_font_000000
          }, this.props.titleStyle],
          onPress: onPressTitle
        }, title || ''), _react.default.createElement(_reactNative.View, {
          style: styles.statusContainer
        }, mintBattery != _consts.MINT_BATTERY.DEVICE_CONNECTING && mintBattery != _consts.MINT_BATTERY.DEVICE_NOT_CONNECTED && _react.default.createElement(_reactNative.Image, {
          source: this.getBatteryIcon(mintBattery),
          style: styles.batteryIcon
        }), _react.default.createElement(_reactNative.Text, {
          style: [styles.statusText, {
            color: this.getTextColor(mintBattery)
          }]
        }, this.getBatteryText(mintBattery)))));
      }
    }, {
      key: "getTextColor",
      value: function getTextColor(battery) {
        var color;

        switch (battery) {
          case _consts.MINT_BATTERY.POWER_CAP_POWER_OFF:
          case _consts.MINT_BATTERY.POWER_CAP_CRITICAL:
          case _consts.MINT_BATTERY.POWER_CAP_NO_CHARGE_POWER_OFF:
          case _consts.MINT_BATTERY.POWER_CAP_NO_CHARGE_CRITICAL:
            color = '#F43F31';
            break;

          case _consts.MINT_BATTERY.POWER_CAP_CHARGE_CRITICAL:
          case _consts.MINT_BATTERY.POWER_CAP_CHARGE_POWER_OFF:
          case _consts.MINT_BATTERY.POWER_CAP_CHARGE_LOW:
          case _consts.MINT_BATTERY.POWER_CAP_CHARGE_MEDIAN:
          case _consts.MINT_BATTERY.POWER_CAP_CHARGE_HIGH:
          case _consts.MINT_BATTERY.POWER_CAP_CHARGE_FULL:
            color = '#1BB078';
            break;

          case _consts.MINT_BATTERY.POWER_CAP_LOW:
          case _consts.MINT_BATTERY.POWER_CAP_MEDIAN:
          case _consts.MINT_BATTERY.POWER_CAP_HIGH:
          case _consts.MINT_BATTERY.POWER_CAP_FULL:
          case _consts.MINT_BATTERY.DEVICE_CONNECTING:
          case _consts.MINT_BATTERY.DEVICE_NOT_CONNECTED:
          case _consts.MINT_BATTERY.POWER_CAP_NO_CHARGE_LOW:
          case _consts.MINT_BATTERY.POWER_CAP_NO_CHARGE_MEDIAN:
          case _consts.MINT_BATTERY.POWER_CAP_NO_CHARGE_HIGH:
          case _consts.MINT_BATTERY.POWER_CAP_NO_CHARGE_FULL:
          case _consts.MINT_BATTERY.POWER_CAP_NO_CHARGE_FULL_TEMP_ERROR:
            color = _UtilsHeadFile.HTColor.c_font_000000;
            break;
        }

        return color;
      }
    }, {
      key: "getBatteryText",
      value: function getBatteryText(battery) {
        var text;

        switch (battery) {
          case _consts.MINT_BATTERY.POWER_CAP_POWER_OFF:
            text = _resources.default.getString("battery_off");
            break;

          case _consts.MINT_BATTERY.POWER_CAP_CRITICAL:
            text = _resources.default.getString("battery_2low_sub");
            break;

          case _consts.MINT_BATTERY.POWER_CAP_LOW:
            text = _resources.default.getString("charge_tips");
            break;

          case _consts.MINT_BATTERY.POWER_CAP_MEDIAN:
            text = _resources.default.getString("battery_middle");
            break;

          case _consts.MINT_BATTERY.POWER_CAP_HIGH:
            text = _resources.default.getString("battery_high");
            break;

          case _consts.MINT_BATTERY.POWER_CAP_FULL:
            text = _resources.default.getString("battery_full_sub");
            break;

          case _consts.MINT_BATTERY.POWER_CAP_CHARGE_POWER_OFF:
            text = _resources.default.getString("battery_2low_sub");
            break;

          case _consts.MINT_BATTERY.POWER_CAP_CHARGE_CRITICAL:
            text = _resources.default.getString("battery_2low_sub");
            break;

          case _consts.MINT_BATTERY.POWER_CAP_CHARGE_LOW:
            text = _resources.default.getString("battery_low");
            break;

          case _consts.MINT_BATTERY.POWER_CAP_CHARGE_MEDIAN:
            text = _resources.default.getString("battery_middle");
            break;

          case _consts.MINT_BATTERY.POWER_CAP_CHARGE_HIGH:
            text = _resources.default.getString("battery_high");
            break;

          case _consts.MINT_BATTERY.POWER_CAP_CHARGE_FULL:
            text = _resources.default.getString("battery_full_sub");
            break;

          case _consts.MINT_BATTERY.POWER_CAP_NO_CHARGE_POWER_OFF:
            text = _resources.default.getString("battery_off");
            break;

          case _consts.MINT_BATTERY.POWER_CAP_NO_CHARGE_CRITICAL:
            text = _resources.default.getString("battery_2low_sub");
            break;

          case _consts.MINT_BATTERY.POWER_CAP_NO_CHARGE_LOW:
            text = _resources.default.getString("battery_low");
            break;

          case _consts.MINT_BATTERY.POWER_CAP_NO_CHARGE_MEDIAN:
            text = _resources.default.getString("battery_middle");
            break;

          case _consts.MINT_BATTERY.POWER_CAP_NO_CHARGE_HIGH:
            text = _resources.default.getString("battery_high");
            break;

          case _consts.MINT_BATTERY.POWER_CAP_NO_CHARGE_FULL:
            text = _resources.default.getString("battery_full");
            break;

          case _consts.MINT_BATTERY.POWER_CAP_NO_CHARGE_FULL_TEMP_ERROR:
            text = _resources.default.getString("battery_full_sub");
            break;

          case _consts.MINT_BATTERY.DEVICE_CONNECTING:
            text = _resources.default.getString("device_connecting_text");
            break;

          case _consts.MINT_BATTERY.DEVICE_NOT_CONNECTED:
            text = _resources.default.getString("device_not_connected");
            break;
        }

        return text;
      }
    }, {
      key: "getBatteryIcon",
      value: function getBatteryIcon(battery) {
        var icon;

        switch (battery) {
          case _consts.MINT_BATTERY.POWER_CAP_POWER_OFF:
            icon = _UtilsHeadFile.HTImage.batteryOff;
            break;

          case _consts.MINT_BATTERY.POWER_CAP_CRITICAL:
            icon = _UtilsHeadFile.HTImage.batteryCritical;
            break;

          case _consts.MINT_BATTERY.POWER_CAP_LOW:
            icon = _UtilsHeadFile.HTImage.batteryLow;
            break;

          case _consts.MINT_BATTERY.POWER_CAP_MEDIAN:
            icon = _UtilsHeadFile.HTImage.batteryMiddle;
            break;

          case _consts.MINT_BATTERY.POWER_CAP_HIGH:
            icon = _UtilsHeadFile.HTImage.batteryHigh;
            break;

          case _consts.MINT_BATTERY.POWER_CAP_FULL:
            icon = _UtilsHeadFile.HTImage.batteryFull;
            break;

          case _consts.MINT_BATTERY.POWER_CAP_CHARGE_POWER_OFF:
            icon = _UtilsHeadFile.HTImage.batteryChargeOff;
            break;

          case _consts.MINT_BATTERY.POWER_CAP_CHARGE_CRITICAL:
            icon = _UtilsHeadFile.HTImage.batteryChargeCritical;
            break;

          case _consts.MINT_BATTERY.POWER_CAP_CHARGE_LOW:
            icon = _UtilsHeadFile.HTImage.batteryChargeLow;
            break;

          case _consts.MINT_BATTERY.POWER_CAP_CHARGE_MEDIAN:
            icon = _UtilsHeadFile.HTImage.batteryChargeMiddle;
            break;

          case _consts.MINT_BATTERY.POWER_CAP_CHARGE_HIGH:
            icon = _UtilsHeadFile.HTImage.batteryChargeHigh;
            break;

          case _consts.MINT_BATTERY.POWER_CAP_CHARGE_FULL:
            icon = _UtilsHeadFile.HTImage.batteryChargeFull;
            break;

          case _consts.MINT_BATTERY.POWER_CAP_NO_CHARGE_POWER_OFF:
            icon = _UtilsHeadFile.HTImage.batteryNoChargeOff;
            break;

          case _consts.MINT_BATTERY.POWER_CAP_NO_CHARGE_CRITICAL:
            icon = _UtilsHeadFile.HTImage.batteryNoChargeCritical;
            break;

          case _consts.MINT_BATTERY.POWER_CAP_NO_CHARGE_LOW:
            icon = _UtilsHeadFile.HTImage.batteryNoChargeLow;
            break;

          case _consts.MINT_BATTERY.POWER_CAP_NO_CHARGE_MEDIAN:
            icon = _UtilsHeadFile.HTImage.batteryNoChargeMiddle;
            break;

          case _consts.MINT_BATTERY.POWER_CAP_NO_CHARGE_HIGH:
            icon = _UtilsHeadFile.HTImage.batteryNoChargeHigh;
            break;

          case _consts.MINT_BATTERY.POWER_CAP_NO_CHARGE_FULL:
          case _consts.MINT_BATTERY.POWER_CAP_NO_CHARGE_FULL_TEMP_ERROR:
            icon = _UtilsHeadFile.HTImage.batteryNoChargeFull;
            break;

          default:
            icon = null;
            break;
        }

        return icon;
      }
    }, {
      key: "isBatteryLegal",
      value: function isBatteryLegal(battery) {
        if (battery === _consts.MINT_BATTERY.POWER_CAP_CRITICAL || battery === _consts.MINT_BATTERY.POWER_CAP_LOW || battery === _consts.MINT_BATTERY.POWER_CAP_MEDIAN || battery === _consts.MINT_BATTERY.POWER_CAP_HIGH || battery === _consts.MINT_BATTERY.POWER_CAP_FULL || battery === _consts.MINT_BATTERY.POWER_CAP_POWER_OFF || battery === _consts.MINT_BATTERY.POWER_CAP_CHARGE_CRITICAL || battery === _consts.MINT_BATTERY.POWER_CAP_CHARGE_LOW || battery === _consts.MINT_BATTERY.POWER_CAP_CHARGE_MEDIAN || battery === _consts.MINT_BATTERY.POWER_CAP_CHARGE_HIGH || battery === _consts.MINT_BATTERY.POWER_CAP_CHARGE_FULL || battery === _consts.MINT_BATTERY.POWER_CAP_CHARGE_POWER_OFF || battery === _consts.MINT_BATTERY.DEVICE_CONNECTING || battery === _consts.MINT_BATTERY.DEVICE_NOT_CONNECTED || battery === _consts.MINT_BATTERY.POWER_CAP_NO_CHARGE_CRITICAL || battery === _consts.MINT_BATTERY.POWER_CAP_NO_CHARGE_LOW || battery === _consts.MINT_BATTERY.POWER_CAP_NO_CHARGE_MEDIAN || battery === _consts.MINT_BATTERY.POWER_CAP_NO_CHARGE_HIGH || battery === _consts.MINT_BATTERY.POWER_CAP_NO_CHARGE_FULL || battery === _consts.MINT_BATTERY.POWER_CAP_NO_CHARGE_POWER_OFF || battery === _consts.MINT_BATTERY.POWER_CAP_NO_CHARGE_FULL_TEMP_ERROR) {
          return true;
        } else {
          return false;
        }
      }
    }]);
    return HTNavigationBar;
  }(_react.Component);

  exports.default = HTNavigationBar;
  HTNavigationBar.propTypes = {
    style: _propTypes.default.object,
    containComponent: _react.default.Component,
    left: _propTypes.default.array,
    right: _propTypes.default.array,
    title: _propTypes.default.string,
    onPressTitle: _propTypes.default.func,
    hiddenLine: _propTypes.default.bool,
    mintBattery: _propTypes.default.number
  };
  HTNavigationBar.defaultProps = {
    containComponent: null,
    left: [],
    right: [],
    hiddenLine: true,
    mintBattery: -1
  };

  var styles = _reactNative.StyleSheet.create({
    container: {
      width: WIDTH,
      height: titleHeight,
      paddingHorizontal: (0, _screenAdapter.scale)(9),
      flexDirection: 'row',
      alignItems: 'center'
    },
    titleContainer: {
      flex: 1,
      alignSelf: 'stretch',
      justifyContent: 'center',
      alignItems: 'stretch',
      marginHorizontal: (0, _screenAdapter.scale)(5)
    },
    icon: {
      position: 'absolute',
      width: iconSize,
      height: iconSize
    },
    title: {
      fontSize: (0, _screenAdapter.scale)(17),
      lineHeight: (0, _screenAdapter.scale)(24),
      fontFamily: _UtilsHeadFile.FONT_FAMILY.MILanPro_Semibold,
      fontWeight: 'bold',
      textAlignVertical: 'center',
      textAlign: 'center'
    },
    titleView: {
      fontSize: (0, _screenAdapter.scale)(16),
      fontFamily: _UtilsHeadFile.FONT_FAMILY.MILanPro_Semibold,
      fontWeight: 'bold',
      flexDirection: 'row',
      alignItems: 'center',
      justifyContent: 'center'
    },
    hiddenLine: {
      borderBottomWidth: (0, _screenAdapter.scale)(0.5)
    },
    statusContainer: {
      flexDirection: 'row',
      alignItems: 'center'
    },
    batteryIcon: {
      width: 20,
      height: 20,
      marginRight: 5,
      resizeMode: 'contain'
    },
    statusText: {
      fontSize: (0, _screenAdapter.scale)(12)
    },
    titleContainer1: {
      flex: 1,
      alignItems: 'center'
    }
  });
},12419,[14317,14314,14323,14329,14332,14383,14389,14398,10351,10297,10033,10034,10055,10037,12173,10077]);