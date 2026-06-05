package io.github.ergoo.onelist

import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
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
class ListBinderTest {

    data class Item(val id: Int, val name: String, val liked: Boolean = false)

    private lateinit var binder: TestListBinder
    private val context get() = RuntimeEnvironment.getApplication()

    private class TestListBinder : ListBinder<Item, RecyclerView.ViewHolder>() {

        var convertCount = 0
        var lastConvertedData: Item? = null
        var lastPayloads: List<Any>? = null
        var viewRecycledCount = 0
        var failedToRecycleResult = false

        override fun getViewType(): Int = 1001

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val layout = LinearLayout(parent.context).apply {
                val child = TextView(parent.context).apply { id = android.R.id.text1 }
                addView(child)
            }
            return object : RecyclerView.ViewHolder(layout) {}
        }

        override fun convert(holder: RecyclerView.ViewHolder, data: Item) {
            convertCount++
            lastConvertedData = data
        }

        override fun convert(holder: RecyclerView.ViewHolder, data: Item, payloads: List<Any>) {
            lastPayloads = payloads
            if (payloads.contains("like")) {
                // partial update
            } else {
                convert(holder, data)
            }
        }

        override fun spanCount(): Int = 2

        override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
            viewRecycledCount++
        }

        override fun onFailedToRecycleView(holder: RecyclerView.ViewHolder): Boolean {
            return failedToRecycleResult
        }
    }

    @Before
    fun setUp() {
        binder = TestListBinder()
    }

    // --- Basic properties ---

    @Test
    fun `getViewType returns correct value`() {
        assertEquals(1001, binder.getViewType())
    }

    @Test
    fun `spanCount returns custom value`() {
        assertEquals(2, binder.spanCount())
    }

    @Test
    fun `default spanCount is 1`() {
        val defaultBinder = object : ListBinder<Item, RecyclerView.ViewHolder>() {
            override fun getViewType() = 999
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
                object : RecyclerView.ViewHolder(FrameLayout(parent.context)) {}
            override fun convert(holder: RecyclerView.ViewHolder, data: Item) {}
        }
        assertEquals(1, defaultBinder.spanCount())
    }

    // --- Adapter / Context access ---

    @Test(expected = IllegalStateException::class)
    fun `adapter throws before addListBinder`() {
        binder.adapter
    }

    @Test(expected = IllegalStateException::class)
    fun `recyclerView throws before attach`() {
        binder.recyclerView
    }

    @Test(expected = IllegalStateException::class)
    fun `context throws before attach`() {
        binder.context
    }

    @Test
    fun `adapter accessible after setting _adapter`() {
        val fakeAdapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
                object : RecyclerView.ViewHolder(FrameLayout(parent.context)) {}
            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {}
            override fun getItemCount() = 0
        }
        binder._adapter = fakeAdapter
        assertSame(fakeAdapter, binder.adapter)
    }

    @Test
    fun `recyclerView accessible after setting _recyclerView`() {
        val rv = RecyclerView(context)
        binder._recyclerView = rv
        assertSame(rv, binder.recyclerView)
    }

    @Test
    fun `context accessible after setting _context`() {
        binder._context = context
        assertSame(context, binder.context)
    }

    // --- Click listeners ---

    @Test
    fun `initial click listeners are null`() {
        assertNull(binder.clickListener)
        assertNull(binder.longClickListener)
    }

    @Test
    fun `setting click listener stores it`() {
        val listener = object : ClickListener<Item, RecyclerView.ViewHolder> {
            override fun onClick(data: Item, view: View, holder: RecyclerView.ViewHolder) {}
        }
        binder.clickListener = listener
        assertSame(listener, binder.clickListener)
    }

    @Test
    fun `addOnItemChildClickListener and remove work`() {
        val listener = object : ClickListener<Item, RecyclerView.ViewHolder> {
            override fun onClick(data: Item, view: View, holder: RecyclerView.ViewHolder) {}
        }
        binder.addOnItemChildClickListener(android.R.id.text1, listener)
        binder.removeOnItemChildClickListener(android.R.id.text1)
    }

    @Test
    fun `addOnItemChildLongClickListener and remove work`() {
        val listener = object : LongClickListener<Item, RecyclerView.ViewHolder> {
            override fun onLongClick(data: Item, view: View, holder: RecyclerView.ViewHolder) = true
        }
        binder.addOnItemChildLongClickListener(android.R.id.text1, listener)
        binder.removeOnItemChildLongClickListener(android.R.id.text1)
    }

    // --- bindClick / unbindClick ---

    @Test
    fun `onViewAttachedToWindow binds click and onViewDetachedFromWindow unbinds`() {
        val items = listOf(Item(1, "a"))
        binder.dataHolder = object : ListBinder.DataHolder {
            override fun getItem(position: Int) = items.getOrNull(position)
        }

        var clicked = false
        binder.clickListener = object : ClickListener<Item, RecyclerView.ViewHolder> {
            override fun onClick(data: Item, view: View, holder: RecyclerView.ViewHolder) {
                clicked = true
            }
        }

        val parent = FrameLayout(context)
        val holder = binder.onCreateViewHolder(parent, 1001)

        // Simulate attach
        binder.onViewAttachedToWindow(holder)
        assertTrue(holder.itemView.hasOnClickListeners())

        // Simulate detach
        binder.onViewDetachedFromWindow(holder)
        assertFalse(holder.itemView.hasOnClickListeners())
    }

    @Test
    fun `child click listener is bound on attach`() {
        val items = listOf(Item(1, "a"))
        binder.dataHolder = object : ListBinder.DataHolder {
            override fun getItem(position: Int) = items.getOrNull(position)
        }

        binder.addOnItemChildClickListener(android.R.id.text1,
            object : ClickListener<Item, RecyclerView.ViewHolder> {
                override fun onClick(data: Item, view: View, holder: RecyclerView.ViewHolder) {}
            })

        val parent = FrameLayout(context)
        val holder = binder.onCreateViewHolder(parent, 1001)

        binder.onViewAttachedToWindow(holder)

        val childView = holder.itemView.findViewById<View>(android.R.id.text1)
        assertNotNull(childView)
        assertTrue(childView.hasOnClickListeners())

        binder.onViewDetachedFromWindow(holder)
        assertFalse(childView.hasOnClickListeners())
    }

    // --- onViewRecycled / onFailedToRecycleView ---

    @Test
    fun `onViewRecycled is forwarded`() {
        val parent = FrameLayout(context)
        val holder = binder.onCreateViewHolder(parent, 1001)
        binder.onViewRecycled(holder)
        assertEquals(1, binder.viewRecycledCount)
    }

    @Test
    fun `onFailedToRecycleView returns configured value`() {
        val parent = FrameLayout(context)
        val holder = binder.onCreateViewHolder(parent, 1001)

        binder.failedToRecycleResult = false
        assertFalse(binder.onFailedToRecycleView(holder))

        binder.failedToRecycleResult = true
        assertTrue(binder.onFailedToRecycleView(holder))
    }

    // --- convert ---

    @Test
    fun `convert is called with correct data`() {
        val parent = FrameLayout(context)
        val holder = binder.onCreateViewHolder(parent, 1001)
        val item = Item(1, "test")

        binder.convert(holder, item)

        assertEquals(1, binder.convertCount)
        assertEquals(item, binder.lastConvertedData)
    }

    @Test
    fun `convert with payloads receives payloads`() {
        val parent = FrameLayout(context)
        val holder = binder.onCreateViewHolder(parent, 1001)
        val item = Item(1, "test", liked = true)

        binder.convert(holder, item, listOf("like"))

        assertEquals(listOf("like"), binder.lastPayloads)
        // "like" payload does partial update, so convertCount stays 0
        assertEquals(0, binder.convertCount)
    }

    @Test
    fun `convert with non-matching payload falls back to full convert`() {
        val parent = FrameLayout(context)
        val holder = binder.onCreateViewHolder(parent, 1001)
        val item = Item(1, "test")

        binder.convert(holder, item, listOf("other"))

        assertEquals(listOf("other"), binder.lastPayloads)
        assertEquals(1, binder.convertCount) // full convert called
    }

    @Test
    fun `default convert with payloads delegates to full convert`() {
        val defaultBinder = object : ListBinder<Item, RecyclerView.ViewHolder>() {
            var convertCount = 0
            override fun getViewType() = 999
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
                object : RecyclerView.ViewHolder(FrameLayout(parent.context)) {}
            override fun convert(holder: RecyclerView.ViewHolder, data: Item) {
                convertCount++
            }
        }

        val parent = FrameLayout(context)
        val holder = defaultBinder.onCreateViewHolder(parent, 999)

        defaultBinder.convert(holder, Item(1, "x"), listOf("any"))
        assertEquals(1, defaultBinder.convertCount)
    }
}

