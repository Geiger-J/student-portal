package com.example.student_portal.service;

import com.example.student_portal.entity.Subject;
import com.example.student_portal.repository.SubjectRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Thin service wrapper around SubjectRepository.
 * Useful for future business logic (e.g., preventing duplicate names).
 */
@Service
public class SubjectService {

    private final SubjectRepository subjectRepository;

    public SubjectService(SubjectRepository subjectRepository) {
        this.subjectRepository = subjectRepository;
    }

    public List<Subject> findAll() {
        return subjectRepository.findAll();
    }
}