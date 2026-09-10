package io.github.wa_otomia.darkroom.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.wa_otomia.darkroom.R
import io.github.wa_otomia.darkroom.ui.theme.Amber
import io.github.wa_otomia.darkroom.ui.theme.Danger
import io.github.wa_otomia.darkroom.ui.theme.MonoFont
import io.github.wa_otomia.darkroom.ui.theme.PaperDim

/*
 * The data and core layers throw plain exceptions carrying Chinese prose. This
 * file is the single place that turns those messages back into localised
 * strings.
 *
 * Matching on message text is fragile by nature, so three things keep it from
 * failing silently:
 *
 *  1. Needles are short, distinctive fragments rather than whole sentences, and
 *     both sides are normalised (lower-cased, whitespace stripped) before
 *     comparison. Re-punctuating or re-spacing a message upstream no longer
 *     breaks the mapping.
 *  2. Where the thrown text is ASCII (the printer protocol, OkHttp, java.io)
 *     the ASCII fragment is matched directly, which is far more stable than
 *     prose.
 *  3. Anything that still fails to match resolves to `ds_error_generic` instead
 *     of being rendered raw. The original text stays available through
 *     [userErrorDetail] / [UserErrorMessage].
 *
 * Every rule notes where its message is thrown. If a message is reworded
 * upstream and the needle no longer appears, the user sees the generic line —
 * never a stack-trace fragment.
 */

private fun normalize(raw: String): String = raw.lowercase().filterNot { it.isWhitespace() }

private class Rule(val res: Int, vararg needles: String) {
    val needles: List<String> = needles.map(::normalize)
}

/**
 * Ordered: the first rule with a matching needle wins, so more specific rules
 * come first. Two orderings are load-bearing and marked below.
 */
private val RULES: List<Rule> = listOf(
    // --- Print job outcome -------------------------------------------------
    // core/Protocol.kt JOB_STATE_USER_MESSAGE["aborted"]. Its own text lists
    // 缺纸 as a possible cause, so it MUST precede the no-paper rule.
    Rule(R.string.ds_error_print_aborted, "打印任务被中断"),
    // core/Protocol.kt JOB_STATE_USER_MESSAGE["failed"|"error"] and the
    // "打印失败：<state>" fallback in jobErrorMessage(). Its text mentions
    // 相纸, so it MUST precede the no-paper rule too.
    Rule(R.string.ds_error_print_failed, "打印机报告", "打印失败"),
    // data/jobs/AiJobs.kt cancel emits Failed(..., "") — Studio maps that to
    // 已取消生成. Must precede the print-cancel needle, which is a prefix.
    Rule(R.string.ai_cancelled, "已取消生成", "已取消修图", "ai cancelled"),
    // data/printer/PrintQueue.kt, data/printer/XiaomiPrinter.kt error("已取消"),
    // core/Protocol.kt JOB_STATE_USER_MESSAGE["cancelled"|"cancel"].
    Rule(R.string.print_cancelled, "已取消", "已请求取消", "cancelled"),
    // data/printer/PrintQueue.kt cancel() with no running job.
    Rule(R.string.cancel_no_active, "没有进行中的打印", "no print in progress"),
    // core/Protocol.kt NO_PAPER_MESSAGE, NoPaperError, PAPER_HINT matches.
    Rule(
        R.string.ds_error_no_paper,
        "缺纸",
        "no paper",
        "out of paper",
        "paper empty",
        "load paper",
        "NoPaperError",
    ),
    // data/printer/XiaomiPrinter.kt waitForJob() overall deadline.
    Rule(R.string.ds_error_print_timeout, "打印超时"),
    // core/Protocol.kt formatPrintJobError().
    Rule(R.string.ds_error_print_job_create, "无法创建打印任务", "print_job failed"),

    // --- Printer transport -------------------------------------------------
    // data/printer/PrintQueue.kt error("打印机未绑").
    Rule(R.string.error_printer_unbound, "打印机未绑", "printer not bound"),
    // data/printer/BluetoothTransport.kt error("找不到蓝牙设备").
    Rule(R.string.ds_error_bluetooth_device_missing, "找不到蓝牙设备", "bluetooth device not found"),
    // data/printer/BluetoothTransport.kt connect() when the adapter is disabled.
    Rule(R.string.ds_error_bluetooth_off, "蓝牙未开启", "bluetooth is off"),
    // data/printer/BluetoothTransport.kt connect() wrap. Must precede
    // ds_error_network, which also matches timeout / connection refused.
    Rule(
        R.string.ds_error_bluetooth_connect,
        "无法连接蓝牙设备",
        "unable to connect bluetooth",
    ),
    // data/printer/XiaomiPrinter.kt handshake and RPC plumbing, plus the
    // frame/crypto invariants in core/Protocol.kt and core/Crypto.kt.
    Rule(
        R.string.ds_error_printer_offline,
        "not connected",
        "printer rejected handshake",
        "bad server hello",
        "server hello body",
        "expected frame not received",
        "no response to",
        "frame too short",
        "missing 0x7e",
        "invalid dh modulus",
        "HandshakeError",
    ),
    // data/printer/XiaomiPrinter.kt readFrame(), data/printer/BluetoothTransport.kt.
    // Must precede the generic network rule, which also matches "timeout".
    Rule(R.string.ds_error_printer_timeout, "read timeout"),

    // --- AI image providers ------------------------------------------------
    // data/ai/AiImageClient.kt and data/printer/PrintQueue.kt missing-key guards.
    // Keep the old Grok needles so historical logs still map.
    Rule(
        R.string.error_grok_key,
        "未配置 grok",
        "未设置 grok",
        "grok api key",
        "未配置 ai",
        "未设置 ai",
        "未配置 AI",
    ),
    Rule(
        R.string.ds_error_grok_auth,
        "unauthorized",
        "invalid api key",
        "invalid_api_key",
        "incorrect api key",
        "authentication",
        "forbidden",
    ),
    Rule(R.string.ds_error_grok_model, "model_not_found", "model not found"),
    Rule(
        R.string.ds_error_grok_rate_limit,
        "rate limit",
        "too many requests",
        "quota",
        "insufficient balance",
    ),
    // data/ai/AiImageClient.kt image extraction.
    Rule(
        R.string.ds_error_grok_no_image,
        "grok 没有返回图片",
        "无法下载 grok 返回的图片",
        "没有返回图片",
        "无法下载返回的图片",
        "no image returned",
    ),
    Rule(R.string.ds_error_grok_failed, "grok 修图失败", "修图失败"),

    // --- Preset validation -------------------------------------------------
    // core/PhotoLineage.kt.
    Rule(R.string.error_choose_preset_or_prompt, "请选择预设或填写", "choose a preset"),
    // core/AiPresets.kt validatePreset().
    Rule(R.string.error_preset_title_required, "请填写预设标题"),
    Rule(R.string.error_preset_prompt_required, "请填写预设提示词"),
    Rule(R.string.error_preset_title_too_long, "标题最多"),
    Rule(R.string.error_preset_prompt_too_long, "提示词最多"),
    // data/settings/SettingsRepository.kt, data/printer/PrintQueue.kt.
    Rule(R.string.error_preset_missing, "预设不存在", "preset not found"),

    // --- Catalog and imaging ----------------------------------------------
    // data/catalog/CatalogRepository.kt.
    Rule(R.string.error_photo_missing, "照片不存在", "photo not found"),
    // These two MUST precede error_not_jpeg: their text ends in "不是 JPEG".
    Rule(R.string.error_edit_not_jpeg, "修图结果不是 jpeg"),
    Rule(R.string.error_generate_not_jpeg, "生成结果不是 jpeg"),
    Rule(R.string.error_jpg_only, "只处理 jpg", "jpg only"),
    Rule(R.string.error_not_jpeg, "不是 jpeg", "not a jpeg"),
    // data/imaging/ImagePipeline.kt, plus the SOF scan in core/PrintCrop.kt.
    Rule(R.string.error_decode_jpeg, "无法解码 jpeg", "no sof marker", "bad segment at"),
    // data/catalog/CatalogRepository.kt ingest of a truncated JPEG.
    Rule(R.string.transfer_error_incomplete, "文件不完整"),
    // data/catalog/CatalogRepository.kt non-JPEG decode failure (A8).
    Rule(R.string.transfer_error_undecodable, "无法解码图片"),
    // data/transfer/TransferRegistry.kt stale Receiving sweeper.
    Rule(R.string.transfer_error_stalled, "传输中断"),
    Rule(R.string.error_encode_jpeg, "jpeg 编码失败"),

    // --- FTP ---------------------------------------------------------------
    // data/ftp/CameraFtpServer.kt.
    Rule(R.string.ds_error_ftp_bind, "ftp bind failed", "bind failed", "address already in use"),
    // ui/settings validation, kept here so the same text maps either way.
    Rule(R.string.error_ftp_port, "端口须为", "端口需在", "port must be"),
    Rule(R.string.error_ftp_password, "密码留空或至少", "密码需留空", "password must be"),

    // --- Platform ----------------------------------------------------------
    // java.io failures reaching the UI from the catalog or the print pipeline.
    Rule(
        R.string.ds_error_storage,
        "filenotfoundexception",
        "no such file",
        "no space left",
        "enospc",
        "permission denied",
        "eacces",
        "read-only file system",
    ),
    // OkHttp / java.net. Last, so the printer rules above claim their timeouts.
    Rule(
        R.string.ds_error_network,
        "unable to resolve host",
        "failed to connect",
        "unknownhost",
        "sockettimeout",
        "timeout",
        "connection reset",
        "connection refused",
        "network is unreachable",
        "software caused connection abort",
        "sslexception",
        "sslhandshake",
    ),
)

fun userErrorRes(message: String?): Int? {
    val normalized = normalize(message.orEmpty())
    if (normalized.isEmpty()) return null
    return RULES.firstOrNull { rule -> rule.needles.any { normalized.contains(it) } }?.res
}

/**
 * Technical text worth keeping behind a disclosure.
 *
 * Only returns something when nothing matched, i.e. exactly the case where the
 * user is shown the generic line and a developer still needs the original.
 */
fun userErrorDetail(message: String?): String? {
    if (message.isNullOrBlank()) return null
    if (userErrorRes(message) != null) return null
    return message.trim()
}

/**
 * Localised, presentable text for a thrown message.
 *
 * An unmatched message resolves to the generic line rather than the raw
 * exception text, so `FileNotFoundException: /data/...` never reaches the
 * screen. Prefer [UserErrorMessage], which also offers the original text.
 */
@Composable
fun localizedUserError(message: String?): String? {
    if (message.isNullOrBlank()) return message
    val res = userErrorRes(message)
    return stringResource(res ?: R.string.ds_error_generic)
}

/** Job failure that must be read before the user continues. */
@Composable
fun UserErrorDialog(
    message: String?,
    onDismiss: () -> Unit,
    title: String = stringResource(R.string.job_error_title),
) {
    if (message.isNullOrBlank()) return
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { UserErrorMessage(message) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_back))
            }
        },
    )
}

/**
 * Renders a thrown message as an error line, with the untranslated original
 * available behind a disclosure when nothing matched.
 */
@Composable
fun UserErrorMessage(message: String?, modifier: Modifier = Modifier) {
    val shown = localizedUserError(message)
    val detail = userErrorDetail(message)
    var expanded by remember(message) { mutableStateOf(false) }
    if (!shown.isNullOrBlank()) {
        Column(modifier.fillMaxWidth()) {
            Text(shown, color = Danger)
            if (detail != null) {
                TextButton(onClick = { expanded = !expanded }) {
                    Text(
                        stringResource(
                            if (expanded) R.string.ds_error_details_hide else R.string.ds_error_details_show,
                        ),
                        color = Amber,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                    )
                }
                AnimatedVisibility(expanded) {
                    Text(
                        detail,
                        color = PaperDim,
                        fontFamily = MonoFont,
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                }
            }
        }
    }
}
