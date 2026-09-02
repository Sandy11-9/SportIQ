package com.sportiq.backend.service;

import com.sportiq.backend.entity.AnalysisSession;
import com.sportiq.backend.entity.AppUser;
import com.sportiq.backend.entity.Skill;
import com.sportiq.backend.entity.Sport;
import com.sportiq.backend.exception.ResourceNotFoundException;
import com.sportiq.backend.repository.AnalysisSessionRepository;
import com.sportiq.backend.web.dto.AnalysisSessionResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HistoryServiceTest {

    private AnalysisSessionRepository sessionRepository;
    private HistoryService service;

    @BeforeEach
    void setUp() {
        sessionRepository = mock(AnalysisSessionRepository.class);
        service = new HistoryService(sessionRepository);
    }

    private AnalysisSession session() {
        AppUser user = new AppUser("a@b.dev", "h", "Coach");
        Sport sport = new Sport(2, "Football");
        Skill skill = new Skill(4, sport, "Passing", "football_passing");
        AnalysisSession session = new AnalysisSession(user, sport, skill,
                List.of("g"), List.of("b"), List.of("i"));
        session.addScore("Accuracy", 80);
        return session;
    }

    @Test
    void listsAllWhenNoFilters() {
        when(sessionRepository.findByUserIdOrderByAnalyzedAtDesc(anyLong(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(session())));

        Page<AnalysisSessionResponse> page = service.list(1L, null, null, PageRequest.of(0, 10));

        assertEquals(1, page.getTotalElements());
        assertEquals("Passing", page.getContent().get(0).skillName());
        assertEquals(Integer.valueOf(80), page.getContent().get(0).scores().get(0).score());
    }

    @Test
    void listsBySportAndSkill() {
        when(sessionRepository.findByUserIdAndSportIdAndSkillIdOrderByAnalyzedAtDesc(
                anyLong(), org.mockito.ArgumentMatchers.eq(2), org.mockito.ArgumentMatchers.eq(4), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(session())));

        Page<AnalysisSessionResponse> page = service.list(1L, 2, 4, PageRequest.of(0, 10));

        assertEquals(1, page.getTotalElements());
        verify(sessionRepository).findByUserIdAndSportIdAndSkillIdOrderByAnalyzedAtDesc(
                anyLong(), org.mockito.ArgumentMatchers.eq(2), org.mockito.ArgumentMatchers.eq(4), any(Pageable.class));
    }

    @Test
    void getsOwnedSession() {
        when(sessionRepository.findByIdAndUserId(5L, 1L)).thenReturn(Optional.of(session()));

        AnalysisSessionResponse response = service.get(1L, 5L);

        assertEquals("Football", response.sportName());
    }

    @Test
    void throwsWhenSessionNotOwned() {
        when(sessionRepository.findByIdAndUserId(5L, 1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.get(1L, 5L));
    }
}