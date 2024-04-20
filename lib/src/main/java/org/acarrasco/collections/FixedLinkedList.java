package org.acarrasco.collections;

import java.util.Iterator;

/**
 * Custom implementation of a doubly linked list that is backed by an array, should be locality and GC friendly.
 * 
 * This collection is not thread-safe.
 */
public class FixedLinkedList<T> implements Iterable<T> {
    
    class Node {
        private final int idx;
        private int previous;
        private int next;
        public T value;

        public Node(int idx) {
            this.idx = idx;
        }

        public Node getNext() {
            return nodes[next];
        }

        public Node getPrevious() {
            return nodes[previous];
        }

        public void remove() {
            size--;
            if (head == this.idx) {
                head = this.next;
            }
            getPrevious().next = getNext().idx;
            getNext().previous = getPrevious().idx;
            value = null;
            next = -1;
            previous = -1;
            freeIndices[size] = idx;
        }

        public Node append(T value) {
            if (size == nodes.length) {
                throw new IndexOutOfBoundsException("Trying to add more than " + nodes.length + " elements");
            }
            int newNodeIdx = freeIndices[size];
            Node newNode = nodes[newNodeIdx];
            size++;
            newNode.value = value;
            newNode.previous = this.idx;
            newNode.next = this.next;
            this.next = newNodeIdx;
            newNode.getNext().previous = newNodeIdx;
            return newNode;
        }
    }

    private final Node[] nodes;
    private final int[] freeIndices;
    private int size = 0;
    private int head = -1;

    @SuppressWarnings("unchecked")
    public FixedLinkedList(int capacity) {
        this.nodes = new FixedLinkedList.Node[capacity];
        this.freeIndices = new int[capacity];

        for (int i = 0; i < capacity; i++) {
            this.nodes[i] = new Node(i);
            this.freeIndices[i] = i;
        }
    }

    /**
     * The maximum number of elements this list can hold.
     */
    public int capacity() {
        return this.nodes.length;
    }

    /**
     * The number of elements present in the list.
     */
    public int size() {
        return this.size;
    }

    /**
     * The first element of the list.
     */
    public Node head() {
        if (this.size == 0) {
            return null; 
        }
        return this.nodes[this.head];
    }

    /**
     * Adds an element at the end of the list.
     */
    public Node add(T value) {
        if (this.size > 0) {
            return head().getPrevious().append(value);
        } else {
            head = this.freeIndices[0];
            Node node = this.nodes[head];
            node.value = value;
            node.next = head;
            node.previous = head;
            size++;
            return node;
        }
    }

    /**
     * Adds an element at the beginning of the list.
     */
    public Node addFirst(T value) {
        Node node = add(value);
        head = node.idx;
        return node;
    }


    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("FixedLinkedList([");
        for (T value : this) {
            builder.append(value);
            builder.append(", ");
        }
        builder.append("])");
        return builder.toString();
    }

    @Override
    public Iterator<T> iterator() {
        return new Iterator<T>() {
            int idx = -1;

            @Override
            public boolean hasNext() {
                if (idx == -1) {
                    return size > 0;
                } else {
                    return idx != head;
                }
            }

            @Override
            public T next() {
                if (idx == -1) {
                    idx = head;
                }
                final T result = nodes[idx].value;
                idx = nodes[idx].next;
                return result;
            }
        };
    }

}
