const {chromium}=require('playwright');
const iso=d=>d.toISOString().slice(0,10);
function journal(doneToday){const days={};const now=new Date('2026-11-03T08:00:00');
 const notes=['Merci pour la santé des enfants.','Parlé du travail, beaucoup de calme après.','Tehilim 23, puis mes mots à moi.','',''];
 for(let i=doneToday?0:1;i<=21;i++){const d=new Date(now);d.setDate(d.getDate()-i); if(d.getDay()===6) continue; days[iso(d)]={minutes:[30,45,60,20,40][i%5],note:notes[i%5]};}
 return {days};}
(async()=>{const b=await chromium.launch();
const shots=[['accueil',false,null],['seance',false,'seance'],['parcours',true,'Mon parcours'],['zmanim',false,'Zmanim'],['fait',true,null]];
for (const g of ['m','f']) for (const [name,done,act] of shots){
 const ctx=await b.newContext({viewport:{width:390,height:844},deviceScaleFactor:3,locale:'fr-FR',timezoneId:'Asia/Jerusalem'});
 await ctx.addInitScript(([g,j])=>{ localStorage.setItem('myhitbodedut',JSON.stringify({onboarded_version:999,adresse:g,paliers_vus:[],trophees_vus:[],grade_vu:0,lieu_zmanim:{nom:'Jérusalem',latitude:31.778,longitude:35.235,altitude:754,fuseau:'Asia/Jerusalem',ville:null},in_israel:true})); localStorage.setItem('myhitbodedut-journal',JSON.stringify(j)); },[g,journal(done)]);
 const p=await ctx.newPage(); await p.clock.install({time:new Date('2026-11-03T07:40:00+02:00')}).catch(()=>{});
 await p.goto('http://localhost:8765/index.html');await p.waitForTimeout(2500);
 if(name==='fait'){await p.waitForTimeout(1500);await p.screenshot({path:`/home/user/film/captures/${g}_trophee.png`});} for(let k=0;k<12;k++){const c=await p.evaluate(()=>{const e=[...document.querySelectorAll('button')].find(e=>e.offsetParent&&/^(Continuer|Fermer|OK)$/.test((e.innerText||'').trim()));if(e){e.click();return true}return false}); if(!c)break; await p.waitForTimeout(1200);} await p.waitForTimeout(3000);
 if(act==='seance'){await p.evaluate(()=>{const e=[...document.querySelectorAll('button')].find(e=>e.offsetParent&&/Commencer une hitbodedout/.test(e.innerText));e&&e.click()});await p.waitForTimeout(1500);await p.clock.runFor?.(65000).catch(()=>{});await p.waitForTimeout(800);}
 else if(act){await p.evaluate(l=>{const e=[...document.querySelectorAll('button,a')].find(e=>e.offsetParent&&(e.innerText||'').trim()===l);e&&e.click()},act);await p.waitForTimeout(1800);}
 await p.screenshot({path:`/home/user/film/captures/${g}_${name}.png`}); console.log(g,name);
 await ctx.close();}
await b.close();})();
