package core;

import soot.SootClass;
import soot.SootMethod;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class CollectionInfo {
    private static final String[] readCollection = { "get", "elementAt", "firstElement","lastElement","pop","peek" };
    private static final String[] addCollection = { "add","addElement","insertElementAt", "push", "addLast" };
    private static final String[] getIterator = { "iterator","elements", "listIterator" };
    private static final String[] skipCollection = { "remove" };
    private static final String[] readIterator = { "next", "nextElement" };
    private static final String[] addIterator = { "add" };
    private static final String[] skipIterator = { "remove" };
    private static final String[] addAllCollection = { "addAll" };
    private static final String[] collectiontoArray = { "toArray","copyInto","subList"};

    public static boolean isReadIterator(String name) {
        for (int i = 0; i < readIterator.length; ++i) {
            if (readIterator[i].equals(name))
                return true;
        }
        return false;
    }

    public static boolean isGetIterator(String name) {
        for (int i = 0; i < getIterator.length; ++i) {
            if (getIterator[i].equals(name))
                return true;
        }
        return false;
    }

    public static boolean isReadCollection(String name) {
        for (int i = 0; i < readCollection.length; ++i) {
            if (readCollection[i].equals(name))
                return true;
        }
        return false;
    }

    public static boolean isAddCollection(String name) {
        for (int i = 0; i < addCollection.length; ++i) {
            if (addCollection[i].equals(name))
                return true;
        }
        return false;
    }

    public static boolean isAddIterator(String name) {
        for (int i = 0; i < addIterator.length; ++i) {
            if (addIterator[i].equals(name))
                return true;
        }
        return false;
    }

    public static boolean isAddAllCollection(String name) {
        for (int i = 0; i < addAllCollection.length; ++i) {
            if (addAllCollection[i].equals(name))
                return true;
        }
        return false;
    }

    public static boolean isCollectionToArray(String name) {
        for (int i = 0; i < collectiontoArray.length; ++i) {
            if (collectiontoArray[i].equals(name))
                return true;
        }
        return false;
    }

    public static boolean isSkipIterator(String name) {
        for (int i = 0; i < skipIterator.length; ++i) {
            if (skipIterator[i].equals(name))
                return true;
        }
        return false;
    }

    public static boolean isSkipCollection(String name) {
        for (int i = 0; i < skipCollection.length; ++i) {
            if (skipCollection[i].equals(name))
                return true;
        }
        return false;
    }

    public static boolean isIterator(SootMethod method) {

        String name = method.getName();
        SootClass declaringClass = method.getDeclaringClass();
        if (declaringClass.isJavaLibraryClass()) {
            if (NPVerifier.collectionClass.contains(declaringClass) ||
                NPVerifier.iteratorClass.contains(declaringClass)) {
                if (isReadIterator(name) ||
                    isAddIterator(name) ||
                    isSkipIterator(name))
                    return true;
            }
        }
        return false;
    }

    public static boolean isCollectionInterface(SootMethod method) {
        String name = method.getName();
        SootClass klass = method.getDeclaringClass();
        if (klass.isJavaLibraryClass()) {
            if (NPVerifier.collectionClass.contains(klass)) {
                if (isReadCollection(name) ||
                    isAddCollection(name) ||
                    isGetIterator(name) ||
                    isSkipCollection(name))
                    return true;
            }
        }
        return false;
    }

    public static boolean isCollectionSubClass(SootClass klass) {
        if (klass.isJavaLibraryClass()) {
            if (NPVerifier.collectionClass.contains(klass) ||
                NPVerifier.iteratorClass.contains(klass) ||
                NPVerifier.enumClass.contains(klass))
                return true;
            else {
                Set<String> methodNames = new HashSet<String>();
                for (SootMethod m : klass.getMethods()) {
                    methodNames.add(m.getName());
                }

                if (collectionInterfaces.containsAll(methodNames)) {
                    NPVerifier.collectionClass.add(klass);
                    return true;
                }
            }
        }
        return false;
    }

    public static final Set<String> collectionInterfaces = new HashSet<String>(
        Arrays.asList(
            "iterator",
            "contains",
            "remove",
            "size",
            "clear",
            "hasNext",
            "next",
            "remove",
            "hasMoreElements",
            "nextElement",
            "hashCode",
            "equals",
            "<init>"
        )
    );
}
