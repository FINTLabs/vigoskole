package no.novari.vigoskole.application;

import no.novari.vigoskole.domain.model.SupplierInfo;

public record SubmitterContext(
    String submitterOrgNumber, String submitterName, SupplierInfo supplier) {}
