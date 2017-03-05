
package common;

import java.io.IOException;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;

/**
 *
 * @author dwaipayan
 */
public class CommonMethods {

    /**
     * Returns a string-buffer in the TREC-res format for the passed queryId
     * @param queryId
     * @param hits
     * @param searcher
     * @param runName
     * @return
     * @throws IOException 
     */
    static final public StringBuffer writeTrecResFileFormat(String queryId, ScoreDoc[] hits, 
        IndexSearcher searcher, String runName) throws IOException {
        StringBuffer resBuffer = new StringBuffer();
        int hits_length = hits.length;
        for (int i = 0; i < hits_length; ++i) {
            int luceneDocId = hits[i].doc;
            Document d = searcher.doc(luceneDocId);
            resBuffer.append(queryId).append("\tQ0\t").
                append(d.get("docid")).append("\t").
                append((i)).append("\t").
                append(hits[i].score).append("\t").
                append(runName).append("\n");                
        }

        return resBuffer;
    }
}
