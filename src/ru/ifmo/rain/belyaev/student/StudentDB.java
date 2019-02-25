package ru.ifmo.rain.belyaev.student;

import info.kgeorgiy.java.advanced.student.Group;
import info.kgeorgiy.java.advanced.student.Student;

import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StudentDB implements info.kgeorgiy.java.advanced.student.StudentGroupQuery {


    private static final String DEFAULT_RESULT = "";

    private Comparator<Student> namesOrder = Comparator
            .comparing(Student::getLastName)
            .thenComparing(Student::getFirstName)
            .thenComparing(Student::getId);

    private <R> Stream<R> mappedStream(final Collection<Student> students, Function<Student, R> function) {
        return students.stream().map(function);
    }

    private List<String> toListBy(final Collection<Student> students, Function<Student, String> function) {
        return mappedStream(students, function).collect(Collectors.toList());
    }

    private List<Student> sortBy(final Collection<Student> students, Comparator<Student> comparator) {
        return students.stream().sorted(comparator).collect(Collectors.toList());
    }

    private List<Student> findBy(final Collection<Student> students, Predicate<Student> filter) {
        return getFilteredStream(students, filter).sorted(namesOrder).collect(Collectors.toList());
    }

    private <E> Stream<E> getFilteredStream(Collection<E> collection, Predicate<E> filter) {
        return collection.stream().filter(filter);
    }


    @Override
    public List<String> getFirstNames(List<Student> students) {
        return toListBy(students, Student::getFirstName);
    }

    @Override
    public List<String> getLastNames(List<Student> students) {
        return toListBy(students, Student::getLastName);
    }

    @Override
    public List<String> getGroups(List<Student> students) {
        return toListBy(students, Student::getGroup);
    }

    @Override
    public List<String> getFullNames(List<Student> students) {
        return toListBy(students, student -> String.format("%s %s", student.getFirstName(), student.getLastName()));
    }

    @Override
    public Set<String> getDistinctFirstNames(List<Student> students) {
        return mappedStream(students, Student::getFirstName).collect(Collectors.toCollection(TreeSet::new));
    }

    @Override
    public String getMinStudentFirstName(List<Student> students) {
        return students.stream().min(Student::compareTo).map(Student::getFirstName).orElse(DEFAULT_RESULT);
    }

    @Override
    public List<Student> sortStudentsById(Collection<Student> students) {
        return sortBy(students, Student::compareTo);
    }

    @Override
    public List<Student> sortStudentsByName(Collection<Student> students) {
        return sortBy(students, namesOrder);
    }

    @Override
    public List<Student> findStudentsByFirstName(Collection<Student> students, String name) {
        return findBy(students, student -> student.getFirstName().equals(name));
    }

    @Override
    public List<Student> findStudentsByLastName(Collection<Student> students, String name) {
        return findBy(students, student -> student.getLastName().equals(name));
    }

    @Override
    public List<Student> findStudentsByGroup(Collection<Student> students, String group) {
        return findBy(students, student -> student.getGroup().equals(group));
    }

    @Override
    public Map<String, String> findStudentNamesByGroup(Collection<Student> students, String group) {
        return getFilteredStream(students, student -> student.getGroup().equals(group))
                .collect(Collectors.toMap(Student::getLastName, Student::getFirstName, BinaryOperator.minBy(String::compareTo)));
    }


    /**
     * Hard version
     **/


    private Stream<Map.Entry<String, List<Student>>> toMapEntryStream(Collection<Student> collection) {
        return collection.stream()
                .collect(Collectors.groupingBy(Student::getGroup, TreeMap::new, Collectors.toList()))
                .entrySet()
                .stream();

    }


    private List<Group> getGroupBy(Collection<Student> students, Function<List<Student>, List<Student>> function) {
        return toMapEntryStream(students)
                .map(group -> new Group(group.getKey(), function.apply(group.getValue())))
                .collect(Collectors.toList());
    }


    private String getLargestGroupBy(Collection<Student> students, Function<List<Student>, Integer> function) {
        return toMapEntryStream(students)
                .max(
                        Comparator
                                .comparingInt((Map.Entry<String, List<Student>> group) -> function.apply(group.getValue()))
                                .thenComparing(Map.Entry::getKey, Collections.reverseOrder(String::compareTo))
                ).map(Map.Entry::getKey).orElse(DEFAULT_RESULT);
    }


    @Override
    public List<Group> getGroupsByName(Collection<Student> students) {
        return getGroupBy(students, this::sortStudentsByName);
    }

    @Override
    public List<Group> getGroupsById(Collection<Student> students) {
        return getGroupBy(students, this::sortStudentsById);
    }

    @Override
    public String getLargestGroup(Collection<Student> students) {
        return getLargestGroupBy(students, List::size);
    }

    @Override
    public String getLargestGroupFirstName(Collection<Student> students) {
        return getLargestGroupBy(students, u -> getDistinctFirstNames((u)).size());
    }
}
