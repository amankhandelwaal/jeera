package com.jeera.model;

import com.jeera.model.enums.IssuePriority;
import com.jeera.model.enums.IssueStatus;
import com.jeera.model.enums.IssueType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "issues", uniqueConstraints = {
    @UniqueConstraint(name = "uk_issue_project_issue_number", columnNames = { "project_id", "issue_number" })
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Issue {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "issue_number", nullable = false)
  private Integer issueNumber;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "project_id", nullable = false)
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  private Project project;

  @Column(nullable = false)
  private String title;

  @Column(columnDefinition = "TEXT")
  private String description;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private IssueType type;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private IssuePriority priority;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private IssueStatus status;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "reporter_id", nullable = false)
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  private User reporter;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "assignee_id")
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  private User assignee;

  @Column(nullable = false)
  private LocalDateTime createdAt;

  @Column(nullable = false)
  private LocalDateTime updatedAt;

  private LocalDateTime closedAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "duplicate_of_id")
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  private Issue duplicateOf;

  @OneToMany(mappedBy = "duplicateOf")
  @Builder.Default
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  private List<Issue> duplicateChildren = new ArrayList<>();

  @OneToMany(mappedBy = "issue")
  @Builder.Default
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  private List<Comment> comments = new ArrayList<>();

  @OneToMany(mappedBy = "issue")
  @Builder.Default
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  private List<ActivityLog> activityLogs = new ArrayList<>();

  @OneToMany(mappedBy = "issue")
  @Builder.Default
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  private List<Notification> notifications = new ArrayList<>();
}
