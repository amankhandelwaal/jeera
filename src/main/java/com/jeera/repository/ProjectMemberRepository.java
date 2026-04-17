package com.jeera.repository;

import com.jeera.model.ProjectMember;
import com.jeera.model.enums.ProjectRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {

  Optional<ProjectMember> findByProjectIdAndUserId(Long projectId, Long userId);

  boolean existsByProjectIdAndUserId(Long projectId, Long userId);

  List<ProjectMember> findByProjectId(Long projectId);

  List<ProjectMember> findByUserId(Long userId);

  List<ProjectMember> findByProjectIdAndProjectRole(Long projectId, ProjectRole projectRole);
}
