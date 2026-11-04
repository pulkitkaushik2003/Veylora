package com.example.veylora.Repository;

import com.example.veylora.Model.UserWorkspaceData;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserWorkspaceDataRepository extends JpaRepository<UserWorkspaceData, Long> {

    List<UserWorkspaceData> findByUser_Id(Long userId);

    Optional<UserWorkspaceData> findByUser_IdAndDataKey(Long userId, String dataKey);
}
