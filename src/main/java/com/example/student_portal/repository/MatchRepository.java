package com.example.student_portal.repository;

import com.example.student_portal.entity.Match;
import com.example.student_portal.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for Match entity.
 * Used to fetch existing matches for display on dashboards.
 */
@Repository
public interface MatchRepository extends JpaRepository<Match, Long> {

    /**
     * Find all matches where the given user appears as tutor or tutee.
     */
    List<Match> findByTutorRequest_UserOrTuteeRequest_User(User tutor, User tutee);

    /**
     * Find all matches where user is the tutor.
     */
    List<Match> findByTutorRequest_User(User tutor);

    /**
     * Find all matches where user is the tutee.
     */
    List<Match> findByTuteeRequest_User(User tutee);

    /**
     * Find matches by tutor or tutee request ID.
     * Used for chat access control validation.
     */
    List<Match> findByTutorRequestIdOrTuteeRequestId(Long tutorRequestId, Long tuteeRequestId);
}