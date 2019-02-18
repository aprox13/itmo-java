package ru.ifmo.rain.belyaev.student;

import info.kgeorgiy.java.advanced.student.Student;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StudentDB implements info.kgeorgiy.java.advanced.student.StudentQuery {


    private static final String DEFAULT_RESULT = "";

    private <R> Stream<R> mappedStream(final Collection<Student> students, Function<Student, R> function){
        return students.stream().map(function);
    }


    private List<String> toList(final Collection<Student> students, Function<Student, String> function){
        return mappedStream(students, function).collect(Collectors.toList());
    }

    private List<Student> sortBy(final Collection<Student> students, Comparator<Student> comparator){
        return students.stream().sorted(comparator).collect(Collectors.toList());
    }

    private Stream<Student> filter(final Collection<Student> students, Predicate<Student> filter){
        return sortStudentsByName(students).stream().filter(filter);
    }

    private List<Student> findBy(final Collection<Student> students, Predicate<Student> filter){
        return filter(students, filter).collect(Collectors.toList());
    }





    @Override
    public List<String> getFirstNames(List<Student> students) {
        return toList(students, Student::getFirstName);
    }

    @Override
    public List<String> getLastNames(List<Student> students) {
        return toList(students, Student::getLastName);
    }

    @Override
    public List<String> getGroups(List<Student> students) {
        return toList(students, Student::getGroup);
    }

    @Override
    public List<String> getFullNames(List<Student> students) {
        return toList(students, student -> String.format("%s %s", student.getFirstName(), student.getLastName()));
    }

    @Override
    public Set<String> getDistinctFirstNames(List<Student> students) {
        return mappedStream(students, Student::getFirstName).collect(Collectors.toSet());
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
        return sortBy(students,
                Comparator
                .comparing(Student::getLastName)
                .thenComparing(Student::getFirstName)
                .thenComparing(Student::getId)
        );
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
        return null;
    }

    @Override
    public List<Map.Entry<String, String>> findStudentNamesByGroupList(List<Student> students, String group) {
        return null;
    }
}
