package io.github.ergoo.onelist

import androidx.recyclerview.widget.AdapterListUpdateCallback
import androidx.recyclerview.widget.AsyncDifferConfig
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import java.util.concurrent.Executor

/**
 * A [MergeAdapter] that uses [AsyncListDiffer] to compute list updates
 * on a background thread via [DiffUtil].
 *
 * Each item type can optionally supply its own [DiffUtil.ItemCallback] when
 * registering via [addListBinder]; types without a callback fall back to
 * [equals][Any.equals] for identity and content comparison (see [MergeItemCallback]).
 *
 * @param backgroundThreadExecutor optional executor for running diffs;
 *        defaults to the shared [AsyncDifferConfig] background executor.
 */
@Suppress("UNCHECKED_CAST")
abstract class DifferMergeAdapter(
    backgroundThreadExecutor: Executor? = null
) : MergeAdapter() {

    /** Composite [DiffUtil.ItemCallback] that routes to per-type callbacks. */
    private val diffCallback = MergeItemCallback()

    private val config: AsyncDifferConfig<Any> = AsyncDifferConfig.Builder(diffCallback)
        .setBackgroundThreadExecutor(backgroundThreadExecutor)
        .build()

    /**
     * Lazily created [AsyncListDiffer]. Deferred to avoid leaking `this` during
     * construction (before subclass initialisation completes).
     */
    private val differ: AsyncListDiffer<Any> by lazy {
        AsyncListDiffer(AdapterListUpdateCallback(this), config)
    }

    /** Returns the number of items currently held by the differ. */
    override fun getItemCount(): Int {
        return differ.currentList.size
    }

    /** Returns the item at [position] from the current differ list. */
    override fun getItem(position: Int): Any {
        return differ.currentList[position]
    }

    /**
     * Returns an unmodifiable snapshot of the current list.
     *
     * The returned list is safe to read but must not be mutated;
     * use [submitList] to update the data.
     */
    fun getCurrentList(): List<Any> {
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
    open fun submitList(list: List<Any>?) {
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
    open fun submitList(list: List<Any>?, commitCallback: Runnable?) {
        differ.submitList(list, commitCallback)
    }

    /**
     * Registers a [listener] that will be notified each time the current list
     * is replaced by a new one.
     */
    fun addListListener(listener: AsyncListDiffer.ListListener<Any>) {
        differ.addListListener(listener)
    }

    /** Removes a previously registered [listener]. */
    fun removeListListener(listener: AsyncListDiffer.ListListener<Any>) {
        differ.removeListListener(listener)
    }

    /**
     * Registers a [ListBinder] to handle items of type [clazz], with an optional
     * per-type [DiffUtil.ItemCallback].
     *
     * @param clazz the exact class of items this binder handles.
     * @param listBinder the delegate responsible for creating and binding views.
     * @param callback optional diff callback for this type; if `null`, falls back
     *                 to [equals][Any.equals] comparison.
     * @return this adapter for chaining.
     */
    @JvmOverloads
    fun <T : Any> addListBinder(
        clazz: Class<out T>,
        listBinder: ListBinder<T, *>,
        callback: DiffUtil.ItemCallback<T>? = null
    ): DifferMergeAdapter {
        super.addListBinder(clazz, listBinder)
        callback?.let {
            diffCallback.addItemCallback(clazz, it as DiffUtil.ItemCallback<Any>)
        }
        return this
    }

    /**
     * Registers a [ListBinder] using the reified type [T] as the item class,
     * with an optional per-type [DiffUtil.ItemCallback].
     *
     * Shorthand for `addListBinder(T::class.java, listBinder, callback)`.
     */
    inline fun <reified T : Any> addListBinder(
        listBinder: ListBinder<T, *>,
        callback: DiffUtil.ItemCallback<T>? = null
    ): DifferMergeAdapter {
        addListBinder(T::class.java, listBinder, callback)
        return this
    }

}