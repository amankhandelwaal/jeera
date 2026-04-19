package com.jeera.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "projects")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Project {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String name;

  @Column(columnDefinition = "TEXT")
  private String description;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "owner_id", nullable = false)
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  private User owner;

  @Column(nullable = false)
  private LocalDateTime createdAt;

  @OneToMany(mappedBy = "project")
  @Builder.Default
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  private List<ProjectMember> members = new ArrayList<>();

  @OneToMany(mappedBy = "project")
  @Builder.Default
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  private List<Issue> issues = new ArrayList<>();
}
