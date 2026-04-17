package com.jeera.repository;

import com.jeera.model.ActivityLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {

  List<ActivityLog> findByIssueIdOrderByTimestampDesc(Long issueId);
}
