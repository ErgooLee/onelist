package io.github.ergoo.onelist

import android.content.Context
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

/**
 * A multi-type adapter that delegates view creation, binding, and click handling
 * to [ListBinder] instances, one per item type.
 *
 * Each item type is registered via [addListBinder], which maps an item's [Class]
 * to a unique view-type integer provided by [ListBinder.getViewType].
 *
 * **Note:** Click listeners must be configured on individual [ListBinder]s, not
 * on this adapter directly — setting [itemClickListener] or
 * [itemLongClickListener] will throw [IllegalStateException].
 *
 * **Inheritance caveat:** [findViewType] uses exact class matching.
 * Registering `Animal::class` will **not** cover `Dog` subclass instances;
 * each concrete class must be registered separately.
 */
@Suppress("UNCHECKED_CAST")
abstract class MergeAdapter : OneListAdapter<Any, RecyclerView.ViewHolder>() {

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

    /**
     * Registers a [ListBinder] to handle items of type [clazz].
     *
     * @param clazz the exact class of items this binder handles (no inheritance lookup).
     * @param listBinder the delegate responsible for creating and binding views.
     * @return this adapter for chaining.
     */
    fun <T : Any> addListBinder(
        clazz: Class<out T>,
        listBinder: ListBinder<T, *>,
    ): MergeAdapter {
        binderDelegate.addListBinder(clazz, listBinder, this)
        return this
    }

    /**
     * Registers a [ListBinder] using the reified type [T] as the item class.
     *
     * Shorthand for `addListBinder(T::class.java, listBinder)`.
     */
    inline fun <reified T : Any> addListBinder(
        listBinder: ListBinder<T, *>,
    ): MergeAdapter {
        addListBinder(T::class.java, listBinder)
        return this
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        binderDelegate.onAttachedToRecyclerView(context, recyclerView)
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        binderDelegate.onDetachedFromRecyclerView()
    }

    /** Delegates view creation to the [ListBinder] registered for [viewType]. */
    override fun onCreateViewHolder(
        context: Context,
        parent: ViewGroup,
        viewType: Int,
    ): RecyclerView.ViewHolder {
        return binderDelegate.onCreateViewHolder(parent, viewType)
    }

    /** Delegates binding to the [ListBinder] that owns [holder]'s view type. */
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int, item: Any) {
        binderDelegate.onBindViewHolder(holder, item)
    }

    /** Delegates partial binding (with payloads) to the corresponding [ListBinder]. */
    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
        item: Any,
        payloads: List<Any>,
    ) {
        binderDelegate.onBindViewHolder(holder, item, payloads)
    }

    override fun isFullSpanByViewType(itemType: Int): Boolean {
        return super.isFullSpanByViewType(itemType) || binderDelegate.isFullSpan(itemType)
    }

    override fun spanCount(position: Int): Int {
        return binderDelegate.spanCount(getItemViewType(position))
    }

    /**
     * Returns the [ListBinder] for the given [viewType].
     *
     * @throws IllegalStateException if no binder is registered for [viewType].
     */
    open fun getListBinder(viewType: Int): ListBinder<Any, RecyclerView.ViewHolder> {
        return binderDelegate.getListBinder(viewType)
    }

    /** Returns the [ListBinder] for [viewType], or `null` if none is registered. */
    open fun getListBinderOrNull(viewType: Int): ListBinder<Any, RecyclerView.ViewHolder>? {
        return binderDelegate.getListBinderOrNull(viewType)
    }

    override fun getItemViewType(position: Int, item: Any): Int {
        return binderDelegate.findViewType(item.javaClass)
    }

    /** No-op — click listeners are bound in individual [ListBinder]s. */
    override fun bindViewClickListener(viewHolder: RecyclerView.ViewHolder) {
        // Handled by ListBinder.onViewAttachedToWindow
    }

    /** No-op — click listeners are unbound in individual [ListBinder]s. */
    override fun unBindViewClickListener(viewHolder: RecyclerView.ViewHolder) {
        // Handled by ListBinder.onViewDetachedFromWindow
    }

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

    /**
     * Looks up the view-type integer for [clazz] using exact class matching.
     *
     * @throws IllegalStateException if [clazz] has not been registered via [addListBinder].
     */
    fun findViewType(clazz: Class<*>): Int {
        return binderDelegate.findViewType(clazz)
    }

    override fun addOnItemChildClickListener(
        id: Int,
        listener: ClickListener<Any, RecyclerView.ViewHolder>,
    ): MergeAdapter {
        throw IllegalStateException("please set in listBinder")
    }

    override fun removeOnItemChildClickListener(id: Int): MergeAdapter {
        throw IllegalStateException("please set in listBinder")
    }

    override fun addOnItemChildLongClickListener(
        id: Int,
        listener: LongClickListener<Any, RecyclerView.ViewHolder>,
    ): MergeAdapter {
        throw IllegalStateException("please set in listBinder")
    }

    override fun removeOnItemChildLongClickListener(id: Int): MergeAdapter {
        throw IllegalStateException("please set in listBinder")
    }

}