const {chromium}=require('playwright');(async()=>{const b=await chromium.launch();
for(const g of ['m','f']){const p=await b.newPage({viewport:{width:390,height:844},deviceScaleFactor:3});
await p.goto('file:///home/user/film/captures/barriere_'+g+'.html');await p.waitForTimeout(500);await p.screenshot({path:'/home/user/film/captures/'+g+'_barriere.png'});}
await b.close();})();
