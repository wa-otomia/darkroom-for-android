'use strict';
// Executes only reviewed pure source fragments; no RN/native/network modules.
const fs = require('fs'), path = require('path'), vm = require('vm'), assert = require('assert');
const ROOT = __dirname;
const source = id => fs.readFileSync(path.join(ROOT,'modules',`${id}.js`),'utf8');
const evaluate = (code, env={}) => vm.runInNewContext(code,env,{timeout:1500});
function literal(id,name) {
  const s=source(id), start=s.indexOf(`var ${name} = `), end=s.indexOf(`\n  exports.${name}`,start);
  if(start<0 || end<0) throw Error(`Literal not found: ${name}`);
  return evaluate(s.slice(start,end)+`\n${name};`);
}
function method(id,name,env={}) {
  const s=source(id), marker=`key: "${name}",\n      value: `;
  const start=s.indexOf(marker);
  if(start<0) throw Error(`Method not found: ${name}`);
  const begin=start+marker.length;
  let end=s.indexOf('\n    }, {',begin);
  const endLast=s.indexOf('\n    }]);',begin);
  if(end<0 || (endLast>=0 && endLast<end)) end=endLast;
  if(end<0) throw Error('Method end not found');
  return evaluate('('+s.slice(begin,end)+')',env);
}
function loadNoImports(id) {
  let result;
  evaluate(source(id),{__d:(factory,mid,deps)=>{
    assert.equal(deps.length,0);
    const mod={exports:{}};
    factory({},()=>{throw Error('Imports prohibited');},null,null,mod,mod.exports,[]);
    result=mod.exports;
  }});
  return result;
}
const consts={};
for(const name of ['MINT_PRINTER_ERROR_CODE','RICOTTA_PRINTER_ERROR_CODE','RICOTTA_JOB_ERROR_CODE','MINT_BATTERY','MINT_TEMP','MINT_JOB_STATUS','MINT_PRINTER_STATUS_CATEGORY','MINT_PRINTER_SUB_CATEGORY']) consts[name]=literal(12173,name);
const strings=loadNoImports(13484).default;
const aes=loadNoImports(12308);
const cryptoSrc=source(12365);
const cryptoBegin=cryptoSrc.indexOf('var crypto = '),cryptoEnd=cryptoSrc.indexOf('\n  var _default = crypto;');
const crypto=evaluate(cryptoSrc.slice(cryptoBegin,cryptoEnd)+'\ncrypto;', {_aesJs:{default:aes}});
const wire=loadNoImports(12371);
const helpers={isUint8Array:x=>Object.prototype.toString.call(x)==='[object Uint8Array]',isArrayBuffer:x=>Object.prototype.toString.call(x)==='[object ArrayBuffer]'};
const environment={_ht_model:wire,_ht_utils:{default:helpers},_aesJs:{default:aes},_ht_crypto:{default:crypto}};
function buildFrame(body,sn,channel=3,pkgTotal=0,pkgNum=0,encoding=3) {
  const ctx={version:100,reserve:0,channelId:channel,interactive:6,encoding,arcMsgSn:sn,msgSn:sn,
    encryptType:'101',msgPackageTotal:pkgTotal,currentPackageNum:pkgNum,isStream:false,msgBody:body};
  for(const name of ['getView6to13','getView14to19','encryptBody','build'])ctx[name]=method(12368,name,environment);
  return Buffer.from(ctx.build(false));
}
const errorSource=source(12332), start=errorSource.indexOf('getErrorInfo: ')+14, end=errorSource.indexOf('\n    deviceAlertsEntity:',start);
const errorFunction=evaluate('('+errorSource.slice(start,end).trim().replace(/,$/, '')+')',{
 _consts:consts,_resources:{default:{getString:k=>strings[k]??k}}
});
(async()=>{
  const cases=[['getMixStatus','mixed_status',{},[]],['getDeviceInfo','get_prop',['device_info'],[]],
   ['cancelJob','cancel_job',[37],[37]],['resumePrinter','resume_printer',{},[]],
   ['getJobInfo','job_info',[37],[37]],['confirmJob','confirm_job',[37],[37]]];
  const key=Buffer.from('30313233343536373839414243444546','hex'), vectors=[]; let count=0;
  for(const [name,expected,params,args] of cases){
    let captured;
    const ctx={getMsgSn:()=>101,handleCommond:data=>{captured=data;return Promise.resolve({id:101,result:{}});}};
    await method(12407,name).apply(ctx,args);
    assert.equal(captured.method,expected); assert.equal(JSON.stringify(captured.params),JSON.stringify(params));
    const plaintext=JSON.stringify(captured), body=crypto.encryptECB(key,Buffer.from(plaintext));
    const frame=buildFrame(body,101);
    vectors.push({name,request:captured,key_hex:key.toString('hex'),plaintext,body_hex:Buffer.from(body).toString('hex'),frame_hex:frame.toString('hex')});
    count++;console.log(`PASS original method: ${name}`);
  }
  const codeList=[-7001,-7103,-7104,-7105,-7110,-7111,-7112,-7114,-7201,-7204,-7205,-7208,-7308,-7309,-7310,-7311];
  const errors=codeList.map(c=>({code:c,...JSON.parse(JSON.stringify(errorFunction(c,'printing_Y')))}));
  assert.equal(errors.find(x=>x.code===-7103).isResume,true);
  assert.equal(errors.find(x=>x.code===-7208).isResume,true);
  assert.equal(errors.find(x=>x.code===-7204).isResume,false);
  for(const e of errors){assert.equal(e.status,strings[`ricotta_error_${-e.code}_title`]);assert.equal(e.isResume,new Set([-7103,-7110,-7111,-7112,-7114,-7201,-7208]).has(e.code));count++;console.log(`PASS original error policy: ${e.code} resume=${e.isResume}`);}
  for(const [name,arg,expected] of [['isUSBConnected',8,true],['isUSBConnected',4,false],['isPaperTrayColsed',4,true],['isPaperTrayColsed',0,false],['isCharging',20,true],['isCharging',4,false],['isCharging',0,false]]){
    assert.equal(method(12311,name,{_consts:consts}).call({},arg),expected);count++;console.log(`PASS original sensor: ${name}(${arg})`);
  }
  const observed=source(13166).match(/var state1 = (\{[\s\S]*?\n            \});/)[1];
  const sample=evaluate('('+observed+')');assert.equal(sample.battery,4);assert.equal(sample['battery-level'],94);count++;
  console.log('PASS embedded sample has battery=4, battery-level=94 (not a live capture)');
  for(const n of [1,16,17,31,32,988,992]){
    const input=Buffer.alloc(n,65),enc=crypto.encryptECB(key,input),dec=crypto.decryptECB(key,enc,false);
    assert.equal(enc.length,Math.ceil(n/16)*16);assert.equal(Buffer.from(dec).subarray(0,n).compare(input),0);count++;
    console.log(`PASS original AES zero padding: ${n}`);
  }
  const out=path.join(ROOT,'generated');
  fs.mkdirSync(out,{recursive:true});
  fs.writeFileSync(path.join(out,'official_vectors.json'),JSON.stringify(vectors,null,2));
  fs.writeFileSync(path.join(out,'official_error_policy.json'),JSON.stringify(errors,null,2));
  fs.writeFileSync(path.join(out,'official_constants.json'),JSON.stringify(consts,null,2));
  console.log(`TOTAL ${count} original-source checks passed; no physical printer tested.`);
})().catch(e=>{console.error(e);process.exitCode=1});
