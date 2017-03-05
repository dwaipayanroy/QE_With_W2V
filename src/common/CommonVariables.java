
package common;

/**
 *
 * @author dwaipayan
 */
public class CommonVariables {
    /**
     * The unique document id of each of the documents.
     */
    static final public String FIELD_ID = "docid";
//    static final public String FIELD_ID = "id";
    /**
     * The analyzed content of each of the documents.
     */
    static final public String FIELD_BOW = "content";
//    static final public String FIELD_BOW = "words";
    /**
     * Analyzed full content (including tag informations): Mainly used for WT10G initial retrieval.
     */
    static final public String FIELD_FULL_BOW = "full-content";
}
