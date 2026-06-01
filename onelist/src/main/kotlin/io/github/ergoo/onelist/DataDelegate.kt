package io.github.ergoo.onelist

import androidx.annotation.IntRange

/**
 * Abstraction for managing a mutable data list backing a RecyclerView adapter.
 *
 * Implementations are responsible for both mutating the underlying data and
 * dispatching the appropriate `notifyItem*` calls to the adapter.
 *
 * @param T the type of data items.
 * @see DefDataDelegate
 */
interface DataDelegate<T> {

    /** Returns an immutable snapshot of the current data list. */
    fun getData(): List<T>

    /** Returns the number of items in the data list. */
    fun dataSize(): Int

    /** Returns the item at the given [position]. */
    fun getItem(position: Int): T

    /**
     * Replaces the entire data list with [newList].
     * Passing `null` clears the list.
     */
    fun setData(newList: List<T>?)

    /** Replaces the item at [position] with [item]. */
    operator fun set(@IntRange(from = 0) position: Int, item: T)

    /** Inserts [item] at the given [position]. */
    fun add(@IntRange(from = 0) position: Int, item: T)

    /** Appends [item] to the end of the list. */
    fun add(item: T)

    /** Inserts all elements of [newCollection] starting at [position]. */
    fun addAll(@IntRange(from = 0) position: Int, newCollection: Collection<T>)

    /** Appends all elements of [newCollection] to the end of the list. */
    fun addAll(newCollection: Collection<T>)

    /** Removes the item at [position]. */
    fun removeAt(@IntRange(from = 0) position: Int)

    /** Removes the first occurrence of [item] from the list, if present. */
    fun remove(item: T)

    /** Swaps the items at [fromPosition] and [toPosition]. */
    fun swap(fromPosition: Int, toPosition: Int)

}

