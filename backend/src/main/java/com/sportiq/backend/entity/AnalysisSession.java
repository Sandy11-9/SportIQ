package com.sportiq.backend.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * One persisted text-only analysis result. The video/frames are never stored;
 * only scores and free-text points survive (matches {@code hld.md} §6).
 */
@Entity
@Table(name = "analysis_session")
public class AnalysisSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private AppUser user;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "sport_id")
    private Sport sport;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "skill_id")
    private Skill skill;

    @Column(name = "analyzed_at", nullable = false, updatable = false)
    private Instant analyzedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "good_points", nullable = false)
    private List<String> goodPoints = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "bad_points", nullable = false)
    private List<String> badPoints = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "suggestions", nullable = false)
    private List<String> improvements = new ArrayList<>();

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AnalysisScore> scores = new ArrayList<>();

    protected AnalysisSession() {
    }

    public AnalysisSession(AppUser user, Sport sport, Skill skill,
                           List<String> goodPoints, List<String> badPoints, List<String> improvements) {
        this.user = user;
        this.sport = sport;
        this.skill = skill;
        this.goodPoints = goodPoints;
        this.badPoints = badPoints;
        this.improvements = improvements;
    }

    @PrePersist
    void prePersist() {
        if (analyzedAt == null) {
            analyzedAt = Instant.now();
        }
    }

    public void addScore(String category, int value) {
        scores.add(new AnalysisScore(this, category, value));
    }

    public Long getId() {
        return id;
    }

    public AppUser getUser() {
        return user;
    }

    public Sport getSport() {
        return sport;
    }

    public Skill getSkill() {
        return skill;
    }

    public Instant getAnalyzedAt() {
        return analyzedAt;
    }

    public List<String> getGoodPoints() {
        return goodPoints;
    }

    public List<String> getBadPoints() {
        return badPoints;
    }

    public List<String> getImprovements() {
        return improvements;
    }

    public List<AnalysisScore> getScores() {
        return scores;
    }
}