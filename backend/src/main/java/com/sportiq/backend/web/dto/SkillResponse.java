package com.sportiq.backend.web.dto;

import com.sportiq.backend.entity.Skill;

public record SkillResponse(Integer id, Integer sportId, String name) {

    public static SkillResponse from(Skill skill) {
        return new SkillResponse(skill.getId(), skill.getSport().getId(), skill.getName());
    }
}