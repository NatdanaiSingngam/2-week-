/*
 * ==== Workmatch · Backend · ทดสอบ (JUnit) ====
 * ทดสอบ: เกณฑ์ป้ายระดับ 0–40 ไม่เหมาะ / 41–89 ปานกลาง / 90–100 เหมาะ
 * รัน: cd backend แล้ว mvnw test (ใช้ฐานข้อมูล H2 และจำลอง NLP ด้วย Mockito ไม่ต้องเปิด MySQL/NLP)
 */
package com.workmatch.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MatchLevelsTest {

    @Test
    void boundaries() {
        assertThat(MatchLevels.level(0)).isEqualTo("low");
        assertThat(MatchLevels.level(40.0)).isEqualTo("low");
        assertThat(MatchLevels.level(40.1)).isEqualTo("medium");
        assertThat(MatchLevels.level(41)).isEqualTo("medium");
        assertThat(MatchLevels.level(89.9)).isEqualTo("medium");
        assertThat(MatchLevels.level(90)).isEqualTo("high");
        assertThat(MatchLevels.level(100)).isEqualTo("high");
        assertThat(MatchLevels.level(null)).isEqualTo("low");
    }

    @Test
    void thaiLabels() {
        assertThat(MatchLevels.label("high")).isEqualTo("เหมาะ");
        assertThat(MatchLevels.label("medium")).isEqualTo("ปานกลาง");
        assertThat(MatchLevels.label("low")).isEqualTo("ไม่เหมาะ");
    }
}
