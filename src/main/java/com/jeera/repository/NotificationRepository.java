package com.jeera.repository;

import com.jeera.model.Notification;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

  List<Notification> findByRecipientIdOrderByCreatedAtDesc(Long recipientId);

  List<Notification> findByRecipientIdAndIsReadFalseOrderByCreatedAtDesc(Long recipientId);

  long countByRecipientIdAndIsReadFalse(Long recipientId);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      """
      delete from Notification n
      where n.project is not null and n.project.id = :projectId
      """)
  int deleteByDirectProjectId(Long projectId);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      """
      delete from Notification n
      where n.issue is not null and n.issue.id in (
        select i.id from Issue i where i.project.id = :projectId
      )
      """)
  int deleteByIssueProjectId(Long projectId);

  @Query(
      """
      select n from Notification n
      left join fetch n.issue i
      left join fetch i.project ip
      left join fetch n.project p
      where n.recipient.id = :recipientId
      order by n.createdAt desc
      """)
  List<Notification> findPageFeedByRecipientId(Long recipientId);
}
