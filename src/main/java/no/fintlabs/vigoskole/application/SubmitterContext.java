package no.fintlabs.vigoskole.application;

import no.fintlabs.vigoskole.domain.model.SupplierInfo;

public record SubmitterContext(String orgNumber, String displayName, SupplierInfo supplier) {}
