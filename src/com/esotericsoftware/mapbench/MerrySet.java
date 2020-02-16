
package com.esotericsoftware.mapbench;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Collections;
import com.badlogic.gdx.utils.GdxRuntimeException;

/** An unordered set where the keys are objects. This implementation uses linear probing with the backward-shift algorithm for
 * removal, and finds space for keys using Fibonacci hashing instead of the more-common power-of-two mask. Null keys are not
 * allowed. No allocation is done except when growing the table size. <br>
 * This set uses Fibonacci hashing to help distribute what may be very bad hashCode() results across the whole capacity. See
 * <a href=
 * "https://probablydance.com/2018/06/16/fibonacci-hashing-the-optimization-that-the-world-forgot-or-a-better-alternative-to-integer-modulo/">Malte
 * Skarupke's blog post</a> for more information on Fibonacci hashing. It uses linear probing to resolve collisions, which is far
 * from the academically optimal algorithm, but performs considerably better in practice than most alternatives, and combined with
 * Fibonacci hashing, it can handle "normal" generated hashCode() implementations, and not just theoretically optimal hashing
 * functions. Even if all hashCode()s this is given collide, it will still work, just slowly; the older libGDX implementation
 * using cuckoo hashing would crash with an OutOfMemoryError with under 50 collisions. <br>
 * This map performs very fast contains and remove (typically O(1), worst case O(n) due to occasional probing, but still very
 * fast). Add may be a bit slower, depending on hash collisions, but this data structure is somewhat collision-resistant. Load
 * factors greater than 0.91 greatly increase the chances the set will have to rehash to the next higher POT size. Memory usage is
 * excellent, and the aforementioned collision-resistance helps avoid too much capacity resizing. <br>
 * Iteration should be fast with OrderedSet and OrderedMap, whereas ObjectSet and ObjectMap aren't designed to provide especially
 * quick iteration.
 *
 * @author Tommy Ettinger
 * @author Nathan Sweet */
public class MerrySet<T> implements Iterable<T> {
	public int size;

	T[] keyTable;

	float loadFactor;
	int threshold;

	/** Used by {@link #place(Object)} to bit shift the upper bits of a {@code long} into a usable range (&gt;= 0 and &lt;=
	 * {@link #mask}). The shift can be negative, which is convenient to match the number of bits in mask: if mask is a 7-bit
	 * number, a shift of -7 shifts the upper 7 bits into the lowest 7 positions. This class sets the shift &gt; 32 and &lt; 64,
	 * which if used with an int will still move the upper bits of an int to the lower bits due to Java's implicit modulus on
	 * shifts.
	 * <p>
	 * {@link #mask} can also be used to mask the low bits of a number, which may be faster for some hashcodes, if
	 * {@link #place(Object)} is overridden. */
	protected int shift;

	/** A bitmask used to confine hashcodes to the size of the table. Must be all 1 bits in its low positions, ie a power of two
	 * minus 1. If {@link #place(Object)} is overriden, this can be used instead of {@link #shift} to isolate usable bits of a
	 * hash. */
	protected int mask;

	private ObjectSetIterator iterator1, iterator2;

	/** Creates a new set with an initial capacity of 51 and a load factor of 0.8. */
	public MerrySet () {
		this(51, 0.8f);
	}

	/** Creates a new set with a load factor of 0.8.
	 * @param initialCapacity If not a power of two, it is increased to the next nearest power of two. */
	public MerrySet (int initialCapacity) {
		this(initialCapacity, 0.8f);
	}

	/** Creates a new set with the specified initial capacity and load factor. This set will hold initialCapacity items before
	 * growing the backing table.
	 * @param initialCapacity If not a power of two, it is increased to the next nearest power of two. */
	public MerrySet (int initialCapacity, float loadFactor) {
		if (initialCapacity < 0) throw new IllegalArgumentException("initialCapacity must be >= 0: " + initialCapacity);
		if (loadFactor <= 0f || loadFactor >= 1f)
			throw new IllegalArgumentException("loadFactor must be > 0 and < 1: " + loadFactor);
		initialCapacity = MathUtils.nextPowerOfTwo((int)Math.ceil(Math.max(1, initialCapacity) / loadFactor));
		if (initialCapacity > 1 << 30) throw new IllegalArgumentException("initialCapacity is too large: " + initialCapacity);

		this.loadFactor = loadFactor;

		threshold = (int)(initialCapacity * loadFactor);
		mask = initialCapacity - 1;
		shift = Long.numberOfLeadingZeros(mask);
		keyTable = (T[])(new Object[initialCapacity]);
	}

	/** Creates a new set identical to the specified set. */
	public MerrySet (MerrySet<? extends T> set) {
		this((int)Math.ceil(set.keyTable.length * set.loadFactor), set.loadFactor);
		System.arraycopy(set.keyTable, 0, keyTable, 0, set.keyTable.length);
		size = set.size;
	}

	/** Returns an index >= 0 and <= {@link #mask} for the specified {@code item}.
	 * <p>
	 * The default implementation uses Fibonacci hashing on the item's {@link Object#hashCode()}: the hashcode is multiplied by a
	 * long constant (2 to the 64th, divided by the golden ratio) then the uppermost bits are shifted into the lowest positions to
	 * obtain an index in the desired range. Multiplication by a long may be slower than int (eg on GWT) but greatly improves
	 * rehashing, allowing even very poor hashcodes, such as those that only differ in their upper bits, to be used without high
	 * collision rates. Fibonacci hashing has increased collision rates when all or most hashcodes are multiples of larger
	 * Fibonacci numbers (see <a href=
	 * "https://probablydance.com/2018/06/16/fibonacci-hashing-the-optimization-that-the-world-forgot-or-a-better-alternative-to-integer-modulo/">Malte
	 * Skarupke's blog post</a>).
	 * <p>
	 * This method can be overriden to customizing hashing. This may be useful eg in the unlikely event that most hashcodes are
	 * Fibonacci numbers, if keys provide poor or incorrect hashcodes, or to simplify hashing if keys provide high quality
	 * hashcodes and don't need Fibonacci hashing: {@code return item.hashCode() & mask;} */
	protected int place (T item) {
		return (int)(item.hashCode() * 0x9E3779B97F4A7C15L >>> shift);
	}

	/** Returns the index of the key if already present, else -(index + 1) for the next empty index. This can be overridden in this
	 * pacakge to compare for equality differently than {@link Object#equals(Object)}. */
	int locateKey (T key) {
		if (key == null) throw new IllegalArgumentException("key cannot be null.");
		T[] keyTable = this.keyTable;
		for (int i = place(key);; i = i + 1 & mask) {
			T other = keyTable[i];
			if (other == null) return -(i + 1); // Empty space is available.
			if (other.equals(key)) return i; // Same key was found.
		}
	}

	/** Returns true if the key was not already in the set. If this set already contains the key, the call leaves the set unchanged
	 * and returns false. */
	public boolean add (T key) {
		int i = locateKey(key);
		if (i >= 0) return false; // Existing key was found.
		i = -(i + 1); // Empty space was found.
		keyTable[i] = key;
		if (++size >= threshold) resize(keyTable.length << 1);
		return true;
	}

	public void addAll (Array<? extends T> array) {
		addAll(array.items, 0, array.size);
	}

	public void addAll (Array<? extends T> array, int offset, int length) {
		if (offset + length > array.size)
			throw new IllegalArgumentException("offset + length must be <= size: " + offset + " + " + length + " <= " + array.size);
		addAll(array.items, offset, length);
	}

	public boolean addAll (T... array) {
		return addAll(array, 0, array.length);
	}

	public boolean addAll (T[] array, int offset, int length) {
		ensureCapacity(length);
		int oldSize = size;
		for (int i = offset, n = i + length; i < n; i++)
			add(array[i]);
		return oldSize != size;
	}

	public void addAll (MerrySet<T> set) {
		ensureCapacity(set.size);
		T[] keyTable = set.keyTable;
		for (int i = 0, n = keyTable.length; i < n; i++) {
			T key = keyTable[i];
			if (key != null) add(key);
		}
	}

	/** Skips checks for existing keys, doesn't increment size. */
	private void addResize (T key) {
		T[] keyTable = this.keyTable;
		for (int i = place(key);; i = (i + 1) & mask) {
			if (keyTable[i] == null) {
				keyTable[i] = key;
				return;
			}
		}
	}

	/** Returns true if the key was removed. */
	public boolean remove (T key) {
		int i = locateKey(key);
		if (i < 0) return false;
		T[] keyTable = this.keyTable;
		int next = i + 1 & mask;
		while ((key = keyTable[next]) != null && next != place(key)) {
			keyTable[i] = key;
			i = next;
			next = next + 1 & mask;
		}
		keyTable[i] = null;
		size--;
		return true;
	}

	/** Returns true if the set has one or more items. */
	public boolean notEmpty () {
		return size > 0;
	}

	/** Returns true if the set is empty. */
	public boolean isEmpty () {
		return size == 0;
	}

	/** Reduces the size of the backing arrays to be the specified capacity or less. If the capacity is already less, nothing is
	 * done. If the set contains more items than the specified capacity, the next highest power of two capacity is used instead. */
	public void shrink (int maximumCapacity) {
		if (maximumCapacity < 0) throw new IllegalArgumentException("maximumCapacity must be >= 0: " + maximumCapacity);
		if (size > maximumCapacity) maximumCapacity = size;
		if (keyTable.length <= maximumCapacity) return;
		maximumCapacity = MathUtils.nextPowerOfTwo(maximumCapacity);
		resize(maximumCapacity);
	}

	/** Clears the set and reduces the size of the backing arrays to be the specified capacity, if they are larger. The reduction
	 * is done by allocating new arrays, though for large arrays this can be faster than clearing the existing array. */
	public void clear (int maximumCapacity) {
		if (keyTable.length <= maximumCapacity) {
			clear();
			return;
		}
		size = 0;
		resize(maximumCapacity);
	}

	/** Clears the set, leaving the backing arrays at the current capacity. When the capacity is high and the population is low,
	 * iteration can be unnecessarily slow. {@link #clear(int)} can be used to reduce the capacity. */
	public void clear () {
		if (size == 0) return;
		size = 0;
		Arrays.fill(keyTable, null);
	}

	public boolean contains (T key) {
		return locateKey(key) >= 0;
	}

	public T get (T key) {
		int i = locateKey(key);
		return i < 0 ? null : keyTable[i];
	}

	public T first () {
		T[] keyTable = this.keyTable;
		for (int i = 0, n = keyTable.length; i < n; i++)
			if (keyTable[i] != null) return keyTable[i];
		throw new IllegalStateException("ObjectSet is empty.");
	}

	/** Increases the size of the backing array to accommodate the specified number of additional items. Useful before adding many
	 * items to avoid multiple backing array resizes. */
	public void ensureCapacity (int additionalCapacity) {
		if (additionalCapacity < 0) throw new IllegalArgumentException("additionalCapacity must be >= 0: " + additionalCapacity);
		int sizeNeeded = size + additionalCapacity;
		if (sizeNeeded >= threshold) resize(MathUtils.nextPowerOfTwo((int)Math.ceil(sizeNeeded / loadFactor)));
	}

	private void resize (int newSize) {
		int oldCapacity = keyTable.length;
		threshold = (int)(newSize * loadFactor);
		mask = newSize - 1;
		shift = Long.numberOfLeadingZeros(mask);
		T[] oldKeyTable = keyTable;

		keyTable = (T[])(new Object[newSize]);

		if (size > 0) {
			for (int i = 0; i < oldCapacity; i++) {
				T key = oldKeyTable[i];
				if (key != null) addResize(key);
			}
		}
	}

	public int hashCode () {
		int h = size;
		T[] keyTable = this.keyTable;
		for (int i = 0, n = keyTable.length; i < n; i++)
			if (keyTable[i] != null) h += keyTable[i].hashCode();
		return h;
	}

	public boolean equals (Object obj) {
		if (!(obj instanceof MerrySet)) return false;
		MerrySet other = (MerrySet)obj;
		if (other.size != size) return false;
		T[] keyTable = this.keyTable;
		for (int i = 0, n = keyTable.length; i < n; i++)
			if (keyTable[i] != null && !other.contains(keyTable[i])) return false;
		return true;
	}

	public String toString () {
		return '{' + toString(", ") + '}';
	}

	public String toString (String separator) {
		if (size == 0) return "";
		java.lang.StringBuilder buffer = new java.lang.StringBuilder(32);
		T[] keyTable = this.keyTable;
		int i = keyTable.length;
		while (i-- > 0) {
			T key = keyTable[i];
			if (key == null) continue;
			buffer.append(key == this ? "(this)" : key);
			break;
		}
		while (i-- > 0) {
			T key = keyTable[i];
			if (key == null) continue;
			buffer.append(separator);
			buffer.append(key == this ? "(this)" : key);
		}
		return buffer.toString();
	}

	/** Returns an iterator for the keys in the set. Remove is supported.
	 * <p>
	 * If {@link Collections#allocateIterators} is false, the same iterator instance is returned each time this method is called.
	 * Use the {@link ObjectSetIterator} constructor for nested or multithreaded iteration. */
	public ObjectSetIterator<T> iterator () {
		if (Collections.allocateIterators) return new ObjectSetIterator(this);
		if (iterator1 == null) {
			iterator1 = new ObjectSetIterator(this);
			iterator2 = new ObjectSetIterator(this);
		}
		if (!iterator1.valid) {
			iterator1.reset();
			iterator1.valid = true;
			iterator2.valid = false;
			return iterator1;
		}
		iterator2.reset();
		iterator2.valid = true;
		iterator1.valid = false;
		return iterator2;
	}

	static public <T> MerrySet<T> with (T... array) {
		MerrySet<T> set = new MerrySet<T>();
		set.addAll(array);
		return set;
	}

	static public class ObjectSetIterator<K> implements Iterable<K>, Iterator<K> {
		public boolean hasNext;

		final MerrySet<K> set;
		int nextIndex, currentIndex;
		boolean valid = true;

		public ObjectSetIterator (MerrySet<K> set) {
			this.set = set;
			reset();
		}

		public void reset () {
			currentIndex = -1;
			nextIndex = -1;
			findNextIndex();
		}

		private void findNextIndex () {
			hasNext = false;
			K[] keyTable = set.keyTable;
			for (int n = set.keyTable.length; ++nextIndex < n;) {
				if (keyTable[nextIndex] != null) {
					hasNext = true;
					break;
				}
			}
		}

		public void remove () {
			if (currentIndex < 0) throw new IllegalStateException("next must be called before remove.");

			K[] keyTable = set.keyTable;
			int mask = set.mask;
			int loc = currentIndex, nl = (loc + 1 & mask);
			K key;
			while ((key = keyTable[nl]) != null && nl != set.place(key)) {
				keyTable[loc] = key;
				loc = nl;
				nl = loc + 1 & mask;
			}
			if (loc != currentIndex) --nextIndex;
			keyTable[loc] = null;
			currentIndex = -1;
			set.size--;
		}

		public boolean hasNext () {
			if (!valid) throw new GdxRuntimeException("#iterator() cannot be used nested.");
			return hasNext;
		}

		public K next () {
			if (!hasNext) throw new NoSuchElementException();
			if (!valid) throw new GdxRuntimeException("#iterator() cannot be used nested.");
			K key = set.keyTable[nextIndex];
			currentIndex = nextIndex;
			findNextIndex();
			return key;
		}

		public ObjectSetIterator<K> iterator () {
			return this;
		}

		/** Adds the remaining values to the array. */
		public Array<K> toArray (Array<K> array) {
			while (hasNext)
				array.add(next());
			return array;
		}

		/** Returns a new array containing the remaining values. */
		public Array<K> toArray () {
			return toArray(new Array<K>(true, set.size));
		}
	}
}
