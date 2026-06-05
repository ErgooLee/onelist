package io.github.ergoo.onelist

import android.view.View
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
class ClickDelegateTest {

    data class Item(val id: Int, val name: String)

    private lateinit var clickDelegate: ClickDelegate<Item, RecyclerView.ViewHolder>
    private lateinit var viewHolder: RecyclerView.ViewHolder
    private lateinit var itemView: View
    private val items = mutableListOf<Item>()

    @Before
    fun setUp() {
        clickDelegate = ClickDelegate()
        val context = RuntimeEnvironment.getApplication()
        itemView = FrameLayout(context)
        viewHolder = object : RecyclerView.ViewHolder(itemView) {}
        items.clear()
        items.add(Item(1, "test"))
    }

    @Test
    fun `initial state has no listeners`() {
        assertNull(clickDelegate.itemClickListener)
        assertNull(clickDelegate.itemLongClickListener)
    }

    @Test
    fun `setting itemClickListener stores it`() {
        val listener = object : ClickListener<Item, RecyclerView.ViewHolder> {
            override fun onClick(data: Item, view: View, holder: RecyclerView.ViewHolder) {}
        }
        clickDelegate.itemClickListener = listener
        assertSame(listener, clickDelegate.itemClickListener)
    }

    @Test
    fun `setting itemLongClickListener stores it`() {
        val listener = object : LongClickListener<Item, RecyclerView.ViewHolder> {
            override fun onLongClick(data: Item, view: View, holder: RecyclerView.ViewHolder): Boolean = true
        }
        clickDelegate.itemLongClickListener = listener
        assertSame(listener, clickDelegate.itemLongClickListener)
    }

    @Test
    fun `unBindViewClickListener clears click listener on itemView`() {
        clickDelegate.itemClickListener = object : ClickListener<Item, RecyclerView.ViewHolder> {
            override fun onClick(data: Item, view: View, holder: RecyclerView.ViewHolder) {}
        }
        clickDelegate.bindViewClickListener(viewHolder) { pos -> items.getOrNull(pos) }

        // itemView should have a click listener set
        assertTrue(itemView.hasOnClickListeners())

        clickDelegate.unBindViewClickListener(viewHolder)
        assertFalse(itemView.hasOnClickListeners())
    }

    @Test
    fun `addOnItemChildClickListener and removeOnItemChildClickListener`() {
        val listener = object : ClickListener<Item, RecyclerView.ViewHolder> {
            override fun onClick(data: Item, view: View, holder: RecyclerView.ViewHolder) {}
        }
        clickDelegate.addOnItemChildClickListener(android.R.id.text1, listener)
        // No exception = success
        clickDelegate.removeOnItemChildClickListener(android.R.id.text1)
    }

    @Test
    fun `addOnItemChildLongClickListener and removeOnItemChildLongClickListener`() {
        val listener = object : LongClickListener<Item, RecyclerView.ViewHolder> {
            override fun onLongClick(data: Item, view: View, holder: RecyclerView.ViewHolder): Boolean = false
        }
        clickDelegate.addOnItemChildLongClickListener(android.R.id.text1, listener)
        clickDelegate.removeOnItemChildLongClickListener(android.R.id.text1)
    }

    @Test
    fun `bindViewClickListener without any listeners does not crash`() {
        clickDelegate.bindViewClickListener(viewHolder) { pos -> items.getOrNull(pos) }
        // No listeners set, should not crash
        assertFalse(itemView.hasOnClickListeners())
    }

    @Test
    fun `unBindViewClickListener without bind does not crash`() {
        clickDelegate.unBindViewClickListener(viewHolder)
        // Should not crash
    }
}

