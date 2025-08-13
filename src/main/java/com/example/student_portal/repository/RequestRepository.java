package com.example.student_portal.repository;

import com.example.student_portal.entity.Request;
import com.example.student_portal.entity.Subject;
import com.example.student_portal.entity.User;
import com.example.student_portal.model.RequestStatus;
import com.example.student_portal.model.RequestType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for Request entity.
 */
@Repository
public interface RequestRepository extends JpaRepository<Request, Long> {

    List<Request> findByUser(User user);

    List<Request> findByTypeAndStatus(RequestType type, RequestStatus status);

    boolean existsByUserAndSubjectAndTypeAndStatus(User user, Subject subject, RequestType type, RequestStatus status);
}