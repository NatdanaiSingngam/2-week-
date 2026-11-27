/*
 * ==== Workmatch · Frontend · JavaScript ====
 * หน้าที่: เพิ่มความสะดวกในฟอร์ม (ปุ่มกำลังบันทึก, กันกดซ้ำ, ตัวนับตัวอักษร) ปิด JavaScript แล้วฟอร์มยังใช้ได้
 * ถูกโหลดโดย: fragments/layout.html (scripts) ทุกหน้า
 * ทำงานกับ: ฟอร์มที่มี data-busy, ช่องที่มี data-counter ในทุก template
 */
// Progressive enhancement only: the Spring Boot forms also work without JavaScript.
(() => {
  function resetForms() {
    document.querySelectorAll('form[data-busy]').forEach(form => {
      form.querySelectorAll('[data-original-label]').forEach(button => {
        button.textContent = button.dataset.originalLabel;
        button.disabled = false;
        delete button.dataset.originalLabel;
      });
      form.querySelectorAll('input[data-submitter-copy]').forEach(input => input.remove());
      form.removeAttribute('aria-busy');
    });
  }
  document.querySelectorAll('[data-counter]').forEach(input => {
    const counter = document.getElementById(input.dataset.counter);
    const update = () => { if (counter) counter.textContent = input.value.length.toLocaleString('th-TH') + ' / ' + input.maxLength.toLocaleString('th-TH'); };
    input.addEventListener('input', update);
    update();
  });
  document.querySelectorAll('form[data-busy]').forEach(form => {
    form.addEventListener('submit', event => {
      if (event.defaultPrevented || !form.checkValidity()) return;
      // ปุ่มที่ถูกกด (เช่น "บันทึกแบบร่าง" name=action value=draft): disabled แล้วค่าจะไม่ถูกส่ง จึงคัดลอกเป็น hidden input ก่อน
      const button = event.submitter || form.querySelector('[type="submit"]');
      if (!button || button.disabled || form.getAttribute('aria-busy') === 'true') { event.preventDefault(); return; }
      if (button.name) {
        const copy = document.createElement('input');
        copy.type = 'hidden';
        copy.name = button.name;
        copy.value = button.value;
        copy.dataset.submitterCopy = 'true';
        form.appendChild(copy);
      }
      button.dataset.originalLabel = button.textContent;
      button.textContent = form.dataset.busy;
      button.disabled = true;
      form.setAttribute('aria-busy', 'true');
    });
  });
  // Show/hide password: lets users check what they typed (fewer login errors), still works with password managers
  document.querySelectorAll('.password-field').forEach(wrap => {
    const input = wrap.querySelector('input');
    if (!input) return;
    const toggle = document.createElement('button');
    toggle.type = 'button';
    toggle.className = 'password-toggle';
    toggle.setAttribute('aria-controls', input.id);
    const sync = () => {
      const shown = input.type === 'text';
      toggle.textContent = shown ? 'ซ่อน' : 'แสดง';
      toggle.setAttribute('aria-pressed', String(shown));
      toggle.setAttribute('aria-label', shown ? 'ซ่อนรหัสผ่าน' : 'แสดงรหัสผ่าน');
    };
    toggle.addEventListener('click', () => { input.type = input.type === 'password' ? 'text' : 'password'; sync(); input.focus(); });
    // never submit the password while it is visible as plain text in the field type
    input.form && input.form.addEventListener('submit', () => { input.type = 'password'; sync(); });
    sync();
    wrap.appendChild(toggle);
  });
  // Avatar menu (<details>): close when clicking outside or pressing Escape
  document.querySelectorAll('details.user-menu').forEach(menu => {
    document.addEventListener('click', event => { if (menu.open && !menu.contains(event.target)) menu.open = false; });
    document.addEventListener('keydown', event => { if (event.key === 'Escape' && menu.open) { menu.open = false; menu.querySelector('summary').focus(); } });
  });
  // Register: change the name label/placeholder to match the selected role (นักศึกษา / สถานประกอบการ)
  const nameLabel = document.querySelector('[data-label-student]');
  if (nameLabel) {
    const nameInput = document.getElementById(nameLabel.getAttribute('for'));
    const syncRole = () => {
      const company = document.querySelector('input[name=role][value=COMPANY]:checked') !== null;
      nameLabel.textContent = company ? nameLabel.dataset.labelCompany : nameLabel.dataset.labelStudent;
      if (nameInput) nameInput.placeholder = company ? nameInput.dataset.phCompany : nameInput.dataset.phStudent;
    };
    document.querySelectorAll('input[name=role]').forEach(radio => radio.addEventListener('change', syncRole));
    syncRole();
  }
  // Register (รูปที่ 34-35): show the field group for the chosen role; hidden groups are disabled so they are not submitted
  const roleSections = document.querySelectorAll('[data-role-section]');
  if (roleSections.length) {
    const syncSections = () => {
      const checked = document.querySelector('input[name=role]:checked');
      const role = checked ? checked.value : 'STUDENT';
      roleSections.forEach(section => {
        const show = section.dataset.roleSection === role;
        section.hidden = !show;
        section.querySelectorAll('input, select, textarea').forEach(el => { el.disabled = !show; });
      });
      document.querySelectorAll('[data-role-text]').forEach(el => {
        el.textContent = role === 'COMPANY' ? el.dataset.textCompany : el.dataset.textStudent;
      });
      document.body.classList.toggle('theme-company', role === 'COMPANY' && document.body.dataset.roleTheme === 'true');
    };
    document.querySelectorAll('input[name=role]').forEach(radio => radio.addEventListener('change', syncSections));
    syncSections();
  }
  // Job form: structured inputs write into the original text field, which stays the submitted value (works without JavaScript)
  document.querySelectorAll('[data-builder]').forEach(wrap => {
    const box = wrap.querySelector('.builder');
    const text = wrap.querySelector('#compensation, #workSchedule');
    const kind = box.querySelector('[data-kind]');
    const amount = box.querySelector('[data-amount]');
    const times = box.querySelector('[data-times]');
    const start = box.querySelector('[data-start]');
    const end = box.querySelector('[data-end]');
    const saved = text.value.trim();
    let m;
    if (amount) {
      if (!saved) kind.value = '';
      else if (saved === 'ไม่มีค่าตอบแทน') kind.value = 'none';
      else if ((m = saved.match(/^([1-9][\d,]*) (บาท\/วัน|บาท\/เดือน)$/))) { amount.value = m[1].replace(/,/g, ''); kind.value = m[2]; }
      else kind.value = 'custom';
    } else {
      if (!saved) kind.value = '';
      else if ((m = saved.match(/^(จันทร์ - ศุกร์|จันทร์ - เสาร์|ทุกวัน)(?: (\d{2}:\d{2})-(\d{2}:\d{2}))?$/))) { kind.value = m[1]; start.value = m[2] || ''; end.value = m[3] || ''; }
      else kind.value = 'custom';
    }
    const sync = () => {
      const custom = kind.value === 'custom';
      text.hidden = !custom;
      if (amount) {
        const paid = kind.value.startsWith('บาท');
        amount.hidden = amount.disabled = !paid;
        amount.required = paid;
        if (!custom) text.value = paid ? (amount.value ? Number(amount.value).toLocaleString('en-US') + ' ' + kind.value : '') : (kind.value === 'none' ? 'ไม่มีค่าตอบแทน' : '');
      } else {
        const days = kind.value && !custom;
        times.hidden = !days;
        start.disabled = end.disabled = !days;
        const half = Boolean(start.value) !== Boolean(end.value);
        const same = start.value && start.value === end.value;
        end.setCustomValidity(days && half ? 'กรุณากรอกเวลาเริ่มและเวลาเลิกงานให้ครบ' : days && same ? 'เวลาเลิกงานต้องไม่ตรงกับเวลาเริ่มงาน' : '');
        if (!custom) text.value = days ? kind.value + (start.value && end.value ? ' ' + start.value + '-' + end.value : '') : '';
      }
    };
    [kind, amount, start, end].forEach(el => el && el.addEventListener('input', sync));
    kind.setAttribute('id', text.id + '-kind');
    wrap.querySelector('label').htmlFor = kind.id;
    text.setAttribute('aria-label', wrap.querySelector('label').textContent + ' (กำหนดเอง)');
    box.hidden = false;
    sync();
  });
  // ฟอร์มประกาศงาน: ช่องทักษะแบบป้าย (ทักษะที่เลือก กด × ลบได้ + พิมพ์ค้นหาเพิ่มจากพจนานุกรม)
  // checkbox name=skills ในรายการเดิมยังเป็นค่าที่ส่งไปกับฟอร์ม ป้ายแค่ติ๊ก/เอาติ๊กออกให้
  document.querySelectorAll('[data-skill-picker]').forEach(picker => {
    const form = picker.closest('form');
    const fallback = picker.querySelector('.skill-fallback');
    const list = picker.querySelector('.skill-toggles');
    const description = form.querySelector('#description');
    const extract = picker.querySelector('#extract-skills');
    const note = picker.querySelector('#extract-status');
    if (!fallback || !list) return;
    const boxes = () => [...list.querySelectorAll('input[name=skills]')];
    const find = name => boxes().find(cb => cb.value.toLowerCase() === name.toLowerCase());
    const removed = new Set(); // ทักษะที่ผู้ใช้ลบเอง ดึงจากรายละเอียดงานอีกรอบก็ไม่ใส่กลับ

    fallback.hidden = true;
    const flag = document.createElement('input');
    flag.type = 'hidden'; flag.name = 'skillsEdited'; flag.value = 'true';
    form.appendChild(flag);

    const box = document.createElement('div');
    box.className = 'skill-picker';
    box.innerHTML = '<div class="skill-chips" aria-live="polite"></div>'
      + '<p class="skill-empty field-note">ยังไม่มีทักษะ ดึงจากรายละเอียดงาน หรือพิมพ์ค้นหาด้านล่าง</p>'
      + '<div class="skill-search"><label for="skill-search" class="visually-hidden">ค้นหาทักษะเพื่อเพิ่ม</label>'
      + '<input id="skill-search" type="search" autocomplete="off" placeholder="พิมพ์เพื่อค้นหาและเพิ่มทักษะ เช่น Java, Figma" role="combobox" aria-expanded="false" aria-controls="skill-suggest" aria-autocomplete="list" />'
      + '<ul id="skill-suggest" class="skill-suggest" role="listbox" hidden></ul></div>';
    fallback.after(box);
    const chips = box.querySelector('.skill-chips');
    const empty = box.querySelector('.skill-empty');
    const search = box.querySelector('#skill-search');
    const suggest = box.querySelector('#skill-suggest');
    let active = -1;

    // ทักษะสำคัญ (★ = ทักษะที่ผู้สมัครต้องมี): ค่าตั้งต้นจาก hidden input ที่เซิร์ฟเวอร์ส่งมา
    const reqBox = form.querySelector('[data-required-skills]');
    const important = new Set(reqBox ? [...reqBox.querySelectorAll('input')].map(i => i.value) : []);
    const importantNote = form.querySelector('[data-important-note]');
    if (importantNote) importantNote.hidden = false;

    const render = () => {
      const selected = boxes().filter(cb => cb.checked);
      const names = selected.map(cb => cb.value);
      [...important].forEach(n => { if (!names.includes(n)) important.delete(n); }); // ลบทักษะ = ไม่สำคัญแล้ว
      chips.replaceChildren(...selected.map(cb => {
        const name = cb.value;
        const on = important.has(name);
        const chip = document.createElement('span');
        chip.className = 'skill-chip' + (on ? ' is-important' : '');
        // กดที่ชื่อทักษะ = สลับเป็นทักษะสำคัญ / ทักษะทั่วไป
        const label = document.createElement('button');
        label.type = 'button'; label.className = 'skill-chip-label';
        label.textContent = (on ? '★ ' : '') + name;
        label.setAttribute('aria-pressed', on ? 'true' : 'false');
        label.title = on ? 'ทักษะสำคัญ (กดเพื่อยกเลิก)' : 'กดเพื่อตั้งเป็นทักษะสำคัญ';
        label.addEventListener('click', () => {
          if (important.has(name)) important.delete(name); else important.add(name);
          render();
          chips.querySelectorAll('.skill-chip-label').forEach(b => { if (b.textContent.replace('★ ', '') === name) b.focus(); });
        });
        const x = document.createElement('button');
        x.type = 'button'; x.className = 'skill-chip-x'; x.textContent = '×';
        x.setAttribute('aria-label', 'ลบทักษะ ' + name);
        x.addEventListener('click', () => { cb.checked = false; removed.add(name.toLowerCase()); render(); search.focus(); });
        chip.append(label, x);
        return chip;
      }));
      empty.hidden = selected.length > 0;
      if (reqBox) reqBox.replaceChildren(...names.filter(n => important.has(n)).map(n => {
        const i = document.createElement('input'); i.type = 'hidden'; i.name = 'requiredSkills'; i.value = n; return i;
      }));
    };
    const add = name => {
      let cb = find(name);
      if (!cb) { // ทักษะที่ NLP พบแต่ไม่อยู่ในรายการ: เพิ่ม checkbox ซ่อนไว้ให้ส่งไปด้วย
        cb = document.createElement('input');
        cb.type = 'checkbox'; cb.name = 'skills'; cb.value = name;
        const label = document.createElement('label'); label.className = 'skill-toggle'; label.appendChild(cb);
        list.appendChild(label);
      }
      cb.checked = true;
      removed.delete(cb.value.toLowerCase());
    };
    const closeSuggest = () => { suggest.hidden = true; search.setAttribute('aria-expanded', 'false'); active = -1; };
    const showSuggest = () => {
      const q = search.value.trim().toLowerCase();
      if (!q) { closeSuggest(); return; }
      const hits = boxes().filter(cb => !cb.checked && cb.value.toLowerCase().includes(q))
        .sort((a, b) => a.value.toLowerCase().indexOf(q) - b.value.toLowerCase().indexOf(q)).slice(0, 8);
      suggest.replaceChildren(...(hits.length ? hits.map((cb, i) => {
        const li = document.createElement('li');
        li.id = 'skill-opt-' + i; li.setAttribute('role', 'option'); li.textContent = cb.value;
        li.addEventListener('mousedown', e => { e.preventDefault(); pick(cb.value); });
        return li;
      }) : [Object.assign(document.createElement('li'), { className: 'skill-suggest-none', textContent: 'ไม่พบทักษะนี้ในพจนานุกรม' })]));
      suggest.hidden = false;
      search.setAttribute('aria-expanded', 'true');
      active = hits.length ? 0 : -1;
      mark();
    };
    const options = () => [...suggest.querySelectorAll('[role=option]')];
    const mark = () => {
      options().forEach((li, i) => li.classList.toggle('active', i === active));
      if (active >= 0) search.setAttribute('aria-activedescendant', 'skill-opt-' + active); else search.removeAttribute('aria-activedescendant');
    };
    const pick = name => { add(name); render(); search.value = ''; closeSuggest(); search.focus(); };
    search.addEventListener('input', showSuggest);
    search.addEventListener('blur', closeSuggest);
    search.addEventListener('keydown', e => {
      const opts = options();
      if (e.key === 'ArrowDown' && opts.length) { e.preventDefault(); active = (active + 1) % opts.length; mark(); }
      else if (e.key === 'ArrowUp' && opts.length) { e.preventDefault(); active = (active - 1 + opts.length) % opts.length; mark(); }
      else if (e.key === 'Enter') { e.preventDefault(); if (active >= 0 && opts[active]) pick(opts[active].textContent); }
      else if (e.key === 'Escape') closeSuggest();
    });

    // ดึงทักษะจากรายละเอียดงาน: ใส่ป้ายให้ (ยกเว้นที่ผู้ใช้ลบเอง) ทำเองตอนออกจากช่องรายละเอียด หรือกดปุ่ม
    let extractedFor = description ? description.value.trim() : '';
    const run = async manual => {
      const text = description.value.trim();
      if (!text) { if (manual) { note.textContent = 'กรุณากรอกรายละเอียดงานก่อน'; description.focus(); } return; }
      extract.disabled = true;
      note.textContent = 'กำลังดึงทักษะ...';
      try {
        const body = new URLSearchParams({ description: description.value, _csrf: form.querySelector('input[name=_csrf]').value });
        const response = await fetch(extract.dataset.url, { method: 'POST', body, headers: { Accept: 'application/json' } });
        const isJson = (response.headers.get('content-type') || '').includes('application/json');
        const data = isJson ? await response.json() : {};
        if (!response.ok || !data.skills) throw new Error(data.error || (response.status === 403 || response.redirected ? 'เซสชันหมดอายุ กรุณาคัดลอกข้อความแล้วเข้าสู่ระบบใหม่' : 'ดึงทักษะไม่สำเร็จ ลองใหม่อีกครั้ง'));
        const fresh = manual ? data.skills : data.skills.filter(name => !removed.has(name.toLowerCase()));
        fresh.forEach(add);
        render();
        extractedFor = text;
        note.textContent = data.skills.length ? 'พบ ' + data.skills.length + ' ทักษะในรายละเอียดงาน' : 'ไม่พบทักษะในรายละเอียดงาน พิมพ์ค้นหาเพิ่มได้';
      } catch (e) {
        note.textContent = e instanceof TypeError ? 'เชื่อมต่อระบบไม่ได้ ลองใหม่อีกครั้ง' : e.message;
      } finally {
        extract.disabled = false;
      }
    };
    if (extract && description) {
      extract.hidden = false;
      extract.addEventListener('click', () => run(true));
      description.addEventListener('change', () => { if (description.value.trim() !== extractedFor) run(false); });
    }
    // ส่งฟอร์มก่อนดึงทักษะจากข้อความล่าสุด: ให้เซิร์ฟเวอร์อ่านทักษะจากรายละเอียดงานรวมเข้าไปด้วยเหมือนเดิม
    form.addEventListener('submit', () => { if (description && description.value.trim() !== extractedFor) flag.remove(); });
    render();
  });

  // ปุ่มที่เปิดกล่องยืนยัน (data-open-dialog="id ของ dialog") / ปุ่มปิดกล่อง (data-close-dialog)
  document.querySelectorAll('[data-open-dialog]').forEach(btn => {
    const dialog = document.getElementById(btn.dataset.openDialog);
    if (!dialog || typeof dialog.showModal !== 'function') return;
    btn.addEventListener('click', () => dialog.showModal());
    dialog.querySelectorAll('[data-close-dialog]').forEach(c => c.addEventListener('click', () => dialog.close()));
    dialog.addEventListener('click', e => { if (e.target === dialog) dialog.close(); }); // คลิกนอกกล่อง = ยกเลิก
  });

  // Forms that change or delete data ask for confirmation (data-confirm="ข้อความ")
  document.querySelectorAll('form[data-confirm]').forEach(form => {
    form.addEventListener('submit', event => { if (!window.confirm(form.dataset.confirm)) event.preventDefault(); });
  });
  // File inputs inside a drop zone show the chosen file name
  document.querySelectorAll('.dropzone input[type=file]').forEach(input => {
    const label = input.closest('.dropzone').querySelector('[data-file-name]');
    input.addEventListener('change', () => { if (label && input.files.length) label.textContent = input.files[0].name; });
  });
  window.addEventListener('pageshow', resetForms);
  const current = location.pathname.replace(/\/$/, '');
  document.querySelectorAll('.nav-links a').forEach(link => {
    const path = new URL(link.href).pathname.replace(/\/$/, '');
    const prefix = link.dataset.prefix;
    if (path === current || (prefix && current.startsWith(prefix))) link.setAttribute('aria-current', 'page');
  });
})();
