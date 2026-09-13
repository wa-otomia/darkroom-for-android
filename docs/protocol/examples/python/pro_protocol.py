"""Offline reference for the supplied Ricotta plugin. Not a hardware-tested driver.

The battery-level field is evidenced by the plugin's embedded sample only.
No function opens Bluetooth, connects to an account, prints, or cancels a job.
"""
from __future__ import annotations
import json
import math
import struct
from dataclasses import dataclass
from pathlib import Path
from typing import Any, Literal

from cryptography.hazmat.primitives.ciphers import Cipher, algorithms, modes

POLICY_PATH = Path(__file__).resolve().parents[1] / 'data' / 'official_error_policy.json'
ERROR_POLICY = {entry['code']: entry for entry in json.loads(POLICY_PATH.read_text(encoding='utf-8'))}
TERMINAL_JOB_STATES = {'finished': 'success', 'canceled': 'canceled', 'aborted': 'failed'}
ACTIVE_JOB_STATES = {'waiting', 'init', 'initlization', 'downloading', 'printing_Y',
                     'printing_M', 'printing_C', 'printing_OC', 'home_feed', 'cool_down'}


def _u32(value: int, name: str, nonzero: bool = True) -> int:
    if isinstance(value, bool) or not isinstance(value, int) or not (int(nonzero) <= value <= 0xFFFFFFFF):
        raise ValueError(f'{name} must be an integer in the supported uint32 range')
    return value


def request(method: str, request_id: int, params: dict[str, Any] | list[Any]) -> dict[str, Any]:
    _u32(request_id, 'request_id')
    return {'method': method, 'id': request_id, 'params': params}


def mixed_status_request(request_id: int) -> dict[str, Any]:
    return request('mixed_status', request_id, {})


def device_info_request(request_id: int) -> dict[str, Any]:
    return request('get_prop', request_id, ['device_info'])


def job_info_request(request_id: int, job_id: int) -> dict[str, Any]:
    return request('job_info', request_id, [_u32(job_id, 'job_id')])


def resume_request(request_id: int) -> dict[str, Any]:
    return request('resume_printer', request_id, {})


def cancel_request(request_id: int, job_id: int) -> dict[str, Any]:
    return request('cancel_job', request_id, [_u32(job_id, 'job_id')])


def print_job_request(request_id: int, file_size: int, copies: int = 1,
                      job_type: int = 0, channel: int = 64) -> dict[str, Any]:
    """JSON platform field. Official printJob2: iOS=64, Android=576. Not frame channel 3/4."""
    if copies < 1:
        raise ValueError('copies must be >= 1')
    return request('print_job', request_id, {
        'file_size': _u32(file_size, 'file_size'),
        'copies': copies,
        'job_type': job_type,
        'channel': channel,
    })


def file_chunk(job_id: int, data: bytes, request_id: int, key: bytes,
               package_total: int, package_number: int) -> bytes:
    """Channel 4 / encoding HEX. Payload is job_id u32 LE + up to 988 file bytes."""
    if not (1 <= len(data) <= 988):
        raise ValueError('File chunk must be 1..988 bytes')
    payload = struct.pack('<I', _u32(job_id, 'job_id')) + data
    return build_frame(
        encrypt_ecb(key, payload), request_id,
        channel=4, interactive=6, encoding=2,
        package_total=package_total, package_number=package_number,
    )


def encrypt_ecb(key: bytes, body: bytes) -> bytes:
    if len(key) != 16:
        raise ValueError('Expected 16-byte session key')
    padded = body + b'\0' * (-len(body) % 16)
    encryptor = Cipher(algorithms.AES(key), modes.ECB()).encryptor()
    return encryptor.update(padded) + encryptor.finalize()


def decrypt_ecb(key: bytes, body: bytes, strip_json_padding: bool = False) -> bytes:
    if len(key) != 16 or len(body) % 16:
        raise ValueError('Invalid AES key or ciphertext length')
    decryptor = Cipher(algorithms.AES(key), modes.ECB()).decryptor()
    data = decryptor.update(body) + decryptor.finalize()
    return data.rstrip(b'\0') if strip_json_padding else data


def build_frame(body: bytes, request_id: int, *, channel: int = 3, interactive: int = 6,
                encoding: int = 3, encrypt_type: int = 5, package_total: int = 0,
                package_number: int = 0, arc_msg_sn: int | None = None) -> bytes:
    _u32(request_id, 'request_id')
    if arc_msg_sn is None:
        arc_msg_sn = request_id
    _u32(arc_msg_sn, 'arc_msg_sn', nonzero=False)
    if len(body) > 992:
        raise ValueError('Official parser accepts at most 992 body bytes per frame')
    if not (0 <= channel <= 255 and 0 <= interactive <= 255 and 0 <= encoding <= 255):
        raise ValueError('Invalid single-byte frame field')
    if not (0 <= encrypt_type <= 6 and 0 <= package_total <= 65535 and 0 <= package_number <= 65535):
        raise ValueError('Invalid frame attributes')
    attribute = len(body) | (encrypt_type << 10) | (0x2000 if package_total > 1 else 0)
    header = struct.pack('<6BIIHHH', 0x7E, 0x64, 0, channel, interactive, encoding,
                         arc_msg_sn, request_id, package_total, package_number, attribute)
    checksum = sum(header[1:] + body) & 0xFF
    return header + body + bytes([checksum, 0x7E])


def encode_request(message: dict[str, Any], key: bytes) -> bytes:
    if not isinstance(message.get('params'), (list, dict)):
        raise ValueError('params must preserve its array/object type')
    plain = json.dumps(message, ensure_ascii=False, separators=(',', ':')).encode('utf-8')
    return build_frame(encrypt_ecb(key, plain), message['id'])


@dataclass(frozen=True)
class Frame:
    raw: bytes
    channel: int
    interactive: int
    encoding: int
    arc_msg_sn: int
    msg_sn: int
    package_total: int
    package_number: int
    encrypt_type: int
    body: bytes


def parse_frame(data: bytes) -> Frame:
    if len(data) < 22 or data[:3] != b'\x7e\x64\x00':
        raise ValueError('Invalid header')
    attribute = int.from_bytes(data[18:20], 'little')
    length = attribute & 0x3FF
    if length > 992 or len(data) != 22 + length or data[-1] != 0x7E:
        raise ValueError('Invalid body length or frame tail')
    if (sum(data[1:-2]) & 0xFF) != data[-2]:
        raise ValueError('Invalid checksum')
    arc, sn, total, number = struct.unpack_from('<IIHH', data, 6)
    return Frame(data, data[3], data[4], data[5], arc, sn, total, number,
                 (attribute >> 10) & 7, data[20:-2])


class StreamDecoder:
    """Length-driven framing. A 0x7E within ciphertext is not a separator."""
    def __init__(self) -> None:
        self.buffer = bytearray()
        self.rejected_frames = 0

    def feed(self, data: bytes) -> list[Frame]:
        self.buffer.extend(data)
        frames: list[Frame] = []
        while len(self.buffer) >= 3:
            pos = self.buffer.find(b'\x7e\x64\x00')
            if pos < 0:
                self.buffer[:] = self.buffer[-2:]
                break
            del self.buffer[:pos]
            if len(self.buffer) < 20:
                break
            size = int.from_bytes(self.buffer[18:20], 'little') & 0x3FF
            if size > 992:
                del self.buffer[0]
                self.rejected_frames += 1
                continue
            total = size + 22
            if len(self.buffer) < total:
                break
            try:
                parsed = parse_frame(bytes(self.buffer[:total]))
            except ValueError:
                del self.buffer[0]
                self.rejected_frames += 1
                continue
            frames.append(parsed)
            del self.buffer[:total]
        return frames


def decode_json(frame: Frame, key: bytes) -> dict[str, Any]:
    if frame.channel != 3 or frame.encoding != 3 or frame.encrypt_type != 5:
        raise ValueError('Not an AES-ECB JSON data-channel frame')
    value = json.loads(decrypt_ecb(key, frame.body, True).decode('utf-8'))
    if not isinstance(value, dict):
        raise ValueError('Expected a JSON object')
    return value


class CommandRejected(RuntimeError):
    def __init__(self, response: dict[str, Any]) -> None:
        super().__init__(json.dumps(response, ensure_ascii=False))
        self.response = response


def result_of(response: dict[str, Any]) -> Any:
    if 'error' in response or str(response.get('method', '')).startswith('event.'):
        raise CommandRejected(response)
    if 'result' not in response:
        raise ValueError('No result; do not treat a transport acknowledgement as success')
    return response['result']


def parse_mixed_status(response: dict[str, Any]) -> dict[str, Any]:
    value = result_of(response)
    if not isinstance(value, dict):
        raise ValueError('mixed_status.result is an object, not an array')
    raw = dict(value)
    level = raw.get('battery-level')
    valid_percent = (not isinstance(level, bool) and isinstance(level, (int, float))
                     and math.isfinite(level) and 0 <= level <= 100)
    battery = raw.get('battery')
    sensor = raw.get('sensor')
    valid_sensor = isinstance(sensor, int) and not isinstance(sensor, bool) and sensor >= 0
    valid_battery = (isinstance(battery, int) and not isinstance(battery, bool)
                     and battery in {*range(6), *range(16, 22)})
    error = raw.get('error')
    policy = ERROR_POLICY.get(error) if isinstance(error, int) and not isinstance(error, bool) else None
    return {
        'raw': raw,
        'category': raw.get('category'), 'sub_category': raw.get('sub_category'),
        'error': error,
        'has_device_error': (raw['category'] == 'error' if raw.get('category') in
            {'idle', 'processing', 'error', 'sleep', 'off', 'updating', 'maintenance',
             'updating firmware', 'factory reset'} else None),
        'official_error_title': policy['status'] if policy else None,
        'official_resume_button': policy['isResume'] if policy else None,
        'battery_state': battery,
        'battery_percent': level if valid_percent else None,
        'battery_percent_evidence': 'embedded-plugin-sample; verify on hardware',
        'charging': 16 <= battery <= 21 if valid_battery else None,
        'usb_connected': bool(sensor & 8) if valid_sensor else None,
        'paper_tray_closed': bool(sensor & 4) if valid_sensor else None,
        'job_id': raw.get('job_id'),
    }


def job_outcome(job_state: str) -> Literal['success', 'canceled', 'failed', 'active', 'unknown']:
    if job_state in TERMINAL_JOB_STATES:
        return TERMINAL_JOB_STATES[job_state]
    return 'active' if job_state in ACTIVE_JOB_STATES else 'unknown'


def inspect_response(response: dict[str, Any], pending_id: int | None) -> str:
    """Classification, not a live RPC dispatcher. Never silently consume id-less events.

    Real id-less result replies need captured evidence plus single-flight schema
    validation; this helper returns 'idless-result' for the application to decide.
    """
    if str(response.get('method', '')).startswith('event.'):
        return 'event'
    if 'id' not in response:
        return 'idless-result' if 'result' in response or 'error' in response else 'unknown'
    if pending_id is None or response['id'] != pending_id:
        return 'unmatched'
    return 'matched' if 'result' in response or 'error' in response else 'unknown'
