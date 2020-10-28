package org.immregistries.iis.kernal.logic;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;

public class Context {
    private static final FhirContext ctx = FhirContext.forR4();
    private static final IParser xml = ctx.newXmlParser();
    private static final IParser json = ctx.newJsonParser();

    public static FhirContext getCtx() {
        return ctx;
    }

    public static IParser fhir_parser(String message){ //Returns appropriate parser
        if (message.charAt(0) == '<'){ //If XML
            return xml;
        }else{ //else should be json
            return json;
        }
    }
}
