package com.jeera.service;

import com.jeera.model.Comment;
import com.jeera.model.Issue;
import com.jeera.model.User;
import com.jeera.repository.CommentRepository;
import com.jeera.repository.IssueRepository;
import com.jeera.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CommentService {

  private final CommentRepository commentRepository;
  private final IssueRepository issueRepository;
  private final UserRepository userRepository;

  public Comment addComment(Issue issue, User author, String body) {
    Issue resolvedIssue = issueRepository.findById(issue.getId())
        .orElseThrow(() -> new EntityNotFoundException("Issue not found with id: " + issue.getId()));
    User resolvedAuthor = userRepository.findById(author.getId())
        .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + author.getId()));

    Comment comment = Comment.builder()
        .issue(resolvedIssue)
        .author(resolvedAuthor)
        .body(body)
        .createdAt(LocalDateTime.now())
        .build();

    return commentRepository.save(comment);
  }
}
