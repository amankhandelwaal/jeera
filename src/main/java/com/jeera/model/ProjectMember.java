package com.jeera.model;

import com.jeera.model.enums.ProjectRole;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "project_members", uniqueConstraints = {
    @UniqueConstraint(name = "uk_project_member_project_user", columnNames = { "project_id", "user_id" })
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectMember {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "project_id", nullable = false)
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  private Project project;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  private User user;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private ProjectRole projectRole;

  @Column(nullable = false)
  private LocalDateTime addedAt;
}
