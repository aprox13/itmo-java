package ru.ifmo.rain.belyaev.implementor;

import info.kgeorgiy.java.advanced.implementor.ImplerException;
import info.kgeorgiy.java.advanced.implementor.JarImpler;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;


/**
 * Implementation class for {@link JarImpler} interface.
 */
public class Implementor implements JarImpler {


    /**
     * Lambda for add <b>Impl</b> suffix for {@link Class} simple name
     */
    private final Function<Class<?>, String> implementedClassName = cls -> cls.getSimpleName() + "Impl";


    /**
     * {@link Map} of implemented methods ( declaration as {@link String} : method as {@link GeneratedMethod}
     */
    private Map<String, GeneratedMethod> implementedMethodsMap;


    /**
     * New line {@link String} as {@link System#lineSeparator()}
     */
    private final String NEW_LINE = System.lineSeparator();

    /**
     * Two new lines
     */
    private final String IMPLEMENTED_SEPARATOR = NEW_LINE + NEW_LINE;


    /**
     * Default argument name as {@link String}
     */
    private final String DEFAULT_ARGUMENT_NAME = "argument";


    /**
     * Class used for correct representing {@link Method}
     */
    private class GeneratedMethod {


        /**
         * Flag for constructors
         */
        private boolean isConstructor;

        /**
         * Create new instance of {@link GeneratedMethod} from {@link Executable}
         *
         * @param executable instance of {@link Executable} method
         */
        GeneratedMethod(Executable executable) {
            isConstructor = false;
            declaration = getMethodDeclaration(executable);
            implementation = getMethodBody(executable);
        }


        /**
         * Declaration geber
         *
         * @return {@link String} of declaration method
         */
        String getDeclaration() {
            return declaration;
        }


        /**
         * Implementation geber
         *
         * @return {@link String} of implementation
         */
        String getImplementation() {
            return implementation;
        }


        /**
         * Declaration {@link String} holder
         */
        private String declaration;

        /**
         * Implementation {@link String} holder
         */
        private String implementation;


        /**
         * Parsing method declaration from {@link Executable}
         *
         * @param executable instance of method as {@link Executable}
         * @return {@link String} of parsed declaration of {@link Executable}
         */
        private String getMethodDeclaration(Executable executable) {
            return Modifier.toString(executable.getModifiers() & (Modifier.classModifiers() ^ Modifier.ABSTRACT)) +
                    " " +
                    getReturnType(executable) +
                    getName(executable) +
                    "(" + getArgumentsString(executable) + ")" +
                    getExceptionsString(executable);
        }


        /**
         * Geber for {@link Executable} name
         *
         * @param executable instance of {@link Executable}
         * @return {@link String} method name if {@link Executable} instance of {@link Method}, implemented class name {@link Implementor#implementedClassName} otherwise
         */
        private String getName(Executable executable) {
            return executable instanceof Constructor ? implementedClassName.apply(executable.getDeclaringClass()) : executable.getName();
        }

        /**
         * Parsing method default body from {@link Executable}
         *
         * @param executable instance of method as {@link Executable}
         * @return {@link String} of parsed default method of {@link Executable}
         */
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
                if (!returnType.isPrimitive()) {
                    return "return null;";
                } else {
                    if (returnType == void.class) {
                        return "return;";
                    }
                    if (returnType == boolean.class) {
                        return "return false;";
                    }
                    return "return 0;";
                }
            }
            return "";
        }

        /**
         * Parsing method return type from {@link Executable}
         *
         * @param executable instance of method as {@link Executable}
         * @return {@link String} of parsed return type of {@link Executable} if it's instance of {@link Method}, empty {@link String} otherwise
         */
        private String getReturnType(Executable executable) {
            if (executable instanceof Method) {
                return ((Method) executable).getReturnType().getTypeName() + " ";
            }
            isConstructor = true;
            return "";
        }


        /**
         * Parsing method exception throws from {@link Executable}
         *
         * @param executable instance of method as {@link Executable}
         * @return {@link String} of parsed throws of {@link Executable}
         */
        private String getExceptionsString(Executable executable) {
            if (executable.getExceptionTypes().length == 0) {
                return "";
            }
            return Arrays
                    .stream(executable.getExceptionTypes())
                    .map(Class::getCanonicalName)
                    .collect(Collectors.joining(" , ", " throws ", ""));

        }


        /**
         * Parsing method arguments list from {@link Executable}
         *
         * @param executable instance of method as {@link Executable}
         * @return {@link String} of parsed arguments list of {@link Executable}
         */
        private String getArgumentsString(Executable executable) {
            AtomicInteger argNum = new AtomicInteger(0);
            return Arrays
                    .stream(executable.getParameters())
                    .map(parameter -> parameter.getType().getCanonicalName() + " " + DEFAULT_ARGUMENT_NAME + (argNum.getAndIncrement()))
                    .collect(Collectors.joining(", "));
        }


        /**
         * @return <b>true</b> if {@link #getImplementation()} is not <b>null</b>
         */
        boolean isImplemented() {
            return getImplementation() != null;
        }

        /**
         * @return <b>@Override</b> {@link String} for generated  methods
         */
        private String overrideAnnotation() {
            return !isConstructor ? "@Override " : "";
        }


        /**
         * Converting method to {@link String}
         *
         * @return {@link String} of method representation
         */
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

        root = getPathToJavaFile(token, root);
        createDirectory(root);

        String result = getGeneratedClass(token);
        try (BufferedWriter outFile = Files.newBufferedWriter(root)) {
            outFile.write(result);
        } catch (IOException e) {
            throw new ImplerException("Error with writing output file");
        }

    }

    private Path getFilePath(Path path, Class<?> token, String end) {
        return path.resolve(token.getPackage().getName().replace('.', File.separatorChar))
                .resolve(implementedClassName.apply(token) + end);
    }


    /**
     * Produces <b>.jar</b> file implementing class or interface specified by provided <b>token</b>.
     * Generated class full name should be same as full name of the type token with <b>Impl</b> suffix
     * added.
     * During implementation creates temporary folder to store temporary <b>.java</b> and <b>.class</b> files.
     * If program fails to delete temporary folder, it informs user about it.
     *
     * @param token   as {@link Class} to implement
     * @param jarFile as {@link Path} output <b>.jar</b> file path
     * @throws ImplerException if the given class cannot be generated for one of such reasons:
     *                         <ul>
     *                         <li> Some arguments are <b>null</b></li>
     *                         <li> Error occurs during implementation via {@link #implement(Class, Path)} </li>
     *                         <li> {@link JavaCompiler} failed to compile implemented class </li>
     *                         <li> The problems with I/O occurred during implementation. </li>
     *                         </ul>
     */
    @Override
    public void implementJar(Class<?> token, Path jarFile) throws ImplerException {
        Path tmpDir = null;

        try {
            createDirectory(jarFile);

            try {
                tmpDir = Files.createTempDirectory(jarFile.toAbsolutePath().getParent(), "implementorBuild");
            } catch (IOException e) {
                throw new ImplerException("Unable to create temporary directory");
            }

            implement(token, tmpDir);

            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            String[] compilerArgs = new String[]{
                    "-cp",
                    tmpDir.toString() + File.pathSeparator + System.getProperty("java.class.path"),
                    getPathToJavaFile(token, tmpDir).toString()
            };


            if (compiler == null || compiler.run(null, null, null, compilerArgs) != 0) {
                throw new ImplerException("Can't compile implemented class " + tmpDir.toString() + File.pathSeparator + System.getProperty("java.class.path"));
            }
            Manifest manifest = new Manifest();
            Attributes attributes = manifest.getMainAttributes();
            attributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");
            attributes.put(Attributes.Name.IMPLEMENTATION_VENDOR, "Belyaev Roman");
            try (JarOutputStream writer = new JarOutputStream(Files.newOutputStream(jarFile), manifest)) {
                writer.putNextEntry(new ZipEntry(token.getName().replace('.', '/') + "Impl.class"));
                Files.copy(getFilePath(tmpDir, token, ".class"), writer);
            } catch (IOException e) {
                throw new ImplerException("Unable to write to JAR file", e);
            }
        } finally {
            try {
                if (tmpDir != null) {
                    Files.walkFileTree(tmpDir, new Cleaner());
                }
            } catch (IOException e) {
                System.err.println("Unable to remove temp directory");
            }
        }

    }


    /**
     * Getter of implementation of {@link Class}
     *
     * @param token class, which need to be generate
     * @return {@link String} with generated class
     * @throws ImplerException if input class contains only private constructors
     */
    private String getGeneratedClass(Class<?> token) throws ImplerException {
        StringBuilder generated = new StringBuilder();


        addPackage(token, generated);
        addClassDeclaration(token, generated);


        Constructor<?>[] constructors = token.getDeclaredConstructors();
        boolean containsConstructor = constructors.length != 0;
        boolean containsAccessableConstructors = false;
        List<GeneratedMethod> methodList = new ArrayList<>();
        for (Constructor<?> constructor : token.getDeclaredConstructors()) {
            if (!Modifier.isPrivate(constructor.getModifiers())) {
                containsAccessableConstructors = true;
                methodList.add(new GeneratedMethod(constructor));
            }
        }

        assertTrue(containsConstructor != containsAccessableConstructors, e -> e, "Class contains only private constructors. Can't extend");

        methodList.forEach(element -> implementedMethodsMap.put(element.getDeclaration(), element));


        while (token != null) {
            Stream.concat(
                    Arrays.stream(token.getDeclaredMethods()),
                    Arrays.stream(token.getMethods()))
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


    /**
     * Adding class declaration to generated
     *
     * @param cls       {@link Class} token
     * @param generated generated string class of <b>cls</b>
     */
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


    /**
     * Adding package to generated
     *
     * @param cls       {@link Class} token
     * @param generated generated string class of <b>cls</b>
     */
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


    /**
     * Assertion helper method
     *
     * @param value    value to check
     * @param function function for checking value
     * @param message  error message
     * @throws ImplerException with {@param message} if {@param function} check {@param value} and return <code>true</code>
     */
    private static <T> void assertTrue(T value, Predicate<T> function, String message) throws ImplerException {
        if (function.test(value)) {
            throw new ImplerException(message);
        }
    }

    /**
     * Assertion helper method
     *
     * @param value   value to check
     * @param varName name of var
     * @throws ImplerException with message "%NAME% required non null" if {@code value} is <code>null</code>
     */
    private static <T> void assertIfNull(T value, String varName) throws ImplerException {
        assertTrue(value, Objects::isNull, varName + " required non null");
    }


    /**
     * Creating directory from specified {@link Path}
     *
     * @param path directory path
     * @throws ImplerException if unable to create dir from {@code path}
     */
    private void createDirectory(Path path) throws ImplerException {
        if (path.getParent() != null) {
            try {
                Files.createDirectories(path.getParent());
            } catch (IOException | SecurityException e) {
                throw new ImplerException("Unable to create directories for output file", e);
            }
        }
    }


    /**
     * Return path to file, containing implementation of given class, with specific file extension
     * located in directory represented by <b>path</b>
     *
     * @param path path to parent directory of class
     * @param cls  class to get name from
     * @return {@link Path} representing path to certain file
     */
    private Path getPathToJavaFile(Class cls, Path path) {
        return path
                .resolve(cls.getPackageName().replace('.', File.separatorChar))
                .resolve(implementedClassName.apply(cls) + ".java");
    }


    private static class Cleaner extends SimpleFileVisitor<Path> {

        /**
         * Creates new instance if {@link Cleaner}
         */
        Cleaner() {
            super();
        }

        /**
         * Deletes file represented by <b>file</b>
         *
         * @param file  current file in fileTree
         * @param attrs attributes of file
         * @return {@link FileVisitResult#CONTINUE}
         * @throws IOException if error occurred during deleting of file
         */
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            Files.delete(file);
            return FileVisitResult.CONTINUE;
        }

        /**
         * Deletes directory represented by <b>dir</b>
         *
         * @param dir current visited directory in fileTree
         * @param exc <b>null</b> if the iteration of the directory completes without an error;
         *            otherwise the I/O exception that caused the iteration of the directory to complete prematurely
         * @return {@link FileVisitResult#CONTINUE}
         * @throws IOException if error occurred during deleting of directory
         */
        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
            Files.delete(dir);
            return FileVisitResult.CONTINUE;
        }
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

        JarImpler implementor = new Implementor();

        try {
            implementor.implement(Class.forName(args[0]), Path.of(args[1]));
        } catch (ImplerException e) {
            System.err.println("Something went wrong while implemented: " + e.getMessage());
        } catch (ClassNotFoundException e) {
            System.err.println("Incorrect class name: " + args[0]);
        }

    }
}
