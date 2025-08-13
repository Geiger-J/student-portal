package com.example.student_portal.util;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.student_portal.service.MatchingService;
import com.example.student_portal.service.RecurrenceService;

/**
 * Weekly matching algorithm coordinator.
 *
 * This component orchestrates the complete weekly matching lifecycle:
 * 1. Generate recurring requests from previous week's matches
 * 2. Run advanced Hopcroft-Karp matching algorithm
 * 3. Log results for monitoring
 * 
 * The advanced matching algorithm (Hopcroft-Karp) is implemented in MatchingService
 * and respects tutor capacity constraints and prevents double-allocation of timeslots.
 */
@Component
public class MatchingAlgorithm {

    private static final Logger logger = LoggerFactory.getLogger(MatchingAlgorithm.class);

    private final MatchingService matchingService;
    private final RecurrenceService recurrenceService;

    public MatchingAlgorithm(MatchingService matchingService,
                           RecurrenceService recurrenceService) {
        this.matchingService = matchingService;
        this.recurrenceService = recurrenceService;
    }

    /**
     * Run weekly at 2:30 AM on Mondays (cron format: sec min hour day-of-month
     * month day-of-week). This runs the complete weekly matching lifecycle.
     */
    @Scheduled(cron = "0 30 2 * * MON")
    public void runWeeklyMatching() {
        logger.info("Starting weekly matching cycle");
        runMatching();
    }

    /**
     * The core matching logic (can be invoked manually or by scheduler).
     * 
     * This method runs the complete matching lifecycle:
     * 1. Generate recurring requests for active recurring pairs
     * 2. Run the advanced matching algorithm (Hopcroft-Karp)
     * 3. Log results
     */
    public void runMatching() {
        try {
            // Step 1: Generate recurring requests from previous matches
            logger.info("Generating recurring requests...");
            recurrenceService.generateRecurringRequests();
            
            // Step 2: Run advanced matching algorithm
            logger.info("Running advanced matching algorithm...");
            int matchesCreated = matchingService.performMatching();
            
            // Step 3: Log completion
            logger.info("✅ Weekly matching complete: {} pairs created", matchesCreated);
            
        } catch (Exception e) {
            logger.error("❌ Weekly matching failed: {}", e.getMessage(), e);
            throw e;
        }
    }
}
