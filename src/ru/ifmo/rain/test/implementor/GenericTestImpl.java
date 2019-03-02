package ru.ifmo.rain.test.implementor;

import javax.swing.*;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.System.out;

public class GenericTestImpl extends GenericTest {


    int cooler(int a) {
        return 10;
    }

    interface m {
    }

    interface p {
    }


    static void prtForMethod(Method m) {
        final String fmt = "%24s: %s%n";

        out.format("%s%n", m.toGenericString());

        out.format(fmt, "ReturnType", m.getReturnType());
        out.format(fmt, "GenericReturnType", m.getGenericReturnType());

        Class<?>[] pType = m.getParameterTypes();
        Type[] gpType = m.getGenericParameterTypes();
        for (int i = 0; i < pType.length; i++) {
            out.format(fmt, "ParameterType", pType[i]);
            out.format(fmt, "GenericParameterType", gpType[i]);
        }

        Class<?>[] xType = m.getExceptionTypes();
        Type[] gxType = m.getGenericExceptionTypes();
        for (int i = 0; i < xType.length; i++) {
            out.format(fmt, "ExceptionType", xType[i]);
            out.format(fmt, "GenericExceptionType", gxType[i]);
        }
    }


    public static void main(String[] args) {
        Class<?> c = GenericTest.class;


        Arrays.stream(c.getDeclaredMethods())
                .filter(m -> !m.getName().equals("main") && !m.getName().startsWith("lambda"))
                .forEach(m -> {

                    System.out.println("From oracle:");
                    prtForMethod(m);
                    System.out.println("!!!");
                    System.out.println("Method named: " + m.getName());


                    System.out.println("All generic typeNames: " + getGenericVarsNames(m));

                    System.out.println("Return type: " + m.getReturnType());
                    getReturnType(m);

                    System.out.println("Parametrized returnType " + m.getGenericReturnType());
                    System.out.println("Magic: " + m.toGenericString());

                    System.out.println("----------");
                    System.out.println("My own magic: " + methodToStr(m));
                    System.out.println("####\n");
                });

    }



    static String methodToStr(Method m){
        return modifiers(m) + " " + getReturnType(m);
    }

    private static String modifiers(Method m) {
        return Modifier.toString(m.getModifiers());
    }

    static String getReturnType(Method m){
        return m.getGenericReturnType().getTypeName();
    }

    static String getGenericVarsNames(Method m) {
        List<String> genericsNames = new ArrayList<>();

        Arrays.stream(m.getTypeParameters())
                .forEach(tv -> {
                    Type type = tv.getBounds()[0];
                    if (!Object.class.getCanonicalName().equals(type.getTypeName())) {
                        genericsNames.add(tv.getName() + " extends " + type.getTypeName()); // как узнать ебучий супер/наследник
                    } else {
                        genericsNames.add(tv.getName());
                    }
                });

        return genericsNames.stream().collect(Collectors.joining(", ", "<", ">"));
    }


    @Override
    public <Y, X extends UIDefaults> List<? extends Number> calculate(List<? extends Y> list, int a, String n) {
        return null;
    }

    // production code should handle these exceptions more gracefully

    // }
}
