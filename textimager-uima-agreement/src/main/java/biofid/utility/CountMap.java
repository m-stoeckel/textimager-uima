package biofid.utility;

import java.util.Collection;
import java.util.HashMap;

public class CountMap<T> extends HashMap<T, Integer> {
	
	/**
	 * Add the key to the CountMap. This will not affect keys already added.
	 *
	 * @param key
	 */
	public void add(T key) {
		this.put(key, this.getOrDefault(key, 0));
	}
	
	/**
	 * Increase the count for this key. If the key does not exist, it will be added.
	 *
	 * @param key
	 */
	public void inc(T key) {
		this.put(key, this.getOrDefault(key, 0) + 1);
	}
	
	/**
	 * Increase the count for each of these keys. If any key does not exist, it will be added.
	 *
	 * @param keys
	 */
	public void incAll(Collection<T> keys) {
		for (T key : keys) {
			this.put(key, this.getOrDefault(key, 0) + 1);
		}
	}
	
	/**
	 * Get the count for this key, or 0 if it does not exist.
	 *
	 * @param key
	 * @return
	 */
	public Integer get(Object key) {
		return this.getOrDefault(key, 0);
	}
	
}
