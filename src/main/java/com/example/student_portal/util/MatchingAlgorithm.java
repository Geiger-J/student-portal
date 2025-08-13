package com.example.student_portal.util;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.example.student_portal.entity.Match;
import com.example.student_portal.entity.Request;
import com.example.student_portal.entity.Timeslot;
import com.example.student_portal.model.YearGroup;
import com.example.student_portal.service.MatchService;
import com.example.student_portal.service.RequestService;

/**
 * Weekly matching algorithm.
 *
 * Strategy (simple greedy for small datasets): 1. Get outstanding tutor
 * requests and outstanding tutee requests. 2. For each tutor, try to find a
 * tutee with: - Same subject - Tutor's year group >= Tutee's year group -
 * Overlapping timeslot - If Year 12 or 13, exam board compatibility should be
 * checked (omitted here due to simplicity). 3. On first overlapping timeslot,
 * create a match and mark both requests as MATCHED.
 *
 * Note: Greedy is acceptable for small datasets. For maximum bipartite
 * matching, adapt to use a proper algorithm (e.g., Hopcroft–Karp) over the
 * timeslot/subject graph.
 */
@Component
public class MatchingAlgorithm {

    private final RequestService requestService;
    private final MatchService matchService;

    public MatchingAlgorithm(RequestService requestService,
            MatchService matchService) {
        this.requestService = requestService;
        this.matchService = matchService;
    }

    /**
     * Run weekly at 2:30 AM on Mondays (cron format: sec min hour day-of-month
     * month day-of-week). Adjust as needed. Also callable manually if you
     * expose an admin endpoint.
     */
    @Scheduled(cron = "0 30 2 * * MON")
    public void runWeeklyMatching() {
        runMatching();
    }

    /**
     * The core matching logic (can be invoked manually or by scheduler).
     */
    public void runMatching() {
        List<Request> tutors = requestService.getOutstandingTutorRequests();
        List<Request> tutees = requestService.getOutstandingTuteeRequests();

        // Keep track of which requests were already matched to avoid duplicates
        Set<Long> usedTutorIds = new HashSet<>();
        Set<Long> usedTuteeIds = new HashSet<>();

        int created = 0;

        for (Request tutor : tutors) {
            if (usedTutorIds.contains(tutor.getId())) {
                continue;
            }

            for (Request tutee : tutees) {
                if (usedTuteeIds.contains(tutee.getId())) {
                    continue;
                }

                // Subject must match
                if (!tutor.getSubject().getId().equals(tutee.getSubject().getId())) {
                    continue;
                }

                // Year group constraint: tutor >= tutee
                if (!isTutorYearEligible(tutor.getYearGroup(), tutee.getYearGroup())) {
                    continue;
                }

                // Find first overlapping timeslot (by id for robustness)
                Optional<Timeslot> overlap = tutor.getPossibleTimeslots().stream()
                        .filter(ts -> tutee.getPossibleTimeslots().stream().anyMatch(u -> Objects.equals(u.getId(), ts.getId())))
                        .findFirst();

                if (overlap.isPresent()) {
                    Match match = matchService.saveMatch(tutor, tutee, overlap.get());
                    usedTutorIds.add(tutor.getId());
                    usedTuteeIds.add(tutee.getId());
                    created++;
                    break; // Move to next tutor
                }
            }
        }

        System.out.println("✅ Matching complete: " + created + " pairs created.");
    }

    private boolean isTutorYearEligible(YearGroup tutorYear, YearGroup tuteeYear) {
        return tutorYear.ordinal() >= tuteeYear.ordinal();
    }
}
