package com.flowable.demo.web.rest;

import com.flowable.demo.domain.model.User;
import com.flowable.demo.domain.model.Role;
import com.flowable.demo.domain.repository.UserRepository;
import com.flowable.demo.domain.repository.RoleRepository;
import com.flowable.demo.web.rest.dto.UserDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 用户管理 REST API
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "用户管理", description = "用户的管理操作")
public class UserResource {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final EntityManager entityManager;

    /**
     * 获取当前用户信息
     */
    @GetMapping("/current")
    @Operation(summary = "获取当前用户", description = "获取当前登录用户的详细信息")
    public ResponseEntity<UserDTO> getCurrentUser() {
        log.debug("REST request to get current user");

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            return ResponseEntity.badRequest().build();
        }

        String username = authentication.getName();
        Optional<User> userOptional = userRepository.findByUsernameAndActiveTrueWithRoles(username);

        if (userOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        User user = userOptional.get();
        return ResponseEntity.ok(convertToDTO(user));
    }

    /**
     * 根据用户名获取用户信息
     */
    @GetMapping("/username/{username}")
    @Operation(summary = "根据用户名获取用户", description = "根据用户名获取用户信息")
    public ResponseEntity<UserDTO> getUserByUsername(
            @Parameter(description = "用户名") @PathVariable String username) {
        log.debug("REST request to get user by username: {}", username);

        Optional<User> userOptional = userRepository.findByUsername(username);

        if (userOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        User user = userOptional.get();
        return ResponseEntity.ok(convertToDTO(user));
    }

    /**
     * 获取所有用户
     */
    @GetMapping
    @Operation(summary = "获取所有用户", description = "获取所有用户的分页列表")
    public ResponseEntity<Page<UserDTO>> getAllUsers(
            @Parameter(description = "是否只获取活跃用户") @RequestParam(defaultValue = "true") boolean activeOnly,
            Pageable pageable) {
        log.debug("REST request to get all users, activeOnly: {}", activeOnly);

        List<User> users;
        long total;

        if (activeOnly) {
            users = userRepository.findAllActiveUsers();
            total = users.size();
        } else {
            users = userRepository.findAll();
            total = userRepository.count();
        }

        // 手动分页
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), users.size());
        List<User> pageContent = users.subList(start, end);

        List<UserDTO> userDTOs = pageContent.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        Page<UserDTO> result = new PageImpl<>(userDTOs, pageable, total);
        return ResponseEntity.ok(result);
    }

    /**
     * 根据ID获取用户
     */
    @GetMapping("/{id}")
    @Operation(summary = "根据ID获取用户", description = "根据ID获取用户详细信息")
    public ResponseEntity<UserDTO> getUser(
            @Parameter(description = "用户ID") @PathVariable String id) {
        log.debug("REST request to get user: {}", id);

        Optional<User> userOptional = userRepository.findById(UUID.fromString(id));

        if (userOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        User user = userOptional.get();
        return ResponseEntity.ok(convertToDTO(user));
    }

    /**
     * 创建用户
     */
    @PostMapping
    @Transactional
    @Operation(summary = "创建用户", description = "创建新的用户")
    public ResponseEntity<UserDTO> createUser(@RequestBody UserDTO userDTO) {
        log.debug("REST request to create user: {}", userDTO.getUsername());

        if (userRepository.existsByUsername(userDTO.getUsername())) {
            return ResponseEntity.badRequest().build();
        }

        if (userRepository.existsByEmail(userDTO.getEmail())) {
            return ResponseEntity.badRequest().build();
        }

        User user = convertToEntity(userDTO);
        // 不设置ID，让JPA自动生成
        user.setActive(true);

        // 使用persist而不是save来确保创建新实体
        entityManager.persist(user);
        entityManager.flush(); // 确保立即获取生成的ID
        
        return ResponseEntity.ok(convertToDTO(user));
    }

    /**
     * 更新用户
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新用户", description = "更新指定用户的信息")
    public ResponseEntity<UserDTO> updateUser(
            @Parameter(description = "用户ID") @PathVariable String id,
            @RequestBody UserDTO userDTO) {
        log.debug("REST request to update user: {}", id);

        Optional<User> userOptional = userRepository.findById(UUID.fromString(id));
        if (userOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        User existingUser = userOptional.get();

        // 检查用户名和邮箱是否与其他用户冲突
        if (!existingUser.getUsername().equals(userDTO.getUsername()) &&
            userRepository.existsByUsername(userDTO.getUsername())) {
            return ResponseEntity.badRequest().build();
        }

        if (!existingUser.getEmail().equals(userDTO.getEmail()) &&
            userRepository.existsByEmail(userDTO.getEmail())) {
            return ResponseEntity.badRequest().build();
        }

        // 更新用户信息
        existingUser.setUsername(userDTO.getUsername());
        existingUser.setEmail(userDTO.getEmail());
        existingUser.setDepartment(userDTO.getDepartment());
        existingUser.setPhone(userDTO.getPhone());
        existingUser.setActive("ACTIVE".equals(userDTO.getStatus()));

        User savedUser = userRepository.save(existingUser);
        return ResponseEntity.ok(convertToDTO(savedUser));
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除用户", description = "删除指定的用户")
    public ResponseEntity<Void> deleteUser(
            @Parameter(description = "用户ID") @PathVariable String id) {
        log.debug("REST request to delete user: {}", id);

        if (!userRepository.existsById(UUID.fromString(id))) {
            return ResponseEntity.notFound().build();
        }

        userRepository.deleteById(UUID.fromString(id));
        return ResponseEntity.ok().build();
    }

    /**
     * 根据角色获取用户
     */
    @GetMapping("/role/{role}")
    @Operation(summary = "根据角色获取用户", description = "根据角色获取用户列表")
    public ResponseEntity<List<UserDTO>> getUsersByRole(
            @Parameter(description = "角色") @PathVariable String role) {
        log.debug("REST request to get users by role: {}", role);

        List<User> users = userRepository.findByRoleName(role);
        List<UserDTO> userDTOs = users.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(userDTOs);
    }

    /**
     * 根据部门获取用户
     */
    @GetMapping("/department/{department}")
    @Operation(summary = "根据部门获取用户", description = "根据部门获取用户列表")
    public ResponseEntity<List<UserDTO>> getUsersByDepartment(
            @Parameter(description = "部门") @PathVariable String department) {
        log.debug("REST request to get users by department: {}", department);

        List<User> users = userRepository.findByDepartmentAndActiveTrue(department);
        List<UserDTO> userDTOs = users.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(userDTOs);
    }

    /**
     * 转换为 DTO
     */
    private UserDTO convertToDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId().toString());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setFullName(user.getFullName());
        dto.setDepartment(user.getDepartment());
        dto.setPhone(user.getPhone());
        dto.setStatus(user.getActive() ? "ACTIVE" : "INACTIVE");
        dto.setCreatedAt(user.getCreatedAt());
        dto.setUpdatedAt(user.getUpdatedAt());

        if (user.getRoles() != null) {
            Set<String> roleNames = user.getRoles().stream()
                    .map(role -> role.getName())
                    .collect(Collectors.toSet());
            dto.setRoles(roleNames);
        }

        return dto;
    }

    /**
     * 转换为实体
     */
    private User convertToEntity(UserDTO dto) {
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setEmail(dto.getEmail());
        user.setDepartment(dto.getDepartment());
        user.setPhone(dto.getPhone());
        user.setActive("ACTIVE".equals(dto.getStatus()));
        
        // 设置密码
        if (dto.getPassword() != null && !dto.getPassword().trim().isEmpty()) {
            user.setPassword(passwordEncoder.encode(dto.getPassword()));
        } else {
            // 设置默认密码（在实际应用中应该要求用户提供密码）
            user.setPassword(passwordEncoder.encode("defaultPassword123"));
        }
        
        // 解析fullName为firstName和lastName
        if (dto.getFullName() != null && !dto.getFullName().trim().isEmpty()) {
            String[] nameParts = dto.getFullName().trim().split("\\s+", 2);
            if (nameParts.length == 1) {
                user.setFirstName(nameParts[0]);
                user.setLastName("");
            } else {
                user.setFirstName(nameParts[0]);
                user.setLastName(nameParts[1]);
            }
        }
        
        // 处理角色 - 确保角色是独立的新实例，避免关联问题
        Set<Role> roles = new java.util.HashSet<>();
        
        if (dto.getRoles() != null && !dto.getRoles().isEmpty()) {
            for (String roleName : dto.getRoles()) {
                Optional<Role> roleOpt = roleRepository.findByName(roleName);
                if (roleOpt.isPresent()) {
                    // 创建新的角色引用，只包含必要的信息
                    Role role = new Role();
                    role.setId(roleOpt.get().getId());
                    role.setName(roleOpt.get().getName());
                    roles.add(role);
                }
            }
        }
        
        // 如果没有找到任何角色，添加默认的USER角色
        if (roles.isEmpty()) {
            Optional<Role> userRole = roleRepository.findByName("USER");
            if (userRole.isPresent()) {
                Role role = new Role();
                role.setId(userRole.get().getId());
                role.setName(userRole.get().getName());
                roles.add(role);
            }
        }
        
        user.setRoles(roles);
        return user;
    }
}
