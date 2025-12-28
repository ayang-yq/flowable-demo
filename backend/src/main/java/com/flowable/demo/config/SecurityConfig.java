package com.flowable.demo.config;

import com.flowable.demo.service.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.security.web.firewall.StrictHttpFirewall;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * Spring Security 配置类
 *
 * @author Flowable Demo
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService customUserDetailsService;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList("http://localhost:3000", "http://127.0.0.1:3000"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public HttpFirewall allowJsonBodyHttpFirewall() {
        StrictHttpFirewall firewall = new StrictHttpFirewall();
        // 允许包含JSON字符的参数名
        firewall.setAllowUrlEncodedSlash(true);
        firewall.setAllowBackSlash(true);
        firewall.setAllowUrlEncodedDoubleSlash(true);
        return firewall;
    }

    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
        AuthenticationManagerBuilder auth = http.getSharedObject(AuthenticationManagerBuilder.class);
        auth.userDetailsService(customUserDetailsService).passwordEncoder(passwordEncoder);
        return auth.build();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .authorizeHttpRequests(authz -> authz
                // 允许 Swagger UI 相关路径无需认证
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-resources/**", "/webjars/**").permitAll()
                // 允许带 context path 的 Swagger 路径
                .requestMatchers("/api/swagger-ui/**", "/api/v3/api-docs/**", "/api/swagger-resources/**", "/api/webjars/**").permitAll()
                // 允许额外的 Swagger 路径
                .requestMatchers("/swagger/**", "/api/swagger/**").permitAll()
                // 允许 OpenAPI 路径
                .requestMatchers("/api-docs/**", "/api/api-docs/**").permitAll()
                // 允许 H2 Console 路径（开发环境）
                .requestMatchers("/h2-console/**", "/api/h2-console/**").permitAll()
                // 允许健康检查端点无需认证
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                // 允许获取当前用户信息的端点（用于登录验证）
                .requestMatchers("/api/users/current", "/api/users/username/*").permitAll()
                // 允许创建用户端点（用于初始化）
                .requestMatchers("/api/users").permitAll()
                // 允许 Flowable REST API 端点
                .requestMatchers("/flowable-rest/**").permitAll()
                // 其他API端点需要认证
                .requestMatchers("/api/**").authenticated()
                // 其他所有请求需要认证
                .anyRequest().authenticated()
            )
            .httpBasic(httpBasic -> {})
            .formLogin(formLogin -> formLogin.disable())
            // 禁用 X-Frame-Options header 以允许 H2 Console 在 iframe 中显示
            .headers(headers -> headers.frameOptions(frameOptions -> frameOptions.disable()));

        return http.build();
    }
}
