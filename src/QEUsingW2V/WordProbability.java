/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package QEUsingW2V;

/**
 *
 * @author dwaipayan
 */

class WordProbability {
    String w;
    float p_w_given_R;      // proba. of w given R

    public WordProbability() {
    }

    public WordProbability(String w, float p_w_given_R) {
        this.w = w;
        this.p_w_given_R = p_w_given_R;
    }

}
