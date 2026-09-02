package com.sportiq.backend.config;

import com.sportiq.backend.entity.Skill;
import com.sportiq.backend.entity.Sport;
import com.sportiq.backend.repository.SkillRepository;
import com.sportiq.backend.repository.SportRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Seeds the fixed sport/skill catalog ({@code lld.md} §1.1) at startup.
 * Uses the explicit ids from the catalog, and is idempotent:
 * a skill is only inserted when its {@code prompt_key} is missing.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private final SportRepository sportRepository;
    private final SkillRepository skillRepository;

    public DataSeeder(SportRepository sportRepository, SkillRepository skillRepository) {
        this.sportRepository = sportRepository;
        this.skillRepository = skillRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        Sport cricket = sportOrSave(1, "Cricket");
        Sport football = sportOrSave(2, "Football");

        List<Skill> skills = new ArrayList<>();
        skills.add(new Skill(1, cricket, "Bowling", "cricket_bowling"));
        skills.add(new Skill(2, cricket, "Batting", "cricket_batting"));
        skills.add(new Skill(3, football, "Shooting", "football_shooting"));
        skills.add(new Skill(4, football, "Passing", "football_passing"));
        skills.add(new Skill(5, football, "Dribbling", "football_dribbling"));

        for (Skill skill : skills) {
            if (!skillRepository.existsByPromptKey(skill.getPromptKey())) {
                skillRepository.save(skill);
            }
        }
    }

    private Sport sportOrSave(Integer id, String name) {
        return sportRepository.findById(id).orElseGet(() -> sportRepository.save(new Sport(id, name)));
    }
}