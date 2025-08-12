package com.travelplanner.TravelPlanner.repo;

import com.travelplanner.TravelPlanner.entity.Trip;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TripRepository extends JpaRepository<Trip,Long> {
    List<Trip> findByUsersId(Long id);
}
