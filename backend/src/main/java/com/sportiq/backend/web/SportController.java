package com.sportiq.backend.web;

import com.sportiq.backend.service.SportSkillService;
import com.sportiq.backend.web.dto.SkillResponse;
import com.sportiq.backend.web.dto.SportResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/sports")
public class SportController {

    private final SportSkillService sportSkillService;

    public SportController(SportSkillService sportSkillService) {
        this.sportSkillService = sportSkillService;
    }

    @GetMapping
    public List<SportResponse> listSports() {
        return sportSkillService.listSports();
    }

    @GetMapping("/{sportId}/skills")
    public List<SkillResponse> listSkills(@PathVariable Integer sportId) {
        return sportSkillService.listSkillsBySport(sportId);
    }
}