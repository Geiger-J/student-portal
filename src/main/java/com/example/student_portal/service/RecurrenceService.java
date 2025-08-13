package com.example.student_portal.service;

import com.example.student_portal.entity.Request;
import com.example.student_portal.model.RequestStatus;
import com.example.student_portal.model.RequestType;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing weekly recurring tutoring requests.
 *
 * Responsibilities:
 * - Generate next week's auto-requests for active recurring pairs
 * - Handle recurrence negotiation between tutees and tutors
 * - Manage recurrence lifecycle (start, pause, stop)
 * 
 * Weekly lifecycle:
 * 1. Tutee requests weekly recurrence on matched request
 * 2. Tutor accepts/declines weekly recurrence
 * 3. If accepted, system generates new requests for next week
 * 4. Process repeats until either party cancels recurrence
 */
@Service
@Transactional
public class RecurrenceService {
    
    private static final Logger logger = LoggerFactory.getLogger(RecurrenceService.class);
    
    private final RequestService requestService;
    
    public RecurrenceService(RequestService requestService) {
        this.requestService = requestService;
    }
    
    /**
     * Generate auto-requests for the upcoming week based on active recurring pairs.
     * Called by scheduled job before the weekly matching run.
     */
    public void generateRecurringRequests() {
        LocalDate upcomingWeek = getUpcomingMondayDate();
        generateRecurringRequestsForWeek(upcomingWeek);
    }
    
    /**
     * Generate recurring requests for a specific target week.
     */
    public void generateRecurringRequestsForWeek(LocalDate targetWeek) {
        logger.info("Generating recurring requests for week {}", targetWeek);
        
        // Find all matched requests that are marked as recurring
        List<Request> recurringRequests = getAllRecurringActiveRequests();
        
        int generatedCount = 0;
        
        for (Request request : recurringRequests) {
            if (request.getMatchedPartner() != null && 
                request.getMatchedPartner().isRecurring()) {
                
                try {
                    generateRecurringPair(request, request.getMatchedPartner(), targetWeek);
                    generatedCount++;
                } catch (Exception e) {
                    logger.error("Failed to generate recurring request pair for {}: {}", 
                               request.getUser().getFullName(), e.getMessage());
                }
            }
        }
        
        logger.info("Generated {} recurring request pairs for week {}", generatedCount, targetWeek);
    }
    
    /**
     * Request weekly recurrence for a matched tutee request.
     * This marks the tutee request as wanting recurrence.
     */
    public void requestRecurrence(Long tuteeRequestId) {
        Request tuteeRequest = requestService.getRequestById(tuteeRequestId);
        
        if (tuteeRequest == null) {
            throw new IllegalArgumentException("Request not found");
        }
        
        if (tuteeRequest.getType() != RequestType.TUTEE) {
            throw new IllegalArgumentException("Only tutee requests can initiate recurrence");
        }
        
        if (tuteeRequest.getStatus() != RequestStatus.MATCHED) {
            throw new IllegalArgumentException("Request must be matched to request recurrence");
        }
        
        tuteeRequest.setIsRecurring(true);
        requestService.updateRequest(tuteeRequest);
        
        logger.info("Recurrence requested for tutee request {}", tuteeRequestId);
    }
    
    /**
     * Accept weekly recurrence for a matched tutor request.
     * This confirms the recurrence and both requests become recurring.
     */
    public void acceptRecurrence(Long tutorRequestId) {
        Request tutorRequest = requestService.getRequestById(tutorRequestId);
        
        if (tutorRequest == null) {
            throw new IllegalArgumentException("Request not found");
        }
        
        if (tutorRequest.getType() != RequestType.TUTOR) {
            throw new IllegalArgumentException("Only tutor requests can accept recurrence");
        }
        
        if (tutorRequest.getStatus() != RequestStatus.MATCHED) {
            throw new IllegalArgumentException("Request must be matched to accept recurrence");
        }
        
        Request matchedPartner = tutorRequest.getMatchedPartner();
        if (matchedPartner == null || !matchedPartner.isRecurring()) {
            throw new IllegalArgumentException("Partner has not requested recurrence");
        }
        
        tutorRequest.setIsRecurring(true);
        requestService.updateRequest(tutorRequest);
        
        logger.info("Recurrence accepted for tutor request {} with partner {}", 
                   tutorRequestId, matchedPartner.getId());
    }
    
    /**
     * Cancel recurrence for a request (either tutor or tutee can cancel).
     */
    public void cancelRecurrence(Long requestId) {
        Request request = requestService.getRequestById(requestId);
        
        if (request == null) {
            throw new IllegalArgumentException("Request not found");
        }
        
        request.setIsRecurring(false);
        requestService.updateRequest(request);
        
        // Also cancel partner's recurrence
        Request partner = request.getMatchedPartner();
        if (partner != null && partner.isRecurring()) {
            partner.setIsRecurring(false);
            requestService.updateRequest(partner);
        }
        
        logger.info("Recurrence cancelled for request {} and its partner", requestId);
    }
    
    /**
     * Generate a new pair of requests for the next week based on a recurring pair.
     */
    private void generateRecurringPair(Request originalTutorRequest, Request originalTuteeRequest, LocalDate targetWeek) {
        // Create new tutee request
        Request newTuteeRequest = createRecurringRequest(originalTuteeRequest, targetWeek);
        
        // Create new tutor request  
        Request newTutorRequest = createRecurringRequest(originalTutorRequest, targetWeek);
        
        // Both new requests are automatically marked as recurring since they come from recurring originals
        newTuteeRequest.setIsRecurring(true);
        newTutorRequest.setIsRecurring(true);
        
        logger.debug("Generated recurring pair for week {}: tutee={}, tutor={}", 
                    targetWeek, newTuteeRequest.getId(), newTutorRequest.getId());
    }
    
    /**
     * Create a new request based on an existing request for a specific target week.
     */
    private Request createRecurringRequest(Request original, LocalDate targetWeek) {
        Request newRequest = new Request();
        newRequest.setUser(original.getUser());
        newRequest.setSubject(original.getSubject());
        newRequest.setType(original.getType());
        newRequest.setYearGroup(original.getYearGroup());
        newRequest.setTargetWeek(targetWeek);
        newRequest.setStatus(RequestStatus.OUTSTANDING);
        newRequest.setPossibleTimeslots(original.getPossibleTimeslots());
        newRequest.setIsRecurring(true);
        
        return requestService.createRequest(newRequest);
    }
    
    /**
     * Get all requests that are both matched and marked as recurring.
     */
    private List<Request> getAllRecurringActiveRequests() {
        return requestService.getAllRequests()
            .stream()
            .filter(r -> r.getStatus() == RequestStatus.MATCHED)
            .filter(Request::isRecurring)
            .filter(r -> r.getMatchedPartner() != null)
            .collect(Collectors.toList());
    }
    
    /**
     * Get the upcoming Monday date (ISO week format).
     */
    private LocalDate getUpcomingMondayDate() {
        LocalDate today = LocalDate.now();
        WeekFields weekFields = WeekFields.ISO;
        return today.with(weekFields.dayOfWeek(), 1).plusWeeks(1); // Next Monday
    }
}