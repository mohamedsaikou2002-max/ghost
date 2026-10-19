const fs=require('fs'),vm=require('vm');
const source=fs.readFileSync(__dirname+'/../preview/app.js','utf8').replace(/\nrender\(\);\s*$/,'\n');
const ctx={localStorage:{getItem:()=>null},document:{},window:{addEventListener:()=>{}},console,devicePixelRatio:1};
vm.createContext(ctx);
vm.runInContext(source,ctx);
const actual=vm.runInContext('stats([800,900,800,900])',ctx);
for(const [field,want] of Object.entries({rmssd:100,sdnn:57.735026918962575,pnn50:100})){
  if(Math.abs(actual[field]-want)>1e-7)throw Error(`${field} expected ${want}, got ${actual[field]}`);
}
for(const fun of ['home','hrv','room','devices','sessions','spectrum']){
  const markup=vm.runInContext(`${fun}()`,ctx);
  if(!markup.includes('class='))throw Error(`Empty screen ${fun}`);
}
let rejected=false;try{vm.runInContext('stats([0,0,0])',ctx)}catch(_){rejected=true}
if(!rejected)throw Error('Out-of-range NN values accepted');
console.log('PASS: 6 rendered screen templates, HRV calculations, NN input validation.');
