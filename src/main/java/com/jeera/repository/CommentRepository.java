package com.jeera.repository;

import com.jeera.model.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

  List<Comment> findByIssueIdOrderByCreatedAtAsc(Long issueId);
}
