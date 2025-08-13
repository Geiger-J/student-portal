package com.example.student_portal.service;

import com.example.student_portal.entity.Request;
import com.example.student_portal.entity.Subject;
import com.example.student_portal.entity.Timeslot;
import com.example.student_portal.entity.User;
import com.example.student_portal.model.RequestStatus;
import com.example.student_portal.model.RequestType;
import com.example.student_portal.repository.RequestRepository;
import com.example.student_portal.repository.SubjectRepository;
import com.example.student_portal.repository.TimeslotRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;

/**
 * Service for managing tutor/tutee requests.
 *
 * Responsibilities:
 *  - Creating new requests
 *  - Ensuring no duplicate active requests per user/subject/type
 *  - Updating request status
 *  - Fetching requests for matching algorithm and dashboards
 */
@Service
@Transactional
public class RequestService {

    private final RequestRepository requestRepository;
    private final SubjectRepository subjectRepository;
    private final TimeslotRepository timeslotRepository;

    public RequestService(RequestRepository requestRepository,
                          SubjectRepository subjectRepository,
                          TimeslotRepository timeslotRepository) {
        this.requestRepository = requestRepository;
        this.subjectRepository = subjectRepository;
        this.timeslotRepository = timeslotRepository;
    }

    /**
     * Creates a new request if a duplicate outstanding one doesn't already exist.
     */
    public Request createRequest(User user, Long subjectId, List<Long> timeslotIds, RequestType type) {
        Subject subject = subjectRepository.findById(subjectId)
            .orElseThrow(() -> new IllegalArgumentException("Subject not found"));

        boolean alreadyExists = requestRepository.existsByUserAndSubjectAndTypeAndStatus(
            user, subject, type, RequestStatus.OUTSTANDING
        );
        if (alreadyExists) {
            throw new IllegalStateException("You already have an active request for this subject and type");
        }

        List<Timeslot> timeslots = timeslotRepository.findAllById(timeslotIds);
        if (timeslots.isEmpty()) {
            throw new IllegalArgumentException("At least one valid timeslot is required");
        }

        Request request = new Request();
        request.setUser(user);
        request.setSubject(subject);
        request.setPossibleTimeslots(new HashSet<>(timeslots));
        request.setType(type);
        request.setStatus(RequestStatus.OUTSTANDING);
        request.setYearGroup(user.getYearGroup()); // store for convenience

        return requestRepository.save(request);
    }

    public Request updateStatus(Long requestId, RequestStatus status) {
        Request req = requestRepository.findById(requestId)
            .orElseThrow(() -> new IllegalArgumentException("Request not found"));
        req.setStatus(status);
        return requestRepository.save(req);
    }

    public List<Request> getOutstandingTutorRequests() {
        return requestRepository.findByTypeAndStatus(RequestType.TUTOR, RequestStatus.OUTSTANDING);
    }

    public List<Request> getOutstandingTuteeRequests() {
        return requestRepository.findByTypeAndStatus(RequestType.TUTEE, RequestStatus.OUTSTANDING);
    }

    public List<Request> getRequestsByUser(User user) {
        return requestRepository.findByUser(user);
    }

    public Request findById(Long id) {
        return requestRepository.findById(id).orElse(null);
    }

    public void deleteRequest(Long id) {
        requestRepository.deleteById(id);
    }

    /**
     * Fetch all outstanding requests regardless of type (for admin dashboard).
     */
    public List<Request> getAllOutstandingRequests() {
        return requestRepository.findByTypeAndStatus(RequestType.TUTOR, RequestStatus.OUTSTANDING)
            .stream()
            .collect(java.util.stream.Collectors.toList());
    }
}