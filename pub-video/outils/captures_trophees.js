const {chromium}=require('playwright');const O='/home/user/pub-video/captures/';
// Historique de démonstration : 24 jours de hitbodedout sur 4 semaines (Chabbat exclus), quelques notes.
const days={};const now=new Date();
for(let k=28;k>=1;k--){const d=new Date(now);d.setDate(now.getDate()-k);if(d.getDay()===6)continue;if(k===19||k===11)continue;
 const iso=d.getFullYear()+'-'+String(d.getMonth()+1).padStart(2,'0')+'-'+String(d.getDate()).padStart(2,'0');
 days[iso]={minutes:[15,20,15,30,15,25,15][k%7],note:k%5===0?'Merci Hachem pour cette journée.':''};}
(async()=>{const b=await chromium.launch();const c=await b.newContext({viewport:{width:412,height:892},deviceScaleFactor:3,locale:'fr-FR',isMobile:true,hasTouch:true});
await c.addInitScript((j)=>{try{if(!localStorage.getItem('demo_ok')){localStorage.setItem('myhitbodedut-journal',JSON.stringify({days:j}));localStorage.setItem('demo_ok','1');}}catch(e){}},days);
const p=await c.newPage();const W=t=>p.waitForTimeout(t||1300);
await p.goto('http://localhost:8765/');await W(2500);
if(await p.getByText('Passer',{exact:true}).count()){await p.getByText('Passer',{exact:true}).first().click();await W();}
if(await p.getByText('Commencer',{exact:true}).count()) {await p.getByText('Commencer',{exact:true}).last().click().catch(()=>{});await W();}
await W(800);
if(await p.locator('.voile').count()){await p.screenshot({path:O+'web_trophees_semaine.png'});
 console.log(JSON.stringify(await p.evaluate(()=>[...document.querySelectorAll('.voile button')].map(b=>b.innerText))));
 const bt=p.locator('.voile button');if(await bt.count()) await bt.last().click(); else await p.keyboard.press('Escape'); await W();}
await p.screenshot({path:O+'web_accueil_serie.png'});
await p.getByText('Trophées',{exact:false}).first().click();await W(1800);
await p.screenshot({path:O+'web_trophees.png'});
await p.evaluate(()=>{document.querySelectorAll('*').forEach(e=>{if(e.scrollTop>0)e.scrollTop=0});window.scrollTo(0,0);});await W(600);
await p.screenshot({path:O+'web_trophees_haut.png'});
console.log(JSON.stringify(await p.evaluate(()=>document.body.innerText.slice(0,500))));
await b.close();})();
