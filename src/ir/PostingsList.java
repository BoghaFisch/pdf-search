/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   First version:  Johan Boye, 2010
 *   Second version: Johan Boye, 2012
 */  

package ir;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;
import java.io.Serializable;

/**
 *   A list of postings for a given word.
 */
public class PostingsList implements Serializable {
    
    /** The postings list as a linked list. */
    private LinkedList<PostingsEntry> list = new LinkedList<PostingsEntry>();


    /**  Number of postings in this list  */
    public int size() {
    	return list.size();
    }

    /**  Returns the ith posting */
    public PostingsEntry get( int i ) {
    	return list.get( i );
    }
    public PostingsEntry getLastEntry() {
    	if (list.isEmpty())
    		return null;
		else return list.getLast();
    }
    public boolean isEmpty() {
    	return list.isEmpty();
    }
    public Iterator<PostingsEntry> getIterator() {
    	return list.iterator();
    }
    public void add(PostingsEntry pe) {
    	if (list.isEmpty() || list.getLast().docID != pe.docID) {
    		list.addLast(pe);
    	}
    	else if (list.getLast().docID == pe.docID) {
    		list.getLast().addPos(pe.getLastPos());
    	}
    }
    /** Put a new  PostingsEntry to PostingsList, score set to 0  */
    public void add(int docID, int pos) {
    	if (list.isEmpty() || list.getLast().docID != docID) {
    		list.addLast(new PostingsEntry(docID, pos));
    	}
    	else if (list.getLast().docID == docID) {
    		list.getLast().addPos(pos);
    	}
    }
    /**
     * Unions this postingslist with another postingslist
     * 
     * Assumes that the lists are sorted by docID
     * 
     * @param other
     */
    public void unionWith(PostingsList other) {
    	
    	ListIterator<PostingsEntry> it1 = list.listIterator();
    	Iterator<PostingsEntry> it2 = other.getIterator();
    	
    	LinkedList<PostingsEntry> merged = new LinkedList<PostingsEntry>();
    	
    	// If both are non-empty, step through them and merge by docID-order
    	if (it1.hasNext() && it2.hasNext()) {
    		PostingsEntry pe1 = it1.next();
    		PostingsEntry pe2 = it2.next();
    		
    		while (true) {
    			
    			// If same document, add score to the existing score
    			if (pe1.docID == pe2.docID) {
    				pe1.score += pe2.score;
    				merged.addLast(pe1);
    				
    				if (!it1.hasNext() || !it2.hasNext())
    					break;
    				else {
    					pe1 = it1.next();
    					pe2 = it2.next();
    				}
    			}
    			// If not the same document, add a new postingsentry for the document
    			else if (pe1.docID > pe2.docID) {
    				merged.addLast(pe2);
    				if (it2.hasNext())
    					pe2 = it2.next();
    				else break;
    			}
    			else if (pe1.docID < pe2.docID) {
    				merged.addLast(pe1);
    				if (it1.hasNext())
    					pe1 = it1.next();
    				else break;
    			}
    		}
    	}
    	if (it1.hasNext() && !it2.hasNext()) {
    		while (it1.hasNext()) {
    			merged.addLast(it1.next());
    		}
    	}
    	if (!it1.hasNext() && it2.hasNext()) {
    		while (it2.hasNext()) {
    			merged.addLast(it2.next());
    		}
    	}
    	list = merged;
    }
    public void sort() {
    	Collections.sort(list);
    }
    public String toString() {
    	String rString = "";
    	for (PostingsEntry pe : list ) 
    		rString += (pe.toString()+" ");
    	return rString;
    }
}
	

			   
