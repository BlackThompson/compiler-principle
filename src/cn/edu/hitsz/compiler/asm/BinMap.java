package cn.edu.hitsz.compiler.asm;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Black
 *
 * @param <K>
 * @param <V>
 */

public class BinMap<K, V> {
    private final Map<K, V> kvMap = new HashMap<>();
    private final Map<V, K> vkMap = new HashMap<>();

    public void put(K key, V value) {
        kvMap.put(key, value);
        vkMap.put(value, key);
    }

    public void removeByKey(K key) {

        vkMap.remove(kvMap.remove(key));
    }

    public void removeByValue(V value) {

        kvMap.remove(vkMap.remove(value));
    }

    public boolean containsKey(K key) {

        return kvMap.containsKey(key);
    }

    public boolean containsValue(V value) {

        return vkMap.containsKey(value);
    }

    public void replace(K key, V value) {
        removeByKey(key);
        removeByValue(value);
        kvMap.put(key, value);
        vkMap.put(value, key);
    }

    public V getByKey(K key) {

        return kvMap.get(key);
    }

    public K getByValue(V value) {

        return vkMap.get(value);
    }
}
