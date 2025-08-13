package com.example.student_portal.service;

import com.example.student_portal.entity.Match;
import com.example.student_portal.entity.Request;
import com.example.student_portal.entity.Timeslot;
import com.example.student_portal.entity.User;
import com.example.student_portal.model.RequestStatus;
import com.example.student_portal.repository.MatchRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service for storing and retrieving tutor–tutee matches.
 * Used by:
 *  - The scheduled matching job (writes matches)
 *  - Controllers (display matches to users/admins)
 */
@Service
@Transactional
public class MatchService {

    private final MatchRepository matchRepository;
    private final RequestService requestService;

    public MatchService(MatchRepository matchRepository,
                        RequestService requestService) {
        this.matchRepository = matchRepository;
        this.requestService = requestService;
    }

    /**
     * Saves a new tutor–tutee match and updates the involved requests' status to MATCHED.
     */
    public Match saveMatch(Request tutorRequest, Request tuteeRequest, Timeslot timeslot) {
        Match match = new Match(tutorRequest, tuteeRequest, timeslot);
        Match saved = matchRepository.save(match);

        // Update requests to MATCHED
        requestService.updateStatus(tutorRequest.getId(), RequestStatus.MATCHED);
        requestService.updateStatus(tuteeRequest.getId(), RequestStatus.MATCHED);

        return saved;
    }

    /**
     * Returns all matches where the user is involved (either as tutor or tutee).
     */
    public List<Match> findMatchesByUser(User user) {
        return matchRepository.findByTutorRequest_UserOrTuteeRequest_User(user, user);
    }

    /**
     * Deletes all matches – used before running the weekly match algorithm
     * if you want a clean slate (optional strategy).
     */
    public void clearAllMatches() {
        matchRepository.deleteAll();
    }

    /**
     * Returns all matches in the system (for admin dashboard).
     */
    public List<Match> findAllMatches() {
        return matchRepository.findAll();
    }
}