package com.example.student_portal.service;

import com.example.student_portal.entity.Subject;
import com.example.student_portal.repository.SubjectRepository;
import com.example.student_portal.repository.RequestRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Enhanced service for managing subjects.
 * 
 * Responsibilities:
 * - CRUD operations for subjects
 * - Business logic for subject management
 * - Validation and constraints checking
 */
@Service
public class SubjectService {

    private final SubjectRepository subjectRepository;
    private final RequestRepository requestRepository;

    public SubjectService(SubjectRepository subjectRepository, RequestRepository requestRepository) {
        this.subjectRepository = subjectRepository;
        this.requestRepository = requestRepository;
    }

    public List<Subject> findAll() {
        return subjectRepository.findAll();
    }

    public Subject findById(Long id) {
        return subjectRepository.findById(id).orElse(null);
    }

    public Subject save(Subject subject) {
        return subjectRepository.save(subject);
    }

    public void deleteById(Long id) {
        subjectRepository.deleteById(id);
    }

    /**
     * Check if a subject is currently being used by any users or requests.
     * Used to prevent deletion of subjects that would break referential integrity.
     */
    public boolean isSubjectInUse(Long subjectId) {
        Subject subject = findById(subjectId);
        if (subject == null) {
            return false;
        }
        
        // Check if any users have this subject
        if (!subject.getUsers().isEmpty()) {
            return true;
        }
        
        // Check if any requests reference this subject
        return !requestRepository.findBySubjectId(subjectId).isEmpty();
    }

    /**
     * Find subject by name (case-insensitive).
     */
    public Subject findByName(String name) {
        return subjectRepository.findByNameIgnoreCase(name);
    }
}