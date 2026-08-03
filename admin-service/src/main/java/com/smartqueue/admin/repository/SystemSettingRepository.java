package com.smartqueue.admin.repository;

import com.smartqueue.admin.entity.SystemSettingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SystemSettingRepository extends JpaRepository<SystemSettingEntity, Long> {
    Optional<SystemSettingEntity> findBySettingKey(String key);
    List<SystemSettingEntity> findByCategory(String category);
}
