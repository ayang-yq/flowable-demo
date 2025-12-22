package com.flowable.demo.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.Optional;
import java.util.UUID;

/**
 * 基础仓储接口
 */
@NoRepositoryBean
public interface BaseRepository<T, ID> extends JpaRepository<T, ID>, JpaSpecificationExecutor<T> {
    
    /**
     * 根据 ID 查找实体
     */
    default Optional<T> findByIdAndActiveTrue(ID id) {
        return findById(id); // Fallback to regular findById if no active field
    }
    
    /**
     * 检查实体是否存在
     */
    default boolean existsByIdAndActiveTrue(ID id) {
        return existsById(id); // Fallback to regular existsById if no active field
    }
    
    /**
     * 软删除（如果有 active 字段）
     */
    default void softDelete(ID id) {
        findById(id).ifPresent(entity -> {
            try {
                entity.getClass().getMethod("setActive", Boolean.class).invoke(entity, false);
                save(entity);
            } catch (Exception e) {
                // 如果没有 setActive 方法，则进行物理删除
                deleteById(id);
            }
        });
    }
    
    /**
     * 根据 UUID 查找实体（如果主键是 UUID）
     */
    default Optional<T> findByUuid(UUID uuid) {
        return findById((ID) uuid);
    }
}
