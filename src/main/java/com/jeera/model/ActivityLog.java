package com.jeera.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "activity_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityLog {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "issue_id", nullable = false)
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  private Issue issue;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "actor_id", nullable = false)
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  private User actor;

  @Column(nullable = false)
  private String action;

  private String oldValue;

  private String newValue;

  @Column(nullable = false)
  private LocalDateTime timestamp;
}
