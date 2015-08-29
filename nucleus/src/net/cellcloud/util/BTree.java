/*
-----------------------------------------------------------------------------
This source file is part of Cell Cloud.

Copyright (c) 2009-2013 Cell Cloud Team (www.cellcloud.net)

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

package net.cellcloud.util;

/**
 * B-Tree implemention
 */
public class BTree<Key extends Comparable<Key>, Value> {

	private static final int M = 4; // max children per B-tree node = M-1

	private Node root; // root of the B-tree
	private int HT; // height of the B-tree
	private int N; // number of key-value pairs in the B-tree

	// helper B-tree node data type
	private static final class Node {
		private int m; // number of children
		private Entry[] children = new Entry[M]; // the array of children

		private Node(int k) {
			m = k;
		} // create a node with k children
	}

	// internal nodes: only use key and next
	// external nodes: only use key and value
	private static class Entry {
		private Comparable<?> key;
		private Object value;
		private Node next; // helper field to iterate over array entries

		public Entry(Comparable<?> key, Object value, Node next) {
			this.key = key;
			this.value = value;
			this.next = next;
		}
	}

	// constructor
	public BTree() {
		root = new Node(0);
	}

	// return number of key-value pairs in the B-tree
	public int size() {
		return N;
	}

	// return height of B-tree
	public int height() {
		return HT;
	}

	// search for given key, return associated value; return null if no such key
	public Value get(Key key) {
		return search(root, key, HT);
	}

	@SuppressWarnings("unchecked")
	private Value search(Node x, Key key, int ht) {
		Entry[] children = x.children;

		// external node
		if (ht == 0) {
			for (int j = 0; j < x.m; j++) {
				if (eq(key, children[j].key))
					return (Value) children[j].value;
			}
		}

		// internal node
		else {
			for (int j = 0; j < x.m; j++) {
				if (j + 1 == x.m || less(key, children[j + 1].key))
					return search(children[j].next, key, ht - 1);
			}
		}
		return null;
	}

	// insert key-value pair
	// add code to check for duplicate keys
	public void put(Key key, Value value) {
		Node u = insert(root, key, value, HT);
		N++;
		if (u == null)
			return;

		// need to split root
		Node t = new Node(2);
		t.children[0] = new Entry(root.children[0].key, null, root);
		t.children[1] = new Entry(u.children[0].key, null, u);
		root = t;
		HT++;
	}

	private Node insert(Node h, Key key, Value value, int ht) {
		int j;
		Entry t = new Entry(key, value, null);

		// external node
		if (ht == 0) {
			for (j = 0; j < h.m; j++) {
				if (less(key, h.children[j].key))
					break;
			}
		}

		// internal node
		else {
			for (j = 0; j < h.m; j++) {
				if ((j + 1 == h.m) || less(key, h.children[j + 1].key)) {
					Node u = insert(h.children[j++].next, key, value, ht - 1);
					if (u == null)
						return null;
					t.key = u.children[0].key;
					t.next = u;
					break;
				}
			}
		}

		for (int i = h.m; i > j; i--)
			h.children[i] = h.children[i - 1];
		h.children[j] = t;
		h.m++;
		if (h.m < M)
			return null;
		else
			return split(h);
	}

	// split node in half
	private Node split(Node h) {
		Node t = new Node(M / 2);
		h.m = M / 2;
		for (int j = 0; j < M / 2; j++)
			t.children[j] = h.children[M / 2 + j];
		return t;
	}

	// for debugging
	@Override
	public String toString() {
		return toString(root, HT, "") + "\n";
	}

	private String toString(Node h, int ht, String indent) {
		String s = "";
		Entry[] children = h.children;

		if (ht == 0) {
			for (int j = 0; j < h.m; j++) {
				s += indent + children[j].key + " " + children[j].value + "\n";
			}
		} else {
			for (int j = 0; j < h.m; j++) {
				if (j > 0)
					s += indent + "(" + children[j].key + ")\n";
				s += toString(children[j].next, ht - 1, indent + "     ");
			}
		}
		return s;
	}

	// comparison functions - make Comparable instead of Key to avoid casts
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private boolean less(Comparable k1, Comparable k2) {
		return k1.compareTo(k2) < 0;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private boolean eq(Comparable k1, Comparable k2) {
		return k1.compareTo(k2) == 0;
	}
}
