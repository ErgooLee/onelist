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
class ListBinderDelegateTest {

    data class ItemA(val id: Int)
    data class ItemB(val id: Int)

    private lateinit var delegate: ListBinderDelegate
    private lateinit var binderA: ListBinder<ItemA, RecyclerView.ViewHolder>
    private lateinit var binderB: ListBinder<ItemB, RecyclerView.ViewHolder>
    private val items = mutableListOf<Any>()

    /** Minimal no-op adapter for registration purposes. */
    private class NoOpAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            object : RecyclerView.ViewHolder(FrameLayout(parent.context)) {}
        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {}
        override fun getItemCount() = 0
    }

    @Before
    fun setUp() {
        items.clear()
        delegate = ListBinderDelegate { position -> items.getOrNull(position) }

        binderA = object : ListBinder<ItemA, RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                return object : RecyclerView.ViewHolder(FrameLayout(parent.context)) {}
            }
            override fun convert(holder: RecyclerView.ViewHolder, data: ItemA) {}
            override fun getViewType(): Int = 100
        }

        binderB = object : ListBinder<ItemB, RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                return object : RecyclerView.ViewHolder(FrameLayout(parent.context)) {}
            }
            override fun convert(holder: RecyclerView.ViewHolder, data: ItemB) {}
            override fun getViewType(): Int = 200
        }

        val adapter = NoOpAdapter()
        delegate.addListBinder(ItemA::class.java, binderA, adapter)
        delegate.addListBinder(ItemB::class.java, binderB, adapter)
    }

    // --- findViewType ---

    @Test
    fun `findViewType returns correct type for registered class`() {
        assertEquals(100, delegate.findViewType(ItemA::class.java))
        assertEquals(200, delegate.findViewType(ItemB::class.java))
    }

    @Test(expected = IllegalStateException::class)
    fun `findViewType throws for unregistered class`() {
        delegate.findViewType(String::class.java)
    }

    // --- getListBinder ---

    @Test
    fun `getListBinder returns correct binder`() {
        assertSame(binderA, delegate.getListBinder(100))
    }

    @Test
    fun `getListBinder returns binderB for viewType 200`() {
        assertSame(binderB, delegate.getListBinder(200))
    }

    @Test(expected = IllegalStateException::class)
    fun `getListBinder throws for unknown viewType`() {
        delegate.getListBinder(999)
    }

    // --- getListBinderOrNull ---

    @Test
    fun `getListBinderOrNull returns binder for known type`() {
        assertNotNull(delegate.getListBinderOrNull(100))
    }

    @Test
    fun `getListBinderOrNull returns null for unknown type`() {
        assertNull(delegate.getListBinderOrNull(999))
    }

    // --- isFullSpan ---

    @Test
    fun `isFullSpan returns false for non-FullSpan binder`() {
        assertFalse(delegate.isFullSpan(100))
    }

    @Test
    fun `isFullSpan returns true for FullSpan binder`() {
        data class ItemC(val id: Int)
        val fullSpanBinder = object : ListBinder<ItemC, RecyclerView.ViewHolder>(), FullSpan {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
                object : RecyclerView.ViewHolder(FrameLayout(parent.context)) {}
            override fun convert(holder: RecyclerView.ViewHolder, data: ItemC) {}
            override fun getViewType(): Int = 300
        }
        delegate.addListBinder(ItemC::class.java, fullSpanBinder, NoOpAdapter())
        assertTrue(delegate.isFullSpan(300))
    }

    // --- spanCount ---

    @Test
    fun `spanCount returns default 1`() {
        assertEquals(1, delegate.spanCount(100))
    }

    @Test
    fun `spanCount returns custom value from binder`() {
        data class ItemD(val id: Int)
        val spanBinder = object : ListBinder<ItemD, RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
                object : RecyclerView.ViewHolder(FrameLayout(parent.context)) {}
            override fun convert(holder: RecyclerView.ViewHolder, data: ItemD) {}
            override fun getViewType(): Int = 400
            override fun spanCount(): Int = 3
        }
        delegate.addListBinder(ItemD::class.java, spanBinder, NoOpAdapter())
        assertEquals(3, delegate.spanCount(400))
    }

    // --- Lifecycle ---

    @Test
    fun `onAttachedToRecyclerView sets context and recyclerView on binders`() {
        val context = RuntimeEnvironment.getApplication()
        val recyclerView = RecyclerView(context)
        delegate.onAttachedToRecyclerView(context, recyclerView)
        assertSame(context, binderA._context)
        assertSame(recyclerView, binderA._recyclerView)
        assertSame(context, binderB._context)
        assertSame(recyclerView, binderB._recyclerView)
    }

    @Test
    fun `onDetachedFromRecyclerView clears context and recyclerView`() {
        val context = RuntimeEnvironment.getApplication()
        val recyclerView = RecyclerView(context)
        delegate.onAttachedToRecyclerView(context, recyclerView)
        delegate.onDetachedFromRecyclerView()
        assertNull(binderA._context)
        assertNull(binderA._recyclerView)
        assertNull(binderB._context)
        assertNull(binderB._recyclerView)
    }

    // --- DataHolder ---

    @Test
    fun `binder dataHolder provides items from delegate`() {
        items.addAll(listOf(ItemA(1), ItemB(2), ItemA(3)))
        val holderA = binderA.dataHolder
        assertNotNull(holderA)
        assertEquals(ItemA(1), holderA!!.getItem(0))
        assertEquals(ItemB(2), holderA.getItem(1))
        assertEquals(ItemA(3), holderA.getItem(2))
    }

    @Test
    fun `binder dataHolder returns null for out of bounds`() {
        items.add(ItemA(1))
        assertNull(binderA.dataHolder!!.getItem(5))
    }
}
