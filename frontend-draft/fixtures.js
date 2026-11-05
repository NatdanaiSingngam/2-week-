// Fictional fixtures only. Never included by Thymeleaf production templates.
// Renders the preview pages with the same markup/classes as backend/src/main/resources/templates (Professional Blue redesign).
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
  const categoryNames = {programming:'ภาษาโปรแกรม',database:'ฐานข้อมูล',frontend:'พัฒนาหน้าเว็บ (Frontend)',backend:'พัฒนาระบบหลังบ้าน (Backend)',mobile:'แอปพลิเคชันมือถือ',data:'ข้อมูลและ AI',tools:'เครื่องมือพัฒนา',design:'ออกแบบ',infrastructure:'เครือข่ายและความปลอดภัย',testing:'การทดสอบซอฟต์แวร์',language:'ภาษาต่างประเทศ',office:'โปรแกรมสำนักงาน',soft:'ทักษะทางสังคม (Soft skills)'};

  const e = value => String(value).replace(/[&<>"']/g, char => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[char]));
  const svg = (paths, cls = 'icon') => `<svg class="${cls}" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true" focusable="false">${paths}</svg>`;
  const I = {
    briefcase:'<rect x="2" y="7" width="20" height="14" rx="2"/><path d="M16 7V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v2"/>',
    trend:'<path d="m22 7-8.5 8.5-5-5L2 17"/><path d="M16 7h6v6"/>',
    star:'<path d="M12 2l3.1 6.3 6.9 1-5 4.9 1.2 6.8L12 17.8 5.8 21l1.2-6.8-5-4.9 6.9-1z"/>',
    users:'<path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M22 21v-2a4 4 0 0 0-3-3.87M16 3.13a4 4 0 0 1 0 7.75"/>',
    layers:'<path d="M12 2 2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5"/>',
    building:'<path d="M3 21h18M5 21V7l8-4v18M19 21V11l-6-4"/>',
    search:'<circle cx="11" cy="11" r="8"/><path d="m21 21-4.3-4.3"/>',
    file:'<path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><path d="M14 2v6h6M12 18v-6M9 15h6"/>',
    plus:'<path d="M12 5v14M5 12h14"/>',
    pen:'<path d="M12 20h9"/><path d="M16.5 3.5a2.1 2.1 0 0 1 3 3L7 19l-4 1 1-4z"/>',
    back:'<path d="m15 18-6-6 6-6"/>',
    arrow:'<path d="M5 12h14M13 6l6 6-6 6"/>',
    chev:'<path d="m6 9 6 6 6-6"/>',
    grid:'<rect x="3" y="3" width="7" height="9" rx="1"/><rect x="14" y="3" width="7" height="5" rx="1"/><rect x="14" y="12" width="7" height="9" rx="1"/><rect x="3" y="16" width="7" height="5" rx="1"/>',
    doc:'<path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><path d="M14 2v6h6M16 13H8M16 17H8"/>',
    circlePlus:'<circle cx="12" cy="12" r="10"/><path d="M12 8v8M8 12h8"/>'
  };
  const level = score => score >= 70 ? 'high' : score >= 40 ? 'medium' : 'low';
  const levelText = lv => ({high:'เหมาะมาก',medium:'ปานกลาง',low:'ควรพัฒนาเพิ่ม'})[lv];
  const badges = (items, missing = false) => items.map(item => `<span class="badge ${missing ? 'badge-missing' : 'badge-ok'}">${e(item)}</span>`).join('');
  const empty = (icon, title, text, href, label, secondary) => `<section class="card empty"><div class="empty-icon">${svg(I[icon])}</div><h2>${title}</h2><p>${text}</p>${href ? `<div class="actions"><a class="button${secondary ? ' secondary' : ''}" href="${href}">${label}</a></div>` : ''}</section>`;
  const stat = (icon, color, label, value) => `<div class="stat"><span class="stat-icon ${color}" aria-hidden="true">${svg(I[icon])}</span><div><span class="stat-label">${label}</span><span class="stat-value">${value}</span></div></div>`;
  const legend = '<div class="legend" aria-label="ความหมายของระดับคะแนน"><span class="level high">เหมาะมาก 70%+</span><span class="level medium">ปานกลาง 40–69%</span><span class="level low">ควรพัฒนาเพิ่ม &lt;40%</span></div>';
  const pageHead = (eyebrowOrCrumb, title, description, actions) => `<div class="page-head"><div>${eyebrowOrCrumb}<h1>${title}</h1><p>${description}</p></div>${actions ? `<div class="actions">${actions}</div>` : ''}</div>`;
  const eyebrow = text => `<p class="eyebrow">${text}</p>`;
  const crumb = (href, label) => `<nav class="breadcrumb" aria-label="ตำแหน่งปัจจุบัน"><a href="${href}">${svg(I.back, 'icon-sm')}${label}</a></nav>`;

  // ---- header: menu and account follow the page's role ----
  const companyPage = ['jobs','job-form','job-detail'].includes(page);
  const authPage = ['login','register'].includes(page);
  const allowedStates = ['login','register','job-form'].includes(page) ? ['populated','error'] : page === 'dashboard' ? ['populated','empty','no-resume'] : ['populated','empty',...(page === 'resume' ? ['error'] : [])];
  [...state.options].forEach(option => { option.hidden = !allowedStates.includes(option.value); option.disabled = option.hidden; });
  document.getElementById('preview-links').innerHTML = companyPage
    ? `<a href="jobs.html">${svg(I.briefcase)}ตำแหน่งงานของฉัน</a><a href="job-form.html">${svg(I.circlePlus)}สร้างตำแหน่งใหม่</a>`
    : authPage ? '<a href="login.html">เข้าสู่ระบบ</a><a href="register.html">สมัครสมาชิก</a>'
    : `<a href="dashboard.html">${svg(I.grid)}ผลจับคู่</a><a href="resume.html">${svg(I.doc)}Resume ของฉัน</a>`;
  const current = location.pathname.split('/').pop();
  document.querySelectorAll('#preview-links a').forEach(link => { if (link.getAttribute('href') === current) link.setAttribute('aria-current', 'page'); });
  const account = document.getElementById('preview-account');
  account.hidden = authPage;
  if (!authPage) {
    account.querySelector('.avatar').textContent = companyPage ? 'บ' : 'น';
    account.querySelector('.user-meta strong').textContent = companyPage ? 'บริษัทสาธิต (สมมติ)' : 'นักศึกษาสาธิต (สมมติ)';
    account.querySelector('.user-meta span').textContent = companyPage ? 'บัญชีบริษัท' : 'บัญชีนักศึกษา';
  }

  function render() {
    const mode = state.value;
    if (page === 'job-form') { const alert = document.querySelector('.alert'); if (alert) alert.className = 'alert error'; }
    document.querySelectorAll('.alert').forEach(el => { el.hidden = true; });
    if (mode === 'error') {
      const alert = document.querySelector('.alert.error');
      if (alert) { alert.textContent = page === 'login' ? 'อีเมลหรือรหัสผ่านไม่ถูกต้อง' : 'ตัวอย่างข้อผิดพลาด: ไม่สามารถบันทึกข้อมูลได้ กรุณาลองอีกครั้ง'; alert.hidden = false; }
    }

    if (page === 'resume') {
      const list = ['empty','no-resume'].includes(mode) ? [] : skills;
      document.querySelector('#skills-count').textContent = `พบ ${list.length} ทักษะ`;
      document.getElementById('skill-results').innerHTML = list.length
        ? [...new Set(list.map(s => s.category))].map(category => `<section class="skill-group"><h3>${categoryNames[category] || category || 'อื่น ๆ'}</h3><div class="chips">${badges(list.filter(s => s.category === category).map(s => s.name))}</div></section>`).join('')
        : `<div class="empty"><div class="empty-icon">${svg(I.search)}</div><p>ยังไม่พบทักษะ ลองเพิ่มรายละเอียดทักษะใน resume</p></div>`;
      const steps = document.querySelectorAll('.progress-steps li');
      const done = list.length > 0;
      steps[0].className = 'done';
      steps[1].className = done ? 'done' : 'current';
      steps[2].className = done ? 'current' : '';
    }

    if (page === 'dashboard') {
      let html = pageHead(eyebrow('แดชบอร์ดนักศึกษา'), 'สวัสดี นักศึกษาสาธิต (สมมติ)', 'ดูว่าทักษะของคุณตรงกับตำแหน่งใด และควรพัฒนาอะไรเพิ่มก่อนเริ่มสหกิจศึกษา', `<a class="button" href="resume.html">${svg(I.pen)}แก้ไข resume</a>`);
      if (mode === 'no-resume') html += empty('file', 'คุณยังไม่ได้กรอก resume', 'เพิ่มประสบการณ์และทักษะของคุณ ระบบจะวิเคราะห์และจับคู่กับตำแหน่งสหกิจศึกษาให้ทันที', 'resume.html', 'เริ่มกรอก resume');
      else if (mode === 'empty') html += empty('search', 'ยังไม่มีตำแหน่งงานที่จับคู่ได้', 'ลองเพิ่มรายละเอียดทักษะใน resume หรือกลับมาดูอีกครั้งเมื่อมีตำแหน่งงานใหม่', 'resume.html', 'แก้ไข resume', true);
      else {
        html += `<div class="stats">${stat('briefcase', '', 'ตำแหน่งที่จับคู่ได้', `${jobs.length} ตำแหน่ง`)}${stat('trend', 'green', 'คะแนนสูงสุด', `${jobs[0].score.toFixed(1)}%`)}${stat('star', 'amber', 'ตำแหน่งที่เหมาะมาก (70%+)', `${jobs.filter(j => j.score >= 70).length} ตำแหน่ง`)}</div>`;
        html += `<div class="section-title"><h2>โอกาสที่เหมาะกับคุณ</h2>${legend}</div><div class="match-list">`;
        html += jobs.map((job, i) => {
          const lv = level(job.score);
          return `<article class="match-card"><div class="match-main"><div class="ring ${lv}" style="--p:${job.score}" role="img" aria-label="คะแนนความเหมาะสม ${job.score.toFixed(1)}%"><strong>${job.score.toFixed(1)}%</strong></div><div class="match-info"><span class="rank-tag">อันดับ ${i + 1}</span><h3>${e(job.title)}</h3><span class="match-company">${svg(I.building, 'icon-sm')}<span>${e(job.companyName)}</span></span><div class="match-meta"><span class="level ${lv}">${levelText(lv)}</span><span title="ค่า Cosine similarity ระหว่างทักษะใน resume กับทักษะของตำแหน่ง">ความคล้ายของทักษะ ${job.cosine.toFixed(1)}%</span></div></div></div>`
            + `<details class="skills"><summary>ดูทักษะทั้งหมด <span class="summary-count">· ตรง ${job.matched.length} จาก ${job.matched.length + job.missing.length} ทักษะ</span>${svg(I.chev, 'icon-sm chev')}</summary><div class="skill-cols"><section><h4>ทักษะที่คุณมี</h4><div class="chips">${badges(job.matched)}</div></section><section><h4>ควรเตรียมเพิ่ม</h4><div class="chips">${job.missing.length ? badges(job.missing, true) : '<span class="badge badge-ok">คุณมีทักษะครบทุกข้อ</span>'}</div></section></div></details></article>`;
        }).join('') + '</div>';
      }
      document.getElementById('preview-content').innerHTML = html;
    }

    if (page === 'jobs') {
      let html = pageHead(eyebrow('พื้นที่ทำงานของบริษัท'), 'ตำแหน่งงานของฉัน', 'จัดการโอกาสสหกิจศึกษา และค้นหานักศึกษาที่มีทักษะตรงกับทีมของคุณ', `<a class="button" href="job-form.html">${svg(I.plus)}สร้างตำแหน่งใหม่</a>`);
      if (mode === 'empty') html += empty('briefcase', 'ยังไม่มีตำแหน่งงาน', 'เริ่มสร้างตำแหน่งแรก ระบบจะดึงทักษะจากรายละเอียดงานและจับคู่กับนักศึกษาให้ทันที', 'job-form.html', 'สร้างตำแหน่งแรก');
      else {
        html += `<div class="stats">${stat('briefcase', '', 'ตำแหน่งที่เปิดรับ', `${jobs.length} ตำแหน่ง`)}${stat('users', 'green', 'ผู้สมัครที่จับคู่ได้ (รวม)', `${jobs.length * 4} คน`)}</div>`;
        html += `<div class="section-title"><h2>ตำแหน่งงานทั้งหมด</h2><span class="count">${jobs.length} ตำแหน่ง</span></div><div class="job-grid">`;
        html += jobs.map(job => `<article class="job-card"><div><p class="eyebrow">สหกิจศึกษา</p><h3><a href="job-detail.html?id=${job.id}">${e(job.title)}</a></h3></div><div class="job-facts"><span>${svg(I.layers, 'icon-sm')}<span>ทักษะที่ต้องการ <strong>${job.matched.length + job.missing.length}</strong></span></span><span>${svg(I.users, 'icon-sm')}<span>ผู้สมัครที่จับคู่ได้ <strong>4</strong></span></span></div><span class="job-link" aria-hidden="true">ดูผู้สมัคร${svg(I.arrow, 'icon-sm')}</span></article>`).join('') + '</div>';
      }
      document.getElementById('preview-content').innerHTML = html;
    }

    if (page === 'job-detail') {
      const id = Number(new URLSearchParams(location.search).get('id') || 1);
      const job = jobs.find(j => j.id === id);
      if (!job) { document.getElementById('preview-content').innerHTML = empty('search', 'ไม่พบตำแหน่งงาน', 'ตำแหน่งนี้อาจถูกลบไปแล้ว หรือเป็นของบริษัทอื่น', 'jobs.html', 'กลับไปรายการตำแหน่ง'); return; }
      const applicants = mode === 'empty' ? [] : [100, 85, 66.7, 20];
      const required = [...job.matched, ...job.missing];
      let html = pageHead(crumb('jobs.html', 'กลับไปรายการตำแหน่ง'), e(job.title), 'ทักษะที่ต้องการ และนักศึกษาที่จับคู่กับตำแหน่งนี้');
      html += `<div class="stats">${stat('layers', '', 'ทักษะที่ต้องการ', `${required.length} ทักษะ`)}${stat('users', 'green', 'นักศึกษาที่จับคู่ได้', `${applicants.length} คน`)}${stat('trend', 'amber', 'คะแนนสูงสุด', applicants.length ? `${applicants[0].toFixed(1)}%` : '–')}</div>`;
      html += `<section class="card" style="margin-top:20px"><h2>รายละเอียดงาน</h2><p class="pre-line">ร่วมพัฒนา ${e(job.title)} กับทีมของเรา โดยมีพี่เลี้ยงให้คำแนะนำตลอดการสหกิจศึกษา</p><h3>ทักษะที่ต้องการ</h3><div class="chips">${required.map(s => `<span class="badge">${e(s)}</span>`).join('')}</div></section>`;
      html += `<section class="card"><div class="card-head"><h2>นักศึกษาที่จับคู่ได้</h2>${legend}</div><p class="field-note" style="margin:-6px 0 14px">เรียงตามคะแนนที่ระบบส่งมา · แสดงเฉพาะชื่อและข้อมูลทักษะ</p>`;
      html += applicants.length === 0
        ? `<div class="empty"><div class="empty-icon">${svg(I.users)}</div><p>ยังไม่มีผู้สมัครที่จับคู่ได้</p></div>`
        : `<div class="table-wrap"><table class="applicants"><thead><tr><th scope="col">อันดับ</th><th scope="col">ชื่อนักศึกษา</th><th scope="col">คะแนน</th><th scope="col" title="ค่า Cosine similarity ระหว่างทักษะของนักศึกษากับตำแหน่ง">ความคล้ายของทักษะ</th><th scope="col">ทักษะที่มี</th><th scope="col">ควรเตรียมเพิ่ม</th></tr></thead><tbody>${applicants.map((score, i) => {
            const lv = level(score);
            return `<tr><td><span class="rank ${i < 3 ? 'top' : ''}">${i + 1}</span></td><td><strong>นักศึกษาสมมติ ${i + 1}</strong></td><td><div class="score-cell"><strong>${score.toFixed(1)}%</strong><div class="bar ${lv}" role="img" aria-label="คะแนน ${score.toFixed(1)}%"><span style="width:${score}%"></span></div><span class="level ${lv}">${levelText(lv)}</span></div></td><td>${Math.max(0, score - 3.2).toFixed(1)}%</td><td><div class="chips">${badges(i === 0 ? required : job.matched.slice(0, 2))}</div></td><td><div class="chips">${i === 0 ? '<span class="badge badge-ok">มีทักษะครบทุกข้อ</span>' : badges(job.missing, true)}</div></td></tr>`;
          }).join('')}</tbody></table></div>`;
      document.getElementById('preview-content').innerHTML = html + '</section>';
    }
  }

  state.addEventListener('change', render);
  if (page === 'resume') { const ta = document.getElementById('resume-text'); ta.value = sampleText; ta.dispatchEvent(new Event('input')); }
  document.querySelectorAll('form').forEach(form => form.addEventListener('submit', event => {
    event.preventDefault(); // Never transmit preview credentials or call the NLP service.
    if (!form.checkValidity()) return;
    if (page === 'resume') {
      state.value = 'populated'; render();
      const alert = document.querySelector('.alert.success'); alert.textContent = 'บันทึกตัวอย่างแล้ว · ทักษะด้านขวาเป็นข้อมูลจำลอง ไม่ใช่ผลวิเคราะห์ข้อความจริง'; alert.hidden = false;
    } else if (page === 'job-form') {
      const alert = document.querySelector('.alert.error'); alert.className = 'alert success'; alert.textContent = 'แสดงตัวอย่างการบันทึกสำเร็จ · ยังไม่มีการสร้างงานในระบบจริง'; alert.hidden = false;
    } else if (page === 'register') { location.href = 'login.html?registered=1'; }
    else if (page === 'login') { location.href = 'dashboard.html'; }
  }));
  if (page === 'register') { const params = new URLSearchParams(location.search); document.querySelectorAll('[name=role]').forEach(input => { input.checked = input.value === (params.get('role') || 'STUDENT'); }); }
  render();
  if (page === 'login' && new URLSearchParams(location.search).has('registered')) { const alerts = document.querySelectorAll('.alert.success'); alerts[alerts.length - 1].hidden = false; }
})();
