package predicate;

import core.AccessPath;

/**
 *
 * @author amogh
 * This predicate is of form KeyNotExist(map,key)
 * This checks if 'map' contains mapping of 'key'
 * This will be invalidated if we encounter instruction 'map.put(key,...)'
 * or if predicate is of form KeyNotExist(map,map.key.elem)
 * This will be validated when we encounter map = new ...;
 */

public class KeyNotExistPredicate extends Predicate {
    private static int hashConst = "KeyNotExistPredicate".hashCode();

    private AccessPath map;
    private AccessPath key;

    public KeyNotExistPredicate(AccessPath m, AccessPath k) {
        map = m;
        key = k;
    }

    @Override
    public boolean isRootPredicate() {
        return false;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof KeyNotExistPredicate) {
            KeyNotExistPredicate other = (KeyNotExistPredicate) obj;
            if (map.equals(other.map) && key.equals(other.key))
                return true;
        }
        return false;
    }

    @Override
    public boolean containsAP(AccessPath ap) {
        return map.contains(ap) || key.contains(ap);
    }

    @Override
    public boolean isNegated() {
        return false;
    }

    @Override
    public AccessPath[] getAllAccessPaths() {
        return new AccessPath[] {map, key};
    }

    @Override
    public Predicate cloneImpl() {
        return new KeyNotExistPredicate(map.clone(), key.clone());
    }

    @Override
    public void replace(AccessPath oldAP, AccessPath newAP) {
        map.replace(oldAP, newAP);
        key.replace(oldAP, newAP);
    }

    @Override
    public void replaceAt(int index, AccessPath oldAP, AccessPath newAP) {
        if (index == 0)
            map.replace(oldAP, newAP);
        else if (index == 1)
            key.replace(oldAP, newAP);
        else
            throw new IllegalArgumentException("Index out of range");
    }

    @Override
    public int generateHashCode() {
        return map.hashCode() ^ key.hashCode() ^ hashConst;
    }

    public AccessPath getKey() {
        return key;
    }

    public AccessPath getMap() {
        return map;
    }

    public String toString() {
        String str = "";
        str += map.toString() + ".keyNotExist(" + key.toString() + ")";
        return str;
    }
}
