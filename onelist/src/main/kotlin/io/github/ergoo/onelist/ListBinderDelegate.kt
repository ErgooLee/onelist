package io.github.ergoo.onelist

import android.content.Context
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

/**
 * Encapsulates the shared ListBinder management logic used by all multi-type
 * (merge) adapters: [MergeAdapter], [MergeMutableListAdapter], and
 * [io.github.ergoo.onelist.paging3.MergePagingAdapter].
 *
 * Handles:
 * - Binder registration ([addListBinder])
 * - View-type ↔ class mapping ([findViewType])
 * - View-type ↔ binder lookup ([getListBinder] / [getListBinderOrNull])
 * - View creation and binding delegation
 * - Lifecycle forwarding to binders
 * - Attach/detach context propagation
 *
 * @param getItem function to retrieve the data item at a given position
 *        from the host adapter.
 */
@Suppress("UNCHECKED_CAST")
class ListBinderDelegate(
    private val getItem: (Int) -> Any?,
) {

    /** Maps each item [Class] to its view-type integer. */
    private val typeMap = HashMap<Class<*>, Int>()

    /** Maps each view-type integer to the [ListBinder] that handles it. */
    private val listBinderMap = HashMap<Int, ListBinder<Any, *>>()

    // -----------------------------------------------------------------------
    // Registration
    // -----------------------------------------------------------------------

    /**
     * Registers a [ListBinder] to handle items of type [clazz].
     *
     * @param clazz the exact class of items this binder handles (no inheritance lookup).
     * @param listBinder the delegate responsible for creating and binding views.
     * @param adapter the host adapter, stored on the binder for later access.
     */
    fun <T : Any> addListBinder(
        clazz: Class<out T>,
        listBinder: ListBinder<T, *>,
        adapter: RecyclerView.Adapter<*>,
    ) {
        val itemType = listBinder.getViewType()
        typeMap[clazz] = itemType
        listBinderMap[itemType] = listBinder as ListBinder<Any, *>
        listBinder._adapter = adapter as RecyclerView.Adapter<RecyclerView.ViewHolder>
        listBinder.dataHolder = object : ListBinder.DataHolder {
            override fun getItem(position: Int): Any? {
                return this@ListBinderDelegate.getItem(position)
            }
        }
    }

    // -----------------------------------------------------------------------
    // Lookup
    // -----------------------------------------------------------------------

    /**
     * Returns the [ListBinder] for the given [viewType].
     *
     * @throws IllegalStateException if no binder is registered for [viewType].
     */
    fun getListBinder(viewType: Int): ListBinder<Any, RecyclerView.ViewHolder> {
        val binder = listBinderMap[viewType]
        checkNotNull(binder) { "getListBinder: viewType '$viewType' no such ListBinder found, please use addListBinder() first!" }
        return binder as ListBinder<Any, RecyclerView.ViewHolder>
    }

    /** Returns the [ListBinder] for [viewType], or `null` if none is registered. */
    fun getListBinderOrNull(viewType: Int): ListBinder<Any, RecyclerView.ViewHolder>? {
        val binder = listBinderMap[viewType]
        return binder as? ListBinder<Any, RecyclerView.ViewHolder>
    }

    /**
     * Looks up the view-type integer for [clazz] using exact class matching.
     *
     * @throws IllegalStateException if [clazz] has not been registered via [addListBinder].
     */
    fun findViewType(clazz: Class<*>): Int {
        val type = typeMap[clazz]
        checkNotNull(type) { "findViewType: ViewType: $clazz Not Find!" }
        return type
    }

    // -----------------------------------------------------------------------
    // View creation & binding
    // -----------------------------------------------------------------------

    /** Delegates view creation to the [ListBinder] registered for [viewType]. */
    fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return getListBinder(viewType).onCreateViewHolder(parent, viewType)
    }

    /** Delegates binding to the [ListBinder] that owns [holder]'s view type. */
    fun onBindViewHolder(holder: RecyclerView.ViewHolder, item: Any) {
        getListBinder(holder.localViewType).convert(holder, item)
    }

    /** Delegates partial binding (with payloads) to the corresponding [ListBinder]. */
    fun onBindViewHolder(holder: RecyclerView.ViewHolder, item: Any, payloads: List<Any>) {
        getListBinder(holder.localViewType).convert(holder, item, payloads)
    }

    // -----------------------------------------------------------------------
    // Spannable support
    // -----------------------------------------------------------------------

    /** Returns `true` if the binder for [itemType] implements [FullSpan]. */
    fun isFullSpan(itemType: Int): Boolean {
        return getListBinderOrNull(itemType) is FullSpan
    }

    /** Returns the span count for the binder at the given view type. */
    fun spanCount(viewType: Int): Int {
        return getListBinder(viewType).spanCount()
    }

    // -----------------------------------------------------------------------
    // Lifecycle
    // -----------------------------------------------------------------------

    /** Propagates attach context to all registered binders. */
    fun onAttachedToRecyclerView(context: Context, recyclerView: RecyclerView) {
        listBinderMap.values.forEach { binder ->
            binder._context = context
            binder._recyclerView = recyclerView
        }
    }

    /** Clears context from all registered binders. */
    fun onDetachedFromRecyclerView() {
        listBinderMap.values.forEach { binder ->
            binder._context = null
            binder._recyclerView = null
        }
    }

    /** Forwards [RecyclerView.Adapter.onViewAttachedToWindow] to the matching binder. */
    fun onViewAttachedToWindow(holder: RecyclerView.ViewHolder) {
        getListBinderOrNull(holder.localViewType)?.onViewAttachedToWindow(holder)
    }

    /** Forwards [RecyclerView.Adapter.onViewDetachedFromWindow] to the matching binder. */
    fun onViewDetachedFromWindow(holder: RecyclerView.ViewHolder) {
        getListBinderOrNull(holder.localViewType)?.onViewDetachedFromWindow(holder)
    }

    /** Forwards [RecyclerView.Adapter.onViewRecycled] to the matching binder. */
    fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        getListBinderOrNull(holder.localViewType)?.onViewRecycled(holder)
    }

    /** Forwards [RecyclerView.Adapter.onFailedToRecycleView] to the matching binder. */
    fun onFailedToRecycleView(holder: RecyclerView.ViewHolder): Boolean? {
        return getListBinderOrNull(holder.localViewType)?.onFailedToRecycleView(holder)
    }
}

