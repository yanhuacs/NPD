package core;

import java.util.*;

public class CollectionFactory {

    private final static boolean DEBUG = false;
    public static <T> Set<T> newSet() {
        if (DEBUG)
            return new HashSet<T>();
        else
            return new LinkedHashSet<T>();
    }

    public static <K, V> Map<K, V> newMap() {
        if (DEBUG)
            return new HashMap<K, V>();
        else
            return new LinkedHashMap<K, V>();
    }

    public static <T> Stack<T> newStack() {
        return new Stack<T>();
    }

    public static <T> List<T> newList() {
        return new LinkedList<T>();
    }
}
