package com.smartqueue.admin.repository;

import com.smartqueue.admin.entity.ManagedServiceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ManagedServiceRepository extends JpaRepository<ManagedServiceEntity, Long> {
    List<ManagedServiceEntity> findByIsActiveTrue();
    Optional<ManagedServiceEntity> findByIdAndIsActiveTrue(Long id);
}
