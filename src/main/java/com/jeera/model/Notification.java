package com.jeera.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "notifications")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "recipient_id", nullable = false)
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  private User recipient;

  @Column(nullable = false)
  private String message;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "issue_id")
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  private Issue issue;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "project_id")
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  private Project project;

  @Builder.Default
  @Column(nullable = false)
  private boolean isRead = false;

  @Column(nullable = false)
  private LocalDateTime createdAt;
}
