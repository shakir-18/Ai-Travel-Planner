package com.travelplanner.TravelPlanner.repo;

import com.travelplanner.TravelPlanner.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<Users,Long> {

}
