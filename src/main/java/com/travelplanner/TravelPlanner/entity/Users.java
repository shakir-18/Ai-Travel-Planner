package com.travelplanner.TravelPlanner.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

import java.util.ArrayList;
import java.util.List;

@Entity
public class Users {
    @Id
    private Long id;
    private String state;

    public Users(Long id) {
        this.id=id;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    @OneToMany
    private List<TripPlan> trips=new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setTrips(List<TripPlan> trips) {
        this.trips = trips;
    }

    public List<TripPlan> getTrips() {
        return trips;
    }
    public Users(Long id, String state)
    {
        this.id=id;this.state=state;
    }
    public Users(){}
}
