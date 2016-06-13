package org.johnnei.javatorrent.internal.utils;

import java.util.LinkedList;

/**
 * A {@link java.util.LinkedList} implementation which only records the last n elements dropping out the least recently used ones.
 */
public class RecentLinkedList<E> {

	private final int limit;

	private final LinkedList<E> list;

	public RecentLinkedList(int limit) {
		this.limit = limit;
		list = new LinkedList<>();
	}

	private void add(E element) {
		list.addFirst(element);
		if (list.size() > limit) {
			list.removeLast();
		}
	}

	public E putIfAbsent(E target) {
		for (E element : list) {
			if (!element.equals(target)) {
				continue;
			}

			list.removeFirstOccurrence(element);
			list.addFirst(element);
			return element;
		}

		add(target);
		return target;
	}
}
