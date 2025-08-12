package com.travelplanner.TravelPlanner.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
@Service
public class StateService {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private String key(Long userId) {
        return String.valueOf(userId);
    }

    public void setUserState(Long userId, String state) {
        redisTemplate.opsForValue().set(key(userId), state);
    }

    public String getUserState(Long userId) {
        String state = redisTemplate.opsForValue().get(key(userId));
        return state;
    }

    public void clearUserState(Long userId) {
        redisTemplate.delete(key(userId));
    }
}
