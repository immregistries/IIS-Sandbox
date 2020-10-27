package org.immregistries.iis.kernal.logic;

import ca.uhn.fhir.context.FhirContext;

public class Context {
    private static final FhirContext ctx = FhirContext.forR4();

    public static FhirContext getCtx() {
        return ctx;
    }
}
