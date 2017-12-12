/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   First version:  Hedvig Kjellström, 2012
 */  

package ir;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.StringTokenizer;

public class Query {
	
	private static double ALPHA = 0.5;
	private static double BETA = 5;
    
    public LinkedList<String> terms = new LinkedList<String>();
    public HashMap<String, Double> weights = new HashMap<String, Double>();
    public int queryLength = 0;

    /**
     *  Creates a new empty Query 
     */
    public Query() {
    	
    }
	
    /**
     *  Creates a new Query from a string of words
     */
    public Query( String queryString  ) {
		StringTokenizer tok = new StringTokenizer( queryString );
		while ( tok.hasMoreTokens() ) {
			String token = tok.nextToken();
		    terms.add( token );
		    weights.put(token, new Double(1) );
		}
		// Normalize with the size of the query
		queryLength = terms.size();
		for (String term : terms) {
			weights.put(term, weights.get(term) / queryLength);
		}
    }
    
    /**
     *  Returns the number of terms
     */
    public int size() {
    	return terms.size();
    }
    
    /**
     *  Returns a shallow copy of the Query
     */
    public Query copy() {
		Query queryCopy = new Query();
		queryCopy.terms = (LinkedList<String>) terms.clone();
		queryCopy.weights = (HashMap<String, Double>) weights.clone();
		return queryCopy;
    }

    /**
     *  Expands the Query using Relevance Feedback
     */
    public void relevanceFeedback (PostingsList results, boolean[] docIsRelevant, Indexer indexer) {
    	ArrayList<Integer> relevantDocs = new ArrayList<Integer>();
    	for (int i = 0; i < docIsRelevant.length; i++) {
    		if (docIsRelevant[i]) {
    			relevantDocs.add(results.get(i).docID);
    		}
    	}
    	if (relevantDocs.size() < 1) {
    		return;
    	}
    	
    	HashMap<Integer, HashMap<String, Double>> tfidfs = new HashMap<Integer, HashMap<String, Double>>();
    	int N = indexer.index.docIDs.size();
    	
    	for (Integer docID : relevantDocs) {
    		HashMap<String, Double> docVector = new HashMap<String, Double>();
    		HashMap<String, Integer> doc = indexer.index.getDocument(docID);
    		for (String word : doc.keySet()) {
    			int tf = doc.get(word);
    			queryLength += tf;
    			double idf = Math.log(N / (double) indexer.index.getPostings(word).size());
    			double tfidf = tf * idf / indexer.index.docLengths.get(docID+"");
    			docVector.put(word, tfidf);
    		}
    		tfidfs.put(docID, docVector);
    	}
    	
    	/** Do Rocchio **/
    	
    	// Multiply old query-terms with ALPHA
    	for (String term : terms) {
    		weights.put(term, weights.get(term) * ALPHA);
    	}
    	// Multiply new term-weights with beta and divide by number of relevant docs
    	for (Integer docID : relevantDocs) {
    		HashMap<String, Double> docVector = tfidfs.get(docID);
    		for (String word : docVector.keySet()) {
    			if (!weights.containsKey(word)) {
    				terms.add(word);
    				weights.put(word, BETA*docVector.get(word)/relevantDocs.size());
    			}
    			else {
    				weights.put(word, weights.get(word) + BETA*(docVector.get(word))/relevantDocs.size());
    			}
    		}
    	}
    }
}