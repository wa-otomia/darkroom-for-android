"""No Bluetooth I/O. Prints the official request shapes as frames.

Obtain a real session key from the DH handshake. Never send the synthetic
fixture key 0x3031…444546 to a live printer.
"""
from __future__ import annotations

import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from pro_protocol import (  # noqa: E402
    cancel_request,
    encode_request,
    file_chunk,
    job_info_request,
    mixed_status_request,
    print_job_request,
    resume_request,
)

# Synthetic offline key used by official_vectors.json only.
KEY = bytes.fromhex('30313233343536373839414243444546')


def show(label: str, frame: bytes) -> None:
    print(f'{label}: {len(frame)} bytes')
    print(frame.hex())
    print()


def main() -> None:
    show('1 mixed_status {}', encode_request(mixed_status_request(101), KEY))
    show('2 print_job (printJob2 iOS channel=64; Android official is 576)',
         encode_request(print_job_request(102, file_size=2000, copies=1), KEY))
    show('3 file chunk: job_id LE + payload, channel 4',
         file_chunk(37, b'A', 103, KEY, package_total=2, package_number=1))
    show('4 job_info [jobId]', encode_request(job_info_request(104, 37), KEY))
    show('5 resume_printer {}', encode_request(resume_request(105), KEY))
    show('6 cancel_job [jobId]', encode_request(cancel_request(106, 37), KEY))


if __name__ == '__main__':
    main()
