package com.example.veylora.Repository;

import com.example.veylora.Model.DeviceTask;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DeviceTaskRepository extends JpaRepository<DeviceTask, Long> {

    /** Locks the row so two polls can never claim the same task. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<DeviceTask> findFirstByDevice_IdAndStatusOrderByIdAsc(
            Long deviceId, DeviceTask.TaskStatus status);

    Optional<DeviceTask> findByIdAndDevice_Id(Long id, Long deviceId);

    List<DeviceTask> findTop50ByDevice_IdOrderByIdDesc(Long deviceId);

    List<DeviceTask> findByDevice_IdAndStatus(Long deviceId, DeviceTask.TaskStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from DeviceTask t where t.id = :id")
    Optional<DeviceTask> lockById(@Param("id") Long id);
}
