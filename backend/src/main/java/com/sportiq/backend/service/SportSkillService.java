package com.sportiq.backend.service;

import com.sportiq.backend.exception.ResourceNotFoundException;
import com.sportiq.backend.repository.SkillRepository;
import com.sportiq.backend.repository.SportRepository;
import com.sportiq.backend.web.dto.SkillResponse;
import com.sportiq.backend.web.dto.SportResponse;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SportSkillService {

    private final SportRepository sportRepository;
    private final SkillRepository skillRepository;

    public SportSkillService(SportRepository sportRepository, SkillRepository skillRepository) {
        this.sportRepository = sportRepository;
        this.skillRepository = skillRepository;
    }

    @Transactional(readOnly = true)
    public List<SportResponse> listSports() {
        return sportRepository.findAll(Sort.by("id")).stream().map(SportResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<SkillResponse> listSkillsBySport(Integer sportId) {
        sportRepository.findById(sportId).orElseThrow(() -> new ResourceNotFoundException("Sport not found"));
        return skillRepository.findBySportId(sportId).stream().map(SkillResponse::from).toList();
    }
}