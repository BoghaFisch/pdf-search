/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   First version:  Johan Boye, 2010
 *   Second version: Johan Boye, 2012
 *   Additions: Hedvig Kjellström, 2012-14
 */  


package ir;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;


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
    	if (!index.containsKey(token)) {
			index.put(token, new PostingsList());
		}
		index.get(token).add(docID, offset);
		
		
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
			
    		if (queryType == Index.INTERSECTION_QUERY) {
	    		rList = intersectQuery(query);
	    	}
    		else if (queryType == Index.PHRASE_QUERY) {
		    	rList = phraseQuery(query);
		    }
    		else if (queryType == Index.RANKED_QUERY) {
		    	System.out.println("NEW QUERY");
		    	rList = rankedQuery(query);
		    }
    		
		    if (SearchGUI.PRINT_TIMES)
		    	System.out.println("Time taken to search: " + (System.currentTimeMillis() - startTime));
    	}
	    
		return rList;
    }
    public PostingsList rankedQuery(Query query) {
    	PostingsList result = new PostingsList();
		int N = docIDs.size();
  
    	for (String t : query.terms) {
    		PostingsList pl = index.get(t);
    		
    		if (pl != null) {
	    		int df = pl.size();
	    		double idf = Math.log(N / (double) df);
	    		Iterator<PostingsEntry> it = pl.getIterator();
	    		
	    		while(it.hasNext()) {
	    			PostingsEntry d = it.next();
	    			int tf = d.getTermFrequency();
	    			double wtd = tf * idf * query.weights.get(t);
	    			d.score = wtd;
	    		}
	    		
	    		// Union the PostingsList for the query term with the result
	    		result.unionWith(pl);
    		}
    	}
    	
    	Iterator<PostingsEntry> it = result.getIterator();
		while (it.hasNext()) {
			PostingsEntry d = it.next();
			d.score = d.score / docLengths.get(d.docID+"");
		}
    	result.sort();
    	return result;
    }
    
    public PostingsList phraseQuery(Query query) {
    	PostingsList result = new PostingsList();
    	
    	if (query.terms.size() > 0) {
    		LinkedList<PostingsList> pls = new LinkedList<PostingsList>();
	    	PostingsList postings;
	    	for (String token : query.terms) {
	    		postings = getPostings(token);
    			pls.add(postings);
	    	}
	    	
	    	result = pls.poll();
	    	while (!pls.isEmpty()) {
	    		result = positionalIntersect(result, pls.poll());
	    	}
    	}
    	return result;
    }
    
    /** 
     * Returns an intersected PostingsList by positions. 
     * TODO:: Should be broken in to multiple methods.
    */
    private PostingsList positionalIntersect( PostingsList pl1, PostingsList pl2) {
    	PostingsList answer = new PostingsList();
    	if (!(pl1 == null || pl2 == null)) {
    		Iterator<PostingsEntry> peIter1 = pl1.getIterator();
	    	Iterator<PostingsEntry> peIter2 = pl2.getIterator();
	    	
	    	// Only meaningful to compare if both PostingsLists are non-empty
	    	if (peIter1.hasNext() && peIter2.hasNext()) {
	    		PostingsEntry pe1 = peIter1.next();
	    		PostingsEntry pe2 = peIter2.next();
	    		
	    		while (true) {
	    			if ( pe1.docID == pe2.docID ) {
	    				Iterator<Integer> posIter1 = pe1.getPosIterator();
	    				Iterator<Integer> posIter2 = pe2.getPosIterator();
	    				
	    				if (posIter1.hasNext() && posIter2.hasNext()) {
	    					int pos1 = posIter1.next();
	    					int pos2 = posIter2.next();
	    					
		    				while(true) {
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
		    					
		    						if (posIter1.hasNext() && posIter2.hasNext()) {
		    							pos1 = posIter1.next();
		    							pos2 = posIter2.next();
		    						}
		    						else break;
		    					}
		    					else if ( (pos1 + 1) < pos2 ) {
		    						if (!posIter1.hasNext())
		    							break;
		    						else
		    							pos1 = posIter1.next();
		    					}
		    					else if ( (pos1 + 1) > pos2 ) {
		    						if (!posIter2.hasNext())
		    							break;
		    						else
		    							pos2 = posIter2.next();
		    					}
		    				}
	    				}

	    				if ( peIter1.hasNext() && peIter2.hasNext() ) {
	    					pe1 = peIter1.next();
	    					pe2 = peIter2.next();
	    				}
	    				else break;
	    			}
	    			else if (pe1.docID < pe2.docID) {
	    				if (peIter1.hasNext()) {
	    					pe1 = peIter1.next();
	    				}
	    				else break;
	    			}
	    			else if (pe1.docID > pe2.docID) {
	    				if (peIter2.hasNext()) {
	    					pe2 = peIter2.next();
	    				}
	    				else break;
	    			}
	    		}
	    	}
    	}
    	
    	return answer;
    }

    public PostingsList intersectQuery(Query query) {
    	PostingsList intersection = new PostingsList();
    	
    	if (query.terms.size() > 0) {
    		LinkedList<PostingsList> pls = new LinkedList<PostingsList>();
	    	PostingsList postings;
	    	for (String token : query.terms) {
	    		postings = getPostings(token);
    			pls.add(postings);
	    	}
	    	
	    	intersection = pls.poll();
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
    	PostingsList answer = new PostingsList();
    	if (!(pl1 == null || pl2 == null)) {
    		Iterator<PostingsEntry> it1 = pl1.getIterator();
	    	Iterator<PostingsEntry> it2 = pl2.getIterator();
	    	
	    	if (it1.hasNext() && it2.hasNext()) {
	    		boolean done = false;
	    		PostingsEntry pe1 = it1.next();
	    		PostingsEntry pe2 = it2.next();
	    		
	    		while (!done) {
	    			if (pe1.docID == pe2.docID) {
	    				answer.add(pe1);
	    				if (!it1.hasNext() || !it2.hasNext())
	    					done = true;
	    				else {
	    					pe1 = it1.next();
	    					pe2 = it2.next();
	    				}
	    			}
	    			else if (pe1.docID < pe2.docID) {
	    				if (it1.hasNext())
	    					pe1 = it1.next();
	    				else done = true;
	    			}
	    			else {
	    				if (it2.hasNext())
	    					pe2 = it2.next();
	    				else done = true;
	    			}
	    		}
	    	}
    	}
    	return answer;
    }
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

	@Override
	public Iterator<String> getDictionary() {
		// TODO Auto-generated method stub
		return null;
	}
}
