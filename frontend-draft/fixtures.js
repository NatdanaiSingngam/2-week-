// Fictional fixtures only. Never included by Thymeleaf production templates.
(() => {
  const page = document.body.dataset.page;
  const state = document.getElementById('preview-state');
  const sampleText = 'นักศึกษาสาขาวิทยาการคอมพิวเตอร์ เคยทำโปรเจกต์เว็บไซต์ด้วย HTML, CSS และ JavaScript ใช้ Git ในการทำงานร่วมกับทีม และออกแบบหน้าจอด้วย Figma';
  const skills = [{name:'HTML',category:'frontend'},{name:'CSS',category:'frontend'},{name:'JavaScript',category:'programming'},{name:'Git',category:'tools'},{name:'Figma',category:'design'}];
  const jobs = [
    {id:1,title:'Frontend Developer',companyName:'บริษัท ใบไม้ดิจิทัล (สมมติ)',score:85,cosine:80.2,matched:['HTML','CSS','JavaScript','Git'],missing:['React']},
    {id:2,title:'UI Developer',companyName:'บริษัท เมฆเทค (สมมติ)',score:66.7,cosine:64.5,matched:['HTML','CSS','Figma'],missing:['TypeScript','UX Research']},
    {id:3,title:'Full Stack Developer',companyName:'บริษัท ใบไม้ดิจิทัล (สมมติ)',score:40,cosine:38.8,matched:['JavaScript','Git'],missing:['Java','Spring Boot','MySQL']},
    {id:4,title:'Data Analyst',companyName:'บริษัท ข้อมูลดี (สมมติ)',score:20,cosine:18.3,matched:['Git'],missing:['Python','SQL','pandas','Power BI']}
  ];
  const e = value => String(value).replace(/[&<>"']/g, char => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[char]));
  const badges = (items,missing=false) => items.map(item=>`<span class="badge ${missing?'badge-missing':'badge-ok'}">${missing?'✗':'✓'} ${e(item)}</span>`).join('');
  const progress = score => `<progress aria-label="คะแนนความเหมาะสม" class="progress ${score>=70?'high':score>=40?'medium':'low'}" max="100" value="${score}">${score.toFixed(1)}%</progress>`;
  const empty = (title,text,href,label) => `<section class="card empty"><div class="symbol" aria-hidden="true">◇</div><h2>${title}</h2><p>${text}</p>${href?`<a class="button" href="${href}">${label}</a>`:''}</section>`;
  const heading = (eyebrow,title,description,href,label) => `<div class="page-heading"><div><p class="eyebrow">${eyebrow}</p><h1>${title}</h1><p>${description}</p></div>${href?`<a class="button" href="${href}">${label}</a>`:''}</div>`;
  const companyPage = ['jobs','job-form','job-detail'].includes(page);
  const allowedStates = ['login','register','job-form'].includes(page) ? ['populated','error'] : page==='dashboard' ? ['populated','empty','no-resume'] : ['populated','empty',...(page==='resume'?['error']:[])];
  [...state.options].forEach(option=>{ option.hidden=!allowedStates.includes(option.value);option.disabled=option.hidden; });
  document.getElementById('preview-links').innerHTML = companyPage ? '<a href="jobs.html">ตำแหน่งงานของฉัน</a>' : ['login','register'].includes(page) ? '<a href="login.html">เข้าสู่ระบบ</a><a href="register.html">สมัครสมาชิก</a>' : '<a href="resume.html">แบบฟอร์ม resume</a><a href="dashboard.html">ผลจับคู่</a>';
  document.getElementById('preview-account').hidden = ['login','register'].includes(page);
  function render() {
    const mode = state.value;
    if(page==='job-form') { const alert=document.querySelector('.alert'); if(alert) alert.className='alert error'; }
    document.querySelectorAll('.alert').forEach(el=>{el.hidden=true});
    if(mode==='error') {
      const alert = document.querySelector('.alert.error');
      if(alert) { alert.textContent=page==='login'?'อีเมลหรือรหัสผ่านไม่ถูกต้อง':'ตัวอย่างข้อผิดพลาด: ไม่สามารถบันทึกข้อมูลได้ กรุณาลองอีกครั้ง';alert.hidden=false; }
    }
    if(page==='resume') {
      const list = ['empty','no-resume'].includes(mode)?[]:skills;
      document.querySelector('.card-title .badge').textContent=`พบ ${list.length} ทักษะ`;
      document.getElementById('skill-results').innerHTML = list.length ? [...new Set(list.map(s=>s.category))].map(category=>`<section class="skill-group"><h3>${({programming:'ภาษาโปรแกรม',database:'ฐานข้อมูล',frontend:'พัฒนาหน้าเว็บ (Frontend)',backend:'พัฒนาระบบหลังบ้าน (Backend)',mobile:'แอปพลิเคชันมือถือ',data:'ข้อมูลและ AI',tools:'เครื่องมือพัฒนา',design:'ออกแบบ',infrastructure:'เครือข่ายและความปลอดภัย',testing:'การทดสอบซอฟต์แวร์',language:'ภาษาต่างประเทศ',office:'โปรแกรมสำนักงาน',soft:'ทักษะทางสังคม (Soft skills)'})[category] || category || 'อื่น ๆ'}</h3>${badges(list.filter(s=>s.category===category).map(s=>s.name))}</section>`).join('') : '<div class="empty"><p>ยังไม่พบทักษะ ลองเพิ่มรายละเอียดทักษะใน resume</p></div>';
    }
    if(page==='dashboard') {
      let html='<section class="hero"><span class="hero-line" aria-hidden="true"></span><p class="eyebrow">YOUR NEXT OPPORTUNITY</p><h1>ค้นพบงานที่ใช่<br />และเตรียมพร้อมไปด้วยกัน</h1><p>ดูว่าทักษะของคุณตรงกับตำแหน่งใด และควรพัฒนาอะไรเพิ่มก่อนเริ่มสหกิจศึกษา</p><a class="button accent" href="resume.html">อัปเดต resume →</a></section>';
      if(mode==='no-resume') html+=empty('คุณยังไม่ได้กรอก resume','เพิ่มประสบการณ์และทักษะ เพื่อเริ่มค้นหาโอกาสที่เหมาะกับคุณ','resume.html','กรอก resume');
      else if(mode==='empty') html+=empty('ยังไม่มีตำแหน่งงานที่จับคู่ได้','ลองเพิ่มรายละเอียดทักษะ หรือกลับมาดูเมื่อมีตำแหน่งงานใหม่','resume.html','แก้ไข resume');
      else html+='<div class="stats"><div class="stat"><span>ตำแหน่งที่จับคู่ได้</span><strong>4 ตำแหน่ง</strong></div><div class="stat"><span>คะแนนสูงสุด</span><strong>85.0%</strong></div></div><div class="page-heading"><h2>โอกาสที่เหมาะกับคุณ</h2><small>เรียงตามผลจับคู่จากระบบ</small></div>'+jobs.map(job=>`<article class="card match-card"><div class="match-top"><div><p class="eyebrow">โอกาสสหกิจศึกษา</p><h2>${e(job.title)}</h2><span class="muted">${e(job.companyName)}</span></div><div class="score"><strong>${job.score.toFixed(1)}%</strong><small>Cosine ${job.cosine.toFixed(1)}%</small></div></div><p class="score-label">คะแนนความเหมาะสมจากทักษะที่ตรงกัน</p>${progress(job.score)}<details><summary>ดูทักษะทั้งหมด</summary><div class="skill-columns"><section><h3>ทักษะที่คุณมี</h3>${badges(job.matched)}</section><section><h3>ควรเตรียมเพิ่ม</h3>${badges(job.missing,true)}</section></div></details></article>`).join('');
      document.getElementById('preview-content').innerHTML=html;
    }
    if(page==='jobs') {
      let html=heading('COMPANY WORKSPACE','ตำแหน่งงานของฉัน','จัดการโอกาสสหกิจศึกษา และค้นหานักศึกษาที่มีทักษะตรงกับทีมของคุณ','job-form.html','+ สร้างตำแหน่งใหม่');
      html+=mode==='empty'?empty('ยังไม่มีตำแหน่งงาน','เริ่มสร้างตำแหน่งแรก เพื่อค้นหานักศึกษาที่เหมาะกับบริษัท','job-form.html','สร้างตำแหน่งแรก'):`<section class="card"><div class="card-title"><h2>ตำแหน่งงานทั้งหมด</h2><span class="badge">${jobs.length} ตำแหน่ง</span></div><div class="table-wrap"><table><thead><tr><th scope="col">ชื่อตำแหน่ง</th><th scope="col">จำนวนทักษะ</th><th scope="col">ผู้สมัครที่จับคู่ได้</th></tr></thead><tbody>${jobs.map(job=>`<tr><td><a href="job-detail.html?id=${job.id}">${e(job.title)}</a></td><td>${job.matched.length+job.missing.length}</td><td>4</td></tr>`).join('')}</tbody></table></div></section>`;
      document.getElementById('preview-content').innerHTML=html;
    }
    if(page==='job-detail') {
      const id = Number(new URLSearchParams(location.search).get('id')||1);
      const job = jobs.find(j=>j.id===id);
      if(!job) { document.getElementById('preview-content').innerHTML=empty('ไม่พบตำแหน่งงาน','ตรวจสอบตำแหน่งอีกครั้ง','jobs.html','กลับไปรายการตำแหน่ง');return; }
      const applicants = [100,85,66.7,20];
      document.getElementById('preview-content').innerHTML=heading('JOB OVERVIEW',e(job.title),'ทักษะที่ต้องการ และนักศึกษาที่จับคู่กับตำแหน่งนี้','jobs.html','กลับไปรายการตำแหน่ง')+`<section class="card"><h2>รายละเอียดงาน</h2><p>ร่วมพัฒนา ${e(job.title)} กับทีมของเรา โดยมีพี่เลี้ยงให้คำแนะนำตลอดการสหกิจศึกษา</p><h3>ทักษะที่ต้องการ</h3>${badges([...job.matched,...job.missing])}</section><section class="card"><div class="card-title"><h2>นักศึกษาที่จับคู่ได้</h2><span class="badge">${mode==='empty'?0:4} คน</span></div>${mode==='empty'?'<div class="empty"><p>ยังไม่มีผู้สมัครที่จับคู่ได้</p></div>':`<div class="table-wrap"><table class="applicants"><thead><tr><th scope="col">อันดับ</th><th scope="col">ชื่อนักศึกษา</th><th scope="col">คะแนน</th><th scope="col">Cosine</th><th scope="col">ทักษะที่มี</th><th scope="col">ควรเตรียมเพิ่ม</th></tr></thead><tbody>${applicants.map((score,i)=>`<tr><td><span class="rank ${i<3?'top':''}">${i+1}</span></td><td>นักศึกษาสมมติ ${i+1}</td><td><strong>${score.toFixed(1)}%</strong>${progress(score)}</td><td>${Math.max(0,score-3.2).toFixed(1)}%</td><td>${badges(job.matched.slice(0,i===0?undefined:2))}</td><td>${i===0?'✓ คุณมีทักษะครบทุกข้อ':badges(job.missing,true)}</td></tr>`).join('')}</tbody></table></div>`}</section>`;
    }
  }
  state.addEventListener('change',render);
  if(page==='resume') document.getElementById('resume-text').value=sampleText;
  document.querySelectorAll('form').forEach(form=>form.addEventListener('submit',event=>{
    event.preventDefault(); // Never transmit preview credentials or call the NLP service.
    if(!form.checkValidity())return;
    if(page==='resume') {
      state.value='populated';render();
      const alert=document.querySelector('.alert.success');alert.textContent='บันทึกตัวอย่างแล้ว · ทักษะด้านขวาเป็นข้อมูลจำลอง ไม่ใช่ผลวิเคราะห์ข้อความจริง';alert.hidden=false;
    } else if(page==='job-form') {
      const alert=document.querySelector('.alert.error');alert.className='alert success';alert.textContent='แสดงตัวอย่างการบันทึกสำเร็จ · ยังไม่มีการสร้างงานในระบบจริง';alert.hidden=false;
    } else if(page==='register') { location.href='login.html?registered=1'; }
    else if(page==='login') { location.href='dashboard.html'; }
  }));
  if(page==='register') { const params=new URLSearchParams(location.search);document.querySelectorAll('[name=role]').forEach(input=>{input.checked=input.value===(params.get('role')||'STUDENT')}); }
  render();
  if(page==='login' && new URLSearchParams(location.search).has('registered')) { const alerts=document.querySelectorAll('.alert.success');alerts[alerts.length-1].hidden=false; }
})();
