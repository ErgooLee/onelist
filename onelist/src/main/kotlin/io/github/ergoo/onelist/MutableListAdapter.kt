package io.github.ergoo.onelist

import androidx.recyclerview.widget.RecyclerView

/**
 * A [OneListAdapter] that manages a mutable data list via [DataDelegate].
 *
 * Data operations (add, remove, swap, setData, etc.) are delegated to the [dataDelegate],
 * which defaults to [DefDataDelegate]. The delegate can be replaced via the [dataDelegate] property.
 *
 * @param T  the type of data items.
 * @param VH the type of ViewHolder.
 */
abstract class MutableListAdapter<T : Any, VH : RecyclerView.ViewHolder>(
    private val delegateHolder: MutableDataDelegate<T> = MutableDataDelegate()
) : OneListAdapter<T, VH>(), DataDelegate<T> by delegateHolder {

    init {
        @Suppress("LeakingThis")
        delegateHolder.delegate = DefDataDelegate(this)
    }

    open var dataDelegate: DataDelegate<T>
        get() = delegateHolder.delegate
        set(value) { delegateHolder.delegate = value }

    override fun getItemCount(): Int = dataDelegate.dataSize()

    override fun getItem(position: Int): T = dataDelegate.getItem(position)
}

/**
 * A [DataDelegate] wrapper that forwards all calls to a mutable [delegate] reference,
 * enabling Kotlin's `by` interface delegation with a replaceable backing delegate.
 */
class MutableDataDelegate<T> : DataDelegate<T> {
    lateinit var delegate: DataDelegate<T>

    override fun getData(): List<T> = delegate.getData()
    override fun dataSize(): Int = delegate.dataSize()
    override fun getItem(position: Int): T = delegate.getItem(position)
    override fun setData(newList: List<T>?) = delegate.setData(newList)
    override fun set(position: Int, item: T) { delegate[position] = item }
    override fun add(position: Int, item: T) = delegate.add(position, item)
    override fun add(item: T) = delegate.add(item)
    override fun addAll(position: Int, newCollection: Collection<T>) = delegate.addAll(position, newCollection)
    override fun addAll(newCollection: Collection<T>) = delegate.addAll(newCollection)
    override fun removeAt(position: Int) = delegate.removeAt(position)
    override fun remove(item: T) = delegate.remove(item)
    override fun swap(fromPosition: Int, toPosition: Int) = delegate.swap(fromPosition, toPosition)
}


