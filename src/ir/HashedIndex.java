/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   First version:  Johan Boye, 2010
 *   Second version: Johan Boye, 2012
 *   Additions: Hedvig Kjellström, 2012-14
 */  


package ir;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;


/**
 *   Implements an inverted index as a Hashtable from words to PostingsLists.
 */
public class HashedIndex implements Index {
	
	public static boolean CREATE_INVERSE = true;
	
    /** The index as a hashtable. */
    private HashMap<String,PostingsList> index = new HashMap<String,PostingsList>();
    private HashMap<Integer,HashMap<String, Integer>> inverseIndex = new HashMap<Integer, HashMap<String,Integer>>();


    /**
     *  Inserts this token in the index.
     */
    public void insert( String token, int docID, int offset ) {
    	// Put into regular index
    	if (!index.containsKey(token)) {
			index.put(token, new PostingsList());
		}
		index.get(token).add(docID, offset);
		
		// If we want to create inverse
		if (CREATE_INVERSE) {
			if (!inverseIndex.containsKey(docID)) {
				inverseIndex.put(docID, new HashMap<String, Integer>());
			}
			if (!inverseIndex.get(docID).containsKey(token)) {
				inverseIndex.get(docID).put(token, 1);
			}
			else {
				Integer val = inverseIndex.get(docID).get(token);
				inverseIndex.get(docID).replace(token, (val+1));
			}
		}
    }


    /**
     *  Returns all the words in the index.
     */
    public Iterator<String> getDictionary() {
		// 
		//  REPLACE THE STATEMENT BELOW WITH YOUR CODE
		//
		return null;
    }
    /**
     *  Returns the postings for a specific term, or null
     *  if the term is not in the index.
     */
    public PostingsList getPostings( String token ) {
		if (index.containsKey(token))
			return index.get(token);
		else
			return null;
    }
    public HashMap<String, Integer> getDocument(int docID) {
    	return inverseIndex.get(docID);
    }


    /**
     *  Searches the index for postings matching the query.
     */
    public PostingsList search( Query query, int queryType, int rankingType, int structureType ) {
    	long startTime = System.currentTimeMillis();
    	PostingsList rList = new PostingsList();
    	
    	// Only do if the user specified a query
		if (!query.terms.isEmpty()) {
			
			// Intersection query
    		if (queryType == Index.INTERSECTION_QUERY) {
	    		rList = intersectQuery(query);
	    	}
    		
    		// Phrase query
		    else if (queryType == Index.PHRASE_QUERY) {
		    	rList = phraseQuery(query);
		    }
    		// Ranked query
		    else if (queryType == Index.RANKED_QUERY) {
		    	System.out.println("NEW QUERY");
		    	rList = rankedQuery(query);
		    }
		    // Print times for debbugging (if enabled)
		    if (SearchGUI.PRINT_TIMES)
		    	System.out.println("Time taken to search: " + (System.currentTimeMillis() - startTime));
    	}
	    // Return the PostingsList containing the results
	    return rList;
    }
    public PostingsList rankedQuery(Query query) {
    	PostingsList result = new PostingsList();
    	
    	// Fetch number of documents
		int N = docIDs.size();
    	
		// For each query term t
    	for (String t : query.terms) {
    		
    		// Fetch PostingsList
    		PostingsList pl = index.get(t);
    		
    		if (pl != null) {
	    		// Calculate document frequency df
	    		int df = pl.size();
	    		
	    		// Calculate idf
	    		double idf = Math.log(N / (double) df);
	    		
	    		// For each document d in pl
	    		Iterator<PostingsEntry> it = pl.getIterator();
	    		while(it.hasNext()) {
	    			
	    			// Fetch next PostingsEntry (document d)
	    			PostingsEntry d = it.next();
	    			
	    			// Get term frequency (tf)
	    			int tf = d.getTermFrequency();
	    			
	    			// Calculate tf_idf score
	    			double wtd = tf * idf * query.weights.get(t);
	    			
	    			// Update score for the document
	    			d.score = wtd;
	    		}
	    		
	    		// Union the PostingsList for the query term with the result
	    		result.unionWith(pl);
    		}
    	}
    	// For each document d
		Iterator<PostingsEntry> it = result.getIterator();
		while (it.hasNext()) {
			PostingsEntry d = it.next();
			
			// Divide score by length
			//d.score = d.score / Math.sqrt(docLengths.get(d.docID+""));
			d.score = d.score / docLengths.get(d.docID+"");
			//d.score = d.score / getEuclLength(d.docID);
		}
    	result.sort();
    	return result;
    }
    
    public PostingsList phraseQuery(Query query) {
    	PostingsList result = new PostingsList();
    	
    	// Double check that we have query terms (else empty return)
    	if (query.terms.size() > 0) {
    		
	    	// Make a list of all the PostingLists from the terms in the query
	    	LinkedList<PostingsList> pls = new LinkedList<PostingsList>();
	    	PostingsList postings;
	    	for (String token : query.terms) {
	    		postings = getPostings(token);
    			pls.add(postings);
	    	}
	    	
	    	// Start by adding the first PostingsLIst to the intersection
	    	result = pls.poll();
	    	
	    	// Intersect the first intersection with all remaining PostingsLists
	    	while (!pls.isEmpty()) {
	    		result = positionalIntersect(result, pls.poll());
	    	}
    	}
    	return result;
    }
    private PostingsList positionalIntersect( PostingsList pl1, PostingsList pl2) {
    	
    	// Array where we will store the positional intersect
    	PostingsList answer = new PostingsList();
    	
    	// Only do if none of the PostingsLists are null
    	if (!(pl1 == null || pl2 == null)) {
    		
	    	// Fetch iterators
	    	Iterator<PostingsEntry> peIter1 = pl1.getIterator();
	    	Iterator<PostingsEntry> peIter2 = pl2.getIterator();
	    	
	    	// Only meaningful to compare if both PostingsLists are non-empty
	    	if (peIter1.hasNext() && peIter2.hasNext()) {
	    		PostingsEntry pe1 = peIter1.next();
	    		PostingsEntry pe2 = peIter2.next();
	    		
	    		while (true) {
	    			
	    			// If document ID's are the same, check for occurences where pos1 = pos2 - 1
	    			if ( pe1.docID == pe2.docID ) {
	    				Iterator<Integer> posIter1 = pe1.getPosIterator();
	    				Iterator<Integer> posIter2 = pe2.getPosIterator();
	    				
	    				if (posIter1.hasNext() && posIter2.hasNext()) {
	    					
	    					// Fetch starting positions
	    					int pos1 = posIter1.next();
	    					int pos2 = posIter2.next();
	    					
		    				while(true) {
		    					
		    					// If pos1 is one before pos2, add to answer
		    					if ( (pos1 + 1) == pos2 ) {
		    						if (!answer.isEmpty()) {
		    							if (answer.getLastEntry().docID == pe1.docID) {
		    								answer.getLastEntry().addPos(pos2);
		    							}
		    							else {
		    								answer.add(pe1.docID, pos2);
		    							}
		    						}
		    						else {
		    							answer.add(pe1.docID, pos2);
		    						}
		    					
		    						// If there are any more positions, advance both position-iterators
		    						if (posIter1.hasNext() && posIter2.hasNext()) {
		    							pos1 = posIter1.next();
		    							pos2 = posIter2.next();
		    						}
		    						else break;
		    					}
		    					// If pos1 is smaller than pos2, advance the iterator on the positions in entry 1
		    					else if ( (pos1 + 1) < pos2 ) {
		    						if (!posIter1.hasNext())
		    							break;
		    						else
		    							pos1 = posIter1.next();
		    					}
		    					// If pos2 is smaller than pos1, advance the iterator on the positions in entry 2
		    					else if ( (pos1 + 1) > pos2 ) {
		    						if (!posIter2.hasNext())
		    							break;
		    						else
		    							pos2 = posIter2.next();
		    					}
		    				}
	    				}
	    				// If both PostingsLists has more PostingsEntries, advance the iterators to them. Else break main loop.
	    				if ( peIter1.hasNext() && peIter2.hasNext() ) {
	    					pe1 = peIter1.next();
	    					pe2 = peIter2.next();
	    				}
	    				else break;
	    			}
	    			// Else If PostingsEntry pe1 has a docID which is lower, advance the iterator to PostingsList1 (pl1).
	    			else if (pe1.docID < pe2.docID) {
	    				if (peIter1.hasNext()) {
	    					pe1 = peIter1.next();
	    				}
	    				else break;
	    			}
	    			// Else if PostingsEntry pe2 has docID which is lower than pe1, advance iterator to PostingsList2 (pl2).
	    			else if (pe1.docID > pe2.docID) {
	    				if (peIter2.hasNext()) {
	    					pe2 = peIter2.next();
	    				}
	    				else break;
	    			}
	    		}
	    	}
    	}
    	// Return the positional intersect
    	return answer;
    }

    public PostingsList intersectQuery(Query query) {
    	PostingsList intersection = new PostingsList();
    	
    	// Double check that we have query terms (else empty return)
    	if (query.terms.size() > 0) {
    		
	    	// Make a list of all the postinglists from the terms in the query
	    	LinkedList<PostingsList> pls = new LinkedList<PostingsList>();
	    	PostingsList postings;
	    	for (String token : query.terms) {
	    		postings = getPostings(token);
    			pls.add(postings);
	    	}
	    	
	    	// Start by adding the first PostingsLIst to the intersection
	    	intersection = pls.poll();
	    	
	    	// Intersect the first intersection with all remaining PostingsLists
	    	while (!pls.isEmpty()) {
	    		intersection = intersect(intersection, pls.poll());
	    	}
    	}
    	return intersection;
    }
    /**
     * Finds the intersection between the two PostingsLists
     * 
     * @param p1 - The first PostingsList to intersect
     * @param p2 - The second
     * @return - The intersection between p1 and p2
     */
    private PostingsList intersect( PostingsList pl1, PostingsList pl2 ) {
    	
    	// Create an empty PostingsList which we are to store the intersection in
    	PostingsList answer = new PostingsList();
    	
    	// Do intersect only if both PL's are not null
    	if (!(pl1 == null || pl2 == null)) {
    		
	    	// Use two iterators from the postingslists to iterate through and find the intersection
	    	Iterator<PostingsEntry> it1 = pl1.getIterator();
	    	Iterator<PostingsEntry> it2 = pl2.getIterator();
	    	
	    	// If none of the PostingsList's are empty, run algorithm from book
	    	if (it1.hasNext() && it2.hasNext()) {
	    		boolean done = false;
	    		PostingsEntry pe1 = it1.next();
	    		PostingsEntry pe2 = it2.next();
	    		
	    		// Step through both sets
	    		while (!done) {
	    			// If docID is same, add to answer
	    			if (pe1.docID == pe2.docID) {
	    				answer.add(pe1);
	    				
	    				// Check if any of the entries were the last in their set. If so => done, else step both (match)
	    				if (!it1.hasNext() || !it2.hasNext())
	    					done = true;
	    				else {
	    					pe1 = it1.next();
	    					pe2 = it2.next();
	    				}
	    			}
	    			// If docID1 is smaller than docID2, step up docID1 (if there are any more entries)
	    			else if (pe1.docID < pe2.docID) {
	    				if (it1.hasNext())
	    					pe1 = it1.next();
	    				else done = true;
	    			}
	    			// If docID1 is larger than docID2, step up docID2 (if there are any more entries)
	    			else {
	    				if (it2.hasNext())
	    					pe2 = it2.next();
	    				else done = true;
	    			}
	    		}
	    	}
    	}
    	// Return the intersection
    	return answer;
    }
    // Returns the euclidian length of the doc
    public double getEuclLength(int docID) {
    	double docLength = 0;
    	HashMap<String, Integer> wordCounts = inverseIndex.get(docID);
    	for (Integer num : wordCounts.values()) {
    		docLength += Math.pow(num, 2);
    	}
    	docLength = Math.sqrt(docLength);
    	return docLength;
    }
    public void printWordcount(int docID) {
    	int words = 0;
    	HashMap<String, Integer> map = inverseIndex.get(docID);
    	for (String token : map.keySet()) {
    		words+= map.get(token);
    	}
    	System.out.println(words);
    }
    /**
     *  No need for cleanup in a HashedIndex.
     */
    public void cleanup() {
    
    }
}
