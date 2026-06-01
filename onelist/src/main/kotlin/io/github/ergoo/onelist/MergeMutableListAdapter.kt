package io.github.ergoo.onelist

import android.content.Context
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

/**
 * A multi-type adapter backed by [MutableListAdapter]'s mutable data list,
 * delegating view creation, binding, and click handling to [ListBinder]
 * instances — one per item type.
 *
 * Behaves identically to [MergeAdapter] but inherits [MutableListAdapter]'s
 * [DataDelegate]-based mutation API (`add`, `remove`, `swap`, `setData`, etc.).
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
abstract class MergeMutableListAdapter : MutableListAdapter<Any, RecyclerView.ViewHolder>() {

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

    fun <T : Any> addListBinder(
        clazz: Class<out T>,
        listBinder: ListBinder<T, *>,
    ): MergeMutableListAdapter {
        binderDelegate.addListBinder(clazz, listBinder, this)
        return this
    }

    inline fun <reified T : Any> addListBinder(
        listBinder: ListBinder<T, *>,
    ): MergeMutableListAdapter {
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

    override fun onCreateViewHolder(
        context: Context, parent: ViewGroup, viewType: Int,
    ): RecyclerView.ViewHolder {
        return binderDelegate.onCreateViewHolder(parent, viewType)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int, item: Any) {
        binderDelegate.onBindViewHolder(holder, item)
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder, position: Int, item: Any, payloads: List<Any>,
    ) {
        binderDelegate.onBindViewHolder(holder, item, payloads)
    }

    override fun isFullSpanByViewType(itemType: Int): Boolean {
        return super.isFullSpanByViewType(itemType) || binderDelegate.isFullSpan(itemType)
    }

    override fun spanCount(position: Int): Int {
        return binderDelegate.spanCount(getItemViewType(position))
    }

    open fun getListBinder(viewType: Int) = binderDelegate.getListBinder(viewType)

    open fun getListBinderOrNull(viewType: Int) = binderDelegate.getListBinderOrNull(viewType)

    override fun getItemViewType(position: Int, item: Any): Int {
        return binderDelegate.findViewType(item.javaClass)
    }

    override fun bindViewClickListener(viewHolder: RecyclerView.ViewHolder) {}

    override fun unBindViewClickListener(viewHolder: RecyclerView.ViewHolder) {}

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

    override fun addOnItemChildClickListener(
        id: Int, listener: ClickListener<Any, RecyclerView.ViewHolder>,
    ): MergeMutableListAdapter = throw IllegalStateException("please set in listBinder")

    override fun removeOnItemChildClickListener(id: Int): MergeMutableListAdapter =
        throw IllegalStateException("please set in listBinder")

    override fun addOnItemChildLongClickListener(
        id: Int, listener: LongClickListener<Any, RecyclerView.ViewHolder>,
    ): MergeMutableListAdapter = throw IllegalStateException("please set in listBinder")

    override fun removeOnItemChildLongClickListener(id: Int): MergeMutableListAdapter =
        throw IllegalStateException("please set in listBinder")

}