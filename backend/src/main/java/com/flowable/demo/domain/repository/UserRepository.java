package com.flowable.demo.domain.repository;

import com.flowable.demo.domain.model.User;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * 用户仓储接口
 */
@Repository
public interface UserRepository extends BaseRepository<User, UUID> {
    
    /**
     * 根据用户名查找用户
     */
    Optional<User> findByUsername(String username);
    
    /**
     * 根据用户名查找活跃用户
     */
    Optional<User> findByUsernameAndActiveTrue(String username);
    
    /**
     * 根据邮箱查找用户
     */
    Optional<User> findByEmail(String email);
    
    /**
     * 根据邮箱查找活跃用户
     */
    Optional<User> findByEmailAndActiveTrue(String email);
    
    /**
     * 根据用户名或邮箱查找用户
     */
    Optional<User> findByUsernameOrEmail(String username, String email);
    
    /**
     * 根据用户名或邮箱查找活跃用户
     */
    Optional<User> findByUsernameOrEmailAndActiveTrue(String username, String email);
    
    /**
     * 根据用户名查找活跃用户，同时加载角色
     */
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.roles WHERE u.username = :username AND u.active = true")
    Optional<User> findByUsernameAndActiveTrueWithRoles(@Param("username") String username);
    
    /**
     * 检查用户名是否存在
     */
    boolean existsByUsername(String username);
    
    /**
     * 检查邮箱是否存在
     */
    boolean existsByEmail(String email);
    
    /**
     * 根据角色名称查找用户
     */
    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.name = :roleName AND u.active = true")
    java.util.List<User> findByRoleName(@Param("roleName") String roleName);
    
    /**
     * 根据部门查找活跃用户
     */
    java.util.List<User> findByDepartmentAndActiveTrue(String department);
    
    /**
     * 查找所有活跃用户
     */
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.roles WHERE u.active = true ORDER BY u.username")
    java.util.List<User> findAllActiveUsers();
}
