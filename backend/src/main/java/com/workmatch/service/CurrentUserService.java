package com.workmatch.service;

import com.workmatch.entity.User;
import com.workmatch.repository.UserRepository;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;

/** หาว่าใครกำลัง login อยู่ (ใช้ในงาน A3/A4) */
@Service
public class CurrentUserService {

    private final UserRepository users;

    public CurrentUserService(UserRepository users) {
        this.users = users;
    }

    /** คืนผู้ใช้ที่ login อยู่ หรือ Optional.empty() ถ้ายังไม่ login */
    public Optional<User> findCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }
        return users.findByEmail(auth.getName());
    }

    /** คืนผู้ใช้ที่ login อยู่ ใช้ในหน้าที่ Spring Security บังคับ login แล้ว */
    public User getCurrentUser() {
        return findCurrentUser().orElseThrow(() -> new IllegalStateException("ยังไม่ได้เข้าสู่ระบบ"));
    }
}
