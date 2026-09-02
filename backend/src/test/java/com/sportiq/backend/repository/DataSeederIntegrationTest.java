package com.sportiq.backend.repository;

import com.sportiq.backend.config.DataSeeder;
import com.sportiq.backend.support.PostgresTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@ImportAutoConfiguration
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(DataSeeder.class)
@ActiveProfiles("test")
class DataSeederIntegrationTest extends PostgresTestBase {

    @Autowired
    private SportRepository sportRepository;

    @Autowired
    private SkillRepository skillRepository;

    @Autowired
    private DataSeeder dataSeeder;

    @Test
    void seedsSportsAndSkills() {
        dataSeeder.run();

        assertEquals(2, sportRepository.count());
        assertEquals(5, skillRepository.count());
        assertTrue(sportRepository.findById(1).map(s -> s.getName().equals("Cricket")).orElse(false));
        assertTrue(skillRepository.existsByPromptKey("football_dribbling"));
    }

    @Test
    void seederIsIdempotent() {
        dataSeeder.run();
        dataSeeder.run();

        assertEquals(2, sportRepository.count());
        assertEquals(5, skillRepository.count());
    }

    @Test
    void skillsBelongToTheirSport() {
        dataSeeder.run();

        assertEquals(2, skillRepository.findBySportId(1).size());
        assertEquals(3, skillRepository.findBySportId(2).size());
    }
}