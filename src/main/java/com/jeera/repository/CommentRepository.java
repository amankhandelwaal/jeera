package com.jeera.repository;

import com.jeera.model.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

  List<Comment> findByIssueIdOrderByCreatedAtAsc(Long issueId);

  @Query("""
      select c from Comment c
      join fetch c.author a
      where c.issue.id = :issueId
      order by c.createdAt asc
      """)
  List<Comment> findPageFeedByIssueId(Long issueId);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("""
      delete from Comment c
      where c.issue.project.id = :projectId
      """)
  int deleteByProjectId(Long projectId);
}
