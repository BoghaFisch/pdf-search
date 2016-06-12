/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   First version:  Johan Boye, 2010
 *   Second version: Johan Boye, 2012
 */  

package ir;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.ListIterator;

public class PostingsEntry implements Comparable<PostingsEntry>, Serializable {
    
    public int docID;
    public double score;
    private LinkedList<Integer> positions = new LinkedList<Integer>();
    
    /**
     * Construct a PostingsEntry for doc
     * 
     * @param docID
     */
    PostingsEntry(int docID) {
    	this.docID = docID;
    }
    /**
     * Creates a new PostingsEntry containing one position from the start in the
     * list of positions for the words
     * 
     * @param docID - Document ID
     * @param pos - First position of word in doc
     */
    PostingsEntry(int docID, int pos) {
    	this.docID = docID;
    	positions.add(pos);
    }
    /**
     *  PostingsEntries are compared by their score (only relevant 
     *  in ranked retrieval).
     *
     *  The comparison is defined so that entries will be put in 
     *  descending order.
     */
    public int compareTo( PostingsEntry other ) {
    	return Double.compare( other.score, score );
    }
    public String toString() {
    	return "[id="+docID+", score="+score+"]";
    }
    public void addPos(int pos) {
    	positions.add(pos);
    }
    public int getLastPos() {
    	return positions.getLast();
    }
    public ListIterator<Integer> getPosIterator() {
    	return positions.listIterator();
    }
    public int getTermFrequency() {
    	return positions.size();
    }
    public LinkedList<Integer> getPositions() {
    	return positions;
    }
}
