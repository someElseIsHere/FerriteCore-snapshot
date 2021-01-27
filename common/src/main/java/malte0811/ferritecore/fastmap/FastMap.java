package malte0811.ferritecore.fastmap;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import net.minecraft.world.level.block.state.properties.Property;

import javax.annotation.Nullable;
import java.util.*;

public class FastMap<Value> {
    private final List<FastMapKey<?>> keys;
    private final List<Property<?>> rawKeys;
    private final List<Value> values;
    // It might be possible to get rid of this (and the equivalent map for values) by sorting the key vectors by
    // property name (natural order for values) and using a binary search above a given size, but choosing that size
    // would likely be more effort than it's worth
    private final Map<Property<?>, Integer> toKeyIndex;

    public FastMap(Collection<Property<?>> properties, Map<Map<Property<?>, Comparable<?>>, Value> valuesMap) {
        this.rawKeys = ImmutableList.copyOf(properties);
        List<FastMapKey<?>> keys = new ArrayList<>(rawKeys.size());
        int factorUpTo = 1;
        ImmutableMap.Builder<Property<?>, Integer> toKeyIndex = ImmutableMap.builder();
        for (Property<?> prop : rawKeys) {
            toKeyIndex.put(prop, keys.size());
            keys.add(new FastMapKey<>(prop, factorUpTo));
            factorUpTo *= prop.getPossibleValues().size();
        }
        this.keys = ImmutableList.copyOf(keys);
        this.toKeyIndex = toKeyIndex.build();

        List<Value> valuesList = new ArrayList<>(factorUpTo);
        for (int i = 0; i < factorUpTo; ++i) {
            valuesList.add(null);
        }
        for (Map.Entry<Map<Property<?>, Comparable<?>>, Value> state : valuesMap.entrySet()) {
            valuesList.set(getIndexOf(state.getKey()), state.getValue());
        }
        this.values = ImmutableList.copyOf(valuesList);
    }

    @Nullable
    public <T extends Comparable<T>>
    Value with(int last, Property<T> prop, T value) {
        final FastMapKey<T> keyToChange = getKeyFor(prop);
        if (keyToChange == null) {
            return null;
        }
        int newIndex = keyToChange.replaceIn(last, value);
        if (newIndex < 0) {
            return null;
        }
        return values.get(newIndex);
    }

    public int getIndexOf(Map<Property<?>, Comparable<?>> state) {
        int id = 0;
        for (FastMapKey<?> k : keys) {
            id += k.toPartialMapIndex(state.get(k.getProperty()));
        }
        return id;
    }

    @Nullable
    public <T extends Comparable<T>>
    T getValue(int stateIndex, Property<T> property) {
        final FastMapKey<T> propId = getKeyFor(property);
        if (propId == null) {
            return null;
        }
        return propId.getValue(stateIndex);
    }

    public List<Property<?>> getProperties() {
        return rawKeys;
    }

    public ImmutableMap<Property<?>, Comparable<?>> makeValuesFor(int index) {
        ImmutableMap.Builder<Property<?>, Comparable<?>> result = ImmutableMap.builder();
        for (Property<?> p : getProperties()) {
            result.put(p, Objects.requireNonNull(getValue(index, p)));
        }
        return result.build();
    }

    public <T extends Comparable<T>>
    Value withUnsafe(int globalTableIndex, Property<T> rowKey, Object columnKey) {
        return with(globalTableIndex, rowKey, (T) columnKey);
    }

    public int numProperties() {
        return keys.size();
    }

    FastMapKey<?> getKey(int keyIndex) {
        return keys.get(keyIndex);
    }

    @Nullable
    private <T extends Comparable<T>>
    FastMapKey<T> getKeyFor(Property<T> prop) {
        Integer index = toKeyIndex.get(prop);
        if (index == null) {
            return null;
        } else {
            return (FastMapKey<T>) getKey(index);
        }
    }
}