package com.example.student_portal.repository;

import com.example.student_portal.entity.Request;
import com.example.student_portal.entity.Subject;
import com.example.student_portal.entity.User;
import com.example.student_portal.model.RequestStatus;
import com.example.student_portal.model.RequestType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Repository for Request entity.
 */
@Repository
public interface RequestRepository extends JpaRepository<Request, Long> {

    List<Request> findByUser(User user);

    List<Request> findByTypeAndStatus(RequestType type, RequestStatus status);

    boolean existsByUserAndSubjectAndTypeAndStatus(User user, Subject subject, RequestType type, RequestStatus status);

    /**
     * Find requests by subject ID.
     */
    @Query("SELECT r FROM Request r WHERE r.subject.id = :subjectId")
    List<Request> findBySubjectId(@Param("subjectId") Long subjectId);

    /**
     * Find requests for a specific target week.
     */
    List<Request> findByTargetWeek(LocalDate targetWeek);

    /**
     * Find requests by type, status, and target week.
     */
    List<Request> findByTypeAndStatusAndTargetWeek(RequestType type, RequestStatus status, LocalDate targetWeek);
}