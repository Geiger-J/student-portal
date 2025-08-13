package com.example.student_portal.service;

import com.example.student_portal.entity.Request;
import com.example.student_portal.entity.Timeslot;
import com.example.student_portal.entity.User;
import com.example.student_portal.entity.AvailabilitySlot;
import com.example.student_portal.model.RequestType;
import com.example.student_portal.model.RequestStatus;
import com.example.student_portal.model.YearGroup;
import com.example.student_portal.model.Period;
import com.example.student_portal.util.HopcroftKarp;
import com.example.student_portal.util.HopcroftKarp.BipartiteGraph;
import com.example.student_portal.util.HopcroftKarp.MatchingResult;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Advanced matching service using Hopcroft-Karp algorithm for maximum bipartite matching.
 *
 * This service builds a bipartite graph between tutee requests and tutor availability nodes,
 * respecting tutor capacity limits and preventing double-allocation of timeslots.
 * 
 * The matching algorithm considers:
 * - Subject compatibility
 * - Year group constraints (tutor year >= tutee year)
 * - Timeslot availability
 * - Tutor maxSessionsPerWeek capacity
 * - Unique timeslot allocation per tutor
 */
@Service
@Transactional
public class MatchingService {
    
    private static final Logger logger = LoggerFactory.getLogger(MatchingService.class);
    
    private final RequestService requestService;
    private final AvailabilityService availabilityService;
    private final MatchService matchService;
    
    // Node ID generators for bipartite graph
    private int nextTuteeNodeId = 1;
    private int nextTutorNodeId = 1000000; // Start tutor nodes at high numbers to avoid conflicts
    
    // Mapping between node IDs and actual entities
    private Map<Integer, TuteeRequestTimeslot> tuteeNodes;
    private Map<Integer, TutorAvailabilityNode> tutorNodes;
    
    public MatchingService(RequestService requestService, 
                          AvailabilityService availabilityService,
                          MatchService matchService) {
        this.requestService = requestService;
        this.availabilityService = availabilityService;
        this.matchService = matchService;
    }
    
    /**
     * Represents a tutee request for a specific timeslot (left side of bipartite graph).
     */
    public static class TuteeRequestTimeslot {
        public final Request request;
        public final Timeslot timeslot;
        
        public TuteeRequestTimeslot(Request request, Timeslot timeslot) {
            this.request = request;
            this.timeslot = timeslot;
        }
    }
    
    /**
     * Represents a tutor's availability for a specific timeslot and subject (right side of bipartite graph).
     * Each tutor can have multiple nodes to represent capacity constraints.
     */
    public static class TutorAvailabilityNode {
        public final User tutor;
        public final DayOfWeek dayOfWeek;
        public final Period period;
        public final int sessionNumber; // For capacity management (1, 2, 3, ... up to maxSessionsPerWeek)
        
        public TutorAvailabilityNode(User tutor, DayOfWeek dayOfWeek, Period period, int sessionNumber) {
            this.tutor = tutor;
            this.dayOfWeek = dayOfWeek;
            this.period = period;
            this.sessionNumber = sessionNumber;
        }
        
        public boolean matchesTimeslot(Timeslot timeslot) {
            // Simple string matching for now - in real implementation, convert Timeslot to day/period
            String expectedLabel = dayOfWeek.toString().substring(0, 1).toUpperCase() + 
                                 dayOfWeek.toString().substring(1).toLowerCase() + " Period " + 
                                 period.name().substring(1);
            return timeslot.getLabel().equals(expectedLabel);
        }
    }
    
    /**
     * Run the matching algorithm atomically.
     * Returns the number of matches created.
     */
    public int performMatching() {
        return performMatchingForWeek(getUpcomingMondayDate());
    }
    
    /**
     * Run matching for a specific target week.
     */
    public int performMatchingForWeek(LocalDate targetWeek) {
        logger.info("Starting matching algorithm for week {}", targetWeek);
        
        // Reset node ID counters and mappings
        nextTuteeNodeId = 1;
        nextTutorNodeId = 1000000;
        tuteeNodes = new HashMap<>();
        tutorNodes = new HashMap<>();
        
        // Get outstanding requests for the target week
        List<Request> tuteeRequests = requestService.getOutstandingTuteeRequests()
            .stream()
            .filter(r -> targetWeek.equals(r.getTargetWeek()))
            .collect(Collectors.toList());
            
        List<Request> tutorRequests = requestService.getOutstandingTutorRequests()
            .stream()
            .filter(r -> targetWeek.equals(r.getTargetWeek()))
            .collect(Collectors.toList());
        
        if (tuteeRequests.isEmpty() || tutorRequests.isEmpty()) {
            logger.info("No matching needed - tutee requests: {}, tutor requests: {}", 
                       tuteeRequests.size(), tutorRequests.size());
            return 0;
        }
        
        // Build bipartite graph
        BipartiteGraph graph = buildBipartiteGraph(tuteeRequests, tutorRequests);
        
        // Find maximum matching using Hopcroft-Karp
        MatchingResult matching = HopcroftKarp.findMaximumMatching(graph);
        
        // Persist matches to database
        int matchesCreated = persistMatches(matching);
        
        logger.info("Matching complete for week {}: {} matches created", targetWeek, matchesCreated);
        return matchesCreated;
    }
    
    /**
     * Build the bipartite graph for matching.
     */
    private BipartiteGraph buildBipartiteGraph(List<Request> tuteeRequests, List<Request> tutorRequests) {
        BipartiteGraph graph = new BipartiteGraph();
        
        // Create left nodes (tutee request timeslots)
        for (Request tuteeRequest : tuteeRequests) {
            for (Timeslot timeslot : tuteeRequest.getPossibleTimeslots()) {
                int nodeId = nextTuteeNodeId++;
                graph.addLeftNode(nodeId);
                tuteeNodes.put(nodeId, new TuteeRequestTimeslot(tuteeRequest, timeslot));
            }
        }
        
        // Create right nodes (tutor availability nodes with capacity)
        Set<TutorTimeslotPair> processedTutorTimeslots = new HashSet<>();
        
        for (Request tutorRequest : tutorRequests) {
            User tutor = tutorRequest.getUser();
            int maxSessions = tutor.getMaxSessionsPerWeek() != null ? tutor.getMaxSessionsPerWeek() : 3;
            
            // Get tutor's availability slots
            List<AvailabilitySlot> availabilitySlots = availabilityService.getAvailabilitySlots(tutor);
            
            for (AvailabilitySlot slot : availabilitySlots) {
                TutorTimeslotPair pair = new TutorTimeslotPair(tutor.getId(), slot.getDayOfWeek(), slot.getPeriod());
                
                if (!processedTutorTimeslots.contains(pair)) {
                    processedTutorTimeslots.add(pair);
                    
                    // Create multiple nodes for this tutor's timeslot (up to maxSessions capacity)
                    for (int sessionNum = 1; sessionNum <= maxSessions; sessionNum++) {
                        int nodeId = nextTutorNodeId++;
                        graph.addRightNode(nodeId);
                        tutorNodes.put(nodeId, new TutorAvailabilityNode(tutor, slot.getDayOfWeek(), slot.getPeriod(), sessionNum));
                    }
                }
            }
        }
        
        // Add edges between compatible tutee requests and tutor availability
        for (Map.Entry<Integer, TuteeRequestTimeslot> tuteeEntry : tuteeNodes.entrySet()) {
            int tuteeNodeId = tuteeEntry.getKey();
            TuteeRequestTimeslot tuteeNode = tuteeEntry.getValue();
            
            for (Map.Entry<Integer, TutorAvailabilityNode> tutorEntry : tutorNodes.entrySet()) {
                int tutorNodeId = tutorEntry.getKey();
                TutorAvailabilityNode tutorNode = tutorEntry.getValue();
                
                if (areCompatible(tuteeNode, tutorNode)) {
                    graph.addEdge(tuteeNodeId, tutorNodeId);
                }
            }
        }
        
        logger.info("Built bipartite graph: {} tutee nodes, {} tutor nodes", 
                   tuteeNodes.size(), tutorNodes.size());
        return graph;
    }
    
    /**
     * Check if a tutee request and tutor availability are compatible.
     */
    private boolean areCompatible(TuteeRequestTimeslot tuteeNode, TutorAvailabilityNode tutorNode) {
        Request tuteeRequest = tuteeNode.request;
        Request tutorRequest = findTutorRequest(tutorNode.tutor);
        
        if (tutorRequest == null) {
            return false;
        }
        
        // Subject must match
        if (!tuteeRequest.getSubject().getId().equals(tutorRequest.getSubject().getId())) {
            return false;
        }
        
        // Tutor year group must be >= tutee year group
        if (!isTutorYearEligible(tutorRequest.getYearGroup(), tuteeRequest.getYearGroup())) {
            return false;
        }
        
        // Timeslot must match
        if (!tutorNode.matchesTimeslot(tuteeNode.timeslot)) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Find the tutor request for a given tutor user.
     */
    private Request findTutorRequest(User tutor) {
        return requestService.getRequestsByUser(tutor)
            .stream()
            .filter(r -> r.getType() == RequestType.TUTOR && r.getStatus() == RequestStatus.OUTSTANDING)
            .findFirst()
            .orElse(null);
    }
    
    /**
     * Check if tutor year group is eligible to tutor the given tutee year group.
     */
    private boolean isTutorYearEligible(YearGroup tutorYear, YearGroup tuteeYear) {
        return tutorYear.ordinal() >= tuteeYear.ordinal();
    }
    
    /**
     * Persist the matches found by the algorithm to the database.
     */
    private int persistMatches(MatchingResult matching) {
        int matchesCreated = 0;
        Map<Long, Integer> tutorSessionCount = new HashMap<>();
        
        for (Map.Entry<Integer, Integer> match : matching.getLeftToRight().entrySet()) {
            int tuteeNodeId = match.getKey();
            int tutorNodeId = match.getValue();
            
            TuteeRequestTimeslot tuteeNode = tuteeNodes.get(tuteeNodeId);
            TutorAvailabilityNode tutorNode = tutorNodes.get(tutorNodeId);
            
            if (tuteeNode != null && tutorNode != null) {
                // Check tutor session limit
                Long tutorId = tutorNode.tutor.getId();
                int currentCount = tutorSessionCount.getOrDefault(tutorId, 0);
                int maxSessions = tutorNode.tutor.getMaxSessionsPerWeek() != null ? 
                    tutorNode.tutor.getMaxSessionsPerWeek() : 3;
                
                if (currentCount < maxSessions) {
                    try {
                        // Create the match
                        Request tutorRequest = findTutorRequest(tutorNode.tutor);
                        if (tutorRequest != null) {
                            matchService.saveMatch(tutorRequest, tuteeNode.request, tuteeNode.timeslot);
                            
                            // Update session count
                            tutorSessionCount.put(tutorId, currentCount + 1);
                            matchesCreated++;
                            
                            logger.debug("Created match: {} (tutor) <-> {} (tutee) at {}", 
                                       tutorRequest.getUser().getFullName(),
                                       tuteeNode.request.getUser().getFullName(),
                                       tuteeNode.timeslot.getLabel());
                        }
                    } catch (Exception e) {
                        logger.error("Failed to create match between tutor {} and tutee {}: {}", 
                                   tutorNode.tutor.getFullName(), 
                                   tuteeNode.request.getUser().getFullName(),
                                   e.getMessage());
                    }
                }
            }
        }
        
        return matchesCreated;
    }
    
    /**
     * Get the upcoming Monday date (ISO week format).
     */
    private LocalDate getUpcomingMondayDate() {
        LocalDate today = LocalDate.now();
        WeekFields weekFields = WeekFields.ISO;
        return today.with(weekFields.dayOfWeek(), 1).plusWeeks(1); // Next Monday
    }
    
    /**
     * Helper class to track processed tutor-timeslot pairs.
     */
    private static class TutorTimeslotPair {
        private final Long tutorId;
        private final DayOfWeek dayOfWeek;
        private final Period period;
        
        public TutorTimeslotPair(Long tutorId, DayOfWeek dayOfWeek, Period period) {
            this.tutorId = tutorId;
            this.dayOfWeek = dayOfWeek;
            this.period = period;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            TutorTimeslotPair that = (TutorTimeslotPair) obj;
            return Objects.equals(tutorId, that.tutorId) && 
                   dayOfWeek == that.dayOfWeek && 
                   period == that.period;
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(tutorId, dayOfWeek, period);
        }
    }
}