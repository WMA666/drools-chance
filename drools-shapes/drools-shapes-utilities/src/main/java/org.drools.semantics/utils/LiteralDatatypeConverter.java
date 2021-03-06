package org.drools.semantics.utils;

import org.drools.semantics.Literal;

import javax.xml.bind.DatatypeConverter;

public class LiteralDatatypeConverter {

    public static Literal parseLiteral( String lit ) {
        return new Literal( lit );
    }

    public static String printLiteral( Literal lit ) {
        return lit.toString();
    }
}
