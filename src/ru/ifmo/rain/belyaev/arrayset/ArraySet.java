package ru.ifmo.rain.belyaev.arrayset;

import java.util.*;

public class ArraySet<E> extends AbstractSet<E> implements SortedSet<E> {

    private final List<E> data;
    private final Comparator<? super E> comparator;

    public ArraySet() {
        this(Collections.emptyList(), null);
    }


    public ArraySet(Comparator<? super E> comparator) {
        this(Collections.emptyList(), comparator);
    }

    public ArraySet(Collection<? extends E> source) {
        this(source, null);
    }

    public ArraySet(Collection<? extends E> source, Comparator<? super E> comparator) {
        this.comparator = comparator;
        TreeSet<E> tempSet = new TreeSet<>(comparator);
        tempSet.addAll(source);
        data = new ArrayList<>(tempSet);
    }

    private ArraySet(List<E> list, Comparator<? super E> comparator ){
        data = list;
        this.comparator = comparator;
    }

    // AbstractCollection

    @Override
    public Iterator<E> iterator() {
        return Collections.unmodifiableList(data).iterator();
    }

    @Override
    public int size() {
        return data.size();
    }

    // SortedSet


    @Override
    public Comparator<? super E> comparator() {
        return comparator;
    }

    @Override
    public SortedSet<E> subSet(E fromElement, E toElement) {
        return sub(fromElement, toElement, false);
    }

    @Override
    public SortedSet<E> headSet(E toElement) {
        if (data.isEmpty()) {
            return new ArraySet<>(comparator);
        }
        return sub(first(), toElement, false);
    }

    @Override
    public SortedSet<E> tailSet(E fromElement) {
        if (data.isEmpty()) {
            return new ArraySet<>(comparator);
        }
        return sub(fromElement, last(), true);
    }

    @Override
    public E first() {
        return requireNonEmpty(data).get(0);
    }

    @Override
    public E last() {
        return requireNonEmpty(data).get(size() - 1);
    }

    private boolean isCorrectIndex(int index) {
        return 0 <= index && index < size();
    }

    private int indexOf(final E element, int shiftFound, int shiftNotFound) {
        int index = Collections.binarySearch(data, Objects.requireNonNull(element), comparator);
        if (index < 0) {
            index = -index - 1;
            return isCorrectIndex(index + shiftNotFound) ? index + shiftNotFound : -1;
        }
        return isCorrectIndex(index + shiftFound) ? index + shiftFound : -1;
    }

    private int indexOf(final E element) {
        return indexOf(element, 0, 0);
    }


    private List<E> requireNonEmpty(final List<E> data) {
        if (data.isEmpty()) {
            throw new NoSuchElementException();
        }
        return data;
    }

    private SortedSet<E> sub(final E from, final E to, boolean inclusiveTo) {
        int startIndex = indexOf(from);
        int endIndex = inclusiveTo ?
                indexOf(to, 0, -1) :
                indexOf(to, -1, -1);
        if (startIndex > endIndex || startIndex == -1 || endIndex == -1) {
            return new ArraySet<>(comparator);
        }

        return new ArraySet<>(data.subList(startIndex, endIndex + 1), comparator);
    }


    @SuppressWarnings("unchecked")
    @Override
    public boolean contains(Object o) {
        return Collections.binarySearch(data, (E) Objects.requireNonNull(o), comparator) >= 0;
    }
}
