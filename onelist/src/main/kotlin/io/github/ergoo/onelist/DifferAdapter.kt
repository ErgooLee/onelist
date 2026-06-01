package io.github.ergoo.onelist

import androidx.recyclerview.widget.AdapterListUpdateCallback
import androidx.recyclerview.widget.AsyncDifferConfig
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView

/**
 * An [OneListAdapter] that uses [AsyncListDiffer] to compute list updates
 * on a background thread via [DiffUtil].
 *
 * Subclasses only need to implement view creation and binding; data diffing
 * and `notifyItem*` dispatching are handled automatically by [submitList].
 *
 * @param T the type of data items.
 * @param VH the type of [RecyclerView.ViewHolder].
 * @param config the [AsyncDifferConfig] that controls diff behaviour and executor.
 * @see DiffUtil.ItemCallback
 */
abstract class DifferAdapter<T : Any, VH : RecyclerView.ViewHolder>(
    private val config: AsyncDifferConfig<T>
) : OneListAdapter<T, VH>() {

    /**
     * Convenience constructor that builds an [AsyncDifferConfig] from a
     * [DiffUtil.ItemCallback], using the default background executor.
     *
     * @param diffCallback callback that determines item identity and content equality.
     */
    constructor(diffCallback: DiffUtil.ItemCallback<T>) : this(
        AsyncDifferConfig.Builder(diffCallback).build()
    )

    /**
     * Lazily created [AsyncListDiffer]. Deferred to avoid leaking `this` during
     * construction (before subclass initialisation completes).
     */
    private val differ: AsyncListDiffer<T> by lazy {
        AsyncListDiffer(AdapterListUpdateCallback(this), config)
    }

    /** Returns the number of items currently held by the differ. */
    override fun getItemCount(): Int {
        return differ.currentList.size
    }

    /** Returns the item at [position] from the current differ list. */
    override fun getItem(position: Int): T {
        return differ.currentList[position]
    }

    /**
     * Returns an unmodifiable snapshot of the current list.
     *
     * The returned list is safe to read but must not be mutated;
     * use [submitList] to update the data.
     */
    fun getCurrentList(): List<T> {
        return differ.currentList
    }

    /**
     * Submits a new list to be diffed against the current one.
     *
     * If a list is already being diffed, the submitted list is queued and
     * will be applied after the current diff completes.
     * Passing `null` is equivalent to submitting an empty list.
     *
     * @param list the new list to display, or `null` to clear.
     */
    open fun submitList(list: List<T>?) {
        differ.submitList(list)
    }

    /**
     * Submits a new list to be diffed, with a callback that is invoked once
     * the list has been committed (i.e. the RecyclerView has been notified).
     *
     * @param list the new list to display, or `null` to clear.
     * @param commitCallback invoked on the main thread after the update is applied;
     *                       may be `null`.
     */
    open fun submitList(list: List<T>?, commitCallback: Runnable?) {
        differ.submitList(list, commitCallback)
    }

    /**
     * Registers a [listener] that will be notified each time the current list
     * is replaced by a new one.
     */
    fun addListListener(listener: AsyncListDiffer.ListListener<T>) {
        differ.addListListener(listener)
    }

    /**
     * Removes a previously registered [listener].
     */
    fun removeListListener(listener: AsyncListDiffer.ListListener<T>) {
        differ.removeListListener(listener)
    }


}