package com.flowable.demo.service;

import com.flowable.demo.domain.model.Role;
import com.flowable.demo.domain.model.User;
import com.flowable.demo.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 自定义用户详情服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("Authenticating user: {}", username);

        // 使用JOIN FETCH查询来避免懒加载问题
        User user = userRepository.findByUsernameAndActiveTrueWithRoles(username)
                .orElseThrow(() -> {
                    log.warn("User not found: {}", username);
                    return new UsernameNotFoundException("User not found: " + username);
                });

        // 检查用户是否激活
        if (!user.getActive()) {
            log.warn("User is not active: {}", username);
            throw new UsernameNotFoundException("User is not active: " + username);
        }

        Set<Role> roles = user.getRoles();

        log.debug("User authenticated successfully: {}", username);
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                user.getActive(),
                true, // account non expired
                true, // credentials non expired
                true, // account non locked
                getAuthorities(roles)
        );
    }

    /**
     * 转换角色为权限
     */
    private Collection<? extends GrantedAuthority> getAuthorities(Collection<Role> roles) {
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName().toUpperCase()))
                .collect(Collectors.toList());
    }
}
