package io.github.ergoo.onelist

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [34])
class DifferMergeAdapterTest {

    data class TypeA(val id: Int, val value: String)
    data class TypeB(val id: Int, val value: String)

    private val typeACallback = object : DiffUtil.ItemCallback<TypeA>() {
        override fun areItemsTheSame(old: TypeA, new: TypeA) = old.id == new.id
        override fun areContentsTheSame(old: TypeA, new: TypeA) = old == new
    }

    private val typeBCallback = object : DiffUtil.ItemCallback<TypeB>() {
        override fun areItemsTheSame(old: TypeB, new: TypeB) = old.id == new.id
        override fun areContentsTheSame(old: TypeB, new: TypeB) = old == new
    }

    private class TestDifferMergeAdapter : DifferMergeAdapter()

    private lateinit var adapter: TestDifferMergeAdapter
    private lateinit var binderA: ListBinder<TypeA, RecyclerView.ViewHolder>
    private lateinit var binderB: ListBinder<TypeB, RecyclerView.ViewHolder>

    @Before
    fun setUp() {
        adapter = TestDifferMergeAdapter()

        binderA = object : ListBinder<TypeA, RecyclerView.ViewHolder>() {
            override fun getViewType(): Int = 100
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
                object : RecyclerView.ViewHolder(FrameLayout(parent.context)) {}
            override fun convert(holder: RecyclerView.ViewHolder, data: TypeA) {}
        }

        binderB = object : ListBinder<TypeB, RecyclerView.ViewHolder>() {
            override fun getViewType(): Int = 200
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
                object : RecyclerView.ViewHolder(FrameLayout(parent.context)) {}
            override fun convert(holder: RecyclerView.ViewHolder, data: TypeB) {}
        }

        adapter.addListBinder(TypeA::class.java, binderA, typeACallback)
        adapter.addListBinder(TypeB::class.java, binderB, typeBCallback)
    }

    @Test
    fun `initial list is empty`() {
        assertEquals(0, adapter.itemCount)
        assertTrue(adapter.getCurrentList().isEmpty())
    }

    @Test
    fun `submitList with heterogeneous items updates correctly`() {
        adapter.submitList(listOf(TypeA(1, "a"), TypeB(2, "b"), TypeA(3, "c")))
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()

        assertEquals(3, adapter.itemCount)
        assertEquals(TypeA(1, "a"), adapter.getItem(0))
        assertEquals(TypeB(2, "b"), adapter.getItem(1))
        assertEquals(TypeA(3, "c"), adapter.getItem(2))
    }

    @Test
    fun `submitList with null clears list`() {
        adapter.submitList(listOf(TypeA(1, "a")))
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
        assertEquals(1, adapter.itemCount)

        adapter.submitList(null)
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
        assertEquals(0, adapter.itemCount)
    }

    @Test
    fun `submitList with commitCallback is invoked`() {
        var committed = false
        adapter.submitList(listOf(TypeA(1, "a"))) { committed = true }
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()

        assertTrue(committed)
    }

    @Test
    fun `getCurrentList returns snapshot`() {
        val items = listOf(TypeA(1, "x"), TypeB(2, "y"))
        adapter.submitList(items)
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()

        assertEquals(items, adapter.getCurrentList())
    }

    @Test
    fun `getItemViewType resolves by item class`() {
        adapter.submitList(listOf(TypeA(1, "a"), TypeB(2, "b")))
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()

        assertEquals(100, adapter.getItemViewType(0))
        assertEquals(200, adapter.getItemViewType(1))
    }

    @Test
    fun `addListBinder without callback still works with equals fallback`() {
        data class TypeC(val id: Int)

        val binderC = object : ListBinder<TypeC, RecyclerView.ViewHolder>() {
            override fun getViewType(): Int = 300
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
                object : RecyclerView.ViewHolder(FrameLayout(parent.context)) {}
            override fun convert(holder: RecyclerView.ViewHolder, data: TypeC) {}
        }

        adapter.addListBinder(TypeC::class.java, binderC) // no callback

        adapter.submitList(listOf(TypeC(1), TypeA(2, "a")))
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()

        assertEquals(2, adapter.itemCount)
        assertEquals(300, adapter.getItemViewType(0))
    }

    @Test
    fun `addListListener receives list updates`() {
        var notified = false
        adapter.addListListener { _, _ -> notified = true }

        adapter.submitList(listOf(TypeA(1, "a")))
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()

        assertTrue(notified)
    }

    @Test
    fun `removeListListener stops notifications`() {
        var count = 0
        val listener = androidx.recyclerview.widget.AsyncListDiffer.ListListener<Any> { _, _ ->
            count++
        }

        adapter.addListListener(listener)
        adapter.submitList(listOf(TypeA(1, "a")))
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
        assertEquals(1, count)

        adapter.removeListListener(listener)
        adapter.submitList(listOf(TypeB(2, "b")))
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
        assertEquals(1, count) // not incremented
    }

    @Test
    fun `submitting updated list changes items`() {
        adapter.submitList(listOf(TypeA(1, "old")))
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
        assertEquals(1, adapter.itemCount)

        // Replace with completely new list
        adapter.submitList(listOf(TypeB(2, "new")))
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()

        assertEquals(1, adapter.itemCount)
    }

    @Test
    fun `addListBinder returns self for chaining`() {
        data class TypeD(val id: Int)
        val result = adapter.addListBinder(TypeD::class.java,
            object : ListBinder<TypeD, RecyclerView.ViewHolder>() {
                override fun getViewType() = 400
                override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
                    object : RecyclerView.ViewHolder(FrameLayout(parent.context)) {}
                override fun convert(holder: RecyclerView.ViewHolder, data: TypeD) {}
            })
        assertSame(adapter, result)
    }
}

