package com.travelplanner.TravelPlanner.repo;

import com.travelplanner.TravelPlanner.entity.TripPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TripPlanRepository extends JpaRepository<TripPlan,Long> {
    List<TripPlan> findByUserId(Long userId);
}