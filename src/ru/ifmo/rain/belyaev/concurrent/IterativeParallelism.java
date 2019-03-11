package ru.ifmo.rain.belyaev.concurrent;

import info.kgeorgiy.java.advanced.concurrent.ListIP;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class IterativeParallelism implements ListIP {


    private <ElementType, ResultType> ResultType operation(int threadsCount,
                                                           List<? extends ElementType> values,
                                                           Function<Stream<? extends ElementType>, ? extends ResultType> mapper,
                                                           Function<Stream<? extends ResultType>, ? extends ResultType> collector) throws InterruptedException {

        threadsCount = Math.max(1, Math.min(threadsCount, values.size()));
        List<ResultType> result = new ArrayList<>(Collections.nCopies(threadsCount, null));
        List<Thread> threads = new ArrayList<>(threadsCount);

        List<Stream<? extends ElementType>> parts = new ArrayList<>();
        int inOnePart = values.size() / threadsCount;

        int left = 0;
        for (int i = 0; i < threadsCount - 1; i++) {
            parts.add(values.subList(left, left + inOnePart).stream());
            left += inOnePart;
        }

        parts.add(values.subList(left, values.size()).stream());


        for (int i = 0; i < threadsCount; i++) {
            final int whereIsMyAtomicInteger = i;
            threads.add(new Thread(
                    () -> result.set(whereIsMyAtomicInteger, mapper.apply(parts.get(whereIsMyAtomicInteger)))
            ));
            threads.get(whereIsMyAtomicInteger).start();
        }


        InterruptedException exceptions = null;

        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                if (exceptions == null) {
                    exceptions = new InterruptedException("Some exceptions from threads");
                }
                exceptions.addSuppressed(e);
            }
        }

        if (exceptions != null) {
            throw exceptions;
        }

        return collector.apply(result.stream());
    }


    // ListIP implementation

    @Override
    public String join(int threads, List<?> values) throws InterruptedException {
        return String.join("", map(threads, values, Object::toString));
    }

    @Override
    public <T> List<T> filter(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return operation(
                threads,
                values,
                stream -> stream.filter(predicate).collect(Collectors.toList()),
                stream -> stream.flatMap(List::stream).collect(Collectors.toList()));
    }

    @Override
    public <T, U> List<U> map(int threads, List<? extends T> values, Function<? super T, ? extends U> f) throws InterruptedException {
        return operation(
                threads,
                values,
                stream -> stream.map(f).collect(Collectors.toList()),
                stream -> stream.flatMap(List::stream).collect(Collectors.toList()));
    }


    // ScalarIP implementation


    @Override
    public <T> T minimum(int threads, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
        return operation(
                threads,
                values,
                value -> value.min(comparator).orElse(null),
                value -> value.min(comparator).orElse(null)
        );
    }

    @Override
    public <T> T maximum(int threads, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
        return minimum(
                threads,
                values,
                Collections.reverseOrder(comparator)
        );
    }



    @Override
    public <T> boolean all(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return operation(
                threads,
                values,
                stream -> stream.allMatch(predicate),
                stream -> stream.allMatch(Boolean::booleanValue)
        );
    }

    @Override
    public <T> boolean any(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return operation(
                threads,
                values,
                stream -> stream.anyMatch(predicate),
                stream -> stream.anyMatch(Boolean::booleanValue)
        );
    }

}
