const {chromium}=require('playwright');const O='/home/user/pub-video/captures/';
(async()=>{const b=await chromium.launch();const c=await b.newContext({viewport:{width:412,height:892},deviceScaleFactor:3,locale:'fr-FR',isMobile:true,hasTouch:true});
const p=await c.newPage();const W=t=>p.waitForTimeout(t||1300);
const btns=async()=>console.log(JSON.stringify(await p.evaluate(()=>[...document.querySelectorAll('button,[role=button],li,input,select')].filter(e=>e.offsetParent).map(e=>(e.innerText||e.placeholder||e.value||'').trim().slice(0,30)).filter(Boolean))));
await p.goto('http://localhost:8765/');await W(2500);
await p.getByText('Passer',{exact:true}).first().click();await W();
if(await p.getByText('Commencer',{exact:true}).count()) {await p.getByText('Commencer',{exact:true}).last().click().catch(()=>{});await W();}
await p.getByText('Zmanim',{exact:true}).first().click();await W(1500);
await p.getByText('Choisir le lieu',{exact:true}).first().click();await W();await btns();
const inp=p.locator('input').first();if(await inp.count()){await inp.fill('Jérusalem');await W(1200);await btns();}
const j=p.getByText(/^Jérusalem/).first();if(await j.count()){await j.click();await W(1500);}
await p.screenshot({path:O+'web_zmanim.png'});
await p.getByText('Calendrier',{exact:true}).first().click();await W(1500);
await p.screenshot({path:O+'web_calendrier.png'});await btns();
await p.mouse.wheel(0,600);await W();await p.screenshot({path:O+'web_calendrier_2.png'});
await b.close();})();
