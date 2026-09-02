package com.sportiq.backend.web.dto;

import com.sportiq.backend.entity.Sport;

public record SportResponse(Integer id, String name) {

    public static SportResponse from(Sport sport) {
        return new SportResponse(sport.getId(), sport.getName());
    }
}