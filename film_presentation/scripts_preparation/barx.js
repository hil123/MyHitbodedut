const {chromium}=require('playwright');(async()=>{const b=await chromium.launch();
for(const g of ['m','f']){const p=await b.newPage({viewport:{width:390,height:770},deviceScaleFactor:3});
await p.goto('file:///home/user/film/captures/barriere_x_'+g+'.html');await p.waitForTimeout(400);
const r=await p.evaluate(()=>{const e=document.querySelector('.p1').getBoundingClientRect();return [e.top,e.bottom]});console.log(g,r);
await p.screenshot({path:'/home/user/film/v1/public/ecrans/'+g+'_barriere_x.png'});}
await b.close();})();
