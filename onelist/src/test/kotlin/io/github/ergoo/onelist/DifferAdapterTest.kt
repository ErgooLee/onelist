package io.github.ergoo.onelist

import android.content.Context
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [34])
class DifferAdapterTest {

    data class Item(val id: Int, val name: String)

    private val diffCallback = object : DiffUtil.ItemCallback<Item>() {
        override fun areItemsTheSame(oldItem: Item, newItem: Item) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Item, newItem: Item) = oldItem == newItem
    }

    private class TestDifferAdapter(
        callback: DiffUtil.ItemCallback<Item>
    ) : DifferAdapter<Item, RecyclerView.ViewHolder>(callback) {

        var lastBoundItem: Item? = null
        var lastPayloadBoundItem: Item? = null
        var lastPayloads: List<Any>? = null

        override fun onCreateViewHolder(
            context: Context, parent: ViewGroup, viewType: Int
        ): RecyclerView.ViewHolder {
            return object : RecyclerView.ViewHolder(FrameLayout(context)) {}
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int, item: Item) {
            lastBoundItem = item
        }

        override fun onBindViewHolder(
            holder: RecyclerView.ViewHolder, position: Int, item: Item, payloads: List<Any>
        ) {
            lastPayloadBoundItem = item
            lastPayloads = payloads
        }
    }

    private lateinit var adapter: TestDifferAdapter
    private val context get() = RuntimeEnvironment.getApplication()

    @Before
    fun setUp() {
        adapter = TestDifferAdapter(diffCallback)
    }

    @Test
    fun `initial list is empty`() {
        assertEquals(0, adapter.itemCount)
        assertTrue(adapter.getCurrentList().isEmpty())
    }

    @Test
    fun `submitList updates item count`() {
        adapter.submitList(listOf(Item(1, "a"), Item(2, "b")))
        // AsyncListDiffer posts to main thread; flush
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()

        assertEquals(2, adapter.itemCount)
    }

    @Test
    fun `getItem returns correct item after submitList`() {
        adapter.submitList(listOf(Item(1, "a"), Item(2, "b"), Item(3, "c")))
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()

        assertEquals(Item(1, "a"), adapter.getItem(0))
        assertEquals(Item(2, "b"), adapter.getItem(1))
        assertEquals(Item(3, "c"), adapter.getItem(2))
    }

    @Test
    fun `getCurrentList returns current items`() {
        val list = listOf(Item(1, "x"), Item(2, "y"))
        adapter.submitList(list)
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()

        assertEquals(list, adapter.getCurrentList())
    }

    @Test
    fun `submitList with null clears list`() {
        adapter.submitList(listOf(Item(1, "a")))
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
        assertEquals(1, adapter.itemCount)

        adapter.submitList(null)
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
        assertEquals(0, adapter.itemCount)
    }

    @Test
    fun `submitList with commitCallback is invoked`() {
        var committed = false
        adapter.submitList(listOf(Item(1, "a"))) {
            committed = true
        }
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()

        assertTrue(committed)
    }

    @Test
    fun `addListListener receives updates`() {
        var previousList: List<Item>? = null
        var currentList: List<Item>? = null

        adapter.addListListener { prev, cur ->
            previousList = prev
            currentList = cur
        }

        val newList = listOf(Item(1, "a"))
        adapter.submitList(newList)
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()

        assertNotNull(previousList)
        assertTrue(previousList!!.isEmpty())
        assertEquals(newList, currentList)
    }

    @Test
    fun `removeListListener stops notifications`() {
        var callCount = 0
        val listener = androidx.recyclerview.widget.AsyncListDiffer.ListListener<Item> { _, _ ->
            callCount++
        }

        adapter.addListListener(listener)
        adapter.submitList(listOf(Item(1, "a")))
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
        assertEquals(1, callCount)

        adapter.removeListListener(listener)
        adapter.submitList(listOf(Item(2, "b")))
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
        assertEquals(1, callCount) // not incremented
    }

    @Test
    fun `submitList with same list is no-op`() {
        val list = listOf(Item(1, "a"))
        adapter.submitList(list)
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()

        var committed = false
        adapter.submitList(list) { committed = true }
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()

        // Same reference → immediate commit
        assertTrue(committed)
        assertEquals(1, adapter.itemCount)
    }
}

