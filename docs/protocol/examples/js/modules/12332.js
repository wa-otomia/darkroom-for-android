__d(function (global, _$$_REQUIRE, _$$_IMPORT_DEFAULT, _$$_IMPORT_ALL, module, exports, _dependencyMap) {
  var _interopRequireDefault = _$$_REQUIRE(_dependencyMap[0]);

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  exports.default = undefined;

  var _consts = _$$_REQUIRE(_dependencyMap[1]);

  var _resources = _interopRequireDefault(_$$_REQUIRE(_dependencyMap[2]));

  var _ThirdPartyHeadFile = _$$_REQUIRE(_dependencyMap[3]);

  var _logUtils = _interopRequireDefault(_$$_REQUIRE(_dependencyMap[4]));

  var _PixelRatio = _$$_REQUIRE(_dependencyMap[5]);

  var lastErrorCode = 0;
  var _default = {
    deviceStatusEntity: function deviceStatusEntity(params) {
      var category = this.deviceCategoryEntity(params[0]);
      var subCategory = this.deviceSubCategoryEntity(params[1]);
      var jobType = params[5];
      var infos = this.deviceAlertsEntity(params[2].split(','));
      var warnnings = this.deviceAlertsEntity(params[3].split(','));
      var errors = this.deviceAlertsEntity(params[4].split(','), jobType);
      return {
        'category': category,
        'subCategory': subCategory,
        'infos': infos,
        'errors': errors,
        'warnnings': warnnings,
        'jobType': jobType
      };
    },
    deviceCategoryEntity: function deviceCategoryEntity(categoryCode) {
      var status = '';
      var description = '';

      switch (Number(categoryCode)) {
        case _consts.PRINTER_STATE.INITIALIZING:
          status = _resources.default.getString('initialize_title');
          description = _resources.default.getString('initialize_title');
          break;

        case _consts.PRINTER_STATE.READY:
          status = _resources.default.getString('standby_title');
          description = _resources.default.getString('standby_title');
          break;

        case _consts.PRINTER_STATE.SLEEP:
          status = _resources.default.getString('sleep_title');
          description = _resources.default.getString('sleep_title');
          break;

        case _consts.PRINTER_STATE.PROCESSING:
          status = _resources.default.getString('printer_busy_title');
          description = _resources.default.getString('printer_busy_title');
          break;

        case _consts.PRINTER_STATE.OFF:
          status = _resources.default.getString('offline_title');
          description = _resources.default.getString('offline_title');
          break;

        case _consts.PRINTER_STATE.ERROR:
          status = _resources.default.getString("printer_error");
          description = _resources.default.getString('printer_error');
          break;
      }

      return {
        'code': Number(categoryCode),
        'status': status,
        'description': description
      };
    },
    deviceSubCategoryEntity: function deviceSubCategoryEntity(subCategoryCode) {
      var status = '';
      var description = '';

      if (subCategoryCode !== '') {
        switch (Number(subCategoryCode)) {
          case _consts.PRINTER_SUB_STATE.INITIALIZING_NONE:
            status = _resources.default.getString('initialize_title');
            description = _resources.default.getString("initialing_no_more");
            break;

          case _consts.PRINTER_SUB_STATE.INITIALIZING_WARMINGUP:
            status = _resources.default.getString('initialize_title');
            description = _resources.default.getString("preheating");
            break;

          case _consts.PRINTER_SUB_STATE.IDLE_NONE:
            status = _resources.default.getString('standby_title');
            description = _resources.default.getString("in_free_no_more");
            break;

          case _consts.PRINTER_SUB_STATE.IDLE_INITIAL_SPITTING:
            status = _resources.default.getString('standby_title');
            description = _resources.default.getString("initialing_ciss");
            break;

          case _consts.PRINTER_SUB_STATE.IDLE_LOW_POWER:
            status = _resources.default.getString('standby_title');
            description = _resources.default.getString('low_power_mode');
            break;

          case _consts.PRINTER_SUB_STATE.PROCESSING_PRINTING:
            status = _resources.default.getString('printing_title');
            description = _resources.default.getString('printing_title');
            break;

          case _consts.PRINTER_SUB_STATE.PROCESSING_FILE_TRANSFERRING:
            status = _resources.default.getString('printing_title');
            description = _resources.default.getString("file_transferring");
            break;

          case _consts.PRINTER_SUB_STATE.PROCESSING_SCANNING:
            status = _resources.default.getString('scaning_title');
            description = _resources.default.getString('scaning_title');
            break;

          case _consts.PRINTER_SUB_STATE.PROCESSING_COPYING:
            status = _resources.default.getString('printer_busy_title');
            description = _resources.default.getString("copying");
            break;

          case _consts.PRINTER_SUB_STATE.PROCESSING_NOZZLE_CLEANING:
            status = _resources.default.getString('printer_busy_title');
            description = _resources.default.getString("print_head_clean");
            break;

          case _consts.PRINTER_SUB_STATE.PROCESSING_CANCELLING:
            status = _resources.default.getString("task_cancel");
            description = _resources.default.getString("task_cancel");
            break;

          case _consts.PRINTER_SUB_STATE.PROCESSING_UPGRADING:
            status = _resources.default.getString('fw_title');
            description = _resources.default.getString('scaning_title');
            break;

          case _consts.PRINTER_SUB_STATE.CALIBRATING:
            status = _resources.default.getString('align_process_txt');
            description = _resources.default.strings('align_process_txt');
            break;

          case _consts.PRINTER_SUB_STATE.SEMI_AUTO_PRINTING:
            status = _resources.default.getString("adjust_page_printing");
            description = _resources.default.getString("adjust_page_printing");
            break;

          case _consts.PRINTER_SUB_STATE.SEMI_AUTO_SCAN_REQUIRED:
            status = _resources.default.getString("adjust_page_done");
            description = _resources.default.getString("adjust_page_done");
            break;

          case _consts.PRINTER_SUB_STATE.SEMI_AUTO_SCANNING:
            status = _resources.default.getString("adjust_page_scanning");
            description = _resources.default.getString("adjust_page_scanning");
            break;

          case _consts.PRINTER_SUB_STATE.SCAN_WAITING:
            status = _resources.default.getString("scan_waiting");
            description = _resources.default.getString("scan_waiting");
            break;

          case _consts.PRINTER_SUB_STATE.COPY_WAITING:
            status = _resources.default.getString("copy_waiting");
            description = _resources.default.getString("copy_waiting");
            break;

          case _consts.PRINTER_SUB_STATE.RENDERING:
            status = _resources.default.getString("file_rendering");
            description = _resources.default.getString("file_rendering");
            break;

          case _consts.PRINTER_SUB_STATE.SLEEP_ENTERING:
            status = _resources.default.getString('sleep_title');
            description = _resources.default.getString("sleep_enter");
            break;

          case _consts.PRINTER_SUB_STATE.SLEEP_NOMAL:
            status = _resources.default.getString('sleep_title');
            description = _resources.default.getString("sleep_normal");
            break;

          case _consts.PRINTER_SUB_STATE.SLEEP_SILENT:
            status = _resources.default.getString("silent_mode");
            description = _resources.default.getString("silent_mode");
            break;

          case _consts.PRINTER_SUB_STATE.SLEEP_EXITING_SLEEP:
            status = _resources.default.getString("silent_mode_exit");
            description = _resources.default.getString("silent_mode_exit");
            break;

          case _consts.PRINTER_SUB_STATE.OFF_ENTERING:
            status = _resources.default.getString('offline_title');
            description = _resources.default.getString("shutdown_enter");
            break;

          case _consts.PRINTER_SUB_STATE.OFF_NOT_REAL:
            status = _resources.default.getString('offline_title');
            description = _resources.default.getString("turn_off_not_real");
            break;

          case _consts.PRINTER_SUB_STATE.OFF_SHUTTING_DOWN:
            status = _resources.default.getString('offline_title');
            description = _resources.default.getString("powering_off");
            break;

          case _consts.PRINTER_SUB_STATE.ERROR_NONE:
            status = _resources.default.getString("unkown_error");
            description = _resources.default.getString("unkown_error");
            break;

          case _consts.PRINTER_SUB_STATE.APPLYING_AI_URL:
            status = _resources.default.getString('printer_busy_title');
            description = _resources.default.getString("printer_busy_title");
            break;

          case _consts.PRINTER_SUB_STATE.APPLYING_AI_PROCESS:
            status = _resources.default.getString('printer_busy_title');
            description = _resources.default.getString("printer_busy_title");
            break;

          case _consts.PRINTER_SUB_STATE.CLOUD_AI_PROCESSING:
            status = _resources.default.getString('printer_busy_title');
            description = _resources.default.getString("printer_busy_title");
            break;

          default:
            status = _resources.default.getString("unknown");
            description = _resources.default.getString("unknown");
            break;
        }
      }

      return {
        'status': status,
        'code': Number(subCategoryCode),
        'description': description
      };
    },
    getErrorInfo: function getErrorInfo(code, sub_category) {
      var isCleaning = false;

      if (sub_category && sub_category == _consts.MINT_PRINTER_SUB_CATEGORY.PRINTER_STATUS_SUB_CATEGORY_CLEANING) {
        isCleaning = true;
      }

      var status = '';
      var subStatus = '';
      var helpButtonName = '';
      var isResume = false;
      var isHelp = false;
      var helpTitle = '';
      var description = '';
      var helpDesription = '';
      var releaseButtonName = '';

      switch (code) {
        case _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_COVER_OPEN:
          status = _resources.default.getString('ricotta_error_7001_title');
          subStatus = _resources.default.getString('ricotta_error_7001_subtitle');
          description = _resources.default.getString('ricotta_error_7001_content');
          isResume = false;
          isHelp = false;
          break;

        case _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_PAPER_LOAD:
          status = _resources.default.getString('ricotta_error_7103_title');
          subStatus = _resources.default.getString('ricotta_error_7103_subtitle');
          description = _resources.default.getString('ricotta_error_7103_content');
          isResume = true;
          isHelp = true;
          helpButtonName = _resources.default.getString('button_video');
          helpTitle = _resources.default.getString('ricotta_error_7103_title');
          helpDesription = _resources.default.getString('ricotta_error_7103_content');
          break;

        case _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_PAPER_JAM:
          status = _resources.default.getString('ricotta_error_7104_title');
          subStatus = _resources.default.getString('ricotta_error_7104_subtitle');
          description = _resources.default.getString('ricotta_error_7104_content');
          isResume = false;
          isHelp = true;
          helpButtonName = _resources.default.getString('button_video');
          helpTitle = _resources.default.getString('ricotta_error_7104_title');
          helpDesription = _resources.default.getString('ricotta_error_7104_content');
          break;

        case _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_PAPER_LENGTH_ERROR:
          status = _resources.default.getString('ricotta_error_7105_title');
          subStatus = _resources.default.getString('ricotta_error_7105_subtitle');
          description = _resources.default.getString('ricotta_error_7105_content');
          isResume = false;
          isHelp = true;
          helpButtonName = _resources.default.getString('button_video');
          helpTitle = _resources.default.getString('ricotta_error_7105_title');
          helpDesription = _resources.default.getString('ricotta_error_7105_content');
          break;

        case _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_PAPER_REMOVE:
          status = _resources.default.getString('ricotta_error_7110_title');
          subStatus = _resources.default.getString('ricotta_error_7110_title');
          description = _resources.default.getString('ricotta_error_7110_content');
          isResume = true;
          isHelp = false;
          break;

        case _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_PAPER_REMOVE_B:
          status = _resources.default.getString('ricotta_error_7111_title');
          subStatus = _resources.default.getString('ricotta_error_7111_subtitle');
          description = _resources.default.getString('ricotta_error_7111_content');
          isResume = true;
          isHelp = true;
          helpButtonName = _resources.default.getString('button_video');
          helpTitle = _resources.default.getString('ricotta_error_7111_title');
          helpDesription = _resources.default.getString('ricotta_error_7111_content');
          break;

        case _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_PAPER_REMOVE_PRINTING:
          status = _resources.default.getString('ricotta_error_7112_title');
          subStatus = _resources.default.getString('ricotta_error_7112_subtitle');
          description = _resources.default.getString('ricotta_error_7112_content');
          isResume = true;
          isHelp = false;
          break;

        case _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_PAPER_REMOVE_LOAD:
          status = _resources.default.getString('ricotta_error_7114_title');
          subStatus = _resources.default.getString('ricotta_error_7114_subtitle');
          description = _resources.default.getString('ricotta_error_7114_content');
          isResume = true;
          isHelp = true;
          helpButtonName = _resources.default.getString('button_video');
          helpTitle = _resources.default.getString('ricotta_error_7114_title');
          helpDesription = _resources.default.getString('ricotta_error_7114_content');
          break;

        case _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_RIBBON_END:
          status = _resources.default.getString('ricotta_error_7201_title');
          subStatus = _resources.default.getString('ricotta_error_7201_subtitle');
          description = _resources.default.getString('ricotta_error_7201_content');
          isResume = true;
          isHelp = false;
          break;

        case _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_NO_RIBBON_MARKER:
          status = _resources.default.getString('ricotta_error_7204_title');
          subStatus = _resources.default.getString('ricotta_error_7204_subtitle');
          description = _resources.default.getString('ricotta_error_7204_content');
          isResume = false;
          isHelp = true;
          helpButtonName = _resources.default.getString('button_video');
          helpTitle = _resources.default.getString('ricotta_error_7204_title');
          helpDesription = _resources.default.getString('ricotta_error_7204_content');
          break;

        case _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_RIBBON_ERROR:
          status = _resources.default.getString('ricotta_error_7205_title');
          subStatus = _resources.default.getString('ricotta_error_7205_subtitle');
          description = _resources.default.getString('ricotta_error_7205_content');
          isResume = false;
          isHelp = true;
          helpButtonName = _resources.default.getString('button_video');
          helpTitle = _resources.default.getString('ricotta_error_7205_title');
          helpDesription = _resources.default.getString('ricotta_error_7205_content');
          break;

        case _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_RIBBON_ERROR_2:
          status = _resources.default.getString('ricotta_error_7208_title');
          subStatus = _resources.default.getString('ricotta_error_7208_subtitle');
          description = _resources.default.getString('ricotta_error_7208_content');
          isResume = true;
          isHelp = true;
          helpButtonName = _resources.default.getString('button_video');
          helpTitle = _resources.default.getString('ricotta_error_7208_title');
          helpDesription = _resources.default.getString('ricotta_error_7208_content');
          break;

        case _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_PRINTER_OVERHEAT:
          status = _resources.default.getString('ricotta_error_7308_title');
          subStatus = _resources.default.getString('ricotta_error_7308_subtitle');
          description = _resources.default.getString('ricotta_error_7308_content');
          isResume = false;
          isHelp = false;
          break;

        case _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_PRINTER_OVERCOOL:
          status = _resources.default.getString('ricotta_error_7309_title');
          subStatus = _resources.default.getString('ricotta_error_7309_subtitle');
          description = _resources.default.getString('ricotta_error_7309_content');
          isResume = false;
          isHelp = false;
          break;

        case _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_PRINTER_BATTERY_CRITICAL:
          status = _resources.default.getString('ricotta_error_7310_title');
          subStatus = _resources.default.getString('ricotta_error_7310_subtitle');
          description = _resources.default.getString('ricotta_error_7310_content');
          isResume = false;
          isHelp = false;
          break;

        case _consts.MINT_PRINTER_ERROR_CODE.PRINTER_DEVICE_ERROR_PRINTER_BATTERY_OFF:
          status = _resources.default.getString('ricotta_error_7311_title');
          subStatus = _resources.default.getString('ricotta_error_7311_subtitle');
          description = _resources.default.getString('ricotta_error_7311_content');
          isResume = false;
          isHelp = false;
          break;

        default:
          status = _resources.default.getString("error_unenumerated");
          subStatus = _resources.default.getString("error_unenumerated");
          description = _resources.default.getString("error_unenumerated");
          isResume = true;
          break;
      }

      var errorInfo = {
        'code': code,
        'status': status,
        'subStatus': subStatus,
        'helpButtonName': helpButtonName,
        'description': description,
        'isResume': isResume,
        'isHelp': isHelp,
        'helpDesription': helpDesription,
        'releaseButtonName': releaseButtonName,
        'helpTitle': helpTitle
      };
      return errorInfo;
    },
    deviceAlertsEntity: function deviceAlertsEntity(codes, jobType) {
      var status = '';
      var subStatus = '';
      var alerts = [];
      var helpButtonName = '';
      var isResume = false;
      var isHelp = false;
      codes.map(function (item, index) {
        if (item !== '') {
          var description = '';
          var helpDesription = '';

          switch (Number(item)) {
            case _consts.PRINTER_STATE_ALERTS.ERR_SYSTEM_OUT_OF_MEMORY:
              status = _resources.default.getString('memory_out_title');
              subStatus = _resources.default.getString('reboot_txt');
              helpButtonName = _resources.default.getString('button_contact');
              description = '';
              isResume = false;
              isHelp = false;
              break;

            case _consts.PRINTER_STATE_ALERTS.ERR_SYSTEM_MEMORY_INVALID:
            case _consts.PRINTER_STATE_ALERTS.ERR_SYSTEM_FATAL_ERROR:
              status = _resources.default.getString('system_error_title');
              subStatus = _resources.default.getString('reboot_txt');
              helpButtonName = _resources.default.getString('button_contact');
              description = '';
              isResume = false;
              isHelp = false;
              break;

            case _consts.PRINTER_STATE_ALERTS.ERR_SYSTEM_SMALLBOOT:
              status = _resources.default.getString('ota_error_title');
              subStatus = _resources.default.getString('ota_error_txt');
              helpButtonName = _resources.default.getString('button_contact');
              description = '';
              isResume = false;
              isHelp = false;
              break;

            case _consts.PRINTER_STATE_ALERTS.ERR_IDS_CARTRIDGE_BLACK_ABSENT:
            case _consts.PRINTER_STATE_ALERTS.ERR_IDS_CARTRIDGE_CMY_ABSENT:
            case _consts.PRINTER_STATE_ALERTS.ERR_IDS_CARTRIDGE_BOTH_ABSENT:
              status = _resources.default.getString('cartridge_miss_title');

              if (item == _consts.PRINTER_STATE_ALERTS.ERR_IDS_CARTRIDGE_BLACK_ABSENT) {
                subStatus = _resources.default.getString('cartridge_missing_bw_sub') + '\n' + _resources.default.getString('print_head_error_txt');
              } else if (item == _consts.PRINTER_STATE_ALERTS.ERR_IDS_CARTRIDGE_CMY_ABSENT) {
                subStatus = _resources.default.getString('cartridge_missing_color_sub') + '\n' + _resources.default.getString('print_head_error_txt');
              } else {
                subStatus = _resources.default.getString('cartridge_missing_both_sub') + '\n' + _resources.default.getString('print_head_error_txt');
              }

              helpButtonName = _resources.default.getString('button_video');
              description = '';
              helpDesription = subStatus;
              isResume = false;
              isHelp = true;
              break;

            case _consts.PRINTER_STATE_ALERTS.ERR_IDS_CARTRIDGE_BLACK_HIGH_TEMP:
            case _consts.PRINTER_STATE_ALERTS.ERR_IDS_CARTRIDGE_CMY_HIGH_TEMP:
            case _consts.PRINTER_STATE_ALERTS.ERR_IDS_CARTRIDGE_BOTH_HIGH_TEMP:
              status = _resources.default.getString('cartridge_heated_title');

              if (item == _consts.PRINTER_STATE_ALERTS.ERR_IDS_CARTRIDGE_BLACK_HIGH_TEMP) {
                subStatus = _resources.default.getString('cartridge_heated_txt_new', _resources.default.getString('cartridge_k_txt'));
              } else if (item == _consts.PRINTER_STATE_ALERTS.ERR_IDS_CARTRIDGE_CMY_HIGH_TEMP) {
                subStatus = _resources.default.getString('cartridge_heated_txt_new', _resources.default.getString('cartridge_cmy_txt'));
              } else {
                subStatus = _resources.default.getString('cartridge_heated_txt_new', _resources.default.getString('cartridge_cmy_txt') + "\u3001" + _resources.default.getString('cartridge_k_txt'));
              }

              description = '';
              helpButtonName = _resources.default.getString('button_video');
              helpDesription = subStatus;
              isResume = false;
              isHelp = true;
              break;

            case _consts.PRINTER_STATE_ALERTS.ERR_IDS_CARTRIDGE_DEFECTIVE:
              status = _resources.default.getString('cartridge_short_title');
              subStatus = _resources.default.getString('cartridge_damaged_txt_new');
              description = '';
              helpButtonName = _resources.default.getString('button_video');
              helpDesription = subStatus;
              isResume = false;
              isHelp = true;
              break;

            case _consts.PRINTER_STATE_ALERTS.ERR_IDS_CARTRIDGE_BLACK_INCORRECT:
            case _consts.PRINTER_STATE_ALERTS.ERR_IDS_CARTRIDGE_CMY_INCORRECT:
            case _consts.PRINTER_STATE_ALERTS.ERR_IDS_CARTRIDGE_BOTH_INCORRECT:
              status = _resources.default.getString('cartridge_error_title');

              if (item == _consts.PRINTER_STATE_ALERTS.ERR_IDS_CARTRIDGE_BLACK_INCORRECT) {
                subStatus = _resources.default.getString('print_head_error_txt_new', "" + _resources.default.getString('cartridge_k_txt'));
              } else if (item == _consts.PRINTER_STATE_ALERTS.ERR_IDS_CARTRIDGE_CMY_INCORRECT) {
                subStatus = _resources.default.getString('print_head_error_txt_new', "" + _resources.default.getString('cartridge_cmy_txt'));
              } else {
                subStatus = _resources.default.getString('print_head_error_txt_new', _resources.default.getString('cartridge_cmy_txt') + "\u3001" + _resources.default.getString('cartridge_k_txt'));
              }

              description = '';
              helpButtonName = _resources.default.getString('button_video');
              helpDesription = subStatus;
              isResume = false;
              isHelp = true;
              break;

            case _consts.PRINTER_STATE_ALERTS.ERR_IDS_CARTRIDGE_BLACK_INCOMPITABLE:
            case _consts.PRINTER_STATE_ALERTS.ERR_IDS_CARTRIDGE_CMY_INCOMPITABLE:
            case _consts.PRINTER_STATE_ALERTS.ERR_IDS_CARTRIDGE_BOTH_INCOMPITABLE:
              status = _resources.default.getString('print_head_info_error_title');

              if (item == _consts.PRINTER_STATE_ALERTS.ERR_IDS_CARTRIDGE_BLACK_INCOMPITABLE) {
                subStatus = _resources.default.getString('print_head_error_txt_new', "" + _resources.default.getString('cartridge_k_txt'));
              } else if (item == _consts.PRINTER_STATE_ALERTS.ERR_IDS_CARTRIDGE_CMY_INCOMPITABLE) {
                subStatus = _resources.default.getString('print_head_error_txt_new', "" + _resources.default.getString('cartridge_cmy_txt'));
              } else {
                subStatus = _resources.default.getString('print_head_error_txt_new', _resources.default.getString('cartridge_cmy_txt') + "\u3001" + _resources.default.getString('cartridge_k_txt'));
              }

              description = '';
              helpButtonName = _resources.default.getString('button_video');
              helpDesription = subStatus;
              isResume = false;
              isHelp = true;
              break;

            case _consts.PRINTER_STATE_ALERTS.ERR_IDS_CARTRIDGE_BLACK_DEFECTIVE:
            case _consts.PRINTER_STATE_ALERTS.ERR_CARTRIDGE_CMY_DEFECTIVE:
            case _consts.PRINTER_STATE_ALERTS.ERR_IDS_CARTRIDGE_BLACK_ERROR:
            case _consts.PRINTER_STATE_ALERTS.ERR_IDS_CARTRIDGE_CMY_ERROR:
            case _consts.PRINTER_STATE_ALERTS.ERR_IDS_CARTRIDGE_BOTH_ERROR:
              status = _resources.default.getString('cartridge_error_title');

              if (item == _consts.PRINTER_STATE_ALERTS.ERR_IDS_CARTRIDGE_BLACK_DEFECTIVE || item == _consts.PRINTER_STATE_ALERTS.ERR_IDS_CARTRIDGE_BLACK_INCORRECT || item == _consts.PRINTER_STATE_ALERTS.ERR_IDS_CARTRIDGE_BLACK_ERROR || item == _consts.PRINTER_STATE_ALERTS.ERR_IDS_CARTRIDGE_BLACK_INCOMPITABLE) {
                subStatus = _resources.default.getString('cartridge_error_txt').replace('%s', _resources.default.getString('cartridge_k_txt'));
              } else if (item == _consts.PRINTER_STATE_ALERTS.ERR_CARTRIDGE_CMY_DEFECTIVE || item == _consts.PRINTER_STATE_ALERTS.ERR_IDS_CARTRIDGE_CMY_INCORRECT || item == _consts.PRINTER_STATE_ALERTS.ERR_IDS_CARTRIDGE_CMY_ERROR || item == _consts.PRINTER_STATE_ALERTS.ERR_IDS_CARTRIDGE_CMY_INCOMPITABLE) {
                subStatus = _resources.default.getString('cartridge_error_txt').replace('%s', _resources.default.getString('cartridge_cmy_txt'));
              } else {
                subStatus = _resources.default.getString('cartridge_error_txt').replace('%s', _resources.default.getString('pen_bw_color_txt'));
              }

              helpButtonName = _resources.default.getString('button_video');
              description = _resources.default.getString('error_help_txt');
              isResume = false;
              isHelp = false;
              break;

            case _consts.PRINTER_STATE_ALERTS.ERR_IDS_CARTRIDGE_BLACK_GAS_GAUGE_END:
            case _consts.PRINTER_STATE_ALERTS.ERR_IDS_CARTRIDGE_CMY_GAS_GAUGE_END:
            case _consts.PRINTER_STATE_ALERTS.ERR_IDS_CARTRIDGE_BOTH_GAS_GAUGE_END:
            case _consts.PRINTER_STATE_ALERTS.ERR_IDS_CARTRIDGE_BLACK_DETACH_END:
            case _consts.PRINTER_STATE_ALERTS.ERR_IDS_CARTRIDGE_CMY_DETACH_END:
            case _consts.PRINTER_STATE_ALERTS.ERR_IDS_CARTRIDGE_BOTH_DETACH_END:
            case _consts.PRINTER_STATE_ALERTS.ERR_IDS_CARTRIDGE_BLACK_PRIMING_END:
            case _consts.PRINTER_STATE_ALERTS.ERR_IDS_CARTRIDGE_CMY_PRIMING_END:
            case _consts.PRINTER_STATE_ALERTS.ERR_IDS_CARTRIDGE_BOTH_PRIMING_END:
              status = _resources.default.getString('cartridge_gauge_end_title');

              if (item == _consts.PRINTER_STATE_ALERTS.ERR_IDS_CARTRIDGE_BLACK_GAS_GAUGE_END || item == _consts.PRINTER_STATE_ALERTS.ERR_IDS_CARTRIDGE_BLACK_DETACH_END || item == _consts.PRINTER_STATE_ALERTS.ERR_IDS_CARTRIDGE_BLACK_PRIMING_END) {
                subStatus = _resources.default.getString('cartridge_error_txt').replace('%s', _resources.default.getString('cartridge_k_txt')) + '\n' + _resources.default.getString('print_head_error_help_txt');
              } else if (item == _consts.PRINTER_STATE_ALERTS.ERR_IDS_CARTRIDGE_CMY_GAS_GAUGE_END || item == _consts.PRINTER_STATE_ALERTS.ERR_IDS_CARTRIDGE_CMY_DETACH_END || item == _consts.PRINTER_STATE_ALERTS.ERR_IDS_CARTRIDGE_CMY_PRIMING_END) {
                subStatus = _resources.default.getString('cartridge_error_txt').replace('%s', _resources.default.getString('cartridge_cmy_txt')) + '\n' + _resources.default.getString('print_head_error_help_txt');
              } else {
                subStatus = _resources.default.getString('cartridge_error_txt').replace('%s', _resources.default.getString('pen_bw_color_txt')) + '\n' + _resources.default.getString('print_head_error_help_txt');
              }

              helpButtonName = _resources.default.getString('button_video');
              description = "";
              isResume = false;
              isHelp = false;
              break;

            case _consts.PRINTER_STATE_ALERTS.ERR_IDS_CARTRIDGE_CONTACT_FAILURE:
              status = _resources.default.getString('cartridge_contact_sub');
              subStatus = _resources.default.getString('cartridge_damaged_txt_new');
              helpButtonName = _resources.default.getString('button_video');
              description = "";
              helpDesription = subStatus;
              isResume = false;
              isHelp = true;
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
              status = _resources.default.getString('ink_empty_title');

              if (item == _consts.PRINTER_STATE_ALERTS.ERR_IDS_INK_BLACK_EMPTY) {
                subStatus = _resources.default.getString('ink_empty_txt').replace('%s', _resources.default.getString('ink_k_txt'));
              } else if (item == _consts.PRINTER_STATE_ALERTS.ERR_IDS_INK_CYAN_EMPTY) {
                subStatus = _resources.default.getString('ink_empty_txt').replace('%s', _resources.default.getString('ink_c_txt'));
              } else if (item == _consts.PRINTER_STATE_ALERTS.ERR_IDS_INK_KC_EMPTY) {
                subStatus = _resources.default.getString('ink_empty_txt').replace('%s', _ThirdPartyHeadFile.language.getString('ink_k_txt') + "\u3001" + _ThirdPartyHeadFile.language.getString('ink_c_txt'));
              } else if (item == _consts.PRINTER_STATE_ALERTS.ERR_IDS_INK_CM_EMPTY) {
                subStatus = _resources.default.getString('ink_empty_txt').replace('%s', _ThirdPartyHeadFile.language.getString('ink_c_txt') + "\u3001" + _ThirdPartyHeadFile.language.getString('ink_m_txt'));
              } else if (item == _consts.PRINTER_STATE_ALERTS.ERR_IDS_INK_MAGENTA_EMPTY) {
                subStatus = _resources.default.getString('ink_empty_txt').replace('%s', _resources.default.getString('ink_m_txt'));
              } else if (item == _consts.PRINTER_STATE_ALERTS.ERR_IDS_INK_KM_EMPTY) {
                subStatus = _resources.default.getString('ink_empty_txt').replace('%s', _ThirdPartyHeadFile.language.getString('pen_bw_txt') + "\u3001" + _ThirdPartyHeadFile.language.getString('ink_m_txt'));
              } else if (item == _consts.PRINTER_STATE_ALERTS.ERR_IDS_INK_KCM_EMPTY) {
                subStatus = _resources.default.getString('ink_empty_txt').replace('%s', _ThirdPartyHeadFile.language.getString('cartridge_k_txt') + "\u3001" + _ThirdPartyHeadFile.language.getString('ink_c_txt') + "\u3001" + _ThirdPartyHeadFile.language.getString('ink_m_txt'));
              } else if (item == _consts.PRINTER_STATE_ALERTS.ERR_IDS_INK_YELLOW_EMPTY) {
                subStatus = _resources.default.getString('ink_empty_txt').replace('%s', _resources.default.getString('ink_y_txt'));
              } else if (item == _consts.PRINTER_STATE_ALERTS.ERR_IDS_INK_KY_EMPTY) {
                subStatus = _resources.default.getString('ink_empty_txt').replace('%s', _ThirdPartyHeadFile.language.getString('cartridge_k_txt') + "\u3001" + _ThirdPartyHeadFile.language.getString('ink_y_txt'));
              } else if (item == _consts.PRINTER_STATE_ALERTS.ERR_IDS_INK_CY_EMPTY) {
                subStatus = _resources.default.getString('ink_empty_txt').replace('%s', _ThirdPartyHeadFile.language.getString('ink_c_txt') + "\u3001" + _ThirdPartyHeadFile.language.getString('ink_y_txt'));
              } else if (item == _consts.PRINTER_STATE_ALERTS.ERR_IDS_INK_KCY_EMPTY) {
                subStatus = _resources.default.getString('ink_empty_txt').replace('%s', _ThirdPartyHeadFile.language.getString('cartridge_k_txt') + "\u3001" + _ThirdPartyHeadFile.language.getString('ink_c_txt') + "\u3001" + _ThirdPartyHeadFile.language.getString('ink_y_txt'));
              } else if (item == _consts.PRINTER_STATE_ALERTS.ERR_IDS_INK_MY_EMPTY) {
                subStatus = _resources.default.getString('ink_empty_txt').replace('%s', _ThirdPartyHeadFile.language.getString('ink_m_txt') + "\u3001" + _ThirdPartyHeadFile.language.getString('ink_y_txt'));
              } else if (item == _consts.PRINTER_STATE_ALERTS.ERR_IDS_INK_KMY_EMPTY) {
                subStatus = _resources.default.getString('ink_empty_txt').replace('%s', _ThirdPartyHeadFile.language.getString('cartridge_k_txt') + "\u3001" + _ThirdPartyHeadFile.language.getString('ink_m_txt') + "\u3001" + _ThirdPartyHeadFile.language.getString('ink_y_txt'));
              } else if (item == _consts.PRINTER_STATE_ALERTS.ERR_IDS_INK_CMY_EMPTY) {
                subStatus = _resources.default.getString('ink_empty_txt').replace('%s', _ThirdPartyHeadFile.language.getString('ink_c_txt') + "\u3001" + _ThirdPartyHeadFile.language.getString('ink_m_txt') + "\u3001" + _ThirdPartyHeadFile.language.getString('ink_y_txt'));
              } else {
                subStatus = _resources.default.getString('ink_empty_txt').replace('%s', _ThirdPartyHeadFile.language.getString('cartridge_k_txt') + "\u3001" + _ThirdPartyHeadFile.language.getString('ink_c_txt') + "\u3001" + _ThirdPartyHeadFile.language.getString('ink_m_txt') + "\u3001" + _ThirdPartyHeadFile.language.getString('ink_y_txt'));
              }

              helpButtonName = _resources.default.getString('button_video');
              description = '';
              helpDesription = _resources.default.getString('change_ink_bottle_txt');
              isResume = false;
              isHelp = true;
              break;

            case _consts.PRINTER_STATE_ALERTS.ERR_IDS_LOI_SENSOR_ERROR:
              status = _resources.default.getString('LOI_sensor_error_title');
              subStatus = _resources.default.getString('LOI_sensor_error_txt');
              helpButtonName = _resources.default.getString('button_contact');
              description = '';
              isResume = false;
              isHelp = false;
              break;

            case _consts.PRINTER_STATE_ALERTS.WRN_IDS_INK_KCMY_LOW:
            case _consts.PRINTER_STATE_ALERTS.WRN_IDS_INK_CMY_LOW:
            case _consts.PRINTER_STATE_ALERTS.WRN_IDS_INK_KMY_LOW:
            case _consts.PRINTER_STATE_ALERTS.WRN_IDS_INK_MY_LOW:
            case _consts.PRINTER_STATE_ALERTS.WRN_IDS_INK_KCY_LOW:
            case _consts.PRINTER_STATE_ALERTS.WRN_IDS_INK_CY_LOW:
            case _consts.PRINTER_STATE_ALERTS.WRN_IDS_INK_KY_LOW:
            case _consts.PRINTER_STATE_ALERTS.WRN_IDS_INK_YELLOW_LOW:
            case _consts.PRINTER_STATE_ALERTS.WRN_IDS_INK_KCM_LOW:
            case _consts.PRINTER_STATE_ALERTS.WRN_IDS_INK_CM_LOW:
            case _consts.PRINTER_STATE_ALERTS.WRN_IDS_INK_BLACK_LOW:
            case _consts.PRINTER_STATE_ALERTS.WRN_IDS_INK_CYAN_LOW:
            case _consts.PRINTER_STATE_ALERTS.WRN_IDS_INK_KC_LOW:
            case _consts.PRINTER_STATE_ALERTS.WRN_IDS_INK_MAGENTA_LOW:
            case _consts.PRINTER_STATE_ALERTS.WRN_IDS_INK_KM_LOW:
              status = _resources.default.getString('ink_low_title');

              if (item == _consts.PRINTER_STATE_ALERTS.WRN_IDS_INK_CMY_LOW) {
                subStatus = _resources.default.getString('ink_low_txt', _resources.default.getString('ink_c_txt') + "\u3001" + _resources.default.getString("magenta") + "\u3001" + _resources.default.getString('ink_y_txt'));
              } else if (item == _consts.PRINTER_STATE_ALERTS.WRN_IDS_INK_KMY_LOW) {
                subStatus = _resources.default.getString('ink_low_txt', _resources.default.getString("magenta") + "\u3001" + _resources.default.getString('ink_y_txt') + "\u3001" + _resources.default.getString('cartridge_k_txt'));
              } else if (item == _consts.PRINTER_STATE_ALERTS.WRN_IDS_INK_MY_LOW) {
                subStatus = _resources.default.getString('ink_low_txt', _resources.default.getString("magenta") + "\u3001" + _resources.default.getString('ink_y_txt'));
              } else if (item == _consts.PRINTER_STATE_ALERTS.WRN_IDS_INK_KCY_LOW) {
                subStatus = _resources.default.getString('ink_low_txt', _resources.default.getString('ink_c_txt') + "\u3001" + _resources.default.getString('ink_y_txt') + "\u3001" + _resources.default.getString('cartridge_k_txt'));
              } else if (item == _consts.PRINTER_STATE_ALERTS.WRN_IDS_INK_CY_LOW) {
                subStatus = _resources.default.getString('ink_low_txt', _resources.default.getString('ink_c_txt') + "\u3001" + _resources.default.getString('ink_y_txt'));
              } else if (item == _consts.PRINTER_STATE_ALERTS.WRN_IDS_INK_KY_LOW) {
                subStatus = _resources.default.getString('ink_low_txt', _resources.default.getString('ink_y_txt') + "\u3001" + _resources.default.getString('cartridge_k_txt'));
              } else if (item == _consts.PRINTER_STATE_ALERTS.WRN_IDS_INK_YELLOW_LOW) {
                subStatus = _resources.default.getString('ink_low_txt', _resources.default.getString('ink_y_txt'));
              } else if (item == _consts.PRINTER_STATE_ALERTS.WRN_IDS_INK_KCM_LOW) {
                subStatus = _resources.default.getString('ink_low_txt', _resources.default.getString('ink_c_txt') + "\u3001" + _resources.default.getString("magenta") + "\u3001" + _resources.default.getString('cartridge_k_txt'));
              } else if (item == _consts.PRINTER_STATE_ALERTS.WRN_IDS_INK_CM_LOW) {
                subStatus = _resources.default.getString('ink_low_txt', _resources.default.getString('ink_c_txt') + "\u3001" + _resources.default.getString("magenta"));
              } else if (item == _consts.PRINTER_STATE_ALERTS.WRN_IDS_INK_BLACK_LOW) {
                subStatus = _resources.default.getString('ink_low_txt', _resources.default.getString('ink_k_txt'));
              } else if (item == _consts.PRINTER_STATE_ALERTS.WRN_IDS_INK_CYAN_LOW) {
                subStatus = _resources.default.getString('ink_low_txt', _resources.default.getString('ink_c_txt'));
              } else if (item == _consts.PRINTER_STATE_ALERTS.WRN_IDS_INK_KC_LOW) {
                subStatus = _resources.default.getString('ink_low_txt', _resources.default.getString('ink_c_txt') + "\u3001" + _resources.default.getString('cartridge_k_txt'));
              } else if (item == _consts.PRINTER_STATE_ALERTS.WRN_IDS_INK_MAGENTA_LOW) {
                subStatus = _resources.default.getString('ink_low_txt', _resources.default.getString('ink_m_txt'));
              } else if (item == _consts.PRINTER_STATE_ALERTS.WRN_IDS_INK_KM_LOW) {
                subStatus = _resources.default.getString('ink_low_txt', _resources.default.getString("magenta") + "\u3001" + _resources.default.getString('cartridge_k_txt'));
              } else {
                subStatus = _resources.default.getString('ink_low_txt', _resources.default.getString('ink_c_txt') + "\u3001" + _resources.default.getString("magenta") + "\u3001" + _resources.default.getString('ink_y_txt') + "\u3001" + _resources.default.getString('cartridge_k_txt'));
              }

              description = '';
              isResume = false;
              isHelp = false;
              break;

            case _consts.PRINTER_STATE_ALERTS.ERR_PFS_INVALID_ALIGNMENT_101:
            case _consts.PRINTER_STATE_ALERTS.ERR_PFS_INVALID_ALIGNMENT_1X0:
            case _consts.PRINTER_STATE_ALERTS.ERR_PFS_INVALID_ALIGNMENT_010:
              status = _resources.default.getString('paper_sensor_error_title');
              subStatus = _resources.default.getString('paper_sensor_error_txt');
              helpButtonName = _resources.default.getString('button_contact');
              description = '';
              isResume = false;
              isHelp = false;
              break;

            case _consts.PRINTER_STATE_ALERTS.ERR_PFS_PAPER_MISMATCH:
              status = _resources.default.getString('paper_match_error_title');
              subStatus = _resources.default.getString('paper_match_error_txt');
              description = '';
              isResume = false;
              isHelp = false;
              break;

            case _consts.PRINTER_STATE_ALERTS.ERR_PFS_PAPER_OUT:
              status = _resources.default.getString('OOP_title');
              subStatus = _resources.default.getString('OOP_txt');
              description = '';
              isResume = true;
              isHelp = false;
              break;

            case _consts.PRINTER_STATE_ALERTS.ERR_PFS_NO_PICK:
              status = _resources.default.getString('paper_pick_title');
              subStatus = _resources.default.getString('paper_pick_txt');
              description = '';
              isResume = true;
              isHelp = false;
              break;

            case _consts.PRINTER_STATE_ALERTS.ERR_PFS_PAPER_JAM:
              status = _resources.default.getString('paper_jam_sub');
              subStatus = _resources.default.getString('paper_jam_txt');
              helpButtonName = _resources.default.getString('button_video');
              description = '';
              helpDesription = _resources.default.getString('move_device_paper_jam');
              isResume = true;
              isHelp = true;
              break;

            case _consts.PRINTER_STATE_ALERTS.ERR_PFS_PAPER_SHORT:
              status = _resources.default.getString('paper_short_sub');
              subStatus = _resources.default.getString('paper_short_txt');
              description = '';
              isResume = true;
              isHelp = false;
              break;

            case _consts.PRINTER_STATE_ALERTS.ERR_TOP_COVER_OPEN_DURING_PROCESSING:
              status = _resources.default.getString('door_opened_title');
              subStatus = _resources.default.getString('door_close_txt');
              description = '';
              isResume = false;
              isHelp = false;
              break;

            case _consts.PRINTER_STATE_ALERTS.ERR_FEED_MOTOR_STALL:
              status = _resources.default.getString('feed_stall_title');
              subStatus = _resources.default.getString('feed_stall_txt');
              helpButtonName = _resources.default.getString('button_video');
              description = '';
              isResume = false;
              isHelp = false;
              break;

            case _consts.PRINTER_STATE_ALERTS.ERR_CARRIER_MOTOR_STALL:
            case _consts.PRINTER_STATE_ALERTS.ERR_CARRIER_MOTOR_LOCK:
              status = _resources.default.getString('carrier_blocked_sub');
              subStatus = _resources.default.getString('carrier_blocked_error_sub');
              helpButtonName = _resources.default.getString('button_video');
              description = '';
              isResume = true;
              isHelp = false;
              break;

            case _consts.PRINTER_STATE_ALERTS.ERR_SC_HOME_NOT_DETECTED:
              status = _resources.default.getString('scan_error_title');
              subStatus = _resources.default.getString('reboot_txt');
              helpButtonName = _resources.default.getString('button_contact');
              description = '';
              isResume = false;
              isHelp = false;
              break;

            case _consts.PRINTER_STATE_ALERTS.ERR_ALIGNMENT_PARAMS_NONE:
            case _consts.PRINTER_STATE_ALERTS.ERR_ALIGNMENT_FAILURE_AS_PRESCAN_TOO_FEW_PATTERN:
            case _consts.PRINTER_STATE_ALERTS.ERR_ALIGNMENT_FAILURE_AS_PRESCAN_TOO_MANY_PATTERN:
            case _consts.PRINTER_STATE_ALERTS.ERR_ALIGNMENT_FAILURE_AS_PRESCAN_NO_TOP_LEFT:
            case _consts.PRINTER_STATE_ALERTS.ERR_ALIGNMENT_FAILURE_AS_PRESCAN_NO_TOP_RIGHT:
            case _consts.PRINTER_STATE_ALERTS.ERR_ALIGNMENT_FAILURE_AS_PRESCAN_NO_BOTTOM_LEFT:
            case _consts.PRINTER_STATE_ALERTS.ERR_ALIGNMENT_FAILURE_AS_PRESCAN_NO_BOTTOM_RIGHT:
            case _consts.PRINTER_STATE_ALERTS.ERR_ALIGNMENT_FAILURE_AS_PRESCAN_NO_TOO_MUCH_SKEW:
            case _consts.PRINTER_STATE_ALERTS.ERR_ALIGNMENT_FAILURE_AS_MAINSCAN_NO_DIAG_PATTERN:
            case _consts.PRINTER_STATE_ALERTS.ERR_ALIGNMENT_FAILURE_AS_MAINSCAN_BIDI_XAO_C:
            case _consts.PRINTER_STATE_ALERTS.ERR_ALIGNMENT_FAILURE_AS_MAINSCAN_BIDI_XAO_M:
            case _consts.PRINTER_STATE_ALERTS.ERR_ALIGNMENT_FAILURE_AS_MAINSCAN_BIDI_XAO_Y:
            case _consts.PRINTER_STATE_ALERTS.ERR_ALIGNMENT_FAILURE_AS_MAINSCAN_BIDI_XAO_K:
            case _consts.PRINTER_STATE_ALERTS.ERR_ALIGNMENT_FAILURE_AS_MAINSCAN_BIDI_DRAFT_K:
            case _consts.PRINTER_STATE_ALERTS.ERR_ALIGNMENT_FAILURE_AS_MAINSCAN_BIDI_DRAFT_C:
            case _consts.PRINTER_STATE_ALERTS.ERR_ALIGNMENT_FAILURE_AS_MAINSCAN_H2H_HORIZENTAL_C:
            case _consts.PRINTER_STATE_ALERTS.ERR_ALIGNMENT_FAILURE_AS_MAINSCAN_H2H_HORIZENTAL_M:
            case _consts.PRINTER_STATE_ALERTS.ERR_ALIGNMENT_FAILURE_AS_MAINSCAN_H2H_HORIZENTAL_Y:
            case _consts.PRINTER_STATE_ALERTS.ERR_ALIGNMENT_FAILURE_AS_MAINSCAN_H2H_VERTICAL_C:
            case _consts.PRINTER_STATE_ALERTS.ERR_ALIGNMENT_FAILURE_AS_MAINSCAN_H2H_VERTICAL_M:
            case _consts.PRINTER_STATE_ALERTS.ERR_ALIGNMENT_FAILURE_AS_MAINSCAN_H2H_VERTICAL_Y:
            case _consts.PRINTER_STATE_ALERTS.ERR_ALIGNMENT_FAILURE_AS_MAINSCAN_SKEW_K:
              status = _resources.default.getString('align_failed_txt');
              subStatus = _resources.default.getString("semi_auto_print_fail");
              description = '';
              isResume = false;
              isHelp = false;
              break;

            case _consts.PRINTER_STATE_ALERTS.ERROR_SELF_CLEARING_ERR:
            case _consts.PRINTER_STATE_ALERTS.ERROR_APPLY_AI_URL_ERR:
            case _consts.PRINTER_STATE_ALERTS.ERROR_APPLY_AI_PROCESS_ERR:
            case _consts.PRINTER_STATE_ALERTS.ERROR_CLOUD_AI_PROCESS_ERR:
            case _consts.PRINTER_STATE_ALERTS.ERROR_FAIL_TO_CORRECTLY_RECOGNIZE_TEXTBOOK:
              if (jobType == _consts.JOB_TYPE.AI_EXAM_PAPER_REMOVE_HANDWEITTEN_JOB) {
                status = _resources.default.getString('ecn_error_title_jobtype_81');
                subStatus = _resources.default.getString('ecn_error_sub_title');
                description = _resources.default.getString('ecn_error_text_jobtype_81');
              } else if (jobType == _consts.JOB_TYPE.AI_TEXT_PREVIEW_JOB) {
                status = _resources.default.getString('ecn_error_title_jobtype_82');
                subStatus = _resources.default.getString('ecn_error_sub_title');
                description = _resources.default.getString('ecn_error_text_jobtype_82');
              } else {
                status = _resources.default.getString('ecn_error_title_jobtype_unknown');
                subStatus = _resources.default.getString('ecn_error_sub_title');
                description = _resources.default.getString('ecn_error_text_jobtype_unknown');
              }

              isResume = false;
              isHelp = false;
              break;

            default:
              status = _resources.default.getString("error_unenumerated");
              subStatus = _resources.default.getString("error_unenumerated");
              description = _resources.default.getString("error_unenumerated");
              isResume = true;
              break;
          }

          alerts[index] = {
            'code': Number(item),
            'status': status,
            'subStatus': subStatus,
            'helpButtonName': helpButtonName,
            'description': description,
            'isResume': isResume,
            'isHelp': isHelp,
            'helpDesription': helpDesription
          };
        }

        return index;
      });
      return alerts;
    },
    handleDeviceState: function handleDeviceState(navigation, printerStatus, isForce, resultCallBack) {
      if (printerStatus.category == _consts.MINT_PRINTER_STATUS_CATEGORY.PRINTER_STATUS_CATEGORY_ERROR) {
        var errorInfo = this.getErrorInfo(printerStatus.error, printerStatus.sub_category);
        errorInfo.isCleaning = printerStatus.sub_category == _consts.MINT_PRINTER_SUB_CATEGORY.PRINTER_STATUS_SUB_CATEGORY_CLEANING;
        var tempStatus = printerStatus;
        tempStatus.errorInfo = errorInfo;

        if (lastErrorCode !== tempStatus.errorInfo.code || global.errorViewVisible || isForce) {
          lastErrorCode = tempStatus.errorInfo.code;
          global.errorViewVisible = false;
          navigation.navigate('error', {
            error: tempStatus.errorInfo,
            callback: function callback(data, navigation) {
              resultCallBack.fromView(data, navigation);
            }
          });
        }
      } else {
        global.errorViewVisible = true;
        global.errorDialogVisible = true;
        resultCallBack.hideErrorDialog();
      }
    },
    navigateToError: function navigateToError(navigation, error) {
      navigation.navigate('error', {
        error: error
      });
    },
    navigateToErrorCallBack: function navigateToErrorCallBack(navigation, error, resultCallBack) {
      navigation.navigate('error', {
        error: error,
        callback: function callback(data, navigation) {
          resultCallBack.fromView(data, navigation);
        }
      });
    }
  };
  exports.default = _default;
},12332,[14314,12173,10077,10034,12194,10327]);