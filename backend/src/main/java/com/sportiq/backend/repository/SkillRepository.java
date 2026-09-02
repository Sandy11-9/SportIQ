package com.sportiq.backend.repository;

import com.sportiq.backend.entity.Skill;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SkillRepository extends JpaRepository<Skill, Integer> {

    List<Skill> findBySportId(Integer sportId);

    Optional<Skill> findByIdAndSportId(Integer id, Integer sportId);

    boolean existsByPromptKey(String promptKey);
}