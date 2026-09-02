package com.sportiq.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Skill catalog row. Id is assigned explicitly by the {@code DataSeeder}
 * (reference catalog, no auto-generation).
 */
@Entity
@Table(name = "skill")
public class Skill {

    @Id
    @Column(nullable = false)
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "sport_id")
    private Sport sport;

    @Column(nullable = false)
    private String name;

    @Column(name = "prompt_key", nullable = false, unique = true)
    private String promptKey;

    protected Skill() {
    }

    public Skill(Integer id, Sport sport, String name, String promptKey) {
        this.id = id;
        this.sport = sport;
        this.name = name;
        this.promptKey = promptKey;
    }

    public Integer getId() {
        return id;
    }

    public Sport getSport() {
        return sport;
    }

    public String getName() {
        return name;
    }

    public String getPromptKey() {
        return promptKey;
    }
}