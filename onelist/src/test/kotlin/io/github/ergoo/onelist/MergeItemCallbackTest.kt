package io.github.ergoo.onelist

import androidx.recyclerview.widget.DiffUtil
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class MergeItemCallbackTest {

    private lateinit var callback: MergeItemCallback

    data class TypeA(val id: Int, val value: String)
    data class TypeB(val id: Int, val value: String)
    data class TypeC(val id: Int)

    private val typeACallback = object : DiffUtil.ItemCallback<TypeA>() {
        override fun areItemsTheSame(oldItem: TypeA, newItem: TypeA) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: TypeA, newItem: TypeA) = oldItem == newItem
        override fun getChangePayload(oldItem: TypeA, newItem: TypeA): Any? {
            return if (oldItem.value != newItem.value) "value_changed" else null
        }
    }

    private val typeBCallback = object : DiffUtil.ItemCallback<TypeB>() {
        override fun areItemsTheSame(oldItem: TypeB, newItem: TypeB) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: TypeB, newItem: TypeB) = oldItem == newItem
    }

    @Before
    fun setUp() {
        callback = MergeItemCallback()
        @Suppress("UNCHECKED_CAST")
        callback.addItemCallback(TypeA::class.java, typeACallback as DiffUtil.ItemCallback<*>)
        @Suppress("UNCHECKED_CAST")
        callback.addItemCallback(TypeB::class.java, typeBCallback as DiffUtil.ItemCallback<*>)
    }

    // --- areItemsTheSame ---

    @Test
    fun `areItemsTheSame returns true for same type same id`() {
        val old = TypeA(1, "hello")
        val new = TypeA(1, "world")
        assertTrue(callback.areItemsTheSame(old, new))
    }

    @Test
    fun `areItemsTheSame returns false for same type different id`() {
        val old = TypeA(1, "hello")
        val new = TypeA(2, "hello")
        assertFalse(callback.areItemsTheSame(old, new))
    }

    @Test
    fun `areItemsTheSame returns false for different types`() {
        val old = TypeA(1, "hello")
        val new = TypeB(1, "hello")
        assertFalse(callback.areItemsTheSame(old, new))
    }

    @Test
    fun `areItemsTheSame falls back to equals for unregistered type`() {
        val old = TypeC(1)
        val new = TypeC(1)
        // TypeC has no registered callback, falls back to equals
        assertTrue(callback.areItemsTheSame(old, new))
    }

    @Test
    fun `areItemsTheSame fallback equals returns false for different values`() {
        val old = TypeC(1)
        val new = TypeC(2)
        assertFalse(callback.areItemsTheSame(old, new))
    }

    // --- areContentsTheSame ---

    @Test
    fun `areContentsTheSame returns true for identical items`() {
        val old = TypeA(1, "hello")
        val new = TypeA(1, "hello")
        assertTrue(callback.areContentsTheSame(old, new))
    }

    @Test
    fun `areContentsTheSame returns false for different content`() {
        val old = TypeA(1, "hello")
        val new = TypeA(1, "world")
        assertFalse(callback.areContentsTheSame(old, new))
    }

    @Test
    fun `areContentsTheSame returns true for unregistered type fallback`() {
        val old = TypeC(1)
        val new = TypeC(1)
        // No callback registered; since areItemsTheSame used equals and passed,
        // areContentsTheSame returns true
        assertTrue(callback.areContentsTheSame(old, new))
    }

    // --- getChangePayload ---

    @Test
    fun `getChangePayload delegates to registered callback`() {
        val old = TypeA(1, "hello")
        val new = TypeA(1, "world")
        assertEquals("value_changed", callback.getChangePayload(old, new))
    }

    @Test
    fun `getChangePayload returns null when no change`() {
        val old = TypeA(1, "hello")
        val new = TypeA(1, "hello")
        assertNull(callback.getChangePayload(old, new))
    }

    @Test
    fun `getChangePayload returns null for different types`() {
        val old = TypeA(1, "hello")
        val new = TypeB(1, "hello")
        assertNull(callback.getChangePayload(old, new))
    }

    @Test
    fun `getChangePayload returns null for unregistered type`() {
        val old = TypeC(1)
        val new = TypeC(2)
        assertNull(callback.getChangePayload(old, new))
    }

    // --- addItemCallback chaining ---

    @Test
    fun `addItemCallback returns same instance for chaining`() {
        val result = MergeItemCallback()
        @Suppress("UNCHECKED_CAST")
        val chained = result.addItemCallback(TypeA::class.java, typeACallback as DiffUtil.ItemCallback<*>)
        assertSame(result, chained)
    }

    // --- TypeB callback ---

    @Test
    fun `TypeB callback works independently`() {
        val old = TypeB(1, "x")
        val new = TypeB(1, "y")
        assertTrue(callback.areItemsTheSame(old, new))
        assertFalse(callback.areContentsTheSame(old, new))
    }
}

