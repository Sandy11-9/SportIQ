package com.sportiq.backend.service;

import com.sportiq.backend.entity.Skill;
import com.sportiq.backend.entity.Sport;
import com.sportiq.backend.exception.ResourceNotFoundException;
import com.sportiq.backend.repository.SkillRepository;
import com.sportiq.backend.repository.SportRepository;
import com.sportiq.backend.web.dto.SkillResponse;
import com.sportiq.backend.web.dto.SportResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SportSkillServiceTest {

    private SportRepository sportRepository;
    private SkillRepository skillRepository;
    private SportSkillService service;

    @BeforeEach
    void setUp() {
        sportRepository = mock(SportRepository.class);
        skillRepository = mock(SkillRepository.class);
        service = new SportSkillService(sportRepository, skillRepository);
    }

    @Test
    void listsSportsInIdOrder() {
        when(sportRepository.findAll(org.springframework.data.domain.Sort.by("id")))
                .thenReturn(List.of(new Sport(1, "Cricket"), new Sport(2, "Football")));

        List<SportResponse> sports = service.listSports();

        assertEquals(2, sports.size());
        assertEquals("Cricket", sports.get(0).name());
        assertEquals(Integer.valueOf(2), sports.get(1).id());
    }

    @Test
    void listsSkillsOfSport() {
        Sport football = new Sport(2, "Football");
        when(sportRepository.findById(2)).thenReturn(Optional.of(football));
        when(skillRepository.findBySportId(2))
                .thenReturn(List.of(new Skill(3, football, "Shooting", "football_shooting")));

        List<SkillResponse> skills = service.listSkillsBySport(2);

        assertEquals(1, skills.size());
        assertEquals("Shooting", skills.get(0).name());
        assertEquals(Integer.valueOf(2), skills.get(0).sportId());
    }

    @Test
    void rejectsUnknownSport() {
        when(sportRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.listSkillsBySport(99));
    }
}