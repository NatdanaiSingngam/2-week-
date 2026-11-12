/*
 * ==== Workmatch · Backend · เข้าถึงฐานข้อมูล (Repository) ====
 * ไฟล์: backend/src/main/java/com/workmatch/repository/UserRepository.java
 * หน้าที่: ค้นหา / บันทึก / ลบข้อมูลในตาราง users (Spring Data สร้างคำสั่ง SQL จากชื่อเมธอดให้เอง)
 * อ่าน/เขียนตาราง: users (entity User) ผ่าน Spring Data JPA
 * ถูกเรียกโดย: AdminAccountInitializer, AdminService, CurrentUserService, CustomUserDetailsService,
 *     DemoDataLoader, ProfileService, RegistrationService
 */
package com.workmatch.repository;

import com.workmatch.entity.Role;
import com.workmatch.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    long countByRole(Role role);
    boolean existsByRole(Role role);
    List<User> findAllByOrderByCreatedAtDesc();
}
