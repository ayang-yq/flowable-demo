package com.flowable.demo.domain.repository;

import com.flowable.demo.domain.model.Role;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * 角色仓储接口
 */
@Repository
public interface RoleRepository extends BaseRepository<Role, UUID> {
    
    /**
     * 根据角色名称查找角色
     */
    Optional<Role> findByName(String name);
    
    /**
     * 检查角色名称是否存在
     */
    boolean existsByName(String name);
}
