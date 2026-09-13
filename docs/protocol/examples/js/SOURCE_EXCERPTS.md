# Source excerpts from the supplied Pro plugin

All line numbers refer to the extracted, unchanged module text; only terminal NUL bytes were removed. Offsets are zero-based byte offsets in Plugin_1065830/ios/main.bundle, not ZIP offsets. These are client source facts, not physical-device captures.

Archive SHA-256: `b1a4f376eb9611ed39c4ad0602d3f4e1223e63074080299c035e84f835f9f461`
Bundle SHA-256: `95b61a95ec6e63c5330ba36a897cb41c3377a51483f3b5360e92586a3ebc2f8c`

## S01. Model identity and plugin provenance

`Plugin_1065830/ios/project.json`
```json
{
  "models": "hannto.printer.lager|hannto.printer.rosemary|hannto.printer.rmy|hannto.printer.rmyty|hannto.printer.rsmycr|xiaomi.printer.mintg|xiaomi.printer.ricott|xiaomi.printer.ricotg|xiaomi.printer.ricotp",
  "package_path": "com.hannto.printer",
  "min_sdk_api_level": 10106,
  "developer_id": "",
  "version_code": 69,
  "entrance_scene": {
    "action_ids": [],
    "trigger_ids": []
  },
  "sdk_api_level": 10115,
  "version": "1.1.15",
  "platform": "ios",
  "build_time": 1782915105033,
  "bundle_type": "indexed-ram-bundle",
  "bundle_minify": false,
  "signature": 1048576
}
```

### 12179.js L18-L79
Original bundle byte offset: `745049` (`0xb5e59`).
```javascript
   18 | 
   19 |   var modelMap = {
   20 |     'hannto.printer.rmy': _deviceConfig.default,
   21 |     'hannto.printer.rmyty': _deviceConfig.default,
   22 |     'hannto.printer.rsmycr': _deviceConfig.default,
   23 |     'hannto.printer.lager': _deviceConfig2.default,
   24 |     'xiaomi.printer.mintg': _deviceConfig3.default,
   25 |     'xiaomi.printer.ricott': _deviceConfig4.default,
   26 |     'xiaomi.printer.ricotg': _deviceConfig4.default,
   27 |     'xiaomi.printer.ricotp': _deviceConfig4.default
   28 |   };
   29 |   var _default = {
   30 |     getDeviceBluetoothName: function getDeviceBluetoothName() {
   31 |       if (this.isMint()) {
   32 |         return "Xiaomi...Photo Printer 1S-";
   33 |       } else if (this.isRicotta()) {
   34 |         return "米家口袋打印机Pro-";
   35 |       } else if (this.isRicottaG()) {
   36 |         return "Xiaomi Photo Printer Pro-";
   37 |       } else if (this.isRicottaP()) {
   38 |         return "随身拍套装-";
   39 |       } else {
   40 |         return "米家口袋打印机Pro-";
   41 |       }
   42 |     },
   43 |     getDeviceName: function getDeviceName() {
   44 |       if (this.isRicottaG()) {
   45 |         return 'Xiaomi Portable Photo Printer Pro';
   46 |       } else if (this.isRicotta()) {
   47 |         return '米家口袋打印机Pro';
   48 |       } else if (this.isRicottaP()) {
   49 |         return '随身拍套装';
   50 |       } else if (this.isMint()) {
   51 |         return 'Xiaomi Portable Photo Printer 1S';
   52 |       } else {
   53 |         return '';
   54 |       }
   55 |     },
   56 |     isPhotoPrinter: function isPhotoPrinter() {
   57 |       return _miot.Device.model == 'xiaomi.printer.mintg' || _miot.Device.model == 'xiaomi.printer.ricott' || _miot.Device.model == 'xiaomi.printer.ricotg' || _miot.Device.model == 'xiaomi.printer.ricotp';
   58 |     },
   59 |     isOverseaModel: function isOverseaModel() {
   60 |       return _miot.Device.model == 'xiaomi.printer.mintg' || _miot.Device.model == 'xiaomi.printer.ricotg';
   61 |     },
   62 |     isRicottaType: function isRicottaType() {
   63 |       return _miot.Device.model == 'xiaomi.printer.ricott' || _miot.Device.model == 'xiaomi.printer.ricotg' || _miot.Device.model == 'xiaomi.printer.ricotp';
   64 |     },
   65 |     isMint: function isMint() {
   66 |       return _miot.Device.model == 'xiaomi.printer.mintg';
   67 |     },
   68 |     isRicotta: function isRicotta() {
   69 |       return _miot.Device.model == 'xiaomi.printer.ricott';
   70 |     },
   71 |     isRicottaG: function isRicottaG() {
   72 |       return _miot.Device.model == 'xiaomi.printer.ricotg';
   73 |     },
   74 |     isRicottaP: function isRicottaP() {
   75 |       return _miot.Device.model == 'xiaomi.printer.ricotp';
   76 |     },
   77 |     getModelConfig: function getModelConfig() {
   78 |       var model = _miot.Device.model;
   79 |       return modelMap[model];
```

## S02. Business request constructors

### 12407.js L201-L300
Original bundle byte offset: `1487279` (`0x16b1af`).
```javascript
  201 |       key: "getMixStatus",
  202 |       value: function getMixStatus() {
  203 |         var _this2 = this;
  204 | 
  205 |         var msgSn = this.getMsgSn();
  206 |         return new Promise(function (resolve, reject) {
  207 |           var data = {
  208 |             'method': 'mixed_status',
  209 |             'id': msgSn,
  210 |             'params': {}
  211 |           };
  212 | 
  213 |           _this2.handleCommond(data).then(function (res) {
  214 |             resolve(res);
  215 |           }).catch(function (error) {
  216 |             reject(error);
  217 |           });
  218 |         });
  219 |       }
  220 |     }, {
  221 |       key: "getDeviceInfo",
  222 |       value: function getDeviceInfo() {
  223 |         var _this3 = this;
  224 | 
  225 |         var msgSn = this.getMsgSn();
  226 |         return new Promise(function (resolve, reject) {
  227 |           var data = {
  228 |             'method': 'get_prop',
  229 |             'id': msgSn,
  230 |             'params': ["device_info"]
  231 |           };
  232 | 
  233 |           _this3.handleCommond(data).then(function (res) {
  234 |             resolve(res);
  235 |           }).catch(function (error) {
  236 |             reject(error);
  237 |           });
  238 |         });
  239 |       }
  240 |     }, {
  241 |       key: "getBigData",
  242 |       value: function getBigData() {
  243 |         var _this4 = this;
  244 | 
  245 |         var msgSn = this.getMsgSn();
  246 |         return new Promise(function (resolve, reject) {
  247 |           var data = {
  248 |             'method': 'get_prop',
  249 |             'id': msgSn,
  250 |             'params': ["big_data"]
  251 |           };
  252 | 
  253 |           _this4.handleCommond(data).then(function (res) {
  254 |             resolve(res);
  255 |           }).catch(function (error) {
  256 |             reject(error);
  257 |           });
  258 |         });
  259 |       }
  260 |     }, {
  261 |       key: "cancelJob",
  262 |       value: function cancelJob(jobId) {
  263 |         var _this5 = this;
  264 | 
  265 |         var msgSn = this.getMsgSn();
  266 |         return new Promise(function (resolve, reject) {
  267 |           var data = {
  268 |             'method': 'cancel_job',
  269 |             'id': msgSn,
  270 |             'params': [jobId]
  271 |           };
  272 | 
  273 |           _this5.handleCommond(data).then(function (res) {
  274 |             _this5.isCanceled = true;
  275 |             resolve(res);
  276 |           }).catch(function (error) {
  277 |             reject(error);
  278 |           });
  279 |         });
  280 |       }
  281 |     }, {
  282 |       key: "resumePrinter",
  283 |       value: function resumePrinter() {
  284 |         var _this6 = this;
  285 | 
  286 |         var msgSn = this.getMsgSn();
  287 |         return new Promise(function (resolve, reject) {
  288 |           var data = {
  289 |             'method': 'resume_printer',
  290 |             'id': msgSn,
  291 |             'params': {}
  292 |           };
  293 | 
  294 |           _this6.handleCommond(data).then(function (res) {
  295 |             resolve(res);
  296 |           }).catch(function (error) {
  297 |             reject(error);
  298 |           });
  299 |         });
  300 |       }
```

### 12407.js L686-L725
Original bundle byte offset: `1502770` (`0x16ee32`).
```javascript
  686 |       key: "getJobInfo",
  687 |       value: function getJobInfo(jobId) {
  688 |         var _this10 = this;
  689 | 
  690 |         var msgSn = this.getMsgSn();
  691 |         return new Promise(function (resolve, reject) {
  692 |           var data = {
  693 |             'method': 'job_info',
  694 |             'id': msgSn,
  695 |             'params': [jobId]
  696 |           };
  697 | 
  698 |           _this10.handleCommond(data).then(function (res) {
  699 |             resolve(res);
  700 |           }).catch(function (error) {
  701 |             reject(error);
  702 |           });
  703 |         });
  704 |       }
  705 |     }, {
  706 |       key: "confirmJob",
  707 |       value: function confirmJob(jobId) {
  708 |         var _this11 = this;
  709 | 
  710 |         var msgSn = this.getMsgSn();
  711 |         return new Promise(function (resolve, reject) {
  712 |           var data = {
  713 |             'method': 'confirm_job',
  714 |             'id': msgSn,
  715 |             'params': [jobId]
  716 |           };
  717 | 
  718 |           _this11.handleCommond(data).then(function (res) {
  719 |             resolve(res);
  720 |           }).catch(function (e) {
  721 |             reject(e);
  722 |           });
  723 |         });
  724 |       }
  725 |     }, {
```

## S03. Polling interval and response envelopes

### 12311.js L104-L115
Original bundle byte offset: `953830` (`0xe8de6`).
```javascript
  104 |       this.initPolingStatus = function () {
  105 |         _this.reportLog('BluetoothPrintManager --> initPolingStatus');
  106 | 
  107 |         _this.timer && clearTimeout(_this.timer);
  108 |         _this.timer = setInterval(function () {
  109 |           _this.log("BluetoothPrintManager --> initPolingStatus activePrinterStaus = " + _this.activePrinterStaus + " isSendFile = " + _HTClassicBluetooth.default.getInstance().getIsSendingFile());
  110 | 
  111 |           if (_this.activePrinterStaus == true) {
  112 |             _this.queryDeviceState();
  113 |           }
  114 |         }, 3000);
  115 |       };
```

### 12311.js L581-L619
Original bundle byte offset: `973539` (`0xedae3`).
```javascript
  581 |     }, {
  582 |       key: "queryDeviceState",
  583 |       value: function queryDeviceState() {
  584 |         var _this10 = this;
  585 | 
  586 |         this.reportLog("BluetoothPrintManager --> getDeviceStatus HTClassicBluetooth.getInstance().getConnectionState() = " + _HTClassicBluetooth.default.getInstance().getConnectionState() + " ");
  587 | 
  588 |         if (_HTClassicBluetooth.default.getInstance().getConnectionState() === _consts.MINT_BLUETOOTH_STATE.AUTH) {
  589 |           this.startConnectTime = 0;
  590 | 
  591 |           if (_HTClassicBluetooth.default.getInstance().getIsSendingFile()) {
  592 |             this.reportLog("BluetoothPrintManager --> getDeviceStatus \u5F53\u524D\u6B63\u5728\u4F20\u8F93\u6587\u4EF6 \u4E0D\u67E5\u8BE2\u72B6\u6001");
  593 |             this.displayState(false, _HTClassicBluetooth.default.getInstance().getConnectionState(), _resources.default.getString("get_status_title"));
  594 |           } else {
  595 |             if (this.deviceInfo) {
  596 |               _HTClassicBluetooth.default.getInstance().getMixStatus().then(function (res) {
  597 |                 _this10.reportLog("getMixStatus res " + JSON.stringify(res));
  598 | 
  599 |                 var state = res.result;
  600 |                 state.isSuccess = true;
  601 |                 state.connectionState = _HTClassicBluetooth.default.getInstance().getConnectionState();
  602 | 
  603 |                 _this10.handlePrinterState(state);
  604 |               }).catch(function (error) {
  605 |                 _this10.reportLog("getMixStatus error " + error);
  606 | 
  607 |                 _DynamicUtil.default.trackEvent(_consts.BIGDATA_KEYS.APP_DEVICE_REQ_EVT_MIXSTATUSFAIL, {
  608 |                   error: JSON.stringify(error)
  609 |                 });
  610 | 
  611 |                 _this10.displayState(false, _HTClassicBluetooth.default.getInstance().getConnectionState(), _resources.default.getString("get_status_title"));
  612 |               });
  613 |             } else {
  614 |               this.reportLog("当前仍未获取到设备状态，先获取设备状态");
  615 |               this.getDeviceInfo();
  616 |             }
  617 |           }
  618 |         } else if (_HTClassicBluetooth.default.getInstance().getConnectionState() === _consts.MINT_BLUETOOTH_STATE.CONNECTING) {
  619 |           this.displayState(false, _HTClassicBluetooth.default.getInstance().getConnectionState(), _resources.default.getString('toast_bt_connecting'));
```

### 12311.js L709-L742
Original bundle byte offset: `980916` (`0xef7b4`).
```javascript
  709 |       key: "getDeviceInfo",
  710 |       value: function getDeviceInfo() {
  711 |         var _this12 = this;
  712 | 
  713 |         this.reportLog("BluetoothPrintManager --> getDeviceInfo");
  714 | 
  715 |         if (_HTClassicBluetooth.default.getInstance().getConnectionState() === _consts.MINT_BLUETOOTH_STATE.AUTH) {
  716 |           _HTClassicBluetooth.default.getInstance().getDeviceInfo().then(function (result) {
  717 |             _this12.reportLog("getDeviceInfo result " + JSON.stringify(result));
  718 | 
  719 |             if (result.method == "event.rpt_err") {
  720 |               _this12.reportLog("BluetoothPrintManager --> getMixStatus method = " + result.method);
  721 | 
  722 |               if (result.params && result.params.code == _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_RPT_ERROR) {
  723 |                 _this12.reportLog("BluetoothPrintManager --> getMixStatus code = " + result.params.code);
  724 | 
  725 |                 _this12.displayState(false, _HTClassicBluetooth.default.getInstance().getConnectionState(), _resources.default.getString('bt_disconnect_title'), _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_RPT_ERROR);
  726 |               }
  727 |             } else {
  728 |               _this12.deviceInfo = result.result[0];
  729 | 
  730 |               _this12.reportLog("getDeviceInfo deviceInfo " + JSON.stringify(_this12.deviceInfo));
  731 | 
  732 |               _DynamicUtil.default.trackEvent(_consts.BIGDATA_KEYS.FW_DEVICE_PROP_DEVICEINFO_COMBO, _this12.deviceInfo);
  733 | 
  734 |               _this12.saveMac(_this12.deviceInfo);
  735 | 
  736 |               _this12.checkFWVersion(_this12.deviceInfo);
  737 | 
  738 |               _this12.getBigData();
  739 | 
  740 |               _this12.initPolingStatus();
  741 |             }
  742 |           }).catch(function (error) {
```

## S04. Embedded battery percentage samples, not live captures

### 13166.js L194-L235
Original bundle byte offset: `5137403` (`0x4e63fb`).
```javascript
  194 |           }
  195 |         }), _react.default.createElement(_ListItem.ListItem, {
  196 |           title: "\u8BBE\u5907\u72B6\u6001\u662F\u5426\u4E00\u81F4",
  197 |           subtitle: "\u8BBE\u5907\u72B6\u6001\u662F\u5426\u4E00\u81F4",
  198 |           onPress: function onPress() {
  199 |             var state1 = {
  200 |               "category": "idle",
  201 |               "sub_category": "init",
  202 |               "error": 0,
  203 |               "battery": 4,
  204 |               "battery-level": 94,
  205 |               "battery-temp": 2,
  206 |               "time_left": 568292,
  207 |               "clean_remain": 0,
  208 |               "sensor": 13,
  209 |               "isSuccess": true,
  210 |               "connectionState": 5
  211 |             };
  212 |             var state2 = {
  213 |               "category": "processing",
  214 |               "sub_category": "printing_M",
  215 |               "error": 0,
  216 |               "battery": 20,
  217 |               "battery-level": 96,
  218 |               "battery-temp": 2,
  219 |               "time_left": 547340,
  220 |               "clean_remain": 0,
  221 |               "job_type": 0,
  222 |               "job_id": 37,
  223 |               "prt_copies": 1,
  224 |               "sensor": 12,
  225 |               "isSuccess": true,
  226 |               "connectionState": 5
  227 |             };
  228 | 
  229 |             var result = _JsonUtil.default.areObjectsEqualExcludingTimeLeft(state1, state2);
  230 | 
  231 |             _this3.setState({
  232 |               result: "state1 state2 is same: " + result
  233 |             });
  234 |           }
  235 |         }), _react.default.createElement(_ListItem.ListItem, {
```

### 13166.js L681-L700
Original bundle byte offset: `5157246` (`0x4eb17e`).
```javascript
  681 |           title: "Mint\u9519\u8BEF\u5904\u7406",
  682 |           subtitle: "PRINTER_DEVICE_ERROR_OUT_OF_MEMORY",
  683 |           onPress: function onPress() {
  684 |             var deviceStatus = {
  685 |               "category": _consts.MINT_PRINTER_STATUS_CATEGORY.PRINTER_STATUS_CATEGORY_ERROR,
  686 |               "sub_category": "printing",
  687 |               "error": _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_OUT_OF_MEMORY,
  688 |               "battery": 19,
  689 |               "battery_pct": 100,
  690 |               "clean_remain": 0,
  691 |               "job_type": 0,
  692 |               "job_id": 96,
  693 |               "prt_copies": 1,
  694 |               "isSuccess": true,
  695 |               "connectionState": 2
  696 |             };
  697 | 
  698 |             _errorHandle.default.handleDeviceState(_this3.props.navigation, deviceStatus, true, {
  699 |               showErrorDialog: function showErrorDialog(result) {},
  700 |               hideErrorDialog: function hideErrorDialog() {},
```

## S05. Production battery presentation and sensors

### 12863.js L811-L839
Original bundle byte offset: `3044444` (`0x2e745c`).
```javascript
  811 |               tempPrinterSubStatusText = _resources.default.getString("toast_bt_get_status");
  812 |             }
  813 | 
  814 |             if (_bluetoothPrintManger.default.ShareInstance().isChargingError(state["battery-temp"], state.battery, state.sensor)) {
  815 |               tempAlertMessageVisible = true;
  816 |               tempAlertMessage = _resources.default.getString("charge_pause_title");
  817 |               tempAlertSubMessage = _resources.default.getString("charge_pause_context");
  818 |               tempAlertSubMessageVisible = true;
  819 |               tempAlertButtonVisiable = false;
  820 |               tempAlertMessageEnable = false;
  821 |               tempAlertIcon = _CommonHeadFile.HTImage.homeIconAlert;
  822 |               tempAlertTextColor = _CommonHeadFile.HTColor.c_font_000000;
  823 |             }
  824 |           }
  825 | 
  826 |           var newBattery = _bluetoothPrintManger.default.ShareInstance().getNewBattery(state["battery-temp"], state.battery, state.sensor);
  827 | 
  828 |           _logUtils.default.reportLog("handleDeviceState newBattery = " + newBattery);
  829 | 
  830 |           this.setState({
  831 |             categoryCode: state.category,
  832 |             deviceStatus: state,
  833 |             categoryTitle: title,
  834 |             error: state.error,
  835 |             bluetoothConnectContainerVisible: false,
  836 |             alertMessageEnable: tempAlertMessageEnable,
  837 |             battery: newBattery,
  838 |             iOSBlueTipDialogVisble: false,
  839 |             connectionState: state.connectionState,
```

### 12419.js L139-L179
Original bundle byte offset: `1545456` (`0x1794f0`).
```javascript
  139 |       value: function renderTitle() {
  140 |         var _this$props2 = this.props,
  141 |             mintBattery = _this$props2.mintBattery,
  142 |             title = _this$props2.title,
  143 |             onPressTitle = _this$props2.onPressTitle;
  144 |         return _react.default.createElement(_reactNative.View, {
  145 |           style: [styles.titleContainer]
  146 |         }, _react.default.isValidElement(title) ? _react.default.createElement(_reactNative.View, {
  147 |           numberOfLines: 1,
  148 |           style: [styles.titleView, {
  149 |             color: _UtilsHeadFile.HTColor.c_font_000000
  150 |           }, this.props.titleStyle],
  151 |           onPress: onPressTitle
  152 |         }, title || '') : !this.isBatteryLegal(mintBattery) ? _react.default.createElement(_reactNative.Text, {
  153 |           numberOfLines: 1,
  154 |           style: [styles.title, {
  155 |             color: _UtilsHeadFile.HTColor.c_font_000000
  156 |           }, this.props.titleStyle],
  157 |           onPress: onPressTitle
  158 |         }, title || '') : _react.default.createElement(_reactNative.View, {
  159 |           style: styles.titleContainer1
  160 |         }, _react.default.createElement(_reactNative.Text, {
  161 |           numberOfLines: 1,
  162 |           style: [styles.title, {
  163 |             color: _UtilsHeadFile.HTColor.c_font_000000
  164 |           }, this.props.titleStyle],
  165 |           onPress: onPressTitle
  166 |         }, title || ''), _react.default.createElement(_reactNative.View, {
  167 |           style: styles.statusContainer
  168 |         }, mintBattery != _consts.MINT_BATTERY.DEVICE_CONNECTING && mintBattery != _consts.MINT_BATTERY.DEVICE_NOT_CONNECTED && _react.default.createElement(_reactNative.Image, {
  169 |           source: this.getBatteryIcon(mintBattery),
  170 |           style: styles.batteryIcon
  171 |         }), _react.default.createElement(_reactNative.Text, {
  172 |           style: [styles.statusText, {
  173 |             color: this.getTextColor(mintBattery)
  174 |           }]
  175 |         }, this.getBatteryText(mintBattery)))));
  176 |       }
  177 |     }, {
  178 |       key: "getTextColor",
  179 |       value: function getTextColor(battery) {
```

### 12419.js L221-L307
Original bundle byte offset: `1549050` (`0x17a2fa`).
```javascript
  221 |         switch (battery) {
  222 |           case _consts.MINT_BATTERY.POWER_CAP_POWER_OFF:
  223 |             text = _resources.default.getString("battery_off");
  224 |             break;
  225 | 
  226 |           case _consts.MINT_BATTERY.POWER_CAP_CRITICAL:
  227 |             text = _resources.default.getString("battery_2low_sub");
  228 |             break;
  229 | 
  230 |           case _consts.MINT_BATTERY.POWER_CAP_LOW:
  231 |             text = _resources.default.getString("charge_tips");
  232 |             break;
  233 | 
  234 |           case _consts.MINT_BATTERY.POWER_CAP_MEDIAN:
  235 |             text = _resources.default.getString("battery_middle");
  236 |             break;
  237 | 
  238 |           case _consts.MINT_BATTERY.POWER_CAP_HIGH:
  239 |             text = _resources.default.getString("battery_high");
  240 |             break;
  241 | 
  242 |           case _consts.MINT_BATTERY.POWER_CAP_FULL:
  243 |             text = _resources.default.getString("battery_full_sub");
  244 |             break;
  245 | 
  246 |           case _consts.MINT_BATTERY.POWER_CAP_CHARGE_POWER_OFF:
  247 |             text = _resources.default.getString("battery_2low_sub");
  248 |             break;
  249 | 
  250 |           case _consts.MINT_BATTERY.POWER_CAP_CHARGE_CRITICAL:
  251 |             text = _resources.default.getString("battery_2low_sub");
  252 |             break;
  253 | 
  254 |           case _consts.MINT_BATTERY.POWER_CAP_CHARGE_LOW:
  255 |             text = _resources.default.getString("battery_low");
  256 |             break;
  257 | 
  258 |           case _consts.MINT_BATTERY.POWER_CAP_CHARGE_MEDIAN:
  259 |             text = _resources.default.getString("battery_middle");
  260 |             break;
  261 | 
  262 |           case _consts.MINT_BATTERY.POWER_CAP_CHARGE_HIGH:
  263 |             text = _resources.default.getString("battery_high");
  264 |             break;
  265 | 
  266 |           case _consts.MINT_BATTERY.POWER_CAP_CHARGE_FULL:
  267 |             text = _resources.default.getString("battery_full_sub");
  268 |             break;
  269 | 
  270 |           case _consts.MINT_BATTERY.POWER_CAP_NO_CHARGE_POWER_OFF:
  271 |             text = _resources.default.getString("battery_off");
  272 |             break;
  273 | 
  274 |           case _consts.MINT_BATTERY.POWER_CAP_NO_CHARGE_CRITICAL:
  275 |             text = _resources.default.getString("battery_2low_sub");
  276 |             break;
  277 | 
  278 |           case _consts.MINT_BATTERY.POWER_CAP_NO_CHARGE_LOW:
  279 |             text = _resources.default.getString("battery_low");
  280 |             break;
  281 | 
  282 |           case _consts.MINT_BATTERY.POWER_CAP_NO_CHARGE_MEDIAN:
  283 |             text = _resources.default.getString("battery_middle");
  284 |             break;
  285 | 
  286 |           case _consts.MINT_BATTERY.POWER_CAP_NO_CHARGE_HIGH:
  287 |             text = _resources.default.getString("battery_high");
  288 |             break;
  289 | 
  290 |           case _consts.MINT_BATTERY.POWER_CAP_NO_CHARGE_FULL:
  291 |             text = _resources.default.getString("battery_full");
  292 |             break;
  293 | 
  294 |           case _consts.MINT_BATTERY.POWER_CAP_NO_CHARGE_FULL_TEMP_ERROR:
  295 |             text = _resources.default.getString("battery_full_sub");
  296 |             break;
  297 | 
  298 |           case _consts.MINT_BATTERY.DEVICE_CONNECTING:
  299 |             text = _resources.default.getString("device_connecting_text");
  300 |             break;
  301 | 
  302 |           case _consts.MINT_BATTERY.DEVICE_NOT_CONNECTED:
  303 |             text = _resources.default.getString("device_not_connected");
  304 |             break;
  305 |         }
  306 | 
  307 |         return text;
```

### 12311.js L1612-L1699
Original bundle byte offset: `1017812` (`0xf87d4`).
```javascript
 1612 |       key: "isCharging",
 1613 |       value: function isCharging(battery) {
 1614 |         if (battery == _consts.MINT_BATTERY.POWER_CAP_CHARGE_CRITICAL || battery == _consts.MINT_BATTERY.POWER_CAP_CHARGE_LOW || battery == _consts.MINT_BATTERY.POWER_CAP_CHARGE_MEDIAN || battery == _consts.MINT_BATTERY.POWER_CAP_CHARGE_HIGH || battery == _consts.MINT_BATTERY.POWER_CAP_CHARGE_FULL || battery == _consts.MINT_BATTERY.POWER_CAP_CHARGE_POWER_OFF) {
 1615 |           return true;
 1616 |         } else {
 1617 |           return false;
 1618 |         }
 1619 |       }
 1620 |     }, {
 1621 |       key: "isUSBConnected",
 1622 |       value: function isUSBConnected(sensor) {
 1623 |         return (sensor & 8) != 0;
 1624 |       }
 1625 |     }, {
 1626 |       key: "isPaperTrayColsed",
 1627 |       value: function isPaperTrayColsed(sensor) {
 1628 |         return (sensor & 4) != 0;
 1629 |       }
 1630 |     }, {
 1631 |       key: "isTempError",
 1632 |       value: function isTempError(temp) {
 1633 |         if (temp == _consts.MINT_TEMP.MINT_TEMP_TO_LOW || temp == _consts.MINT_TEMP.MINT_TEMP_LOW || temp == _consts.MINT_TEMP.MINT_TEMP_HIGH || temp == _consts.MINT_TEMP.MINT_TEMP_TO_HIGH) {
 1634 |           return true;
 1635 |         } else {
 1636 |           return false;
 1637 |         }
 1638 |       }
 1639 |     }, {
 1640 |       key: "isChargingError",
 1641 |       value: function isChargingError(temp, battery, sensor) {
 1642 |         this.log("isChargingError: temp = " + temp + " battery = " + battery + " sensor = " + sensor);
 1643 |         var isUSBConnected = this.isUSBConnected(sensor);
 1644 |         var isCharging = this.isCharging(battery);
 1645 |         var isTempError = this.isTempError(temp);
 1646 |         this.log("getNewBattery: isTempError = " + isTempError + " isUSBConnected = " + isUSBConnected + " isCharging = " + isCharging);
 1647 | 
 1648 |         if (isTempError && isUSBConnected && !isCharging) {
 1649 |           return true;
 1650 |         } else {
 1651 |           return false;
 1652 |         }
 1653 |       }
 1654 |     }, {
 1655 |       key: "getNewBattery",
 1656 |       value: function getNewBattery(temp, battery, sensor) {
 1657 |         this.log("getNewBattery: temp = " + temp + " battery = " + battery + " sensor = " + sensor);
 1658 |         var isUSBConnected = this.isUSBConnected(sensor);
 1659 |         var isCharging = this.isCharging(battery);
 1660 |         var isTempError = this.isTempError(temp);
 1661 |         this.log("getNewBattery: isUSBConnected = " + isUSBConnected + " isCharging = " + isCharging);
 1662 |         var newBattery = battery;
 1663 | 
 1664 |         if (isUSBConnected && !isCharging) {
 1665 |           switch (battery) {
 1666 |             case _consts.MINT_BATTERY.POWER_CAP_CRITICAL:
 1667 |               newBattery = _consts.MINT_BATTERY.POWER_CAP_NO_CHARGE_CRITICAL;
 1668 |               break;
 1669 | 
 1670 |             case _consts.MINT_BATTERY.POWER_CAP_LOW:
 1671 |               newBattery = _consts.MINT_BATTERY.POWER_CAP_NO_CHARGE_LOW;
 1672 |               break;
 1673 | 
 1674 |             case _consts.MINT_BATTERY.POWER_CAP_MEDIAN:
 1675 |               newBattery = _consts.MINT_BATTERY.POWER_CAP_NO_CHARGE_MEDIAN;
 1676 |               break;
 1677 | 
 1678 |             case _consts.MINT_BATTERY.POWER_CAP_HIGH:
 1679 |               newBattery = _consts.MINT_BATTERY.POWER_CAP_NO_CHARGE_HIGH;
 1680 |               break;
 1681 | 
 1682 |             case _consts.MINT_BATTERY.POWER_CAP_FULL:
 1683 |               if (isTempError) {
 1684 |                 newBattery = _consts.MINT_BATTERY.POWER_CAP_NO_CHARGE_FULL_TEMP_ERROR;
 1685 |               } else {
 1686 |                 newBattery = _consts.MINT_BATTERY.POWER_CAP_NO_CHARGE_FULL;
 1687 |               }
 1688 | 
 1689 |               break;
 1690 | 
 1691 |             case _consts.MINT_BATTERY.POWER_CAP_POWER_OFF:
 1692 |               newBattery = _consts.MINT_BATTERY.POWER_CAP_NO_CHARGE_POWER_OFF;
 1693 |               break;
 1694 |           }
 1695 |         }
 1696 | 
 1697 |         return newBattery;
 1698 |       }
 1699 |     }, {
```

## S06. Enum definitions and conflicting job code namespaces

### 12173.js L1197-L1246
Original bundle byte offset: `696651` (`0xaa14b`).
```javascript
 1197 |   var MINT_PRINTER_STATUS_CATEGORY = {
 1198 |     PRINTER_STATUS_CATEGORY_IDLE: "idle",
 1199 |     PRINTER_STATUS_CATEGORY_PROCESSING: "processing",
 1200 |     PRINTER_STATUS_CATEGORY_ERROR: "error",
 1201 |     PRINTER_STATUS_CATEGORY_SLEEP: "sleep",
 1202 |     PRINTER_STATUS_CATEGORY_OFF: "off",
 1203 |     PRINTER_STATUS_CATEGORY_UPDATING: "updating",
 1204 |     PRINTER_STATUS_CATEGORY_MAINTENANCE: "maintenance",
 1205 |     PRINTER_STATUS_CATEGORY_UPDATING_FIRMWARE: "updating firmware",
 1206 |     PRINTER_STATUS_CATEGORY_FACTORY_RESET: "factory reset"
 1207 |   };
 1208 |   exports.MINT_PRINTER_STATUS_CATEGORY = MINT_PRINTER_STATUS_CATEGORY;
 1209 |   var MINT_PRINTER_SUB_CATEGORY = {
 1210 |     PRINTER_STATUS_SUB_CATEGORY_INIT: "init",
 1211 |     PRINTER_STATUS_SUB_CATEGORY_SMART_SHEET: "smart_sheet",
 1212 |     PRINTER_STATUS_SUB_CATEGORY_PRE_HEAT: "pre_heat",
 1213 |     PRINTER_STATUS_SUB_CATEGORY_COOL_DOWN: "cool_down",
 1214 |     PRINTER_STATUS_SUB_CATEGORY_LOAD_PAPER: "load_paper",
 1215 |     PRINTER_STATUS_SUB_CATEGORY_PRINTING: "printing",
 1216 |     PRINTER_STATUS_SUB_CATEGORY_CLEANING: "cleaning",
 1217 |     PRINTER_STATUS_SUB_CATEGORY_DECODING: "decoding",
 1218 |     PRINTER_STATUS_SUB_CATEGORY_INSTALLED: "installed",
 1219 |     PRINTER_STATUS_SUB_CATEGORY_INSTALLING: "installing",
 1220 |     PRINTER_STATUS_SUB_CATEGORY_DOWNLOADING: "downloading",
 1221 |     PRINTER_STATUS_SUB_CATEGORY_IDLE: "idle",
 1222 |     PRINTER_STATUS_SUB_CATEGORY_PRINTING_Y: "printing_Y",
 1223 |     PRINTER_STATUS_SUB_CATEGORY_PRINTING_M: "printing_M",
 1224 |     PRINTER_STATUS_SUB_CATEGORY_PRINTING_C: "printing_C",
 1225 |     PRINTER_STATUS_SUB_CATEGORY_PRINTING_OC: "printing_OC",
 1226 |     PRINTER_STATUS_SUB_CATEGORY_HOME_FEED: "home_feed",
 1227 |     PRINTER_STATUS_SUB_CATEGORY_EJECT: "eject",
 1228 |     PRINTER_STATUS_SUB_CATEGORY_DOWNLOADED: "downloaded"
 1229 |   };
 1230 |   exports.MINT_PRINTER_SUB_CATEGORY = MINT_PRINTER_SUB_CATEGORY;
 1231 |   var MINT_JOB_STATUS = {
 1232 |     PRINTER_JOB_STATUS_WAITING: "waiting",
 1233 |     PRINTER_JOB_STATUS_INIT: "init",
 1234 |     PRINTER_JOB_STATUS_INITLIZATION: "initlization",
 1235 |     PRINTER_JOB_STATUS_DOWNLOADING: "downloading",
 1236 |     PRINTER_JOB_STATUS_PRINTING_Y: "printing_Y",
 1237 |     PRINTER_JOB_STATUS_PRINTING_M: "printing_M",
 1238 |     PRINTER_JOB_STATUS_PRINTING_C: "printing_C",
 1239 |     PRINTER_JOB_STATUS_PRINTING_OC: "printing_OC",
 1240 |     PRINTER_JOB_STATUS_HOME_FEED: "home_feed",
 1241 |     PRINTER_JOB_STATUS_COOL_DOWN: "cool_down",
 1242 |     PRINTER_JOB_STATUS_FINISHED: "finished",
 1243 |     PRINTER_JOB_STATUS_CANCELED: "canceled",
 1244 |     PRINTER_JOB_STATUS_ABORTED: "aborted"
 1245 |   };
 1246 |   exports.MINT_JOB_STATUS = MINT_JOB_STATUS;
```

### 12173.js L1261-L1313
Original bundle byte offset: `699693` (`0xaad2d`).
```javascript
 1261 |   var MINT_PRINTER_ERROR_CODE = {
 1262 |     PRINTER_DEVICE_ERROR_UNSUPPORTED_METHOD: -5001,
 1263 |     PRINTER_DEVICE_ERROR_INVALID_PARAMETERS: -5002,
 1264 |     PRINTER_DEVICE_ERROR_DATA_LENGTH_IS_OVERFLOW: -5003,
 1265 |     PRINTER_DEVICE_ERROR_OUT_OF_MEMORY: -5004,
 1266 |     PRINTER_DEVICE_ERROR_ATTRIBUTE_UNSUPPORTED: -6001,
 1267 |     PRINTER_DEVICE_ERROR_SYSTEM_ERROR: -6002,
 1268 |     PRINTER_DEVICE_ERROR_DECODE_ERROR: -6003,
 1269 |     PRINTER_DEVICE_ERROR_COVER_OPEN: -7001,
 1270 |     PRINTER_DEVICE_ERROR_HEAD_OVER_HEAT: -7002,
 1271 |     PRINTER_DEVICE_ERROR_PAPER_EMPTY: -7101,
 1272 |     PRINTER_DEVICE_ERROR_PAPER_MISMATCH: -7102,
 1273 |     PRINTER_DEVICE_ERROR_PAPER_LOAD: -7103,
 1274 |     PRINTER_DEVICE_ERROR_PAPER_JAM: -7104,
 1275 |     PRINTER_DEVICE_ERROR_PAPER_LENGTH_ERROR: -7105,
 1276 |     PRINTER_DEVICE_ERROR_PAPER_EJECT_ERROR: -7106,
 1277 |     PRINTER_DEVICE_ERROR_PAPER_ERROR: -7107,
 1278 |     PRINTER_DEVICE_ERROR_NO_PAPER_TRAY: -7108,
 1279 |     PRINTER_DEVICE_ERROR_NO_SMARTSHEET: -7109,
 1280 |     PRINTER_DEVICE_ERROR_PAPER_REMOVE: -7110,
 1281 |     PRINTER_DEVICE_ERROR_PAPER_REMOVE_B: -7111,
 1282 |     PRINTER_DEVICE_ERROR_PAPER_REMOVE_PRINTING: -7112,
 1283 |     PRINTER_DEVICE_ERROR_PAPER_REMOVE_LOAD: -7114,
 1284 |     PRINTER_DEVICE_ERROR_RIBBON_END: -7201,
 1285 |     PRINTER_DEVICE_ERROR_RIBBON_JAM: -7202,
 1286 |     PRINTER_DEVICE_ERROR_NO_RIBBON: -7203,
 1287 |     PRINTER_DEVICE_ERROR_NO_RIBBON_MARKER: -7204,
 1288 |     PRINTER_DEVICE_ERROR_RIBBON_ERROR: -7205,
 1289 |     PRINTER_DEVICE_ERROR_INVALLID_RIBBON_TYPE: -7206,
 1290 |     PRINTER_DEVICE_ERROR_RIBBON_ERROR_2: -7208,
 1291 |     PRINTER_DEVICE_ERROR_HW_ERROR: -7301,
 1292 |     PRINTER_DEVICE_ERROR_BROKEN_HEAD_DOTS: -7302,
 1293 |     PRINTER_DEVICE_ERROR_LOAD_ROLLER_UNIT_FAIL: -7303,
 1294 |     PRINTER_DEVICE_ERROR_PLATEN_UNIT_FAIL: -7304,
 1295 |     PRINTER_DEVICE_ERROR_PRINTER_OVERHEAT: -7308,
 1296 |     PRINTER_DEVICE_ERROR_PRINTER_OVERCOOL: -7309,
 1297 |     PRINTER_DEVICE_ERROR_PRINTER_BATTERY_CRITICAL: -7310,
 1298 |     PRINTER_DEVICE_ERROR_PRINTER_BATTERY_OFF: -7311,
 1299 |     PRINTER_DEVICE_ERROR_FIND_NO_JOB: -8001,
 1300 |     PRINTER_DEVICE_ERROR_QUEUE_FULL: -8002,
 1301 |     PRINTER_DEVICE_ERROR_QUEUE_EMPTY: -8003,
 1302 |     PRINTER_DEVICE_ERROR_OVER_LIMITED_SIZE: -8004,
 1303 |     PRINTER_DEVICE_ERROR_TRANSFER_ERROR: -8005,
 1304 |     PRINTER_DEVICE_ERROR_LOW_BATTERY_ERROR: -8006,
 1305 |     PRINTER_DEVICE_ERROR_OVERHEAT: -8108,
 1306 |     PRINTER_DEVICE_ERROR_OVERCOOL: -8109,
 1307 |     PRINTER_DEVICE_ERROR_RPT_ERROR: -8201,
 1308 |     PRINTER_DEVICE_BT_DISCONNECTED_ERROR: -9001,
 1309 |     PRINTER_DEVICE_APP_TRANSFER_ERROR: -9002,
 1310 |     PRINTER_DEVICE_OTA_TRANSFER_ERROR: -9003,
 1311 |     PRINTER_DEVICE_OTA_TIMEOUT_ERROR: -9004
 1312 |   };
 1313 |   exports.MINT_PRINTER_ERROR_CODE = MINT_PRINTER_ERROR_CODE;
```

### 12173.js L1352-L1384
Original bundle byte offset: `703323` (`0xabb5b`).
```javascript
 1352 |   var MINT_BATTERY = {
 1353 |     DEVICE_CONNECTING: -3,
 1354 |     DEVICE_NOT_CONNECTED: -2,
 1355 |     POWER_CAP_UNKNOWN: -1,
 1356 |     POWER_CAP_CRITICAL: 0,
 1357 |     POWER_CAP_LOW: 1,
 1358 |     POWER_CAP_MEDIAN: 2,
 1359 |     POWER_CAP_HIGH: 3,
 1360 |     POWER_CAP_FULL: 4,
 1361 |     POWER_CAP_POWER_OFF: 5,
 1362 |     POWER_CAP_CHARGE_CRITICAL: 16,
 1363 |     POWER_CAP_CHARGE_LOW: 17,
 1364 |     POWER_CAP_CHARGE_MEDIAN: 18,
 1365 |     POWER_CAP_CHARGE_HIGH: 19,
 1366 |     POWER_CAP_CHARGE_FULL: 20,
 1367 |     POWER_CAP_CHARGE_POWER_OFF: 21,
 1368 |     POWER_CAP_NO_CHARGE_CRITICAL: 8,
 1369 |     POWER_CAP_NO_CHARGE_LOW: 9,
 1370 |     POWER_CAP_NO_CHARGE_MEDIAN: 10,
 1371 |     POWER_CAP_NO_CHARGE_HIGH: 11,
 1372 |     POWER_CAP_NO_CHARGE_FULL: 12,
 1373 |     POWER_CAP_NO_CHARGE_POWER_OFF: 13,
 1374 |     POWER_CAP_NO_CHARGE_FULL_TEMP_ERROR: 14
 1375 |   };
 1376 |   exports.MINT_BATTERY = MINT_BATTERY;
 1377 |   var MINT_TEMP = {
 1378 |     MINT_TEMP_TO_LOW: 0,
 1379 |     MINT_TEMP_LOW: 1,
 1380 |     MINT_TEMP_NORMAL: 2,
 1381 |     MINT_TEMP_HIGH: 3,
 1382 |     MINT_TEMP_TO_HIGH: 4,
 1383 |     MINT_TEMP_UNKNOWN: 5
 1384 |   };
```

### 12173.js L1425-L1453
Original bundle byte offset: `707896` (`0xacd38`).
```javascript
 1425 |   var RICOTTA_PRINTER_ERROR_CODE = {
 1426 |     PRINTER_DEVICE_ERROR_COVER_OPEN: -7001,
 1427 |     PRINTER_DEVICE_ERROR_PAPER_LOAD: -7103,
 1428 |     PRINTER_DEVICE_ERROR_PAPER_JAM: -7104,
 1429 |     PRINTER_DEVICE_ERROR_PAPER_LENGTH_ERROR: -7105,
 1430 |     PRINTER_DEVICE_ERROR_PAPER_EJECT_ERROR: -7106,
 1431 |     PRINTER_DEVICE_ERROR_PAPER_ERROR: -7107,
 1432 |     PRINTER_DEVICE_ERROR_PAPER_REMOVE: -7110,
 1433 |     PRINTER_DEVICE_ERROR_RIBBON_END: -7201,
 1434 |     PRINTER_DEVICE_ERROR_NO_RIBBON: -7203,
 1435 |     PRINTER_DEVICE_ERROR_NO_RIBBON_MARKER: -7204,
 1436 |     PRINTER_DEVICE_ERROR_RIBBON_ERROR: -7205
 1437 |   };
 1438 |   exports.RICOTTA_PRINTER_ERROR_CODE = RICOTTA_PRINTER_ERROR_CODE;
 1439 |   var RICOTTA_JOB_ERROR_CODE = {
 1440 |     JOB_MGR_INVALID_ARGUMENT: -8001,
 1441 |     JOB_MGR_OVERFLOW_LENGTH: -8002,
 1442 |     JOB_MGR_OUT_OF_MEMORY: -8003,
 1443 |     JOB_MGR_SYSTEM_ERR: -8004,
 1444 |     JOB_MGR_FIND_NO_JOB: -8005,
 1445 |     JOB_MGR_QUEUE_FULL: -8006,
 1446 |     JOB_MGR_QUEUE_EMPTY: -8007,
 1447 |     JOB_MGR_OVER_SIZE_LIMIT: -8008,
 1448 |     JOB_MGR_TRANSFER_ERR: -8009,
 1449 |     JOB_MGR_PROCESS_BUSY: -8011,
 1450 |     JOB_MGR_JSON_PARSE_ERR: -8012,
 1451 |     JOB_MGR_JOB_TIMEOUT_ERR: -8013
 1452 |   };
 1453 |   exports.RICOTTA_JOB_ERROR_CODE = RICOTTA_JOB_ERROR_CODE;
```

## S07. Ricotta error UI policy and Chinese explanations

### 12332.js L248-L447
Original bundle byte offset: `1214609` (`0x128891`).
```javascript
  248 |     getErrorInfo: function getErrorInfo(code, sub_category) {
  249 |       var isCleaning = false;
  250 | 
  251 |       if (sub_category && sub_category == _consts.MINT_PRINTER_SUB_CATEGORY.PRINTER_STATUS_SUB_CATEGORY_CLEANING) {
  252 |         isCleaning = true;
  253 |       }
  254 | 
  255 |       var status = '';
  256 |       var subStatus = '';
  257 |       var helpButtonName = '';
  258 |       var isResume = false;
  259 |       var isHelp = false;
  260 |       var helpTitle = '';
  261 |       var description = '';
  262 |       var helpDesription = '';
  263 |       var releaseButtonName = '';
  264 | 
  265 |       switch (code) {
  266 |         case _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_COVER_OPEN:
  267 |           status = _resources.default.getString('ricotta_error_7001_title');
  268 |           subStatus = _resources.default.getString('ricotta_error_7001_subtitle');
  269 |           description = _resources.default.getString('ricotta_error_7001_content');
  270 |           isResume = false;
  271 |           isHelp = false;
  272 |           break;
  273 | 
  274 |         case _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_PAPER_LOAD:
  275 |           status = _resources.default.getString('ricotta_error_7103_title');
  276 |           subStatus = _resources.default.getString('ricotta_error_7103_subtitle');
  277 |           description = _resources.default.getString('ricotta_error_7103_content');
  278 |           isResume = true;
  279 |           isHelp = true;
  280 |           helpButtonName = _resources.default.getString('button_video');
  281 |           helpTitle = _resources.default.getString('ricotta_error_7103_title');
  282 |           helpDesription = _resources.default.getString('ricotta_error_7103_content');
  283 |           break;
  284 | 
  285 |         case _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_PAPER_JAM:
  286 |           status = _resources.default.getString('ricotta_error_7104_title');
  287 |           subStatus = _resources.default.getString('ricotta_error_7104_subtitle');
  288 |           description = _resources.default.getString('ricotta_error_7104_content');
  289 |           isResume = false;
  290 |           isHelp = true;
  291 |           helpButtonName = _resources.default.getString('button_video');
  292 |           helpTitle = _resources.default.getString('ricotta_error_7104_title');
  293 |           helpDesription = _resources.default.getString('ricotta_error_7104_content');
  294 |           break;
  295 | 
  296 |         case _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_PAPER_LENGTH_ERROR:
  297 |           status = _resources.default.getString('ricotta_error_7105_title');
  298 |           subStatus = _resources.default.getString('ricotta_error_7105_subtitle');
  299 |           description = _resources.default.getString('ricotta_error_7105_content');
  300 |           isResume = false;
  301 |           isHelp = true;
  302 |           helpButtonName = _resources.default.getString('button_video');
  303 |           helpTitle = _resources.default.getString('ricotta_error_7105_title');
  304 |           helpDesription = _resources.default.getString('ricotta_error_7105_content');
  305 |           break;
  306 | 
  307 |         case _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_PAPER_REMOVE:
  308 |           status = _resources.default.getString('ricotta_error_7110_title');
  309 |           subStatus = _resources.default.getString('ricotta_error_7110_title');
  310 |           description = _resources.default.getString('ricotta_error_7110_content');
  311 |           isResume = true;
  312 |           isHelp = false;
  313 |           break;
  314 | 
  315 |         case _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_PAPER_REMOVE_B:
  316 |           status = _resources.default.getString('ricotta_error_7111_title');
  317 |           subStatus = _resources.default.getString('ricotta_error_7111_subtitle');
  318 |           description = _resources.default.getString('ricotta_error_7111_content');
  319 |           isResume = true;
  320 |           isHelp = true;
  321 |           helpButtonName = _resources.default.getString('button_video');
  322 |           helpTitle = _resources.default.getString('ricotta_error_7111_title');
  323 |           helpDesription = _resources.default.getString('ricotta_error_7111_content');
  324 |           break;
  325 | 
  326 |         case _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_PAPER_REMOVE_PRINTING:
  327 |           status = _resources.default.getString('ricotta_error_7112_title');
  328 |           subStatus = _resources.default.getString('ricotta_error_7112_subtitle');
  329 |           description = _resources.default.getString('ricotta_error_7112_content');
  330 |           isResume = true;
  331 |           isHelp = false;
  332 |           break;
  333 | 
  334 |         case _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_PAPER_REMOVE_LOAD:
  335 |           status = _resources.default.getString('ricotta_error_7114_title');
  336 |           subStatus = _resources.default.getString('ricotta_error_7114_subtitle');
  337 |           description = _resources.default.getString('ricotta_error_7114_content');
  338 |           isResume = true;
  339 |           isHelp = true;
  340 |           helpButtonName = _resources.default.getString('button_video');
  341 |           helpTitle = _resources.default.getString('ricotta_error_7114_title');
  342 |           helpDesription = _resources.default.getString('ricotta_error_7114_content');
  343 |           break;
  344 | 
  345 |         case _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_RIBBON_END:
  346 |           status = _resources.default.getString('ricotta_error_7201_title');
  347 |           subStatus = _resources.default.getString('ricotta_error_7201_subtitle');
  348 |           description = _resources.default.getString('ricotta_error_7201_content');
  349 |           isResume = true;
  350 |           isHelp = false;
  351 |           break;
  352 | 
  353 |         case _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_NO_RIBBON_MARKER:
  354 |           status = _resources.default.getString('ricotta_error_7204_title');
  355 |           subStatus = _resources.default.getString('ricotta_error_7204_subtitle');
  356 |           description = _resources.default.getString('ricotta_error_7204_content');
  357 |           isResume = false;
  358 |           isHelp = true;
  359 |           helpButtonName = _resources.default.getString('button_video');
  360 |           helpTitle = _resources.default.getString('ricotta_error_7204_title');
  361 |           helpDesription = _resources.default.getString('ricotta_error_7204_content');
  362 |           break;
  363 | 
  364 |         case _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_RIBBON_ERROR:
  365 |           status = _resources.default.getString('ricotta_error_7205_title');
  366 |           subStatus = _resources.default.getString('ricotta_error_7205_subtitle');
  367 |           description = _resources.default.getString('ricotta_error_7205_content');
  368 |           isResume = false;
  369 |           isHelp = true;
  370 |           helpButtonName = _resources.default.getString('button_video');
  371 |           helpTitle = _resources.default.getString('ricotta_error_7205_title');
  372 |           helpDesription = _resources.default.getString('ricotta_error_7205_content');
  373 |           break;
  374 | 
  375 |         case _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_RIBBON_ERROR_2:
  376 |           status = _resources.default.getString('ricotta_error_7208_title');
  377 |           subStatus = _resources.default.getString('ricotta_error_7208_subtitle');
  378 |           description = _resources.default.getString('ricotta_error_7208_content');
  379 |           isResume = true;
  380 |           isHelp = true;
  381 |           helpButtonName = _resources.default.getString('button_video');
  382 |           helpTitle = _resources.default.getString('ricotta_error_7208_title');
  383 |           helpDesription = _resources.default.getString('ricotta_error_7208_content');
  384 |           break;
  385 | 
  386 |         case _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_PRINTER_OVERHEAT:
  387 |           status = _resources.default.getString('ricotta_error_7308_title');
  388 |           subStatus = _resources.default.getString('ricotta_error_7308_subtitle');
  389 |           description = _resources.default.getString('ricotta_error_7308_content');
  390 |           isResume = false;
  391 |           isHelp = false;
  392 |           break;
  393 | 
  394 |         case _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_PRINTER_OVERCOOL:
  395 |           status = _resources.default.getString('ricotta_error_7309_title');
  396 |           subStatus = _resources.default.getString('ricotta_error_7309_subtitle');
  397 |           description = _resources.default.getString('ricotta_error_7309_content');
  398 |           isResume = false;
  399 |           isHelp = false;
  400 |           break;
  401 | 
  402 |         case _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_PRINTER_BATTERY_CRITICAL:
  403 |           status = _resources.default.getString('ricotta_error_7310_title');
  404 |           subStatus = _resources.default.getString('ricotta_error_7310_subtitle');
  405 |           description = _resources.default.getString('ricotta_error_7310_content');
  406 |           isResume = false;
  407 |           isHelp = false;
  408 |           break;
  409 | 
  410 |         case _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_PRINTER_BATTERY_OFF:
  411 |           status = _resources.default.getString('ricotta_error_7311_title');
  412 |           subStatus = _resources.default.getString('ricotta_error_7311_subtitle');
  413 |           description = _resources.default.getString('ricotta_error_7311_content');
  414 |           isResume = false;
  415 |           isHelp = false;
  416 |           break;
  417 | 
  418 |         default:
  419 |           status = _resources.default.getString("error_unenumerated");
  420 |           subStatus = _resources.default.getString("error_unenumerated");
  421 |           description = _resources.default.getString("error_unenumerated");
  422 |           isResume = true;
  423 |           break;
  424 |       }
  425 | 
  426 |       var errorInfo = {
  427 |         'code': code,
  428 |         'status': status,
  429 |         'subStatus': subStatus,
  430 |         'helpButtonName': helpButtonName,
  431 |         'description': description,
  432 |         'isResume': isResume,
  433 |         'isHelp': isHelp,
  434 |         'helpDesription': helpDesription,
  435 |         'releaseButtonName': releaseButtonName,
  436 |         'helpTitle': helpTitle
  437 |       };
  438 |       return errorInfo;
  439 |     },
  440 |     deviceAlertsEntity: function deviceAlertsEntity(codes, jobType) {
  441 |       var status = '';
  442 |       var subStatus = '';
  443 |       var alerts = [];
  444 |       var helpButtonName = '';
  445 |       var isResume = false;
  446 |       var isHelp = false;
  447 |       codes.map(function (item, index) {
```

### 12332.js L911-L934
Original bundle byte offset: `1255719` (`0x132927`).
```javascript
  911 |     handleDeviceState: function handleDeviceState(navigation, printerStatus, isForce, resultCallBack) {
  912 |       if (printerStatus.category == _consts.MINT_PRINTER_STATUS_CATEGORY.PRINTER_STATUS_CATEGORY_ERROR) {
  913 |         var errorInfo = this.getErrorInfo(printerStatus.error, printerStatus.sub_category);
  914 |         errorInfo.isCleaning = printerStatus.sub_category == _consts.MINT_PRINTER_SUB_CATEGORY.PRINTER_STATUS_SUB_CATEGORY_CLEANING;
  915 |         var tempStatus = printerStatus;
  916 |         tempStatus.errorInfo = errorInfo;
  917 | 
  918 |         if (lastErrorCode !== tempStatus.errorInfo.code || global.errorViewVisible || isForce) {
  919 |           lastErrorCode = tempStatus.errorInfo.code;
  920 |           global.errorViewVisible = false;
  921 |           navigation.navigate('error', {
  922 |             error: tempStatus.errorInfo,
  923 |             callback: function callback(data, navigation) {
  924 |               resultCallBack.fromView(data, navigation);
  925 |             }
  926 |           });
  927 |         }
  928 |       } else {
  929 |         global.errorViewVisible = true;
  930 |         global.errorDialogVisible = true;
  931 |         resultCallBack.hideErrorDialog();
  932 |       }
  933 |     },
  934 |     navigateToError: function navigateToError(navigation, error) {
```

### 13484.js L934-L981
Original bundle byte offset: `7081409` (`0x6c0dc1`).
```javascript
  934 |     ricotta_error_7001_title: "纸盒盖未关闭",
  935 |     ricotta_error_7001_subtitle: "纸盒盖未关闭",
  936 |     ricotta_error_7001_content: "请关闭纸盒盖。",
  937 |     ricotta_error_7103_title: "未检测到相纸",
  938 |     ricotta_error_7103_subtitle: "未检测到相纸",
  939 |     ricotta_error_7103_content: "检查是否纸张异常或缺纸。\n\n如纸盒内有纸，请取出相纸后重新放入；如缺纸，新相纸整包放入（普通相纸10张/即贴相纸5张）放入纸张时有字面朝上，虚线位置如图。\n\n处理后，请勿取消任务，设备恢复后即可继续打印任务。",
  940 |     ricotta_error_7104_title: "内部卡纸",
  941 |     ricotta_error_7104_subtitle: "内部卡纸",
  942 |     ricotta_error_7104_content: "请关机后重启打印机，待纸张自动吐出。",
  943 |     ricotta_error_7105_title: "取纸异常",
  944 |     ricotta_error_7105_subtitle: "取纸异常",
  945 |     ricotta_error_7105_content: "请关机后重启打印机，待纸张自动吐出。",
  946 |     ricotta_error_7110_title: "照片未取走",
  947 |     ricotta_error_7110_subtitle: "照片未取走",
  948 |     ricotta_error_7110_content: "请及时取出已打印完成的照片，取走后即可开始下一个打印任务。",
  949 |     ricotta_error_7111_title: "相纸放置有误",
  950 |     ricotta_error_7111_subtitle: "相纸放置有误",
  951 |     ricotta_error_7111_content: "请将相纸槽内的相纸重新整理后放入。（请勿触摸相纸光面，以免影响打印质量）",
  952 |     ricotta_error_7112_title: "相纸被移走",
  953 |     ricotta_error_7112_subtitle: "相纸被移走",
  954 |     ricotta_error_7112_content: "请检查相纸槽内是否有相纸，并点击已处理继续打印。",
  955 |     ricotta_error_7114_title: "取纸异常",
  956 |     ricotta_error_7114_subtitle: "取纸异常",
  957 |     ricotta_error_7114_content: "请将纸盒内的相纸重新整理后放入，如相纸无法取出，请关机后重启打印机。\n（请勿触摸相纸光面，以免影响打印质量）",
  958 |     ricotta_error_7201_title: "色带用尽",
  959 |     ricotta_error_7201_subtitle: "色带用尽",
  960 |     ricotta_error_7201_content: "按压橙色卡扣，取出并更换新色带。 \n\n检查是否缺纸，如缺纸，相纸按整包放入（普通相纸10张/ 即贴相纸5张）。放入纸张时有字面朝上，虚线位置如图。",
  961 |     ricotta_error_7204_title: "色带卡住",
  962 |     ricotta_error_7204_subtitle: "色带卡住",
  963 |     ricotta_error_7204_content: "请关机后重启打印机，重新插拔色带。若问题依旧，建议更换新色带。",
  964 |     ricotta_error_7205_title: "色带异常",
  965 |     ricotta_error_7205_subtitle: "色带异常",
  966 |     ricotta_error_7205_content: "请检查是否已正确放入色带，然后重启打印机，并重新插拔色带。若问题依旧，建议更换新色带。",
  967 |     ricotta_error_7208_title: "色带异常",
  968 |     ricotta_error_7208_subtitle: "色带异常",
  969 |     ricotta_error_7208_content: "请安装色带或重新插拔色带，并点击已处理继续打印。",
  970 |     ricotta_error_7308_title: "打印机温度过高",
  971 |     ricotta_error_7308_subtitle: "打印机温度过高",
  972 |     ricotta_error_7308_content: "当前打印机温度过高，待温度恢复后继续开始打印任务。\n请耐心等待，或取消任务稍后再试。",
  973 |     ricotta_error_7309_title: "打印机温度过低",
  974 |     ricotta_error_7309_subtitle: "打印机温度过低",
  975 |     ricotta_error_7309_content: "当前打印机温度过低，待温度恢复后继续开始打印任务。\n请耐心等待，或取消任务稍后再试。",
  976 |     ricotta_error_7310_title: "电池电量低",
  977 |     ricotta_error_7310_subtitle: "电池电量低",
  978 |     ricotta_error_7310_content: "当前打印机电量低，您可以继续打印任务。建议连接电源充电，确保打印顺畅。",
  979 |     ricotta_error_7311_title: "电量过低即将关机",
  980 |     ricotta_error_7311_subtitle: "电量过低即将关机",
  981 |     ricotta_error_7311_content: "当前打印机电量过低，即将关机，请连接电源并等待充电至足够电量后继续打印。",
```

## S08. Already handled button calls resume_printer

### 13145.js L293-L325
Original bundle byte offset: `4958066` (`0x4ba772`).
```javascript
  293 |       key: "progressButtonAction",
  294 |       value: function progressButtonAction() {
  295 |         var _this3 = this;
  296 | 
  297 |         _ErrorHeadFile.LogUtil.reportEvent(_logKey.LOG_KEY.ROSEMARY_TAP_EVENT_ERROR_PROGRESS, _logKey.LOG_KEY.ROSEMARY_TAP_EVENT_ERROR_PROGRESS);
  298 | 
  299 |         _ErrorHeadFile.LogUtil.reportLog('error: 点击了已处理');
  300 | 
  301 |         this.showLoadingTips(_ErrorHeadFile.Language.getString('toast_processing'));
  302 | 
  303 |         if (this.isPhotoPrinter()) {
  304 |           _HTClassicBluetooth.default.getInstance().resumePrinter().then(function () {
  305 |             _ErrorHeadFile.LogUtil.reportLog('error: 处理成功');
  306 | 
  307 |             _this3.dismissTips();
  308 | 
  309 |             setTimeout(function () {
  310 |               global.errorViewVisible = true;
  311 |             }, 1500);
  312 | 
  313 |             _this3.props.navigation.goBack();
  314 |           }).catch(function (err) {
  315 |             _ErrorHeadFile.LogUtil.reportLog("error: \u5904\u7406\u5931\u8D25" + JSON.stringify(err));
  316 | 
  317 |             _this3.dismissTips();
  318 | 
  319 |             _ErrorHeadFile.Toast.show(_ErrorHeadFile.Language.getString('toast_process_fail'), {
  320 |               duration: _ErrorHeadFile.Toast.durations.SHORT,
  321 |               position: _ErrorHeadFile.Toast.positions.BOTTOM,
  322 |               shadow: true,
  323 |               animation: true,
  324 |               hideOnPress: true,
  325 |               delay: 0
```

### 13145.js L744-L753
Original bundle byte offset: `4978773` (`0x4bf855`).
```javascript
  744 |       key: "isPhotoPrinter",
  745 |       value: function isPhotoPrinter() {
  746 |         return _deviceConfig.default.isPhotoPrinter();
  747 |       }
  748 |     }, {
  749 |       key: "getTitleTextColor",
  750 |       value: function getTitleTextColor() {
  751 |         if (_deviceConfig.default.isRicottaType()) {
  752 |           return _CommonHeadFile.HTColor.c_font_000000;
  753 |         } else {
```

### 13154.js L708-L750
Original bundle byte offset: `5031194` (`0x4cc51a`).
```javascript
  708 |       key: "progressButtonAction",
  709 |       value: function progressButtonAction() {
  710 |         var _this3 = this;
  711 | 
  712 |         _ErrorHeadFile.LogUtil.reportEvent(_logKey.LOG_KEY.ROSEMARY_TAP_EVENT_ERROR_HELP_PROGRESS, _logKey.LOG_KEY.ROSEMARY_TAP_EVENT_ERROR_HELP_PROGRESS);
  713 | 
  714 |         _ErrorHeadFile.LogUtil.reportLog('error-help: 点击了已处理');
  715 | 
  716 |         this.showLoadingTips(_ErrorHeadFile.Language.getString('toast_processing'));
  717 |         this.setState({
  718 |           isPaused: true
  719 |         });
  720 | 
  721 |         if (this.isPhotoPrinter()) {
  722 |           _HTClassicBluetooth.default.getInstance().resumePrinter().then(function () {
  723 |             _ErrorHeadFile.LogUtil.reportLog('error-help: 处理成功');
  724 | 
  725 |             _this3.dismissTips();
  726 | 
  727 |             if (_this3.props.navigation.state.params.callback != null) {
  728 |               _this3.props.navigation.state.params.callback(true);
  729 |             }
  730 | 
  731 |             _this3.props.navigation.goBack(_this3.props.navigation.state.params.screen_key);
  732 |           }).catch(function (err) {
  733 |             _ErrorHeadFile.LogUtil.reportLog("error-help: \u5904\u7406\u5931\u8D25" + JSON.stringify(err));
  734 | 
  735 |             _this3.dismissTips();
  736 | 
  737 |             _this3.setState({
  738 |               isPaused: false
  739 |             });
  740 | 
  741 |             _ErrorHeadFile.Toast.show(_ErrorHeadFile.Language.getString('toast_process_fail'), {
  742 |               duration: _ErrorHeadFile.Toast.durations.SHORT,
  743 |               position: _ErrorHeadFile.Toast.positions.BOTTOM,
  744 |               shadow: true,
  745 |               animation: true,
  746 |               hideOnPress: true,
  747 |               delay: 0
  748 |             });
  749 |           });
  750 |         } else {
```

## S09. Cancel current job UI and on-wire id

### 13208.js L820-L848
Original bundle byte offset: `5420310` (`0x52b516`).
```javascript
  820 |     }, {
  821 |       key: "_cancelJob",
  822 |       value: function _cancelJob() {
  823 |         _logUtils.default.reportEvent(_logKey.LOG_KEY.ROSEMARY_TAP_EVENT_PRINTING_CANCEL, _logKey.LOG_KEY.ROSEMARY_TAP_EVENT_PRINTING_CANCEL);
  824 | 
  825 |         this.setState({
  826 |           dialogVisible: true,
  827 |           alertMsg: _resources.default.getString("job_bt_cancel_txt")
  828 |         });
  829 |       }
  830 |     }, {
  831 |       key: "cancelJob",
  832 |       value: function cancelJob() {
  833 |         var _this8 = this;
  834 | 
  835 |         _bluetoothPrintManger.default.ShareInstance().cancelCurrentJob().then(function (result) {
  836 |           _this8.setState({
  837 |             finallyState: _consts.MINT_JOB_STATUS.PRINTER_JOB_STATUS_CANCELED
  838 |           });
  839 |         }).catch(function (err) {
  840 |           _reactNativeRootToast.default.show(_resources.default.getString("toast_cancel_job_fail"), {
  841 |             duration: _reactNativeRootToast.default.durations.SHORT,
  842 |             position: _reactNativeRootToast.default.positions.BOTTOM,
  843 |             shadow: true,
  844 |             animation: true,
  845 |             hideOnPress: true,
  846 |             delay: 0
  847 |           });
  848 |         });
```

### 12311.js L1209-L1235
Original bundle byte offset: `1002189` (`0xf4acd`).
```javascript
 1209 |       key: "cancelCurrentJob",
 1210 |       value: function cancelCurrentJob() {
 1211 |         var _this15 = this;
 1212 | 
 1213 |         this.log("取消当前任务");
 1214 |         return new Promise(function (resolve, reject) {
 1215 |           if (!_this15.currentJob) {
 1216 |             reject("no job");
 1217 |           } else {
 1218 |             _HTClassicBluetooth.default.getInstance().cancelJob(_this15.currentJob.print_job_id).then(function (result) {
 1219 |               _this15.log("\u53D6\u6D88\u4EFB\u52A1\u6210\u529F result = " + result);
 1220 | 
 1221 |               _this15.currentJob = null;
 1222 | 
 1223 |               _miot.Host.storage.set(_this15.currentJobKey, _this15.currentJob);
 1224 | 
 1225 |               resolve(result);
 1226 |             }).catch(function (e) {
 1227 |               _this15.log("\u53D6\u6D88\u4EFB\u52A1\u5931\u8D25 e = " + e);
 1228 | 
 1229 |               reject(e);
 1230 |             });
 1231 |           }
 1232 |         });
 1233 |       }
 1234 |     }, {
 1235 |       key: "cancelLocalJob",
```

### 12407.js L261-L299
Original bundle byte offset: `1488807` (`0x16b7a7`).
```javascript
  261 |       key: "cancelJob",
  262 |       value: function cancelJob(jobId) {
  263 |         var _this5 = this;
  264 | 
  265 |         var msgSn = this.getMsgSn();
  266 |         return new Promise(function (resolve, reject) {
  267 |           var data = {
  268 |             'method': 'cancel_job',
  269 |             'id': msgSn,
  270 |             'params': [jobId]
  271 |           };
  272 | 
  273 |           _this5.handleCommond(data).then(function (res) {
  274 |             _this5.isCanceled = true;
  275 |             resolve(res);
  276 |           }).catch(function (error) {
  277 |             reject(error);
  278 |           });
  279 |         });
  280 |       }
  281 |     }, {
  282 |       key: "resumePrinter",
  283 |       value: function resumePrinter() {
  284 |         var _this6 = this;
  285 | 
  286 |         var msgSn = this.getMsgSn();
  287 |         return new Promise(function (resolve, reject) {
  288 |           var data = {
  289 |             'method': 'resume_printer',
  290 |             'id': msgSn,
  291 |             'params': {}
  292 |           };
  293 | 
  294 |           _this6.handleCommond(data).then(function (res) {
  295 |             resolve(res);
  296 |           }).catch(function (error) {
  297 |             reject(error);
  298 |           });
  299 |         });
```

## S10. Command queue and response id/error handling

### 12407.js L788-L895
Original bundle byte offset: `1505317` (`0x16f825`).
```javascript
  788 |       key: "handleCommond",
  789 |       value: function handleCommond(data) {
  790 |         var _this15 = this;
  791 | 
  792 |         return new Promise(function (resolve, reject) {
  793 |           _this15.queue.push({
  794 |             data: data,
  795 |             resolve: resolve,
  796 |             reject: reject
  797 |           });
  798 | 
  799 |           _this15.processQueue();
  800 |         });
  801 |       }
  802 |     }, {
  803 |       key: "processQueue",
  804 |       value: function processQueue() {
  805 |         var _this16 = this;
  806 | 
  807 |         if (this.isProcessing || this.queue.length === 0) {
  808 |           return;
  809 |         }
  810 | 
  811 |         this.isProcessing = true;
  812 | 
  813 |         var _this$queue$shift = this.queue.shift(),
  814 |             data = _this$queue$shift.data,
  815 |             resolve = _this$queue$shift.resolve,
  816 |             reject = _this$queue$shift.reject;
  817 | 
  818 |         try {
  819 |           var jsonString = JSON.stringify(data);
  820 | 
  821 |           var binary = _buffer.Buffer.from(jsonString, 'utf-8');
  822 | 
  823 |           this.encryptBinary(binary).then(function (tempBinary) {
  824 |             var htPackage = new _ht_package.default();
  825 |             htPackage.setChannelId(_this16.getChannleID(false));
  826 |             htPackage.setMsgSn(data.id);
  827 |             htPackage.setArcMsgSn(data.id);
  828 |             htPackage.setEncryptType(_this16.encryptType);
  829 |             htPackage.setMsgBody(tempBinary);
  830 |             var frame = htPackage.build(false);
  831 |             var isTimeout = false;
  832 |             var timer = setTimeout(function () {
  833 |               isTimeout = true;
  834 |               _this16.isProcessing = false;
  835 |               reject('command response timeout');
  836 | 
  837 |               _this16.processQueue();
  838 |             }, _this16.requestTimeout);
  839 | 
  840 |             _this16.listener = function (Receiveddata) {
  841 |               if (isTimeout) return;
  842 |               _this16.listener = null;
  843 |               clearTimeout(timer);
  844 | 
  845 |               _this16.decryptBinary(Receiveddata.body).then(function (res) {
  846 |                 var jsonString = _buffer.Buffer.from(res).toString('utf-8');
  847 | 
  848 |                 var jsonData = JSON.parse(jsonString);
  849 |                 var checkIdResult = true;
  850 | 
  851 |                 if (data && jsonData && data.id && jsonData.id) {
  852 |                   if (data.id != jsonData.id) {
  853 |                     checkIdResult = false;
  854 |                   }
  855 |                 }
  856 | 
  857 |                 if (checkIdResult) {
  858 |                   resolve(jsonData);
  859 |                   _this16.isProcessing = false;
  860 | 
  861 |                   _this16.processQueue();
  862 |                 } else {
  863 |                   _DynamicUtil.default.trackEvent(_consts.BIGDATA_KEYS.APP_CONN_EVT_CHECKIDFAIL, {});
  864 | 
  865 |                   reject("check id fail");
  866 |                   _this16.isProcessing = false;
  867 | 
  868 |                   _this16.processQueue();
  869 |                 }
  870 |               }).catch(function (error) {
  871 |                 reject(error);
  872 |                 _this16.isProcessing = false;
  873 | 
  874 |                 _this16.processQueue();
  875 |               });
  876 |             };
  877 | 
  878 |             var dataString = _this16.byteArrayToHexString(frame);
  879 | 
  880 |             _miot.ClassicBluetooth.write(dataString).then(function (res) {}).catch(function (error) {
  881 |               reject(error);
  882 |               _this16.isProcessing = false;
  883 | 
  884 |               _this16.processQueue();
  885 |             });
  886 |           }).catch(function (err) {
  887 |             reject(err);
  888 |             _this16.isProcessing = false;
  889 | 
  890 |             _this16.processQueue();
  891 |           });
  892 |         } catch (e) {
  893 |           reject(e);
  894 |           this.isProcessing = false;
  895 |           this.processQueue();
```

### 12311.js L719-L733
Original bundle byte offset: `981372` (`0xef97c`).
```javascript
  719 |             if (result.method == "event.rpt_err") {
  720 |               _this12.reportLog("BluetoothPrintManager --> getMixStatus method = " + result.method);
  721 | 
  722 |               if (result.params && result.params.code == _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_RPT_ERROR) {
  723 |                 _this12.reportLog("BluetoothPrintManager --> getMixStatus code = " + result.params.code);
  724 | 
  725 |                 _this12.displayState(false, _HTClassicBluetooth.default.getInstance().getConnectionState(), _resources.default.getString('bt_disconnect_title'), _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_RPT_ERROR);
  726 |               }
  727 |             } else {
  728 |               _this12.deviceInfo = result.result[0];
  729 | 
  730 |               _this12.reportLog("getDeviceInfo deviceInfo " + JSON.stringify(_this12.deviceInfo));
  731 | 
  732 |               _DynamicUtil.default.trackEvent(_consts.BIGDATA_KEYS.FW_DEVICE_PROP_DEVICEINFO_COMBO, _this12.deviceInfo);
  733 | 
```

## S11. Frame fields, AES padding, and parser

### 12368.js L23-L40
Original bundle byte offset: `1425605` (`0x15c0c5`).
```javascript
   23 |       (0, _classCallCheck2.default)(this, HTPackage);
   24 |       this.version = _ht_model.VERSION_1_01;
   25 |       this.reserve = _ht_model.RESERVE;
   26 |       this.encryptType = _ht_model.ENCRYPT_ECB;
   27 |       this.channelId = _ht_model.CHANNEL_DATA_ID;
   28 |       this.interactive = _ht_model.INTERACTIVE_REQUEST;
   29 |       this.encoding = _ht_model.ENCODING_JSON;
   30 |       this.arcMsgSn = 0;
   31 |       this.msgSn = 0;
   32 |       this.msgPackageTotal = 0;
   33 |       this.currentPackageNum = 0;
   34 |       this.aesKey = null;
   35 |       this.msgBody = null;
   36 |       this.msgLength = 0;
   37 |       this.isStream = false;
   38 |     }
   39 | 
   40 |     (0, _createClass2.default)(HTPackage, [{
```

### 12368.js L121-L172
Original bundle byte offset: `1428526` (`0x15cc2e`).
```javascript
  121 |     }, {
  122 |       key: "getView14to19",
  123 |       value: function getView14to19(bodyLength) {
  124 |         var uint16 = new Uint16Array(3);
  125 |         var msgAttribute = bodyLength;
  126 | 
  127 |         switch (this.encryptType) {
  128 |           case '000':
  129 |             msgAttribute += 0;
  130 |             break;
  131 | 
  132 |           case '001':
  133 |             msgAttribute += 1024;
  134 |             break;
  135 | 
  136 |           case '010':
  137 |             msgAttribute += 2048;
  138 |             break;
  139 | 
  140 |           case '011':
  141 |             msgAttribute += 3072;
  142 |             break;
  143 | 
  144 |           case '100':
  145 |             msgAttribute += 4096;
  146 |             break;
  147 | 
  148 |           case '101':
  149 |             msgAttribute += 5120;
  150 |             break;
  151 | 
  152 |           case '110':
  153 |             msgAttribute += 6144;
  154 |             break;
  155 |         }
  156 | 
  157 |         if (this.msgPackageTotal > 1 || this.isStream) {
  158 |           msgAttribute += 8192;
  159 |         }
  160 | 
  161 |         uint16[0] = this.msgPackageTotal;
  162 |         uint16[1] = this.currentPackageNum;
  163 |         uint16[2] = msgAttribute;
  164 |         return new Uint8Array(uint16.buffer);
  165 |       }
  166 |     }, {
  167 |       key: "encryptBody",
  168 |       value: function encryptBody() {
  169 |         var binary = this.msgBody;
  170 | 
  171 |         switch (this.encryptType) {
  172 |           case _ht_model.ENCRYPT_ECB:
```

### 12368.js L183-L221
Original bundle byte offset: `1430020` (`0x15d204`).
```javascript
  183 |         var msgBodyView = this.msgBody;
  184 | 
  185 |         if (encrypt) {
  186 |           msgBodyView = this.encryptBody();
  187 |         }
  188 | 
  189 |         var frame = new Uint8Array(22 + msgBodyView.length);
  190 |         var frame6to13 = this.getView6to13();
  191 |         var frame14to19 = this.getView14to19(msgBodyView.length);
  192 |         var sum = 0;
  193 |         frame[0] = _ht_model.FRAME_HEAD;
  194 |         frame[1] = this.version;
  195 |         frame[2] = this.reserve;
  196 |         frame[3] = this.channelId;
  197 |         frame[4] = this.interactive;
  198 |         frame[5] = this.encoding;
  199 | 
  200 |         for (var i = 0; i < frame6to13.length; i++) {
  201 |           frame[6 + i] = frame6to13[i];
  202 |         }
  203 | 
  204 |         for (var _i = 0; _i < frame14to19.length; _i++) {
  205 |           frame[14 + _i] = frame14to19[_i];
  206 |         }
  207 | 
  208 |         frame.set(msgBodyView, 20);
  209 |         sum = frame.reduce(function (a, b) {
  210 |           return a + b;
  211 |         });
  212 |         frame[frame.length - 2] = sum - 126;
  213 |         frame[frame.length - 1] = _ht_model.FRAME_TAIL;
  214 |         return frame;
  215 |       }
  216 |     }]);
  217 |     return HTPackage;
  218 |   }();
  219 | 
  220 |   exports.default = HTPackage;
  221 | },12368,[14314,14329,14332,12308,12359,12365,12371]);
```

### 12365.js L11-L41
Original bundle byte offset: `1423855` (`0x15b9ef`).
```javascript
   11 |   var crypto = {
   12 |     encryptECB: function encryptECB(key, data) {
   13 |       var fillLength = data.length % 16 === 0 ? 0 : 16 - data.length % 16;
   14 |       var newData = new Uint8Array(data.length + fillLength);
   15 | 
   16 |       for (var i = 0; i < data.length; i++) {
   17 |         newData[i] = data[i];
   18 |       }
   19 | 
   20 |       var aesEcb = new _aesJs.default.ModeOfOperation.ecb(key);
   21 |       return aesEcb.encrypt(newData);
   22 |     },
   23 |     decryptECB: function decryptECB(key, data, remove) {
   24 |       var aesEcb = new _aesJs.default.ModeOfOperation.ecb(key);
   25 |       var decodeData = aesEcb.decrypt(data);
   26 | 
   27 |       if (!remove) {
   28 |         return decodeData;
   29 |       }
   30 | 
   31 |       var removeLength = 0;
   32 | 
   33 |       for (var i = decodeData.length - 1; i >= 0; i--) {
   34 |         if (decodeData[i] !== 0) break;
   35 |         removeLength++;
   36 |       }
   37 | 
   38 |       var newDecodeData = decodeData.subarray(0, decodeData.length - removeLength);
   39 |       return newDecodeData;
   40 |     }
   41 |   };
```

### 12359.js L43-L78
Original bundle byte offset: `1371169` (`0x14ec21`).
```javascript
   43 |   function getFrameEndIndex(frame) {
   44 |     if (frame.length < 22) {
   45 |       return -1;
   46 |     }
   47 | 
   48 |     var attributeBuffer = new Uint8Array([frame[18], frame[19]]).buffer;
   49 |     var attributeNumber = new Uint16Array(attributeBuffer)[0];
   50 |     var binaryString = attributeNumber.toString(2);
   51 |     var binaryArray = binaryString.split('');
   52 |     var length = binaryArray.length;
   53 |     var bodyLength = 0;
   54 | 
   55 |     for (var i = 0; i < 16 - length; i++) {
   56 |       binaryArray.unshift('0');
   57 |     }
   58 | 
   59 |     for (var j = 15; j > 5; j--) {
   60 |       if (binaryArray[j] === '1') {
   61 |         bodyLength += Math.pow(2, 15 - j);
   62 |       }
   63 |     }
   64 | 
   65 |     if (bodyLength > 992) {
   66 |       return -2;
   67 |     }
   68 | 
   69 |     var flagEndIndex = 21 + bodyLength;
   70 | 
   71 |     if (frame.length < flagEndIndex + 1) {
   72 |       return -1;
   73 |     }
   74 | 
   75 |     if (frame[flagEndIndex] !== 126) {
   76 |       return -2;
   77 |     }
   78 | 
```

### 12359.js L107-L128
Original bundle byte offset: `1372717` (`0x14f22d`).
```javascript
  107 |   function unpack(buffer) {
  108 |     var frame = buffer;
  109 |     var result = {
  110 |       channelId: frame[3],
  111 |       interactive: frame[4],
  112 |       encryptType: '',
  113 |       msgPackageTotal: new Uint16Array(new Uint8Array([frame[14], frame[15]]).buffer)[0],
  114 |       msgPackageNum: new Uint16Array(new Uint8Array([frame[16], frame[17]]).buffer)[0],
  115 |       body: null
  116 |     };
  117 |     var msgAttribute = new Uint16Array(new Uint8Array([frame[18], frame[19]]).buffer)[0];
  118 |     var binaryString = msgAttribute.toString(2).padStart(16, '0');
  119 |     result.encryptType = binaryString.substr(3, 3);
  120 |     var body = buffer.slice(20, buffer.length - 2);
  121 |     result.body = body;
  122 |     return result;
  123 |   }
  124 | 
  125 |   var HTUtils = {
  126 |     uint8Concat: function uint8Concat(arg1, arg2) {
  127 |       var result = new Uint8Array(arg1.length + arg2.length);
  128 |       result.set(arg1, 0);
```

## S12. DH encoding and transport

### 12407.js L900-L924
Original bundle byte offset: `1508788` (`0x1705b4`).
```javascript
  900 |       value: function sayHello() {
  901 |         var _this17 = this;
  902 | 
  903 |         _logUtils.default.reportLog("sayHello this.isHandShaking = " + this.isHandShaking + " this.encryptType = " + this.encryptType);
  904 | 
  905 |         if (this.isHandShaking) {
  906 |           return;
  907 |         } else {
  908 |           this.start();
  909 |         }
  910 | 
  911 |         var msgSn = this.getMsgSn();
  912 |         var htPackage = new _ht_package.default();
  913 |         htPackage.setChannelId(_ht_model.CHANNEL_AUTH_ID);
  914 |         htPackage.setInteractive(_ht_model.INTERACTIVE_CLIENT_HELLO_DH);
  915 |         htPackage.setMsgSn(msgSn);
  916 |         htPackage.setArcMsgSn(msgSn);
  917 |         htPackage.setEncryptType(_ht_model.ENCRYPT_ECB);
  918 |         htPackage.setEncoding(_ht_model.ENCODING_JSON);
  919 |         htPackage.setMsgBody('hello');
  920 |         var frame = htPackage.build();
  921 |         var dataString = this.byteArrayToHexString(frame);
  922 | 
  923 |         _miot.ClassicBluetooth.write(dataString).then(function (res) {}).catch(function (error) {
  924 |           _this17.handleFail(1);
```

### 12407.js L938-L955
Original bundle byte offset: `1510055` (`0x170aa7`).
```javascript
  938 |       key: "handleConfirm",
  939 |       value: function handleConfirm(msg) {
  940 |         if (msg.channelId == _ht_model.CHANNEL_AUTH_ID && msg.interactive == _ht_model.INTERACTIVE_SERVER_CONFIRM_DH) {
  941 |           var newBody = _ht_utils.default.handleTailZero(msg.body);
  942 | 
  943 |           var confirmMeg = _ht_utils.default.byteArrayToString(newBody);
  944 | 
  945 |           if (confirmMeg === HANDSHAKE_CONFIRM_MESSAGE.HANDSHAKE_CONFIRM_MESSAGE_OK) {
  946 |             _logUtils.default.reportLog("confirmMeg OK");
  947 | 
  948 |             this.currentHandShakeStep = HANDSHAKE_STEP.STEP_CONFIRM;
  949 |             this.end();
  950 | 
  951 |             _DynamicUtil.default.trackEvent(_consts.BIGDATA_KEYS.APP_CONN_BT_EVT_HANDSHAKESUCCESS, {});
  952 | 
  953 |             this.classicBlueConnectionState = _consts.MINT_BLUETOOTH_STATE.AUTH;
  954 | 
  955 |             _DynamicUtil.default.trackEvent(_consts.BIGDATA_KEYS.APP_CONN_BT_EVT_CONNSTATECHANGE, {
```

### 12407.js L974-L1055
Original bundle byte offset: `1511475` (`0x171033`).
```javascript
  974 |       key: "handleInfo",
  975 |       value: function handleInfo(msg) {
  976 |         var _this18 = this;
  977 | 
  978 |         if (msg.channelId == _ht_model.CHANNEL_AUTH_ID && msg.interactive == _ht_model.INTERACTIVE_SERVER_HELLO_DH) {
  979 |           if (!msg.body || msg.body.length != 36) {
  980 |             this.handleFail(5);
  981 |             return;
  982 |           }
  983 | 
  984 |           var G = msg.body.slice(0, 4);
  985 |           var P = msg.body.slice(4, 20);
  986 |           var RA = msg.body.slice(20, 36);
  987 | 
  988 |           var G_trimmed = _ht_utils.default.handleTailZero(G);
  989 | 
  990 |           var g_string = _ht_utils.default.byteArrayToString(G_trimmed);
  991 | 
  992 |           var p_string = _ht_utils.default.byteArrayToString(P);
  993 | 
  994 |           var ra_string = _ht_utils.default.byteArrayToString(RA);
  995 | 
  996 |           var g_bigInteger = _ht_utils.default.byteArrayToBigInteger(G_trimmed);
  997 | 
  998 |           var p_bigInteger = _ht_utils.default.byteArrayToBigInteger(P);
  999 | 
 1000 |           var b_bigInteger = _ht_utils.default.nextRandomBigInteger(p_bigInteger.subtract((0, _bigInteger.default)(2)));
 1001 | 
 1002 |           var ra_bigInteger = _ht_utils.default.byteArrayToBigInteger(RA);
 1003 | 
 1004 |           var rb_bigInteger = g_bigInteger.modPow(b_bigInteger, p_bigInteger);
 1005 | 
 1006 |           var rb_bytes = _ht_utils.default.bigIntegerToBytes(rb_bigInteger).toJSON().data;
 1007 | 
 1008 |           if (rb_bytes.length < 16) {
 1009 |             rb_bytes = _ht_utils.default.arrayPacking16(rb_bytes);
 1010 |           } else {}
 1011 | 
 1012 |           var k_bigInteger = ra_bigInteger.modPow(b_bigInteger, p_bigInteger);
 1013 | 
 1014 |           var k_bytes = _ht_utils.default.bigIntegerToBytes(k_bigInteger).toJSON().data;
 1015 | 
 1016 |           if (k_bytes.length < 16) {
 1017 |             k_bytes = _ht_utils.default.arrayPacking16AtEnd(k_bytes);
 1018 |           } else {}
 1019 | 
 1020 |           this.key = k_bytes;
 1021 | 
 1022 |           var p_en_bytes = _ht_crypto.default.encryptECB(k_bytes, P);
 1023 | 
 1024 |           var p_en_bytes16 = new Uint8Array(16);
 1025 | 
 1026 |           for (var i = 0; i < p_en_bytes16.length; i++) {
 1027 |             p_en_bytes16[i] = p_en_bytes[i];
 1028 |           }
 1029 | 
 1030 |           var mesbody = new Uint8Array(32);
 1031 |           mesbody.set(rb_bytes, 0);
 1032 |           mesbody.set(p_en_bytes16, 16);
 1033 |           var msgSn = this.getMsgSn();
 1034 |           var htPackage = new _ht_package.default();
 1035 |           htPackage.setChannelId(_ht_model.CHANNEL_AUTH_ID);
 1036 |           htPackage.setInteractive(_ht_model.INTERACTIVE_CLIENT_CONFIRM_DH);
 1037 |           htPackage.setMsgSn(msgSn);
 1038 |           htPackage.setArcMsgSn(msgSn);
 1039 |           htPackage.setEncryptType(_ht_model.ENCRYPT_ECB);
 1040 |           htPackage.setEncoding(_ht_model.ENCODING_JSON);
 1041 |           htPackage.setMsgBody(mesbody);
 1042 |           var frame = htPackage.build();
 1043 |           var dataString = this.byteArrayToHexString(frame);
 1044 |           this.currentHandShakeStep = HANDSHAKE_STEP.STEP_CONFIRM;
 1045 | 
 1046 |           _miot.ClassicBluetooth.write(dataString).then(function (res) {}).catch(function (error) {
 1047 |             _this18.handleFail(6);
 1048 |           });
 1049 |         } else {
 1050 |           this.handleFail(7);
 1051 |         }
 1052 |       }
 1053 |     }, {
 1054 |       key: "handleFail",
 1055 |       value: function handleFail(code) {
```

### 12359.js L173-L226
Original bundle byte offset: `1374894` (`0x14faae`).
```javascript
  173 |     byteArrayToString: function byteArrayToString(buffer) {
  174 |       return _buffer.Buffer.from(buffer).toString();
  175 |       ;
  176 |     },
  177 |     byteArrayToBigInteger: function byteArrayToBigInteger(buffer) {
  178 |       return (0, _bigInteger.default)(this.byteArrayToString(buffer), 16);
  179 |     },
  180 |     nextRandomBigInteger: function nextRandomBigInteger(n) {
  181 |       var bitLength = n.bitLength();
  182 |       var result;
  183 | 
  184 |       do {
  185 |         result = _bigInteger.default.randBetween((0, _bigInteger.default)(2).pow(bitLength - 1), n.minus(1));
  186 |       } while (result.compare(n) >= 0 || result.compare((0, _bigInteger.default)(2)) <= 0);
  187 | 
  188 |       return result;
  189 |     },
  190 |     bigIntegerToBytes: function bigIntegerToBytes(bigInteger) {
  191 |       return _buffer.Buffer.from(this.bigIntegerToString(bigInteger), 'utf-8');
  192 |     },
  193 |     bigIntegerToString: function bigIntegerToString(bigInteger) {
  194 |       return bigInteger.toString(16).toUpperCase();
  195 |     },
  196 |     arrayPacking16: function arrayPacking16(src) {
  197 |       var temp = new Uint8Array(16);
  198 | 
  199 |       for (var i = 0; i < temp.length; i++) {
  200 |         temp[i] = 48;
  201 |       }
  202 | 
  203 |       temp.set(src, 16 - src.length);
  204 |       return temp;
  205 |     },
  206 |     arrayPacking16AtEnd: function arrayPacking16AtEnd(buffer) {
  207 |       var temp = new Uint8Array(16);
  208 | 
  209 |       for (var i = 0; i < temp.length; i++) {
  210 |         temp[i] = 0x00;
  211 |       }
  212 | 
  213 |       temp.set(buffer, 0);
  214 |       return temp;
  215 |     },
  216 |     handleTailZero: function handleTailZero(a) {
  217 |       var end = 0;
  218 | 
  219 |       for (var i = 0; i < a.length; i++) {
  220 |         if (a[i] === 0) {
  221 |           end = i;
  222 |           break;
  223 |         }
  224 |       }
  225 | 
  226 |       var result = a.slice(0, end);
```

### 12407.js L1197-L1224
Original bundle byte offset: `1519225` (`0x172e79`).
```javascript
 1197 |       key: "connect",
 1198 |       value: function connect() {
 1199 |         var _this22 = this;
 1200 | 
 1201 |         var macAddress = _miot.Device.mac;
 1202 |         var transportUUID = '00001101-0000-1000-8000-00805f9b34fb';
 1203 |         return new Promise(function (resolve, reject) {
 1204 |           _miot.ClassicBluetooth.connectSocket(macAddress, transportUUID).then(function (res) {
 1205 |             if (_reactNative.Platform.OS == 'ios') {
 1206 |               _this22.handleBlueConnectionStateChange(_consts.MINT_BLUETOOTH_STATE.CONNECTED);
 1207 |             }
 1208 | 
 1209 |             resolve(res);
 1210 |           }).catch(function (error) {
 1211 |             if (_reactNative.Platform.OS == 'ios') {
 1212 |               _this22.handleBlueConnectionStateChange(_consts.MINT_BLUETOOTH_STATE.DISCONNECTED);
 1213 |             }
 1214 | 
 1215 |             reject(error);
 1216 |           });
 1217 |         });
 1218 |       }
 1219 |     }, {
 1220 |       key: "disconnect",
 1221 |       value: function disconnect() {
 1222 |         return new Promise(function (resolve, reject) {
 1223 |           _miot.ClassicBluetooth.disconnectSocket().then(function (res) {
 1224 |             resolve(res);
```

## S13. Printing and transfer flow; confirm_job is not auto-called

### 12407.js L327-L375
Original bundle byte offset: `1490473` (`0x16be29`).
```javascript
  327 |       key: "printJob2",
  328 |       value: function printJob2(path, copies, jobType, callback) {
  329 |         var _this8 = this;
  330 | 
  331 |         this.isCanceled = false;
  332 |         var msgSn = this.getMsgSn();
  333 |         var channel = 0;
  334 | 
  335 |         if (_reactNative.Platform.OS === "ios") {
  336 |           channel = 64;
  337 |         } else {
  338 |           channel = 576;
  339 |         }
  340 | 
  341 |         if (path.indexOf('file:///') !== -1) {
  342 |           path = path.toString().replace('file:///', '/');
  343 |         }
  344 | 
  345 |         if (path.indexOf(_miot.Host.file.storageBasePath) !== -1) {
  346 |           path = path.toString().replace(_miot.Host.file.storageBasePath, '');
  347 |         }
  348 | 
  349 |         _miot.Host.file.readFileSegmentToBase64(path, 0, 1).then(function (result) {
  350 |           var data = {
  351 |             'method': 'print_job',
  352 |             'id': msgSn,
  353 |             'params': {
  354 |               file_size: result.totalLength,
  355 |               copies: copies,
  356 |               job_type: jobType,
  357 |               channel: channel
  358 |             }
  359 |           };
  360 | 
  361 |           _this8.handleCommond(data).then(function (res) {
  362 |             if (res.hasOwnProperty('result')) {
  363 |               var jobId = res.result.job_id;
  364 |               callback.onCreatJobSuccess(jobId);
  365 | 
  366 |               _this8.sendFile(path, jobId, result.totalLength, callback);
  367 |             } else if (res.hasOwnProperty('error')) {
  368 |               callback.onCreatJobFailed(res.error.code, '');
  369 |             } else {
  370 |               callback.onCreatJobFailed(-1, res);
  371 |             }
  372 |           }).catch(function (error) {
  373 |             callback.onCreatJobFailed(-2, error);
  374 |           });
  375 |         }).catch(function (error) {
```

### 12407.js L501-L611
Original bundle byte offset: `1496274` (`0x16d4d2`).
```javascript
  501 | 
  502 |                 if (!this.isCanceled) {
  503 |                   _context.next = 30;
  504 |                   break;
  505 |                 }
  506 | 
  507 |                 this.isSendFile = false;
  508 |                 return _context.abrupt("return");
  509 | 
  510 |               case 30:
  511 |                 thisCount++;
  512 |                 begin = this.bodyMessageLength * j;
  513 |                 end = j === frameCount - 1 ? binary.length : this.bodyMessageLength * (j + 1);
  514 |                 frame = binary.subarray(begin, end);
  515 |                 newFrame = this.addBytes(this.int2Bytes(jobId), frame);
  516 | 
  517 |                 if (this.isTestTime) {
  518 |                   this.currentTimeNode = Date.now();
  519 |                   this.time1 += this.currentTimeNode - this.lastTimeNode;
  520 |                   this.lastTimeNode = this.currentTimeNode;
  521 |                 }
  522 | 
  523 |                 _context.next = 38;
  524 |                 return _regenerator.default.awrap(this.encryptBinary(newFrame));
  525 | 
  526 |               case 38:
  527 |                 encryptFrame = _context.sent;
  528 | 
  529 |                 if (this.isTestTime) {
  530 |                   this.currentTimeNode = Date.now();
  531 |                   this.time2 += this.currentTimeNode - this.lastTimeNode;
  532 |                   this.lastTimeNode = this.currentTimeNode;
  533 |                 }
  534 | 
  535 |                 htPackage = new _ht_package.default();
  536 |                 msgSn = this.getMsgSn();
  537 |                 htPackage.setChannelId(this.getChannleID(true));
  538 |                 htPackage.setEncoding(_ht_model.ENCODING_HEX);
  539 |                 htPackage.setEncryptType(this.encryptType);
  540 |                 htPackage.setMsgSn(msgSn);
  541 |                 htPackage.setArcMsgSn(msgSn);
  542 |                 htPackage.setMsgBody(encryptFrame);
  543 |                 htPackage.setCurrentPackageNum(thisCount);
  544 |                 htPackage.setMsgPackageTotal(totalCount);
  545 |                 messageFrame = htPackage.build();
  546 | 
  547 |                 if (this.isTestTime) {
  548 |                   this.currentTimeNode = Date.now();
  549 |                   this.time3 += this.currentTimeNode - this.lastTimeNode;
  550 |                   this.lastTimeNode = this.currentTimeNode;
  551 |                 }
  552 | 
  553 |                 Array.prototype.push.apply(this.packageBuffer, messageFrame);
  554 |                 this.currentSavedPackageNum++;
  555 | 
  556 |                 if (!(this.currentSavedPackageNum >= MAX_PACKAGE_NUM_PER_TIME || j === frameCount - 1 || _reactNative.Platform.OS == 'ios')) {
  557 |                   _context.next = 64;
  558 |                   break;
  559 |                 }
  560 | 
  561 |                 this.currentSavedPackageNum = 0;
  562 |                 finalString = this.byteArrayToHexString(this.packageBuffer);
  563 |                 this.packageBuffer = [];
  564 | 
  565 |                 if (this.isTestTime) {
  566 |                   this.currentTimeNode = Date.now();
  567 |                   this.time4 += this.currentTimeNode - this.lastTimeNode;
  568 |                   this.lastTimeNode = this.currentTimeNode;
  569 |                 }
  570 | 
  571 |                 _context.next = 61;
  572 |                 return _regenerator.default.awrap(this.writeWithTimeout(finalString, 3000));
  573 | 
  574 |               case 61:
  575 |                 if (this.isTestTime) {
  576 |                   this.currentTimeNode = Date.now();
  577 |                   this.time5 += this.currentTimeNode - this.lastTimeNode;
  578 |                   this.lastTimeNode = this.currentTimeNode;
  579 |                 }
  580 | 
  581 |                 _context.next = 64;
  582 |                 break;
  583 | 
  584 |               case 64:
  585 |                 if (thisCount % 50 == 0 || thisCount == totalCount) {
  586 |                   callback.onProgress(thisCount, totalCount, jobId);
  587 |                 }
  588 | 
  589 |               case 65:
  590 |                 j++;
  591 |                 _context.next = 26;
  592 |                 break;
  593 | 
  594 |               case 68:
  595 |                 i++;
  596 |                 _context.next = 13;
  597 |                 break;
  598 | 
  599 |               case 71:
  600 |                 callback.onTransferSuccess(jobId);
  601 |                 this.sendEnd = Date.now();
  602 | 
  603 |                 if (this.isTestTime) {
  604 |                   _logUtils.default.reportLog("time1 = " + this.time1 + " time2_1 = " + this.time2_1 + " time2_2 = " + this.time2_2 + " time2_3 = " + this.time2_3 + " time2 = " + this.time2 + " time3 = " + this.time3 + " time4 = " + this.time4 + " time5 = " + this.time5);
  605 |                 }
  606 | 
  607 |                 this.isSendFile = false;
  608 |                 _context.next = 81;
  609 |                 break;
  610 | 
  611 |               case 77:
```

### 12311.js L1035-L1051
Original bundle byte offset: `995035` (`0xf2edb`).
```javascript
 1035 | 
 1036 |           if (this.allowToPrint && this.localJobs.length !== 0 && this.localJobs[0].taskStatus === _consts.MINT_TASK_STATUS.MINT_TASK_STATUS_UNSTARTED) {
 1037 |             this.log("BluetoothPrintManager --> \u5F85\u6253\u5370\u4EFB\u52A1\u4E0D\u4E3A\u7A7A\uFF0C\u5C1D\u8BD5\u53D1\u8D77\u65B0\u4EFB\u52A1 this.localJobs.length = " + this.localJobs.length);
 1038 |             this.preProcess();
 1039 |             this.localJobs[0].taskStatus = _consts.MINT_TASK_STATUS.MINT_TASK_STATUS_CREATING;
 1040 | 
 1041 |             _DynamicUtil.default.trackEvent(_consts.BIGDATA_KEYS.APP_PRINT_EVT_CREATEJOB, this.localJobs[0]);
 1042 | 
 1043 |             _HTClassicBluetooth.default.getInstance().printJob2(this.localJobs[0].sendPath, this.localJobs[0].copies, this.localJobs[0].job_type, {
 1044 |               onCreatJobSuccess: function onCreatJobSuccess(jobId) {
 1045 |                 _this14.log("onCreatJobSuccess:" + jobId);
 1046 | 
 1047 |                 _this14.localJobs[0].taskStatus = _consts.MINT_TASK_STATUS.MINT_TASK_STATUS_TRANSFERRING;
 1048 |                 _this14.localJobs[0].print_job_id = jobId;
 1049 |                 _this14.currentJob = _this14.localJobs.shift();
 1050 | 
 1051 |                 _DynamicUtil.default.trackEvent(_consts.BIGDATA_KEYS.APP_PRINT_EVT_CREATEJOBSUCCESS, _this14.currentJob);
```

### 12311.js L1091-L1100
Original bundle byte offset: `997758` (`0xf397e`).
```javascript
 1091 |               onTransferSuccess: function onTransferSuccess(jobId) {
 1092 |                 _this14.log("onTransferSuccess:" + jobId);
 1093 | 
 1094 |                 _this14.currentJob.taskStatus = _consts.MINT_TASK_STATUS.MINT_TASK_STATUS_PRINTING;
 1095 | 
 1096 |                 _miot.Host.storage.set(_this14.currentJobKey, _this14.currentJob);
 1097 | 
 1098 |                 _DynamicUtil.default.trackEvent(_consts.BIGDATA_KEYS.APP_PRINT_EVT_TRANSFERJOBSUCCESS, _this14.currentJob);
 1099 |               },
 1100 |               onTransferFailed: function onTransferFailed(error, jobId) {
```

## S14. Job terminal states and queue handling

### 12173.js L1231-L1246
Original bundle byte offset: `698380` (`0xaa80c`).
```javascript
 1231 |   var MINT_JOB_STATUS = {
 1232 |     PRINTER_JOB_STATUS_WAITING: "waiting",
 1233 |     PRINTER_JOB_STATUS_INIT: "init",
 1234 |     PRINTER_JOB_STATUS_INITLIZATION: "initlization",
 1235 |     PRINTER_JOB_STATUS_DOWNLOADING: "downloading",
 1236 |     PRINTER_JOB_STATUS_PRINTING_Y: "printing_Y",
 1237 |     PRINTER_JOB_STATUS_PRINTING_M: "printing_M",
 1238 |     PRINTER_JOB_STATUS_PRINTING_C: "printing_C",
 1239 |     PRINTER_JOB_STATUS_PRINTING_OC: "printing_OC",
 1240 |     PRINTER_JOB_STATUS_HOME_FEED: "home_feed",
 1241 |     PRINTER_JOB_STATUS_COOL_DOWN: "cool_down",
 1242 |     PRINTER_JOB_STATUS_FINISHED: "finished",
 1243 |     PRINTER_JOB_STATUS_CANCELED: "canceled",
 1244 |     PRINTER_JOB_STATUS_ABORTED: "aborted"
 1245 |   };
 1246 |   exports.MINT_JOB_STATUS = MINT_JOB_STATUS;
```

### 12311.js L906-L944
Original bundle byte offset: `989067` (`0xf178b`).
```javascript
  906 |             this.notifyPrintingJobStatus(thisJobState);
  907 |           } else {
  908 |             _HTClassicBluetooth.default.getInstance().getJobInfo(this.currentJob.print_job_id).then(function (result) {
  909 |               _this14.log("BluetoothPrintManager --> \u83B7\u53D6\u4EFB\u52A1\u4FE1\u606F\u6210\u529F = " + JSON.stringify(result));
  910 | 
  911 |               if (result && result.result && result.result[0]) {
  912 |                 var _thisJobState = result.result[0];
  913 | 
  914 |                 if (_thisJobState.job_state == _consts.MINT_JOB_STATUS.PRINTER_JOB_STATUS_ABORTED) {
  915 |                   _this14.log("BluetoothPrintManager --> \u5F53\u524D\u4EFB\u52A1\u72B6\u6001\u4E3Aaborted this.currentJob = " + JSON.stringify(_this14.currentJob));
  916 | 
  917 |                   _DynamicUtil.default.trackEvent(_consts.BIGDATA_KEYS.FW_PRINT_PROP_JOBINFO, _thisJobState);
  918 | 
  919 |                   if (_this14.currentJob) {
  920 |                     _this14.addErrorJob(_this14.currentJob, 2);
  921 |                   }
  922 | 
  923 |                   _this14.currentJob = null;
  924 | 
  925 |                   _miot.Host.storage.set(_this14.currentJobKey, _this14.currentJob);
  926 | 
  927 |                   _miot.Host.storage.set(_this14.localJobsKey, _this14.localJobs);
  928 | 
  929 |                   _miot.Host.storage.set(_this14.errorJobsKey, _this14.errorJobs);
  930 |                 } else if (_thisJobState.job_state == _consts.MINT_JOB_STATUS.PRINTER_JOB_STATUS_FINISHED || _thisJobState.job_state == _consts.MINT_JOB_STATUS.PRINTER_JOB_STATUS_CANCELED) {
  931 |                   _this14.log("BluetoothPrintManager --> \u5F53\u524D\u4EFB\u52A1\u72B6\u6001\u4E3A" + _thisJobState.job_state + " this.currentJob = " + JSON.stringify(_this14.currentJob));
  932 | 
  933 |                   _DynamicUtil.default.trackEvent(_consts.BIGDATA_KEYS.FW_PRINT_PROP_JOBINFO, _thisJobState);
  934 | 
  935 |                   _this14.currentJob = null;
  936 | 
  937 |                   _miot.Host.storage.set(_this14.currentJobKey, _this14.currentJob);
  938 |                 } else {
  939 |                   _this14.log("BluetoothPrintManager->\u5F53\u524D\u4EFB\u52A1\u72B6\u6001\u5F02\u5E381");
  940 | 
  941 |                   if (_this14.currentJob) {
  942 |                     _this14.addErrorJob(_this14.currentJob, 3);
  943 |                   }
  944 | 
```

### 12311.js L694-L706
Original bundle byte offset: `979807` (`0xef35f`).
```javascript
  694 |         if (state.category === _consts.MINT_PRINTER_STATUS_CATEGORY.PRINTER_STATUS_CATEGORY_IDLE && !state.hasOwnProperty("job_id")) {
  695 |           this.updateQuene();
  696 |         } else if (state.category === _consts.MINT_PRINTER_STATUS_CATEGORY.PRINTER_STATUS_CATEGORY_PROCESSING) {
  697 |             this.allowToPrint = true;
  698 | 
  699 |             if (this.currentJob && this.currentJob.print_job_id > 0 && state.job_id && state.job_id > 0 && this.currentJob.print_job_id != state.job_id) {
  700 |               this.reportLog("\u6253\u5370\u673A\u5DF2\u7ECF\u5728\u6253\u5370\u4E0B\u4E00\u4E2A\u4EFB\u52A1\uFF0C\u4E24\u4E2A\u4EFB\u52A1\u4E4B\u95F4\u6CA1\u6709\u51FA\u73B0idle\u72B6\u6001\u3002\u9700\u8981\u66F4\u65B0\u961F\u5217\u4EFB\u52A1\u72B6\u6001");
  701 |               this.reportLog("this.currentJob = " + JSON.stringify(this.currentJob));
  702 |               this.updateQuene();
  703 |             }
  704 |           } else {
  705 |             this.log("BluetoothPrintManager --> \u5F53\u524D\u8BBE\u5907\u72B6\u6001\u4E0B\uFF0C\u4E0D\u9700\u8981\u66F4\u65B0\u961F\u5217 this.currentJob = " + JSON.stringify(this.currentJob));
  706 |           }
```
