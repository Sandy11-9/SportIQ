package com.sportiq.backend.repository;

import com.sportiq.backend.entity.AnalysisSession;
import com.sportiq.backend.entity.AnalysisScore;
import com.sportiq.backend.entity.AppUser;
import com.sportiq.backend.entity.Skill;
import com.sportiq.backend.entity.Sport;
import com.sportiq.backend.support.PostgresTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class AnalysisSessionRepositoryIntegrationTest extends PostgresTestBase {

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private SportRepository sportRepository;

    @Autowired
    private SkillRepository skillRepository;

    @Autowired
    private AnalysisSessionRepository sessionRepository;

    @Autowired
    private AnalysisScoreRepository scoreRepository;

    private AppUser newUser(String email) {
        return appUserRepository.save(new AppUser(email, "hash", "Test User"));
    }

    @Test
    void persistsScoresAndJsonLists() {
        AppUser user = newUser("json@test.dev");
        Sport sport = sportRepository.save(new Sport(1, "Cricket"));
        Skill skill = new Skill(1, sport, "Bowling", "cricket_bowling");
        skillRepository.save(skill);

        AnalysisSession session = new AnalysisSession(
                user, sport, skill,
                List.of("good grip", "straight arm"),
                List.of("too short"),
                List.of("follow through"));
        session.addScore("Action", 85);
        session.addScore("Balance", 70);
        session.addScore("Result", 60);
        session = sessionRepository.save(session);

        AnalysisSession loaded = sessionRepository.findByIdAndUserId(session.getId(), user.getId()).orElseThrow();

        assertEquals("good grip", loaded.getGoodPoints().get(0));
        assertEquals(List.of("too short"), loaded.getBadPoints());
        assertEquals(3, scoreRepository.count());
        assertEquals(3, loaded.getScores().size());
        assertTrue(loaded.getScores().containsAll(loaded.getScores()));

        for (AnalysisScore score : loaded.getScores()) {
            assertTrue(score.getValue() >= 0 && score.getValue() <= 100);
        }
    }

    @Test
    void filtersByUserSportAndSkill() {
        AppUser user = newUser("filters@test.dev");
        Sport sport = sportRepository.save(new Sport(1, "Cricket"));
        Skill skill = new Skill(1, sport, "Bowling", "cricket_bowling");
        skillRepository.save(skill);

        sessionRepository.save(new AnalysisSession(user, sport, skill, List.of("a"), List.of("b"), List.of("c")));
        sessionRepository.save(new AnalysisSession(user, sport, skill, List.of("a2"), List.of("b2"), List.of("c2")));

        Page<AnalysisSession> page = sessionRepository
                .findByUserIdAndSportIdAndSkillIdOrderByAnalyzedAtDesc(user.getId(), 1, 1, PageRequest.of(0, 10));

        assertEquals(2, page.getTotalElements());
    }
}