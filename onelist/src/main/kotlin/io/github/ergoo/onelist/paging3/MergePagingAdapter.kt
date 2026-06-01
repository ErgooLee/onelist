package io.github.ergoo.onelist.paging3

import android.content.Context
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import io.github.ergoo.onelist.ClickListener
import io.github.ergoo.onelist.ListBinder
import io.github.ergoo.onelist.ListBinderDelegate
import io.github.ergoo.onelist.LongClickListener
import io.github.ergoo.onelist.MergeItemCallback
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * A multi-type Paging 3 adapter that delegates view creation, binding, and
 * click handling to [ListBinder] instances — one per item type.
 *
 * This is the Paging 3 counterpart of [io.github.ergoo.onelist.MergeAdapter] /
 * [io.github.ergoo.onelist.DifferMergeAdapter]. Each item type is registered
 * via [addListBinder] with an optional per-type [DiffUtil.ItemCallback].
 *
 * **Note:** Click listeners must be configured on individual [ListBinder]s, not
 * on this adapter directly — setting [itemClickListener] or
 * [itemLongClickListener] will throw [IllegalStateException].
 *
 * **Placeholders:** When Paging returns `null` items (placeholders enabled),
 * [onBindEmptyHolder] is called instead of delegating to a [ListBinder].
 * The view type for placeholder items is determined by [emptyViewType].
 *
 * @param mainDispatcher dispatcher for UI updates.
 * @param workerDispatcher dispatcher for background diff computation.
 */
@Suppress("UNCHECKED_CAST")
abstract class MergePagingAdapter private constructor(
    mainDispatcher: CoroutineDispatcher = Dispatchers.Main,
    workerDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val diffCallback: MergeItemCallback,
) : OneListPagingAdapter<Any, RecyclerView.ViewHolder>(diffCallback, mainDispatcher, workerDispatcher) {

    constructor(
        mainDispatcher: CoroutineDispatcher = Dispatchers.Main,
        workerDispatcher: CoroutineDispatcher = Dispatchers.Default,
    ) : this(mainDispatcher, workerDispatcher, MergeItemCallback())

    /** Delegate that manages all [ListBinder] registration, lookup, and lifecycle. */
    protected val binderDelegate = ListBinderDelegate { position -> getItem(position) }

    /** Always throws — click listeners must be set on individual [ListBinder]s. */
    override var itemClickListener: ClickListener<Any, RecyclerView.ViewHolder>?
        get() = super.itemClickListener
        set(_) {
            throw IllegalStateException("please set click listener in listBinder")
        }

    /** Always throws — long-click listeners must be set on individual [ListBinder]s. */
    override var itemLongClickListener: LongClickListener<Any, RecyclerView.ViewHolder>?
        get() = super.itemLongClickListener
        set(_) {
            throw IllegalStateException("please set click listener in listBinder")
        }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        binderDelegate.onAttachedToRecyclerView(context, recyclerView)
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        binderDelegate.onDetachedFromRecyclerView()
    }

    // -----------------------------------------------------------------------
    // ListBinder registration
    // -----------------------------------------------------------------------

    fun <T : Any> addListBinder(
        clazz: Class<out T>,
        listBinder: ListBinder<T, *>,
        callback: DiffUtil.ItemCallback<T>? = null,
    ): MergePagingAdapter {
        binderDelegate.addListBinder(clazz, listBinder, this)
        callback?.let {
            diffCallback.addItemCallback(clazz, it)
        }
        return this
    }

    inline fun <reified T : Any> addListBinder(
        listBinder: ListBinder<T, *>,
        callback: DiffUtil.ItemCallback<T>? = null,
    ): MergePagingAdapter {
        addListBinder(T::class.java, listBinder, callback)
        return this
    }

    // -----------------------------------------------------------------------
    // View creation & binding
    // -----------------------------------------------------------------------

    override fun onCreateViewHolder(
        context: Context, parent: ViewGroup, viewType: Int,
    ): RecyclerView.ViewHolder {
        return binderDelegate.onCreateViewHolder(parent, viewType)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int, item: Any?) {
        if (item == null) {
            onBindEmptyHolder(holder, position)
        } else {
            binderDelegate.onBindViewHolder(holder, item)
        }
    }

    /** Called when a placeholder item (null) needs to be bound. */
    open fun onBindEmptyHolder(holder: RecyclerView.ViewHolder, position: Int) {}

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder, position: Int, item: Any?, payloads: List<Any>,
    ) {
        if (item == null) {
            onBindEmptyHolder(holder, position)
        } else {
            binderDelegate.onBindViewHolder(holder, item, payloads)
        }
    }

    // -----------------------------------------------------------------------
    // Spannable
    // -----------------------------------------------------------------------

    override fun isFullSpanByViewType(itemType: Int): Boolean {
        return super.isFullSpanByViewType(itemType) || binderDelegate.isFullSpan(itemType)
    }

    override fun spanCount(position: Int): Int {
        return binderDelegate.spanCount(getItemViewType(position))
    }

    // -----------------------------------------------------------------------
    // ListBinder lookup
    // -----------------------------------------------------------------------

    open fun getListBinder(viewType: Int) = binderDelegate.getListBinder(viewType)

    open fun getListBinderOrNull(viewType: Int) = binderDelegate.getListBinderOrNull(viewType)

    override fun getItemViewType(position: Int, item: Any?): Int {
        return if (item == null) emptyViewType()
        else binderDelegate.findViewType(item.javaClass)
    }

    /** Returns the view type used for placeholder (null) items. */
    open fun emptyViewType(): Int = RecyclerView.INVALID_TYPE

    // -----------------------------------------------------------------------
    // Click binding (delegated to ListBinders)
    // -----------------------------------------------------------------------

    override fun bindViewClickListener(viewHolder: RecyclerView.ViewHolder) {}

    override fun unBindViewClickListener(viewHolder: RecyclerView.ViewHolder) {}

    // -----------------------------------------------------------------------
    // Lifecycle delegation
    // -----------------------------------------------------------------------

    override fun onViewAttachedToWindow(holder: RecyclerView.ViewHolder) {
        super.onViewAttachedToWindow(holder)
        binderDelegate.onViewAttachedToWindow(holder)
    }

    override fun onViewDetachedFromWindow(holder: RecyclerView.ViewHolder) {
        super.onViewDetachedFromWindow(holder)
        binderDelegate.onViewDetachedFromWindow(holder)
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        super.onViewRecycled(holder)
        binderDelegate.onViewRecycled(holder)
    }

    override fun onFailedToRecycleView(holder: RecyclerView.ViewHolder): Boolean {
        return binderDelegate.onFailedToRecycleView(holder)
            ?: super.onFailedToRecycleView(holder)
    }

    fun findViewType(clazz: Class<*>): Int = binderDelegate.findViewType(clazz)

    // -----------------------------------------------------------------------
    // Blocked child click APIs
    // -----------------------------------------------------------------------

    override fun addOnItemChildClickListener(
        id: Int, listener: ClickListener<Any, RecyclerView.ViewHolder>,
    ): MergePagingAdapter = throw IllegalStateException("please set in listBinder")

    override fun removeOnItemChildClickListener(id: Int): MergePagingAdapter =
        throw IllegalStateException("please set in listBinder")

    override fun addOnItemChildLongClickListener(
        id: Int, listener: LongClickListener<Any, RecyclerView.ViewHolder>,
    ): MergePagingAdapter = throw IllegalStateException("please set in listBinder")

    override fun removeOnItemChildLongClickListener(id: Int): MergePagingAdapter =
        throw IllegalStateException("please set in listBinder")

}