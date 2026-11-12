/*
 * ==== Workmatch · Backend · ทดสอบ (JUnit) ====
 * ทดสอบ: ชื่อไทยและลำดับหมวดทักษะ
 * รัน: cd backend แล้ว mvnw test (ใช้ฐานข้อมูล H2 และจำลอง NLP ด้วย Mockito ไม่ต้องเปิด MySQL/NLP)
 */
package com.workmatch.service;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class SkillCategoriesTest {

    @Test
    void labelsAreThaiWithFallbacks() {
        assertThat(SkillCategories.label("programming")).isEqualTo("ภาษาโปรแกรม");
        assertThat(SkillCategories.label("SOFT")).isEqualTo("ทักษะทางสังคม (Soft skills)");
        assertThat(SkillCategories.label("brand-new")).isEqualTo("brand-new");
        assertThat(SkillCategories.label(null)).isEqualTo("อื่น ๆ");
        assertThat(SkillCategories.label("other")).isEqualTo("อื่น ๆ");
    }

    @Test
    void orderedFollowsStandardOrderUnknownLastNullLast() {
        assertThat(SkillCategories.ordered(Arrays.asList("soft", null, "zeta", "database", "programming", "alpha", "soft")))
                .containsExactly("programming", "database", "soft", "alpha", "zeta", null);
    }

    @Test
    void adminCategoriesComeFromTheSameList() {
        assertThat(DictionaryService.CATEGORIES).isEqualTo(SkillCategories.CODES).hasSize(13);
    }
}
