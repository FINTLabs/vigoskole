package no.novari.vigoskole.domain.model;

public record SchoolInfo(
    String orgNumber,
    String name,
    String municipalityNumber,
    String countyNumber,
    String schoolNumber) {}
