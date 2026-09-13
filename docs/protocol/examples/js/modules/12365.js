__d(function (global, _$$_REQUIRE, _$$_IMPORT_DEFAULT, _$$_IMPORT_ALL, module, exports, _dependencyMap) {
  var _interopRequireDefault = _$$_REQUIRE(_dependencyMap[0]);

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  exports.default = undefined;

  var _aesJs = _interopRequireDefault(_$$_REQUIRE(_dependencyMap[1]));

  var crypto = {
    encryptECB: function encryptECB(key, data) {
      var fillLength = data.length % 16 === 0 ? 0 : 16 - data.length % 16;
      var newData = new Uint8Array(data.length + fillLength);

      for (var i = 0; i < data.length; i++) {
        newData[i] = data[i];
      }

      var aesEcb = new _aesJs.default.ModeOfOperation.ecb(key);
      return aesEcb.encrypt(newData);
    },
    decryptECB: function decryptECB(key, data, remove) {
      var aesEcb = new _aesJs.default.ModeOfOperation.ecb(key);
      var decodeData = aesEcb.decrypt(data);

      if (!remove) {
        return decodeData;
      }

      var removeLength = 0;

      for (var i = decodeData.length - 1; i >= 0; i--) {
        if (decodeData[i] !== 0) break;
        removeLength++;
      }

      var newDecodeData = decodeData.subarray(0, decodeData.length - removeLength);
      return newDecodeData;
    }
  };
  var _default = crypto;
  exports.default = _default;
},12365,[14314,12308]);