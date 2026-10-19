package com.sitamvan.academic.repository;

import com.sitamvan.academic.model.School;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ISchoolRepository extends JpaRepository<School, Long> { }
