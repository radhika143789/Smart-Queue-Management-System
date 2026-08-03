package com.smartqueue.queue.repository;

import com.smartqueue.queue.entity.ServiceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServiceRepository extends JpaRepository<ServiceEntity, Long> {
    List<ServiceEntity> findByIsActiveTrue();
    Optional<ServiceEntity> findByIdAndIsActiveTrue(Long id);
}
