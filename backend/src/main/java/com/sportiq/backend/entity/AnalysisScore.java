package com.sportiq.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "analysis_score",
        uniqueConstraints = @UniqueConstraint(name = "uq_score_session_category",
                columnNames = {"session_id", "category"}))
public class AnalysisScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id")
    private AnalysisSession session;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private Integer value;

    protected AnalysisScore() {
    }

    public AnalysisScore(AnalysisSession session, String category, Integer value) {
        this.session = session;
        this.category = category;
        this.value = value;
    }

    public Long getId() {
        return id;
    }

    public AnalysisSession getSession() {
        return session;
    }

    public String getCategory() {
        return category;
    }

    public Integer getValue() {
        return value;
    }
}