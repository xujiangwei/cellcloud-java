/*
-----------------------------------------------------------------------------
This source file is part of Cell Cloud.

Copyright (c) 2009-2017 Cell Cloud Team (www.cellcloud.net)

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
-----------------------------------------------------------------------------
*/

package net.cellcloud.util.nio;

import java.util.Collections;
import java.util.HashSet;

/**
 * An extension of the {@link HashSet} implementation, with a reference to a
 * minimum element in the set.
 * 
 * @param <T>
 *            The type of object to be contained, must extend {@link Comparable}
 */
public class MinContainer<T extends Object & Comparable<? super T>> {

	private final HashSet<T> elements = new HashSet<>();
	private T min = null;

	/**
	 * Add an element to the underlying {@link HashSet} and recalculate the
	 * minimum element in this set.
	 * 
	 * @param t
	 *            The element to add to the container
	 * @return if this set did not already contain the specified element
	 */
	public boolean add(T t) {
		boolean ret = elements.add(t);
		if (min == null) {
			min = t;
			return ret;
		}

		if ((t.compareTo(min) < 0) ? true : false) {
			min = t;
		}
		return ret;
	}

	/**
	 * 
	 * @return true if this container contains no elements.
	 */
	public boolean isEmpty() {
		return elements.isEmpty();
	}

	/**
	 * Get the minimum element in the the underlying {@link HashSet}
	 * 
	 * @return the minimum element in the the underlying {@link HashSet}
	 */
	public T getMin() {
		return min;
	}

	/**
	 * Remove the given element from the underlying {@link HashSet} and
	 * recalculate the minimum element contained within. If the underlying
	 * {@link HashSet} is empty, the minimum element is set to null.
	 * 
	 * @param t
	 *            The element to be removed from the container
	 * @return true if this set contained the specified element
	 */
	public boolean remove(T t) {
		boolean ret = elements.remove(t);
		if (t.equals(min) && !elements.isEmpty()) {
			min = Collections.min(elements);
		}
		if (elements.isEmpty()) {
			min = null;
		}
		return ret;
	}

	/**
	 * Clears the underlying {@link HashSet} and nullifies the minimum element.
	 */
	public void clear() {
		min = null;
		elements.clear();
	}

}
