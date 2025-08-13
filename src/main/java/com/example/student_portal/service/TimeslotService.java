package com.example.student_portal.service;

import com.example.student_portal.entity.Timeslot;
import com.example.student_portal.repository.TimeslotRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Thin service wrapper around TimeslotRepository.
 */
@Service
public class TimeslotService {

    private final TimeslotRepository timeslotRepository;

    public TimeslotService(TimeslotRepository timeslotRepository) {
        this.timeslotRepository = timeslotRepository;
    }

    public List<Timeslot> findAll() {
        return timeslotRepository.findAll();
    }
}