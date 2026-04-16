package no.fintlabs.vigoskole.domain.model;

public record SchoolInfo(
    String orgNumber,
    String name,
    String municipalityNumber,
    String countyNumber,
    String schoolNumber) {}
