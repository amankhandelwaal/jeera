package com.jeera.repository;

import com.jeera.model.ActivityLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {

  List<ActivityLog> findByIssueIdOrderByTimestampDesc(Long issueId);

  @Query("""
      select a from ActivityLog a
      join fetch a.actor u
      where a.issue.id = :issueId
      order by a.timestamp desc
      """)
  List<ActivityLog> findPageFeedByIssueId(Long issueId);
}
