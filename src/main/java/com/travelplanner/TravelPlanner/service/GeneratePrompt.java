package com.travelplanner.TravelPlanner.service;

import com.travelplanner.TravelPlanner.entity.Trip;
import org.springframework.stereotype.Service;

@Service
public class GeneratePrompt {
    public String getPrompt(Trip trip,Long id)
    {
        return "Plan a trip for "+trip.getDuration() +" days to "+ trip.getDestination()+ " based on these interests:" +
                trip.getInterests()+". For each day, provide exactly 3 clear, concise points (places to visit, activities, tips)." +
                " Keep each point short (1-2 sentences).";
    }
}
