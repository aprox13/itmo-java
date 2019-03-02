package ru.ifmo.rain.belyaev.implementor;

import info.kgeorgiy.java.advanced.implementor.Impler;
import info.kgeorgiy.java.advanced.implementor.ImplerException;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Implementor implements Impler {


    private final Function<Class<?>, String> implementedClassName = cls -> cls.getSimpleName() + "Impl";

    private Map<String, GeneratedMethod> implementedMethodsMap;


    private final String NEW_LINE = System.lineSeparator();
    private final String IMPLEMENTED_SEPARATOR = NEW_LINE + NEW_LINE;
    private final String DEFAULT_ARGUMENT_NAME = "argument";


    private class GeneratedMethod {


        private boolean isConstructor;

        GeneratedMethod(Executable executable) {
            isConstructor = false;
            declaration = getMethodDeclaration(executable);
            implementation = getMethodBody(executable);
        }


        String getDeclaration() {
            return declaration;
        }

        String getImplementation() {
            return implementation;
        }

        private String declaration;
        private String implementation;


        private String getMethodDeclaration(Executable executable) {
            return Modifier.toString(executable.getModifiers() & (Modifier.classModifiers() ^ Modifier.ABSTRACT)) +
                    " " +
                    getReturnType(executable) +
                    getName(executable) +
                    "(" + getArgumentsString(executable) + ")" +
                    getExceptionsString(executable);
        }

        private String getName(Executable executable) {
            return executable instanceof Constructor ? implementedClassName.apply(executable.getDeclaringClass()) : executable.getName();
        }


        private String getMethodBody(Executable executable) {
            if (executable instanceof Constructor) {
                return IntStream
                        .range(0, executable.getParameterCount())
                        .mapToObj(i -> DEFAULT_ARGUMENT_NAME + i)
                        .collect(Collectors.joining(", ", "super(", ");"));
            }
            if (executable instanceof Method) {
                if (!Modifier.isAbstract(executable.getModifiers())) {
                    return null;
                }
                Method method = (Method) executable;

                Class<?> returnType = method.getReturnType();
                if (returnType == void.class) {
                    return "return;";
                }
                if (!returnType.isPrimitive()) {
                    return "return null;";
                } else {
                    if (returnType == boolean.class) {
                        return "return false;";
                    }
                    return "return 0;";
                }
            }
            return "";
        }

        private String getReturnType(Executable executable) {
            if (executable instanceof Method) {
                return ((Method) executable).getReturnType().getTypeName() + " ";
            }
            isConstructor = true;
            return "";
        }

        private String getExceptionsString(Executable executable) {
            if (executable.getExceptionTypes().length == 0) {
                return "";
            }
            return Arrays
                    .stream(executable.getExceptionTypes())
                    .map(Class::getCanonicalName)
                    .collect(Collectors.joining(" , ", " throws ", ""));

        }

        private String getArgumentsString(Executable executable) {
            AtomicInteger argNum = new AtomicInteger(0);
            return Arrays
                    .stream(executable.getParameters())
                    .map(parameter -> parameter.getType().getCanonicalName() + " " + DEFAULT_ARGUMENT_NAME + (argNum.getAndIncrement()))
                    .collect(Collectors.joining(", "));
        }

        boolean isImplemented() {
            return getImplementation() != null;
        }

        private String overrideAnnotation() {
            return !isConstructor ? "@Override " : "";
        }


        @Override
        public String toString() {
            if (!isImplemented()) {
                return "Already implemented";
            }
            return overrideAnnotation() + declaration + " { " + implementation + " }" + IMPLEMENTED_SEPARATOR;
        }
    }

    @Override
    public void implement(Class<?> token, Path root) throws ImplerException {
        assertIfNull(token, "Class token");
        assertIfNull(root, "Root path");
        assertTrue(token, Class::isPrimitive, "Class must be non primitive");
        assertTrue(token, Class::isArray, "Class must be non array");
        assertTrue(token, t -> t == Enum.class, "Class must be non enum");
        assertTrue(token, t -> Modifier.isFinal(t.getModifiers()), "Class must be non final");

        implementedMethodsMap = new HashMap<>();

        root = getPathToCreatedJavaFile(token, root);
        createDirectory(root);

        String result = getGeneratedClass(token);
        try (BufferedWriter outFile = Files.newBufferedWriter(root)) {
            outFile.write(result);
        } catch (IOException e) {
            System.out.println("Error with opening output file");
        }

    }


    private String getGeneratedClass(Class<?> token) throws ImplerException {
        StringBuilder generated = new StringBuilder();


        addPackage(token, generated);
        addClassDeclaration(token, generated);

        Arrays.stream(token.getDeclaredConstructors())
                .filter(constructor -> !Modifier.isPrivate(constructor.getModifiers()))
                .map(GeneratedMethod::new)
                .forEach(element -> implementedMethodsMap.put(element.getDeclaration(), element));

        //Arrays.stream(token.getMethods()).forEach(System.out::println);
        //Arrays.stream(token.getDeclaredMethods()).forEach(System.out::println);

        while (token != null) {
            //System.out.println("Generating for class " + token.getSimpleName() + ", interfaces: " + Arrays.toString(token.getInterfaces()));
            Stream.concat(
                    Arrays.stream(token.getDeclaredMethods()),
                    Arrays.stream(token.getMethods()))
                    .distinct()
                    .map(GeneratedMethod::new)
                    .forEach(element -> {
                        if (!implementedMethodsMap.containsKey(element.getDeclaration())) {
                            implementedMethodsMap.put(element.getDeclaration(), element);
                        }
                    });
            token = token.getSuperclass();
        }


        implementedMethodsMap
                .entrySet()
                .stream()
                .map(Map.Entry::getValue)
                .filter(GeneratedMethod::isImplemented)
                .forEach(generated::append);

        return toUnicode(generated.append("}").toString());
    }


    private void addClassDeclaration(Class<?> cls, StringBuilder generated) {
        String action = cls.isInterface() ? "implements" : "extends";
        generated
                .append("public class ")
                .append(cls.getSimpleName())
                .append("Impl ")
                .append(action)
                .append(" ")
                .append(cls.getCanonicalName())
                .append(" {")
                .append(IMPLEMENTED_SEPARATOR);
    }

    private void addPackage(Class<?> cls, StringBuilder generated) {
        generated.append("package ").append(cls.getPackageName()).append(";").append(IMPLEMENTED_SEPARATOR);
    }


    private String toUnicode(String in) {
        StringBuilder encoded = new StringBuilder();
        in.chars().forEach(c -> {
            if (c >= 128) {
                encoded.append(String.format("\\u%04X", c));
            } else {
                encoded.append((char) c);
            }
        });
        return encoded.toString();
    }


    private static <T> void assertTrue(T value, Predicate<T> function, String message) throws ImplerException {
        if (function.test(value)) {
            throw new ImplerException(message);
        }
    }


    private static <T> void assertIfNull(T value, String varName) throws ImplerException {
        assertTrue(value, Objects::isNull, varName + " required non null");
    }

    private void createDirectory(Path path) throws ImplerException {
        if (path.getParent() != null) {
            try {
                Files.createDirectories(path.getParent());
            } catch (IOException | SecurityException e) {
                throw new ImplerException("Unable to create directories for output file", e);
            }
        }
    }

    private Path getPathToCreatedJavaFile(Class cls, Path path) {
        return path
                .resolve(cls.getPackageName().replace('.', File.separatorChar))
                .resolve(implementedClassName.apply(cls) + ".java");
    }


    public static void main(String[] args) {
        try {
            assertTrue(args, arguments -> arguments == null || arguments.length != 2, "Excepted 2 params: class name and path to save");
            assertIfNull(args[0], "class name argument");
            assertIfNull(args[1], "path argument");
        } catch (ImplerException e) {
            System.err.println(e.getMessage());
            return;
        }

//        System.out.println("[main] Input arguments: " + Arrays.toString(args));

        Impler implementor = new Implementor();

        try {
            implementor.implement(Class.forName(args[0]), Path.of(args[1]));
        } catch (ImplerException e) {
            System.err.println("Something went wrong while implemented: " + e.getMessage());
        } catch (ClassNotFoundException e) {
            System.err.println("Incorrect class name: " + args[0]);
        }

    }

}
