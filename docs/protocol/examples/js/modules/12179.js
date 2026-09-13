__d(function (global, _$$_REQUIRE, _$$_IMPORT_DEFAULT, _$$_IMPORT_ALL, module, exports, _dependencyMap) {
  var _interopRequireDefault = _$$_REQUIRE(_dependencyMap[0]);

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  exports.default = undefined;

  var _deviceConfig = _interopRequireDefault(_$$_REQUIRE(_dependencyMap[1]));

  var _deviceConfig2 = _interopRequireDefault(_$$_REQUIRE(_dependencyMap[2]));

  var _deviceConfig3 = _interopRequireDefault(_$$_REQUIRE(_dependencyMap[3]));

  var _deviceConfig4 = _interopRequireDefault(_$$_REQUIRE(_dependencyMap[4]));

  var _miot = _$$_REQUIRE(_dependencyMap[5]);

  var modelMap = {
    'hannto.printer.rmy': _deviceConfig.default,
    'hannto.printer.rmyty': _deviceConfig.default,
    'hannto.printer.rsmycr': _deviceConfig.default,
    'hannto.printer.lager': _deviceConfig2.default,
    'xiaomi.printer.mintg': _deviceConfig3.default,
    'xiaomi.printer.ricott': _deviceConfig4.default,
    'xiaomi.printer.ricotg': _deviceConfig4.default,
    'xiaomi.printer.ricotp': _deviceConfig4.default
  };
  var _default = {
    getDeviceBluetoothName: function getDeviceBluetoothName() {
      if (this.isMint()) {
        return "Xiaomi...Photo Printer 1S-";
      } else if (this.isRicotta()) {
        return "米家口袋打印机Pro-";
      } else if (this.isRicottaG()) {
        return "Xiaomi Photo Printer Pro-";
      } else if (this.isRicottaP()) {
        return "随身拍套装-";
      } else {
        return "米家口袋打印机Pro-";
      }
    },
    getDeviceName: function getDeviceName() {
      if (this.isRicottaG()) {
        return 'Xiaomi Portable Photo Printer Pro';
      } else if (this.isRicotta()) {
        return '米家口袋打印机Pro';
      } else if (this.isRicottaP()) {
        return '随身拍套装';
      } else if (this.isMint()) {
        return 'Xiaomi Portable Photo Printer 1S';
      } else {
        return '';
      }
    },
    isPhotoPrinter: function isPhotoPrinter() {
      return _miot.Device.model == 'xiaomi.printer.mintg' || _miot.Device.model == 'xiaomi.printer.ricott' || _miot.Device.model == 'xiaomi.printer.ricotg' || _miot.Device.model == 'xiaomi.printer.ricotp';
    },
    isOverseaModel: function isOverseaModel() {
      return _miot.Device.model == 'xiaomi.printer.mintg' || _miot.Device.model == 'xiaomi.printer.ricotg';
    },
    isRicottaType: function isRicottaType() {
      return _miot.Device.model == 'xiaomi.printer.ricott' || _miot.Device.model == 'xiaomi.printer.ricotg' || _miot.Device.model == 'xiaomi.printer.ricotp';
    },
    isMint: function isMint() {
      return _miot.Device.model == 'xiaomi.printer.mintg';
    },
    isRicotta: function isRicotta() {
      return _miot.Device.model == 'xiaomi.printer.ricott';
    },
    isRicottaG: function isRicottaG() {
      return _miot.Device.model == 'xiaomi.printer.ricotg';
    },
    isRicottaP: function isRicottaP() {
      return _miot.Device.model == 'xiaomi.printer.ricotp';
    },
    getModelConfig: function getModelConfig() {
      var model = _miot.Device.model;
      return modelMap[model];
    },
    getDeviceModel: function getDeviceModel() {
      var model = _miot.Device.model;
      return model;
    },
    getSupportMediaList: function getSupportMediaList(isPhoto) {
      return this.getModelConfig().getSupportMediaList(isPhoto);
    },
    getSupportMediaTypeListFromMediaSize: function getSupportMediaTypeListFromMediaSize(isPhoto, mediaSize) {
      var supportList = this.getModelConfig().getSupportMediaList(isPhoto).mediaList;
      var supportMediaType = [];
      supportList.forEach(function (element) {
        if (element.mediaSize == mediaSize) {
          supportMediaType = element.mediaType;
        }
      });
      return supportMediaType;
    },
    getDefaultMediaTypeListFromMediaSize: function getDefaultMediaTypeListFromMediaSize(isPhoto, mediaSize) {
      var supportList = this.getModelConfig().getSupportMediaList(isPhoto).mediaList;
      var defaultMediaType = [];
      supportList.forEach(function (element) {
        if (element.mediaSize == mediaSize) {
          defaultMediaType = element.default;
        }
      });
      return defaultMediaType;
    },
    getImageBorderInfo: function getImageBorderInfo() {
      return this.getModelConfig().getImageBorderInfo();
    },
    getMediaMargin_top: function getMediaMargin_top() {
      return this.getModelConfig().getMediaMargin_top();
    },
    getMediaMargin_left: function getMediaMargin_left() {
      return this.getModelConfig().getMediaMargin_left();
    },
    getMediaMargin_bottom: function getMediaMargin_bottom() {
      return this.getModelConfig().getMediaMargin_bottom();
    },
    getMediaMargin_right: function getMediaMargin_right() {
      return this.getModelConfig().getMediaMargin_right();
    },
    getMediaMargin_border: function getMediaMargin_border(isPhoto) {
      return this.getModelConfig().getMediaMargin_border(isPhoto);
    }
  };
  exports.default = _default;
},12179,[14314,12182,12185,12188,12191,10074]);