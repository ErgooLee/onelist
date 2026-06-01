package io.github.ergoo.onelist

import androidx.recyclerview.widget.DiffUtil
import java.util.HashMap

/**
 * A composite [DiffUtil.ItemCallback] that routes diff comparisons to
 * per-type callbacks registered via [addItemCallback].
 *
 * Used by [DifferMergeAdapter] to support multi-type lists where each item
 * class can define its own identity and content comparison logic.
 *
 * **Fallback behaviour:**
 * - If two items have the same type but no callback is registered,
 *   [areItemsTheSame] uses [equals][Any.equals] and [areContentsTheSame]
 *   returns `true` (since identity was already confirmed by equality).
 * - If two items have different types, they are always considered different items.
 */
@Suppress("UNCHECKED_CAST")
class MergeItemCallback : DiffUtil.ItemCallback<Any>() {

    /** Per-type callbacks, keyed by the exact item [Class]. */
    private val classDiffMap = HashMap<Class<*>, DiffUtil.ItemCallback<Any>>()

    private fun isSameType(oldItem: Any, newItem: Any): Boolean =
        oldItem.javaClass == newItem.javaClass

    /**
     * Returns `true` if [oldItem] and [newItem] represent the same logical item.
     *
     * Delegates to the registered callback for the item's type if available;
     * falls back to [equals][Any.equals]. Items of different types are always
     * considered different.
     */
    override fun areItemsTheSame(oldItem: Any, newItem: Any): Boolean {
        if (!isSameType(oldItem, newItem)) return false
        classDiffMap[oldItem.javaClass]?.let {
            return it.areItemsTheSame(oldItem, newItem)
        }
        return oldItem == newItem
    }

    /**
     * Returns `true` if the contents of [oldItem] and [newItem] are identical.
     *
     * Only called after [areItemsTheSame] returns `true`. Delegates to the
     * registered callback if available; otherwise returns `true` because the
     * fallback identity check in [areItemsTheSame] already used [equals].
     */
    override fun areContentsTheSame(oldItem: Any, newItem: Any): Boolean {
        if (isSameType(oldItem, newItem)) {
            classDiffMap[oldItem.javaClass]?.let {
                return it.areContentsTheSame(oldItem, newItem)
            }
        }
        // areContentsTheSame is only called after areItemsTheSame returns true.
        // In the fallback path (no registered callback), areItemsTheSame already
        // confirmed equality via ==, so contents can be considered unchanged.
        return true
    }

    /**
     * Returns an optional payload describing the change between [oldItem] and
     * [newItem], or `null` if a full rebind is needed.
     *
     * Delegates to the registered callback if available.
     */
    override fun getChangePayload(oldItem: Any, newItem: Any): Any? {
        if (isSameType(oldItem, newItem)) {
            return classDiffMap[oldItem.javaClass]?.getChangePayload(oldItem, newItem)
        }
        return null
    }

    /**
     * Registers a [DiffUtil.ItemCallback] for items of type [clazz].
     *
     * @param clazz the exact class this callback handles (no inheritance lookup).
     * @param callback the diff callback for this type.
     * @return this instance for chaining.
     */
    fun addItemCallback(clazz: Class<*>, callback: DiffUtil.ItemCallback<*>): MergeItemCallback {
        classDiffMap[clazz] = callback as DiffUtil.ItemCallback<Any>
        return this
    }
}