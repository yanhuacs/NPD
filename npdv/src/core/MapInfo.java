package core;

import soot.SootClass;
import soot.SootMethod;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class MapInfo {
    public static final Set<String> interfaceSet = new HashSet<String>(
        Arrays.asList("put", "putAll", "keySet", "values", "elements", "clear", "get",
        "size", "remove", "containsKey", "equals", "containsValue", "isEmpty", "entrySet",
        "hashCode", "equals", "<init>")
    );

    private static final String[] put = { "put" };
    private static final String[] getKeySet = { "keySet" };
    private static final String[] getValueSet = { "values" };
    private static final String[] getEnumeratedElemSet = { "elements" };
    private static final String[] clear = { "clear" };
    private static final String[] getValue = { "get" };
    private static final String[] idFunc = { "size","remove","containsKey","containsValue","equals","hashcode","isEmpty"};

    public static boolean isMapInterface(SootMethod method) {
        String methodName = method.getName();
        SootClass declaringClass = method.getDeclaringClass();
        if (declaringClass.isJavaLibraryClass()) {
            if (NPVerifier.mapClass.contains(declaringClass)) {
                if (isPut(methodName) ||
                    isGetKeySet(methodName) ||
                    isGetValueSet(methodName) ||
                    isClear(methodName) ||
                    isGetValue(methodName) ||
                    isId(methodName) ||
                    isEnum(methodName) ||
                    methodName.equals("<init>"))
                    return true;
            } else {
                Set<String> methodNames = new HashSet<String>();
                for (SootMethod m : declaringClass.getMethods()) {
                    methodNames.add(m.getName());
                }
                if (interfaceSet.containsAll(methodNames)) {
                    NPVerifier.mapClass.add(declaringClass);
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isPut(String name) {
        for (int i = 0; i < put.length; ++i) {
            if(put[i].equals(name))
                return true;
        }
        return false;
    }

    public static boolean isGetValue(String name) {
        for (int i = 0; i < getValue.length; ++i) {
            if(getValue[i].equals(name))
                return true;
        }
        return false;
    }

    public static boolean isGetKeySet(String name) {
        for (int i = 0; i < getKeySet.length; ++i) {
            if (getKeySet[i].equals(name))
                return true;
        }
        return false;
    }

    public static boolean isGetValueSet(String name) {
        for (int i = 0; i < getValueSet.length; ++i) {
            if (getValueSet[i].equals(name)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isClear(String name) {
        for (int i = 0; i < clear.length; ++i) {
            if (clear[i].equals(name)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isId(String name) {
        for (int i = 0; i < idFunc.length; ++i) {
            if (idFunc[i].equals(name)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isEnum(String name) {
        for (int i = 0; i < getEnumeratedElemSet.length; ++i) {
            if (getEnumeratedElemSet[i].equals(name)) {
                return true;
            }
        }
        return false;
    }
}
