/*
 * ==== Workmatch · Backend · Service (ตรรกะหลักของระบบ) ====
 * ไฟล์: backend/src/main/java/com/workmatch/service/DictionaryService.java
 * หน้าที่: จัดการพจนานุกรมทักษะและคำพ้อง แล้วซิงก์กับ NLP (GET/PUT /dictionary)
 * เรียกใช้: SkillEntryRepository, SkillSynonymRepository, NlpClient
 * ถูกเรียกโดย: AdminController, JobController, JobPostingService, ResumeController, ResumeExtrasService,
 *     SkillEntry
 */
package com.workmatch.service;

import com.workmatch.entity.SkillEntry;
import com.workmatch.entity.SkillSynonym;
import com.workmatch.nlp.NlpClient;
import com.workmatch.nlp.NlpClient.DictionaryItem;
import com.workmatch.repository.SkillEntryRepository;
import com.workmatch.repository.SkillSynonymRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * พจนานุกรมทักษะและคำพ้องความหมาย (เอกสาร 1.4.3.2-1.4.3.3, รูปที่ 50-51)
 * ฐานข้อมูลเป็นต้นฉบับ: เปิดระบบครั้งแรกนำเข้าจาก NLP (skill_dictionary.csv) ต่อจากนั้นทุกครั้งที่ผู้ดูแลแก้ไข
 * จะส่งชุดที่ "เปิดใช้งาน" ทั้งหมดไปที่ NLP PUT /dictionary ให้การสกัดทักษะใช้ชุดใหม่ทันที
 */
@Service
public class DictionaryService {

    private static final Logger log = LoggerFactory.getLogger(DictionaryService.class);
    private static final Pattern NAME = Pattern.compile("^[^|,\\r\\n]{1,100}$");
    /** หมวดที่ผู้ดูแลเลือกได้ (ลำดับและชื่อไทยอยู่ที่ SkillCategories ที่เดียว) */
    public static final List<String> CATEGORIES = SkillCategories.CODES;

    private final SkillEntryRepository skills;
    private final SkillSynonymRepository synonyms;
    private final NlpClient nlpClient;

    public DictionaryService(SkillEntryRepository skills, SkillSynonymRepository synonyms, NlpClient nlpClient) {
        this.skills = skills;
        this.synonyms = synonyms;
        this.nlpClient = nlpClient;
    }

    /** ตอนเปิดระบบ: ตารางว่าง -> นำเข้าจาก NLP, มีข้อมูลแล้ว -> ส่งให้ NLP ใช้ชุดเดียวกัน (NLP ไม่พร้อมก็แค่บันทึก log) */
    @EventListener(ApplicationReadyEvent.class)
    public void syncOnStartup() {
        try {
            // เติมเฉพาะทักษะ/คำพ้องที่ฐานข้อมูลยังไม่มี (เช่น ทีม NLP เพิ่มคำใน skill_dictionary.csv ผ่าน git)
            // แล้วค่อยส่งชุดในฐานข้อมูลกลับให้ NLP ไม่อย่างนั้นชุดเก่าในฐานข้อมูลจะเขียนทับคำใหม่ในไฟล์
            int n = importFromNlp();
            if (n > 0) log.info("[dictionary] เติมทักษะ/คำพ้องใหม่จาก NLP {} รายการ", n);
            if (skills.count() > 0) pushToNlp();
        } catch (RuntimeException e) {
            log.warn("[dictionary] ซิงก์พจนานุกรมกับ NLP ไม่สำเร็จ (เปิด NLP แล้วรีสตาร์ต หรือแก้ไขพจนานุกรมอีกครั้ง): {}", e.getMessage());
        }
    }

    /**
     * นำเข้าแบบเติม: ทักษะที่ยังไม่มี และคำพ้องที่ยังไม่มี (รวมคำพ้องใหม่ของทักษะเดิม) คืนจำนวนรายการที่เพิ่ม
     * ไม่แก้หรือลบสิ่งที่ผู้ดูแลตั้งไว้แล้ว (ชื่อ หมวด สถานะเปิด/ปิด)
     */
    @Transactional
    public int importFromNlp() {
        List<DictionaryItem> items = nlpClient.fetchDictionary();
        if (items == null || items.isEmpty()) return 0;
        int count = 0;
        for (DictionaryItem item : items) {
            if (item == null || item.name() == null || item.name().isBlank()) continue;
            SkillEntry e = skills.findByNameIgnoreCase(item.name().trim()).orElse(null);
            if (e == null) {
                e = new SkillEntry();
                e.setName(item.name().trim());
                e.setCategory(item.category() == null || item.category().isBlank() ? "other" : item.category().trim());
                e = skills.save(e);
                count++;
            }
            if (item.aliases() != null) {
                for (String alias : item.aliases()) {
                    if (alias == null || alias.isBlank() || synonyms.findByAliasIgnoreCase(alias.trim()).isPresent()
                            || skills.findByNameIgnoreCase(alias.trim()).isPresent()) continue;
                    count++;
                    SkillSynonym s = new SkillSynonym();
                    s.setAlias(alias.trim());
                    s.setSkill(e);
                    synonyms.save(s);
                }
            }
        }
        return count;
    }

    /** ส่งพจนานุกรม (เฉพาะที่เปิดใช้งาน) ไปให้ NLP คืน true ถ้าสำเร็จ */
    @Transactional(readOnly = true)
    public boolean pushToNlp() {
        Map<Long, List<String>> aliasesBySkill = new LinkedHashMap<>();
        for (SkillSynonym s : synonyms.findAll()) {
            aliasesBySkill.computeIfAbsent(s.getSkill().getId(), k -> new ArrayList<>()).add(s.getAlias());
        }
        List<DictionaryItem> payload = new ArrayList<>();
        for (SkillEntry e : skills.findAllByOrderByIdAsc()) {
            if (!e.isActive()) continue;
            payload.add(new DictionaryItem(e.getName(), e.getCategory(), aliasesBySkill.getOrDefault(e.getId(), List.of())));
        }
        if (payload.isEmpty()) return false;
        try {
            nlpClient.replaceDictionary(payload);
            return true;
        } catch (RuntimeException e) {
            log.warn("[dictionary] ส่งพจนานุกรมให้ NLP ไม่สำเร็จ: {}", e.getMessage());
            return false;
        }
    }

    // ---------- ทักษะ ----------

    @Transactional(readOnly = true)
    public List<SkillEntry> search(String q) {
        String query = q == null ? "" : q.trim();
        return query.isEmpty() ? skills.findAllByOrderByIdAsc()
                : skills.findByNameContainingIgnoreCaseOrCategoryContainingIgnoreCaseOrderByIdAsc(query, query);
    }

    /** ทักษะที่เปิดใช้งาน (ตัวเลือกในฟอร์มประกาศงานและหน้าเพิ่มทักษะในเรซูเม่) */
    @Transactional(readOnly = true)
    public List<SkillEntry> activeSkills() {
        return skills.findByActiveTrueOrderByNameAsc();
    }

    /** หาทักษะที่เปิดใช้งานจากชื่อหรือคำพ้อง (ไม่สนตัวพิมพ์) */
    @Transactional(readOnly = true)
    public Optional<SkillEntry> resolve(String nameOrAlias) {
        String key = nameOrAlias == null ? "" : nameOrAlias.trim();
        if (key.isEmpty()) return Optional.empty();
        Optional<SkillEntry> direct = skills.findByNameIgnoreCase(key);
        if (direct.isPresent()) return direct.filter(SkillEntry::isActive);
        return synonyms.findByAliasIgnoreCase(key).map(SkillSynonym::getSkill).filter(SkillEntry::isActive);
    }

    @Transactional
    public SkillEntry addSkill(String name, String category) {
        String n = validName(name, "ชื่อทักษะ");
        String c = validCategory(category);
        if (skills.findByNameIgnoreCase(n).isPresent() || synonyms.findByAliasIgnoreCase(n).isPresent()) {
            throw new IllegalArgumentException("มีทักษะหรือคำพ้องชื่อ \"" + n + "\" อยู่แล้ว");
        }
        SkillEntry e = new SkillEntry();
        e.setName(n);
        e.setCategory(c);
        return skills.save(e);
    }

    @Transactional
    public void updateSkill(Long id, String name, String category) {
        SkillEntry e = skills.findById(id).orElseThrow(() -> new IllegalArgumentException("ไม่พบทักษะนี้"));
        String n = validName(name, "ชื่อทักษะ");
        skills.findByNameIgnoreCase(n).filter(o -> !o.getId().equals(id)).ifPresent(o -> {
            throw new IllegalArgumentException("มีทักษะชื่อ \"" + n + "\" อยู่แล้ว");
        });
        if (synonyms.findByAliasIgnoreCase(n).isPresent()) throw new IllegalArgumentException("\"" + n + "\" เป็นคำพ้องของทักษะอื่นอยู่แล้ว");
        e.setName(n);
        e.setCategory(validCategory(category));
        skills.save(e);
    }

    @Transactional
    public boolean toggleSkill(Long id) {
        SkillEntry e = skills.findById(id).orElseThrow(() -> new IllegalArgumentException("ไม่พบทักษะนี้"));
        e.setActive(!e.isActive());
        skills.save(e);
        return e.isActive();
    }

    // ---------- คำพ้องความหมาย ----------

    @Transactional(readOnly = true)
    public List<SkillSynonym> allSynonyms() {
        return synonyms.findAllByOrderByAliasAsc();
    }

    @Transactional
    public void addSynonym(String alias, String targetSkill) {
        String a = validName(alias, "คำศัพท์");
        SkillEntry target = skills.findByNameIgnoreCase(targetSkill == null ? "" : targetSkill.trim())
                .orElseThrow(() -> new IllegalArgumentException("ไม่พบทักษะหลัก \"" + targetSkill + "\" ในพจนานุกรม"));
        if (target.getName().equalsIgnoreCase(a)) throw new IllegalArgumentException("คำพ้องต้องไม่ซ้ำกับชื่อทักษะหลัก");
        if (skills.findByNameIgnoreCase(a).isPresent()) throw new IllegalArgumentException("\"" + a + "\" เป็นชื่อทักษะหลักอยู่แล้ว");
        if (synonyms.findByAliasIgnoreCase(a).isPresent()) throw new IllegalArgumentException("มีคำพ้อง \"" + a + "\" อยู่แล้ว");
        SkillSynonym s = new SkillSynonym();
        s.setAlias(a);
        s.setSkill(target);
        synonyms.save(s);
    }

    @Transactional
    public void deleteSynonym(Long id) {
        synonyms.deleteById(id);
    }

    private static String validName(String value, String label) {
        String v = value == null ? "" : value.trim();
        if (v.isEmpty()) throw new IllegalArgumentException("กรุณากรอก" + label);
        if (!NAME.matcher(v).matches()) throw new IllegalArgumentException(label + " ต้องยาวไม่เกิน 100 ตัวอักษร และห้ามมี | หรือ ,");
        return v;
    }

    private static String validCategory(String category) {
        String c = category == null ? "" : category.trim().toLowerCase(Locale.ROOT);
        if (!CATEGORIES.contains(c)) throw new IllegalArgumentException("กรุณาเลือกหมวดทักษะ");
        return c;
    }
}
