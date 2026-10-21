package com.workmatch.security;

import com.workmatch.entity.User;
import com.workmatch.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

/** โหลดผู้ใช้จากอีเมล แล้วให้สิทธิ์ ROLE_STUDENT หรือ ROLE_COMPANY ตามคอลัมน์ role */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository users;

    public CustomUserDetailsService(UserRepository users) {
        this.users = users;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        String normalized = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        User user = users.findByEmail(normalized)
                .orElseThrow(() -> new UsernameNotFoundException("ไม่พบผู้ใช้"));
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPasswordHash(),
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())));
    }
}
