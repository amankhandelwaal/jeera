package com.jeera.repository;

import com.jeera.model.Issue;
import com.jeera.model.enums.IssueStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface IssueRepository
    extends JpaRepository<Issue, Long>, JpaSpecificationExecutor<Issue> {

  Optional<Issue> findByProjectIdAndIssueNumber(Long projectId, Integer issueNumber);

  List<Issue> findByProjectIdOrderByCreatedAtDesc(Long projectId);

  List<Issue> findByProjectIdAndStatus(Long projectId, IssueStatus status);

  long countByProjectId(Long projectId);

  long countByProjectIdAndStatusNotIn(Long projectId, Collection<IssueStatus> statuses);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      """
      update Issue i
      set i.duplicateOf = null
      where i.project.id = :projectId
      """)
  int clearDuplicateReferencesByProjectId(Long projectId);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  int deleteByProjectId(Long projectId);

  List<Issue> findByAssigneeIdAndStatusIn(Long assigneeId, Collection<IssueStatus> statuses);

  @Query(
      """
      select coalesce(max(i.issueNumber), 0)
      from Issue i
      where i.project.id = :projectId
      """)
  Integer findMaxIssueNumberByProjectId(Long projectId);

  @Query(
      """
      select i from Issue i
      join fetch i.project p
      join fetch i.reporter r
      left join fetch i.assignee a
      where p.id = :projectId
      order by i.createdAt desc
      """)
  List<Issue> findPageListByProjectId(Long projectId);

  @Query(
      """
      select i from Issue i
      join fetch i.project p
      join fetch i.reporter r
      left join fetch i.assignee a
      where i.id = :issueId
      """)
  Optional<Issue> findPageDetailById(Long issueId);
}
