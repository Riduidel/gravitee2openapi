package org.ndx.gravitee.openapi.transform;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A map providing some utility method to easily navigate "deep structures
 * @author nicolas-delsaux
 *
 * @param <String>
 * @param <V>
 */
public class DotAwareMap<V> extends LinkedHashMap<String, V> implements Map<String, V> {
	public static class Builder<V> {
		public class KeyValueBuilder {

			private String key;

			public KeyValueBuilder(String string) {
				this.key = string;
			}

			public Builder<V> to(V value) {
				created.put(key, value);
				return Builder.this;
			}
			
		}

		public static <V> Builder<V> map() {
			return new Builder<V>();
		}
		
		private DotAwareMap<V> created = new DotAwareMap<V>();

		public KeyValueBuilder from(String string) {
			return new KeyValueBuilder(string);
		}

		public DotAwareMap get() {
			return created;
		}
		
	}

	public DotAwareMap() {
		super();
	}

	public DotAwareMap(Map<? extends String, ? extends V> m) {
		super(m);
	}

	/**
	 * Navigate into the path looking for a key. "." is used as separator.
	 * @param path
	 * @return an optional which may contain value we found at key path
	 */
	public Optional getFromPath(String path) {
		String[] pathFragments = path.split("\\.");
		Map current = this;
		Object value = null;
		for (String pathElement : pathFragments) {
			if(current.containsKey(pathElement)) {
				value = current.get(pathElement);
				if (value instanceof Map) {
					current = (Map) value;
				} else {
					current = new HashMap();
				}
			} else {
				return Optional.empty();
			}
		}
		return Optional.of(value);
	}

	/**
	 * @param string
	 * @param value
	 * @return
	 */
	public void setFromPath(String path, V value) {
		List<String> pathFragments = Arrays.asList(path.split("\\."));
		DotAwareMap child = in(String.join(".", pathFragments.subList(0, pathFragments.size()-1)));
		child.put(pathFragments.get(pathFragments.size()-1), value);
	}

	/**
	 * I should have used functionnal pattern to merge get and set, but I'm too in a hurry for that
	 * @param path
	 * @return
	 */
	public DotAwareMap in(String path) {
		List<String> pathFragments = Arrays.asList(path.split("\\."));
		DotAwareMap current = this;
		Iterator<String> iterator = pathFragments.iterator();
		while (iterator.hasNext()) {
			String pathElement = (String) iterator.next();
			if(!current.containsKey(pathElement)) {
				current.put(pathElement, new DotAwareMap());
			}
			current = (DotAwareMap<V>) current.get(pathElement);
		}
		return current;
	}
}
