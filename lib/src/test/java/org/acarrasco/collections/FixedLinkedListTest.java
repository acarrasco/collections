package org.acarrasco.collections;

import org.junit.Test;
import static org.junit.Assert.*;

public class FixedLinkedListTest {
    
    @Test public void testConstructor() {
        final FixedLinkedList<Integer> list = new FixedLinkedList<>(5);
        assertEquals(0, list.size());
        assertEquals(null, list.head());
        assertEquals("FixedLinkedList([])", list.toString());
    }

    @Test public void testAppendOnEmpty() {
        final FixedLinkedList<Integer> list = new FixedLinkedList<>(5);
        FixedLinkedList<Integer>.Node result = list.add(42);

        assertEquals(Integer.valueOf(42), result.value);
        assertSame(result, list.head());
        assertSame(result, result.getNext());
        assertSame(result, result.getPrevious());
        assertEquals(1, list.size());
        assertEquals("FixedLinkedList([42, ])", list.toString());
    }

    @Test public void testAppendTwice() {
        final FixedLinkedList<Integer> list = new FixedLinkedList<>(5);
        FixedLinkedList<Integer>.Node firstResult = list.add(42);
        FixedLinkedList<Integer>.Node secondResult = list.add(43);
        assertSame(firstResult, list.head());
        assertSame(secondResult, firstResult.getNext());
        assertSame(secondResult, firstResult.getPrevious());
        assertSame(firstResult, secondResult.getNext());
        assertSame(firstResult, secondResult.getPrevious());
        assertEquals(Integer.valueOf(42), firstResult.value);
        assertEquals(Integer.valueOf(43), secondResult.value);
        assertEquals(2, list.size());
        assertEquals("FixedLinkedList([42, 43, ])", list.toString());
    }

    @Test public void testAppendAndThenRemove() {
        final FixedLinkedList<Integer> list = new FixedLinkedList<>(5);
        FixedLinkedList<Integer>.Node result = list.add(42);
        result.remove();
        assertEquals(0, list.size());
        assertEquals(null, list.head());
    }

    @Test public void testAppendThrideRemoveInTheMiddle() {
        final FixedLinkedList<Integer> list = new FixedLinkedList<>(5);
        list.add(42);
        FixedLinkedList<Integer>.Node secondResult = list.add(43);
        list.add(44);
        secondResult.remove();
        assertEquals(2, list.size());
        assertEquals("FixedLinkedList([42, 44, ])", list.toString());
    }

    @Test public void testRemoveHead() {
        final FixedLinkedList<Integer> list = new FixedLinkedList<>(5);
        list.add(1);
        list.add(2);
        list.add(3);

        list.head().remove();
        assertEquals(2, list.size());
        assertEquals("FixedLinkedList([2, 3, ])", list.toString());
    }

    @Test public void testAddFirst() {
        final FixedLinkedList<Integer> list = new FixedLinkedList<>(5);
        list.addFirst(1);
        list.addFirst(2);
        list.addFirst(3);
        assertEquals("FixedLinkedList([3, 2, 1, ])", list.toString());
    }

    @Test public void testMaxCapacity() {
        final FixedLinkedList<Integer> list = new FixedLinkedList<>(3);
        list.add(1);
        list.add(2);
        list.add(3);
        assertThrows(IndexOutOfBoundsException.class, () -> {
            list.add(4);
        });
    }
}
