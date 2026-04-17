package com.jeera.model;

import com.jeera.model.enums.UserRole;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private String username;

  @Column(nullable = false, unique = true)
  private String email;

  @Column(nullable = false)
  private String passwordHash;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private UserRole systemRole;

  @Column(nullable = false)
  private LocalDateTime createdAt;

  @OneToMany(mappedBy = "owner")
  @Builder.Default
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  private List<Project> ownedProjects = new ArrayList<>();

  @OneToMany(mappedBy = "user")
  @Builder.Default
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  private List<ProjectMember> projectMemberships = new ArrayList<>();

  @OneToMany(mappedBy = "reporter")
  @Builder.Default
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  private List<Issue> reportedIssues = new ArrayList<>();

  @OneToMany(mappedBy = "assignee")
  @Builder.Default
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  private List<Issue> assignedIssues = new ArrayList<>();

  @OneToMany(mappedBy = "author")
  @Builder.Default
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  private List<Comment> comments = new ArrayList<>();

  @OneToMany(mappedBy = "actor")
  @Builder.Default
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  private List<ActivityLog> activityLogs = new ArrayList<>();

  @OneToMany(mappedBy = "recipient")
  @Builder.Default
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  private List<Notification> notifications = new ArrayList<>();
}
