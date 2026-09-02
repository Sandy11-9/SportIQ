package com.sportiq.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Fixed app-defined catalog row. Id is assigned explicitly by the {@code DataSeeder},
 * there is no auto-generation (matches {@code hld.md} §6 / {@code lld.md} §1.2).
 */
@Entity
@Table(name = "sport")
public class Sport {

    @Id
    @Column(nullable = false)
    private Integer id;

    @Column(nullable = false, unique = true)
    private String name;

    protected Sport() {
    }

    public Sport(Integer id, String name) {
        this.id = id;
        this.name = name;
    }

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}