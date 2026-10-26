package com.sitamvan.academic.service;

import com.sitamvan.academic.dto.request.RequestTimeSlot;
import com.sitamvan.academic.model.TimeSlot;
import org.springframework.data.domain.Page;

public interface ITimeSlotService {
    Page<TimeSlot> search(String query, Long academicYearId, Short dayOfWeek, int page, int size);
    TimeSlot getById(Long id);
    TimeSlot create(RequestTimeSlot request);
    TimeSlot update(Long id, RequestTimeSlot request);
    void delete(Long id);
}
