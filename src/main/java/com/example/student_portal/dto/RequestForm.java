package com.example.student_portal.dto;

import java.util.List;

import com.example.student_portal.model.RequestType;

import jakarta.validation.constraints.NotNull;

/**
 * RequestForm
 *
 * Form-backing bean used for creating tutoring requests from both: - /dashboard
 * inline request creation - /requests/add endpoint
 *
 * Uses standard JavaBean accessor methods so Spring's DataBinder & Thymeleaf
 * th:field work without additional configuration.
 */
public class RequestForm {

    @NotNull
    private Long subjectId;

    @NotNull
    private RequestType type;

    @NotNull
    private List<Long> timeslotIds;

    // ISO date string (YYYY-MM-DD) representing the Monday of the target week.
    private String targetWeek;

    // Getters / Setters
    public Long getSubjectId() { return subjectId; }

    public void setSubjectId(Long subjectId) { this.subjectId = subjectId; }

    public RequestType getType() { return type; }

    public void setType(RequestType type) { this.type = type; }

    public List<Long> getTimeslotIds() { return timeslotIds; }

    public void setTimeslotIds(List<Long> timeslotIds) { this.timeslotIds = timeslotIds; }

    public String getTargetWeek() { return targetWeek; }

    public void setTargetWeek(String targetWeek) { this.targetWeek = targetWeek; }
}