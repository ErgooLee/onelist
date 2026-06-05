package io.github.ergoo.onelist

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.recyclerview.widget.RecyclerView
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [34])
class MergeAdapterTest {

    data class TypeA(val id: Int, val value: String)
    data class TypeB(val id: Int, val value: String)

    private val context get() = RuntimeEnvironment.getApplication()

    private class TestMergeAdapter : MergeAdapter() {
        private val items = mutableListOf<Any>()

        override fun getItemCount(): Int = items.size
        override fun getItem(position: Int): Any = items[position]

        fun setItems(list: List<Any>) {
            items.clear()
            items.addAll(list)
            notifyDataSetChanged()
        }
    }

    private lateinit var adapter: TestMergeAdapter
    private lateinit var binderA: ListBinder<TypeA, RecyclerView.ViewHolder>
    private lateinit var binderB: ListBinder<TypeB, RecyclerView.ViewHolder>

    @Before
    fun setUp() {
        adapter = TestMergeAdapter()

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

        adapter.addListBinder(TypeA::class.java, binderA)
        adapter.addListBinder(TypeB::class.java, binderB)
    }

    // --- addListBinder ---

    @Test
    fun `addListBinder registers binder for class`() {
        val binder = adapter.getListBinder(100)
        assertSame(binderA, binder)
    }

    @Test
    fun `addListBinder supports reified inline variant`() {
        // Already used in setUp, verify both binders are accessible
        assertNotNull(adapter.getListBinderOrNull(100))
        assertNotNull(adapter.getListBinderOrNull(200))
    }

    // --- findViewType ---

    @Test
    fun `findViewType returns correct type for TypeA`() {
        assertEquals(100, adapter.findViewType(TypeA::class.java))
    }

    @Test
    fun `findViewType returns correct type for TypeB`() {
        assertEquals(200, adapter.findViewType(TypeB::class.java))
    }

    @Test(expected = IllegalStateException::class)
    fun `findViewType throws for unregistered class`() {
        adapter.findViewType(String::class.java)
    }

    // --- getListBinder / getListBinderOrNull ---

    @Test
    fun `getListBinder returns correct binder for viewType`() {
        assertSame(binderA, adapter.getListBinder(100))
        assertSame(binderB, adapter.getListBinder(200))
    }

    @Test(expected = IllegalStateException::class)
    fun `getListBinder throws for unknown viewType`() {
        adapter.getListBinder(999)
    }

    @Test
    fun `getListBinderOrNull returns null for unknown viewType`() {
        assertNull(adapter.getListBinderOrNull(999))
    }

    // --- itemClickListener / itemLongClickListener throw ---

    @Test(expected = IllegalStateException::class)
    fun `setting itemClickListener throws`() {
        adapter.itemClickListener = object : ClickListener<Any, RecyclerView.ViewHolder> {
            override fun onClick(data: Any, view: android.view.View, holder: RecyclerView.ViewHolder) {}
        }
    }

    @Test(expected = IllegalStateException::class)
    fun `setting itemLongClickListener throws`() {
        adapter.itemLongClickListener = object : LongClickListener<Any, RecyclerView.ViewHolder> {
            override fun onLongClick(data: Any, view: android.view.View, holder: RecyclerView.ViewHolder) = true
        }
    }

    // --- addOnItemChildClickListener / addOnItemChildLongClickListener throw ---

    @Test(expected = IllegalStateException::class)
    fun `addOnItemChildClickListener throws`() {
        adapter.addOnItemChildClickListener(android.R.id.text1,
            object : ClickListener<Any, RecyclerView.ViewHolder> {
                override fun onClick(data: Any, view: android.view.View, holder: RecyclerView.ViewHolder) {}
            })
    }

    @Test(expected = IllegalStateException::class)
    fun `removeOnItemChildClickListener throws`() {
        adapter.removeOnItemChildClickListener(android.R.id.text1)
    }

    @Test(expected = IllegalStateException::class)
    fun `addOnItemChildLongClickListener throws`() {
        adapter.addOnItemChildLongClickListener(android.R.id.text1,
            object : LongClickListener<Any, RecyclerView.ViewHolder> {
                override fun onLongClick(data: Any, view: android.view.View, holder: RecyclerView.ViewHolder) = true
            })
    }

    @Test(expected = IllegalStateException::class)
    fun `removeOnItemChildLongClickListener throws`() {
        adapter.removeOnItemChildLongClickListener(android.R.id.text1)
    }

    // --- getItemViewType ---

    @Test
    fun `getItemViewType returns correct type based on item class`() {
        adapter.setItems(listOf(TypeA(1, "a"), TypeB(2, "b"), TypeA(3, "c")))

        assertEquals(100, adapter.getItemViewType(0))
        assertEquals(200, adapter.getItemViewType(1))
        assertEquals(100, adapter.getItemViewType(2))
    }

    // --- FullSpan / spanCount integration ---

    @Test
    fun `isFullSpanByViewType returns false for non-FullSpan binder`() {
        assertFalse(adapter.isFullSpanByViewType(100))
    }

    @Test
    fun `isFullSpanByViewType returns true for FullSpan binder`() {
        data class TypeC(val id: Int)
        val fullSpanBinder = object : ListBinder<TypeC, RecyclerView.ViewHolder>(), FullSpan {
            override fun getViewType() = 300
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
                object : RecyclerView.ViewHolder(FrameLayout(parent.context)) {}
            override fun convert(holder: RecyclerView.ViewHolder, data: TypeC) {}
        }
        adapter.addListBinder(TypeC::class.java, fullSpanBinder)
        assertTrue(adapter.isFullSpanByViewType(300))
    }

    // --- Lifecycle: attach / detach ---

    @Test
    fun `onAttachedToRecyclerView propagates to binders`() {
        val rv = RecyclerView(context)
        rv.adapter = adapter // triggers onAttachedToRecyclerView

        assertSame(context, binderA._context)
        assertSame(rv, binderA._recyclerView)
    }

    @Test
    fun `onDetachedFromRecyclerView clears binder references`() {
        val rv = RecyclerView(context)
        rv.adapter = adapter
        rv.adapter = null // triggers onDetachedFromRecyclerView

        assertNull(binderA._context)
        assertNull(binderA._recyclerView)
    }
}

