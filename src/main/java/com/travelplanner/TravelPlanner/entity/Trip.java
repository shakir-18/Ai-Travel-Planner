package com.travelplanner.TravelPlanner.entity;

import jakarta.persistence.*;
import org.json.JSONObject;
import org.telegram.telegrambots.meta.api.objects.Location;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


@Entity
public class Trip {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Date startDate;
    private String destination;
    private String interests;

    public void setInterests(String interests) {
        this.interests = interests;
    }

    public String getInterests() {
        return interests;
    }

    @Lob
    @Column(name = "weather_updates", columnDefinition = "TEXT")
    private String weatherUpdatesJson;

    public String getWeatherUpdatesJson() {
        return weatherUpdatesJson;
    }
    public void setWeatherUpdatesJson(String weatherUpdatesJson) {
        this.weatherUpdatesJson = weatherUpdatesJson;
    }

    private Double latitude;

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    private Double longitude;

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    private String duration;

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }



    @ManyToOne
    @JoinColumn(name = "users_id")
    private Users users;

    public Long getId() {
        return id;
    }

    public void setUsers(Users users) {
        this.users = users;
    }

    public Users getUsers() {
        return users;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }
}
