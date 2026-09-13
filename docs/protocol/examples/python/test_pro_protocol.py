import json
import unittest
from pathlib import Path
import pro_protocol as p

ROOT = Path(__file__).resolve().parents[1]
VECTORS = json.loads((ROOT/'data/official_vectors.json').read_text())
KEY = bytes.fromhex(VECTORS[0]['key_hex'])

class ProtocolTests(unittest.TestCase):
    def test_all_original_source_golden_frames(self):
        for vector in VECTORS:
            with self.subTest(vector=vector['name']):
                frame=p.encode_request(vector['request'],KEY)
                self.assertEqual(frame.hex(),vector['frame_hex'])
                self.assertEqual(p.decode_json(p.parse_frame(frame),KEY),vector['request'])
    def test_request_params_types(self):
        self.assertEqual(p.job_info_request(1,37)['params'],[37])
        self.assertEqual(p.cancel_request(2,37)['params'],[37])
        self.assertEqual(p.resume_request(3)['params'],{})
        self.assertEqual(p.mixed_status_request(4)['params'],{})
        self.assertEqual(p.device_info_request(5)['params'],['device_info'])
        self.assertEqual(p.print_job_request(6,2000,2)['params'],
                         {'file_size':2000,'copies':2,'job_type':0,'channel':64})
    def test_invalid_job_ids(self):
        for val in [True,0,-1,2**32,'37',3.5]:
            with self.subTest(val=val),self.assertRaises(ValueError):p.cancel_request(1,val)
    def test_stream_every_split_position(self):
        raw=bytes.fromhex(VECTORS[0]['frame_hex'])
        for i in range(len(raw)+1):
            decoder=p.StreamDecoder();out=decoder.feed(raw[:i])+decoder.feed(raw[i:])
            self.assertEqual([x.raw for x in out],[raw])
    def test_stream_bytewise(self):
        raw=bytes.fromhex(VECTORS[0]['frame_hex']);d=p.StreamDecoder();out=[]
        for b in raw:out+=d.feed(bytes([b]))
        self.assertEqual([x.raw for x in out],[raw])
    def test_coalesced_frames(self):
        raw=[bytes.fromhex(x['frame_hex']) for x in VECTORS]
        self.assertEqual([x.raw for x in p.StreamDecoder().feed(b''.join(raw))],raw)
    def test_frame_marker_in_body(self):
        raw=p.build_frame(b'\x7e\x64\x00'*10,1)
        self.assertEqual(p.StreamDecoder().feed(raw)[0].body,b'\x7e\x64\x00'*10)
    def test_corrupt_checksum_resync(self):
        raw=bytes.fromhex(VECTORS[0]['frame_hex']);bad=bytearray(raw);bad[-2]^=1
        self.assertEqual([x.raw for x in p.StreamDecoder().feed(bytes(bad)+raw)],[raw])
    def test_bad_tail(self):
        raw=bytearray.fromhex(VECTORS[0]['frame_hex']);raw[-1]=0
        with self.assertRaises(ValueError):p.parse_frame(bytes(raw))
    def test_truncated(self):
        raw=bytes.fromhex(VECTORS[0]['frame_hex']);d=p.StreamDecoder()
        self.assertEqual(d.feed(raw[:-1]),[]);self.assertEqual(len(d.feed(raw[-1:])),1)
    def test_oversize_body(self):
        with self.assertRaises(ValueError):p.build_frame(b'x'*993,1)
    def test_multiframe_attribute(self):
        f=p.parse_frame(p.build_frame(b'x'*992,1,channel=4,encoding=2,package_total=10,package_number=3))
        self.assertEqual((f.package_total,f.package_number,len(f.body)),(10,3,992))
        self.assertEqual(int.from_bytes(f.raw[18:20],'little'),992+5120+8192)
    def test_zero_padding_not_pkcs7(self):
        self.assertEqual(len(p.encrypt_ecb(KEY,b'x'*16)),16)
        self.assertEqual(p.decrypt_ecb(KEY,p.encrypt_ecb(KEY,b'x'*17)),b'x'*17+b'\0'*15)
    def test_binary_zero_preserved(self):
        raw=b'a'*15+b'\0';self.assertEqual(p.decrypt_ecb(KEY,p.encrypt_ecb(KEY,raw)),raw)
    def test_bad_ciphertext(self):
        with self.assertRaises(ValueError):p.decrypt_ecb(KEY,b'abc')
    def test_percent_zero_valid(self):
        v=p.parse_mixed_status({'result':{'battery':0,'battery-level':0}})
        self.assertEqual(v['battery_percent'],0);self.assertFalse(v['charging'])
    def test_percent_sample(self):
        v=p.parse_mixed_status({'result':{'battery':4,'battery-level':94}})
        self.assertEqual(v['battery_percent'],94);self.assertEqual(v['battery_state'],4)
    def test_percent_missing_not_enum(self):
        v=p.parse_mixed_status({'result':{'battery':4,'battery_pct':100}})
        self.assertIsNone(v['battery_percent'])
    def test_invalid_percent(self):
        for val in [True,-1,101,'94',float('nan')]:
            with self.subTest(val=val):
                self.assertIsNone(p.parse_mixed_status({'result':{'battery-level':val}})['battery_percent'])
    def test_charging_and_sensor(self):
        v=p.parse_mixed_status({'result':{'battery':20,'sensor':12}})
        self.assertTrue(v['charging']);self.assertTrue(v['usb_connected']);self.assertTrue(v['paper_tray_closed'])
    def test_missing_sensor_unknown(self):
        v=p.parse_mixed_status({'result':{}});self.assertIsNone(v['usb_connected']);self.assertIsNone(v['has_device_error']);self.assertIsNone(v['charging'])
    def test_error_and_resume_policy(self):
        v=p.parse_mixed_status({'result':{'category':'error','error':-7208}})
        self.assertTrue(v['has_device_error']);self.assertTrue(v['official_resume_button'])
        self.assertFalse(p.ERROR_POLICY[-7204]['isResume'])
    def test_symbol_only_error_not_ui_mapping(self):
        v=p.parse_mixed_status({'result':{'category':'error','error':-7203}})
        self.assertTrue(v['has_device_error']);self.assertIsNone(v['official_resume_button'])
    def test_snapshot_replaces_stale_error(self):
        prior=p.parse_mixed_status({'result':{'category':'error','error':-7103}})
        current=p.parse_mixed_status({'result':{'category':'idle','error':0}})
        self.assertTrue(prior['has_device_error']);self.assertFalse(current['has_device_error'])
    def test_result_shape(self):
        with self.assertRaises(ValueError):p.parse_mixed_status({'result':[{}]})
    def test_rpc_error_is_not_success(self):
        with self.assertRaises(p.CommandRejected):p.result_of({'id':1,'error':{'code':-5002}})
    def test_async_error_is_not_success(self):
        with self.assertRaises(p.CommandRejected):p.result_of({'method':'event.rpt_err','params':{'code':-8201}})
    def test_response_id_routing(self):
        self.assertEqual(p.inspect_response({'id':2,'result':{}},1),'unmatched')
        self.assertEqual(p.inspect_response({'id':1,'result':{}},1),'matched')
        self.assertEqual(p.inspect_response({'result':{}},1),'idless-result')
        self.assertEqual(p.inspect_response({'method':'event.rpt_err','id':1},1),'event')
    def test_terminal_states(self):
        for state,outcome in [('finished','success'),('canceled','canceled'),('aborted','failed')]:
            self.assertEqual(p.job_outcome(state),outcome)
    def test_print_passes_not_terminal(self):
        for state in ['printing_Y','printing_M','printing_C','printing_OC','cool_down','initlization']:
            self.assertEqual(p.job_outcome(state),'active')
        self.assertEqual(p.job_outcome('new_state'),'unknown')
    def test_print_job_and_file_chunk_match_darkroom_vectors(self):
        req=p.print_job_request(101,2000,2)
        self.assertEqual(p.encode_request(req,KEY).hex(),
            '7e64000306036500000065000000000000006014'
            'db7bea5ba6c51c4ba3159761a69af569ef0690bebc27aa4223d65b9738d3f9d6'
            '423bb185a5e4f8d53200e6b7b68f663d77ddd8a653272bbe6abc5490fef1432e'
            '2ba0b722ec776a66173a8ed52e622863a48d038e39f86fb7368d0db32c506550127e')
        frame=p.file_chunk(37,b'A',102,KEY,2,1)
        self.assertEqual(frame.hex(),'7e64000406026600000066000000020001001034bfb44fd67aa0c4af7cfb25a62344a5b6ac7e')

if __name__=='__main__':unittest.main(verbosity=2)
