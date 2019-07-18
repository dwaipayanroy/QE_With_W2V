/**
 * SET THE ANALYZER WITH Standard-ANALYZER AND SMART-STOPWORD LIST
 * 
 * @variables:
 *      Analyzer    analyzer;       // the analyzer
 *      String      stopwordPath;   // path of the smart-stopword file
 * @constructors:
 *      // assumed that the smart-stopword file is present in: /home/dwaipayan/Dropbox/ir/smart-stopwords
 *      public StandardAnalyzerWithSmartStopword() {}
 *      // 
 *      public StandardAnalyzerWithSmartStopword(String stopwordPath) {}
 * @methods:
 *      private void setStandardAnalyzerWithSmartStopword() {}
 *      public Analyzer getStandardAnalyzerWithSmartStopword() {}
 *      public Analyzer setAndGetStandardAnalyzerWithSmartStopword() {}
 */
package common;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.standard.StandardAnalyzer;

/**
 *
 * @author dwaipayan
 */

public class StandardAnalyzerWithSmartStopword {

    Analyzer    analyzer;
    String      stopFilePath;

    /**
     * Assumed that the smart-stopword file is present in the path:
     *      <a href=resources/smart-stopwords>stopword-path</a>
     */
    public StandardAnalyzerWithSmartStopword() {
        this.stopFilePath = "resources/smart-stopwords";
        //setEnglishAnalyzerWithSmartStopword();
    }

    public StandardAnalyzerWithSmartStopword(String stopwordPath) {
        this.stopFilePath = stopwordPath;
        //setEnglishAnalyzerWithSmartStopword();
    }
    /**
     * Set analyzer with EnglishAnalyzer with SMART-stoplist
     */
    public void setStandardAnalyzerWithSmartStopword() {

        List<String> stopwords = new ArrayList<>();

        String line;
        try {
            System.out.println("Stopword Path: "+stopFilePath);
            FileReader fr = new FileReader(stopFilePath);
            BufferedReader br = new BufferedReader(fr);
            while ( (line = br.readLine()) != null ) {
                stopwords.add(line.trim());
            }
            br.close(); fr.close();
        } catch (FileNotFoundException ex) {
            System.err.println("Error: \n"
                + "StandardAnalyzerWithSmartStopword: setAnalyzer()\n"
                + "Stopword file not found in: "+stopFilePath);
            System.exit(1);
        } catch (IOException ex) {
            System.err.println("Error: \n"
                + "StandardAnalyzerWithSmartStopword: setAnalyzer()\n"
                + "IOException occurs");
            System.exit(1);
        }

        analyzer = new StandardAnalyzer(StopFilter.makeStopSet(stopwords));
    }

    /** 
     * Get the EnglishAnalyzer with Smart stopword list
     * @return analyzer
     */
    public Analyzer getStandardAnalyzerWithSmartStopword() { return analyzer; }

    /** 
     * Set and get an EnglishAnalyzer with Smart stopword list
     * @return analyzer
     */
    public Analyzer setAndGetStandardAnalyzerWithSmartStopword() {setStandardAnalyzerWithSmartStopword(); return analyzer; }
}
