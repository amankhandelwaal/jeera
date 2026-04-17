package com.jeera.repository;

import com.jeera.model.Issue;
import com.jeera.model.enums.IssueStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface IssueRepository extends JpaRepository<Issue, Long> {

  Optional<Issue> findByProjectIdAndIssueNumber(Long projectId, Integer issueNumber);

  List<Issue> findByProjectIdOrderByCreatedAtDesc(Long projectId);

  List<Issue> findByProjectIdAndStatus(Long projectId, IssueStatus status);

  List<Issue> findByAssigneeIdAndStatusIn(Long assigneeId, Collection<IssueStatus> statuses);
}
