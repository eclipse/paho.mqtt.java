// Version: %Z% %W% %I% %E% %U%
/********************************************************************\
 *                      IBM Micro Broker
 *  IBM Confidential
 *
 * OCO Source Materials
 *
 *  5724-K75
 *
 * (C) Copyright IBM Corp. 2006
 *
 * The source code for this program is not published or otherwise
 * divested of its trade secrets, irrespective of what has been
 * deposited with the U.S. Copyright Office.
 *
 *
\********************************************************************/
package org.eclipse.paho.jmeclient.mqttv3.test;

import java.util.NoSuchElementException;

/**
 * A simple list that allows adding and removing of elements.
 * This list is used since MIDP does not provide a list imlementation.
 */
public class SimpleList {

	static class Element {
		Element prev, next;
		Object obj;
		private Element(Object obj) {
			this.obj=obj;
			this.prev=null;
			this.next=null;
		}
	};

	/**
	 * A internal class that implements the iterator interface.
	 */
	private class SimpleIterator implements Iterator {

		private Element current;
		private Element lastReturned;
		
		private SimpleIterator(Element root) {
			this.current = root;
			this.lastReturned=null;
		}
		
		/*
		 *  (non-Javadoc)
		 * @see com.ibm.mqttdirect.core.utils.Iterator#hasNext()
		 */
		public boolean hasNext() {
			return this.current!=null && this.current.next!=null; 
		}

		/*
		 *  (non-Javadoc)
		 * @see com.ibm.mqttdirect.core.utils.Iterator#next()
		 */
		public Object next() {
			if(this.current==null || this.current.next==null) {
				throw new NoSuchElementException();
			}
			Object o = this.current.obj;
			this.lastReturned=current;
			this.current = this.current.next;
			return o;
		}

		/**
		 * Remove the last returned element.
		 */
		public void remove() {
			if(this.lastReturned==null) {
				throw new IllegalStateException();
			}
			// remove lastReturned.
			if(this.lastReturned==SimpleList.this.root) {
				SimpleList.this.removeFirst();
			} else {
				SimpleList.this.remove(this.lastReturned);
			}
		}
	}
	
	/*
	 * The root of the list. In order to simplify the implementation, a sentinel is used
	 * that makes sure that root is never null.
	 */
	protected Element root;
	protected Element last;
	private int size;

	/**
	 * Constructs a new, empty simple list.
	 */
	public SimpleList() {
		this.clear();
	}
	
	/**
	 * Adds a new object to the beginning of the list.
	 * @param obj The new object.
	 */
	public void addFirst(Object obj) {
		addBefore(root,obj);
	}
	
	/**
	 * Adds a new object to the end of the list.
	 * @param obj The new object.
	 */
	public void addLast(Object obj) {
		if(this.last==null)
			addBefore(root,obj);
		else
			addAfter(this.last,obj);
	}

	/**
	 * Inserts a new object in front of a provided list element.
	 * @param here The element that indicates the position in the list.
	 * @param obj The object that is inserted.
	 */
	void addBefore(Element here, Object obj) {
		Element e=new Element(obj);
		e.next = here;
		if(here.prev!=null) { here.prev.next=e; }
		e.prev= here.prev;
		here.prev=e;
		if(root==here) {	root=e; }
		if(last==null) { last=e;}
		this.size++;				
	}
	
	/**
	 * Adds a new object after the provided list element.
	 * The list element can not be the sentinel element.
	 * @param here The element that indicates the position in the list.
	 * @param obj The object that is inserted.
	 */
	void addAfter(Element here, Object obj) {
		Element e=new Element(obj);
		e.next = here.next;
		here.next.prev = e;
		here.next=e;
		e.prev = here;
		if(last==here) {
			last=e;
		}
		this.size++;		
	}
	
	/**
	 * Removes the element from the list.
	 * Element must not be root.
	 * @param element The element to be removed.
	 */
	private void remove(Element element) {
		if(this.last==element) {
			this.last=element.prev;
		}
		element.prev.next = element.next;
		element.next.prev =element.prev;
		element.obj=null;
		this.size--;
	}

	/**
	 * Returns the size of the list.
	 * @return The size of the list.
	 */
	public int size() { return this.size; }

	/**
	 * Returns the object that is stored first in the list.
	 * @return The first object in the list.
	 */
	public Object getFirst() {
		Object o=null;
		// If root.next == null, then root points to the sentinel and
		// the list is empty.
		if(this.root.next!=null) {
			o=this.root.obj;
		}
		return o;
	}

	/**
	 * Removes the first object from the list and returns it.
	 * @return The first object in the list.
	 */
	public Object removeFirst() {
		Object o=null;
		// is the list empty?
		if(this.root.next!=null) {
			// Is there just one element left?
			if(last==root) {last=null;}
			o=this.root.obj;
			this.root=this.root.next;
			this.root.prev=null;
			this.size--;
		}
		return o;
	}
	
	/**
	 * Returns true if the list is empty.
	 * @return true, if the list is empty.
	 */
	public boolean isEmpty() {
		return this.size==0;
	}
		
	/**
	 * Returns an iterator, initialized to the first element of the list.
	 * @return An iterator over the list.
	 */
	public Iterator iterator() {
		return new SimpleIterator(this.root);
	}

	/**
	 * Removes all elements from the list.
	 */
	public void clear() {
		this.size=0;
		// allocate sentinel.
		this.root=new Element(null);
		this.last=null;
	}
	
}
