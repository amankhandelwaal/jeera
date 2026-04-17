package com.jeera.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

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

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "issue_id", nullable = false)
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  private Issue issue;

  @Builder.Default
  @Column(nullable = false)
  private boolean isRead = false;

  @Column(nullable = false)
  private LocalDateTime createdAt;
}
