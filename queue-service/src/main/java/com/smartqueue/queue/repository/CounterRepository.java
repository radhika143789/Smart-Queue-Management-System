package com.smartqueue.queue.repository;

import com.smartqueue.queue.entity.CounterEntity;
import com.smartqueue.queue.entity.ServiceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CounterRepository extends JpaRepository<CounterEntity, Long> {
    List<CounterEntity> findByServiceAndIsActiveTrue(ServiceEntity service);
}
