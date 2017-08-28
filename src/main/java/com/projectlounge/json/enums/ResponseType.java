package com.projectlounge.json.enums;

/**
 * Created by main on 24.08.17.
 */
public enum ResponseType {

    /** Unknown. */
    RESPONSE_TYPE_UNSPECIFIED,

    /** Partial updates are applied to the client's existing local database. */
    PARTIAL_UPDATE,

    /**
     * Full updates replace the client's entire local database.
     * This means that either the client was seriously out-of-date
     * or the client is believed to be corrupt.
     * */
    FULL_UPDATE,
    ;

}
