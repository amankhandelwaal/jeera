package com.jeera.repository;

import com.jeera.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

  List<Notification> findByRecipientIdOrderByCreatedAtDesc(Long recipientId);

  List<Notification> findByRecipientIdAndIsReadFalseOrderByCreatedAtDesc(Long recipientId);

  long countByRecipientIdAndIsReadFalse(Long recipientId);

  @Query("""
      select n from Notification n
      left join fetch n.issue i
      left join fetch i.project ip
      left join fetch n.project p
      where n.recipient.id = :recipientId
      order by n.createdAt desc
      """)
  List<Notification> findPageFeedByRecipientId(Long recipientId);
}
