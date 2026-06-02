package com.jeera.repository;

import com.jeera.model.ActivityLog;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {

  List<ActivityLog> findByIssueIdOrderByTimestampDesc(Long issueId);

  @Query(
      """
      select a from ActivityLog a
      join fetch a.actor u
      where a.issue.id = :issueId
      order by a.timestamp desc
      """)
  List<ActivityLog> findPageFeedByIssueId(Long issueId);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("""
      delete from ActivityLog a
      where a.issue.project.id = :projectId
      """)
  int deleteByProjectId(Long projectId);
}
