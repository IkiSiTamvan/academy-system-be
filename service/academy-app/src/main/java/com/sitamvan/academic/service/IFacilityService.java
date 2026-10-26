package com.sitamvan.academic.service;

import com.sitamvan.academic.dto.request.RequestFacility;
import com.sitamvan.academic.model.Facility;
import org.springframework.data.domain.Page;

public interface IFacilityService {
    Page<Facility> search(String query, int page, int size);
    Facility getById(Long id);
    Facility create(RequestFacility request);
    Facility update(Long id, RequestFacility request);
    void delete(Long id);
}
