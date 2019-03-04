package ru.ifmo.rain.test.implementor;

import javax.swing.*;
import java.util.List;

@Deprecated
public abstract class GenericTest {

    public abstract <Y, X extends UIDefaults> List<? extends Number> calculate(List<? extends Y> list, int a, String n);

    public String f(){return null;}
}
