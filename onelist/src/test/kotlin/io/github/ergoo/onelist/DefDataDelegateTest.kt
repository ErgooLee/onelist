package io.github.ergoo.onelist

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.recyclerview.widget.RecyclerView
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [34])
class DefDataDelegateTest {

    /**
     * A fake adapter that tracks notification calls instead of using Mockito.
     */
    private class FakeAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        var dataSetChangedCount = 0
        val itemChangedPositions = mutableListOf<Int>()
        val itemInsertedPositions = mutableListOf<Int>()
        val itemRemovedPositions = mutableListOf<Int>()
        val rangeInserted = mutableListOf<Pair<Int, Int>>()
        val itemMovedPairs = mutableListOf<Pair<Int, Int>>()

        // Must register an observer to allow notify* calls
        init {
            registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
                override fun onChanged() { dataSetChangedCount++ }
                override fun onItemRangeChanged(positionStart: Int, itemCount: Int) {
                    for (i in 0 until itemCount) itemChangedPositions.add(positionStart + i)
                }
                override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                    if (itemCount == 1) itemInsertedPositions.add(positionStart)
                    else rangeInserted.add(positionStart to itemCount)
                }
                override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
                    for (i in 0 until itemCount) itemRemovedPositions.add(positionStart + i)
                }
                override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
                    itemMovedPairs.add(fromPosition to toPosition)
                }
            })
        }

        fun resetTracking() {
            dataSetChangedCount = 0
            itemChangedPositions.clear()
            itemInsertedPositions.clear()
            itemRemovedPositions.clear()
            rangeInserted.clear()
            itemMovedPairs.clear()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return object : RecyclerView.ViewHolder(FrameLayout(parent.context)) {}
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {}
        override fun getItemCount() = 0
    }

    private lateinit var adapter: FakeAdapter
    private lateinit var delegate: DefDataDelegate<String>

    @Before
    fun setUp() {
        adapter = FakeAdapter()
        delegate = DefDataDelegate(adapter)
    }

    @Test
    fun `initial data is empty`() {
        assertEquals(0, delegate.dataSize())
        assertTrue(delegate.getData().isEmpty())
    }

    @Test
    fun `setData replaces all data and notifies`() {
        delegate.setData(listOf("a", "b", "c"))
        assertEquals(3, delegate.dataSize())
        assertEquals(listOf("a", "b", "c"), delegate.getData())
        assertEquals(1, adapter.dataSetChangedCount)
    }

    @Test
    fun `setData with null clears data`() {
        delegate.setData(listOf("a", "b"))
        delegate.setData(null)
        assertEquals(0, delegate.dataSize())
        assertEquals(2, adapter.dataSetChangedCount)
    }

    @Test
    fun `getItem returns correct item`() {
        delegate.setData(listOf("x", "y", "z"))
        assertEquals("x", delegate.getItem(0))
        assertEquals("y", delegate.getItem(1))
        assertEquals("z", delegate.getItem(2))
    }

    @Test(expected = IndexOutOfBoundsException::class)
    fun `getItem throws for out of bounds`() {
        delegate.setData(listOf("a"))
        delegate.getItem(1)
    }

    @Test
    fun `set replaces item at position`() {
        delegate.setData(listOf("a", "b", "c"))
        adapter.resetTracking()

        delegate[1] = "B"
        assertEquals("B", delegate.getItem(1))
        assertTrue(adapter.itemChangedPositions.contains(1))
    }

    @Test(expected = IndexOutOfBoundsException::class)
    fun `set throws for out of bounds position`() {
        delegate.setData(listOf("a"))
        delegate[5] = "x"
    }

    @Test
    fun `add inserts item at position`() {
        delegate.setData(listOf("a", "c"))
        adapter.resetTracking()

        delegate.add(1, "b")
        assertEquals(3, delegate.dataSize())
        assertEquals("b", delegate.getItem(1))
        assertEquals("c", delegate.getItem(2))
        assertTrue(adapter.itemInsertedPositions.contains(1))
    }

    @Test
    fun `add appends item to end`() {
        delegate.setData(listOf("a"))
        adapter.resetTracking()

        delegate.add("b")
        assertEquals(2, delegate.dataSize())
        assertEquals("b", delegate.getItem(1))
        assertTrue(adapter.itemInsertedPositions.contains(1))
    }

    @Test(expected = IndexOutOfBoundsException::class)
    fun `add at negative position throws`() {
        delegate.setData(listOf("a"))
        delegate.add(-1, "x")
    }

    @Test(expected = IndexOutOfBoundsException::class)
    fun `add beyond size throws`() {
        delegate.setData(listOf("a"))
        delegate.add(5, "x")
    }

    @Test
    fun `addAll inserts collection at position`() {
        delegate.setData(listOf("a", "d"))
        adapter.resetTracking()

        delegate.addAll(1, listOf("b", "c"))
        assertEquals(4, delegate.dataSize())
        assertEquals("b", delegate.getItem(1))
        assertEquals("c", delegate.getItem(2))
        assertEquals("d", delegate.getItem(3))
        assertEquals(listOf(1 to 2), adapter.rangeInserted)
    }

    @Test
    fun `addAll appends collection to end`() {
        delegate.setData(listOf("a"))
        adapter.resetTracking()

        delegate.addAll(listOf("b", "c"))
        assertEquals(3, delegate.dataSize())
        assertEquals(listOf(1 to 2), adapter.rangeInserted)
    }

    @Test
    fun `removeAt removes item and notifies`() {
        delegate.setData(listOf("a", "b", "c"))
        adapter.resetTracking()

        delegate.removeAt(1)
        assertEquals(2, delegate.dataSize())
        assertEquals("a", delegate.getItem(0))
        assertEquals("c", delegate.getItem(1))
        assertTrue(adapter.itemRemovedPositions.contains(1))
    }

    @Test(expected = IndexOutOfBoundsException::class)
    fun `removeAt throws for out of bounds`() {
        delegate.setData(listOf("a"))
        delegate.removeAt(5)
    }

    @Test
    fun `remove removes first occurrence`() {
        delegate.setData(listOf("a", "b", "c"))
        adapter.resetTracking()

        delegate.remove("b")
        assertEquals(2, delegate.dataSize())
        assertEquals(listOf("a", "c"), delegate.getData())
        assertTrue(adapter.itemRemovedPositions.contains(1))
    }

    @Test
    fun `remove does nothing for non-existent item`() {
        delegate.setData(listOf("a", "b"))
        adapter.resetTracking()

        delegate.remove("x")
        assertEquals(2, delegate.dataSize())
        assertTrue(adapter.itemRemovedPositions.isEmpty())
    }

    @Test
    fun `swap exchanges two items`() {
        delegate.setData(listOf("a", "b", "c"))
        adapter.resetTracking()

        delegate.swap(0, 2)
        assertEquals("c", delegate.getItem(0))
        assertEquals("a", delegate.getItem(2))
        assertTrue(adapter.itemMovedPairs.contains(0 to 2))
    }

    @Test(expected = IndexOutOfBoundsException::class)
    fun `swap throws for out of bounds fromPosition`() {
        delegate.setData(listOf("a"))
        delegate.swap(5, 0)
    }

    @Test(expected = IndexOutOfBoundsException::class)
    fun `swap throws for out of bounds toPosition`() {
        delegate.setData(listOf("a"))
        delegate.swap(0, 5)
    }

    @Test
    fun `getData returns data after setData with immutable list`() {
        val immutableList = listOf("a", "b")
        delegate.setData(immutableList)
        assertEquals(2, delegate.dataSize())

        // After mutation, the internal list should be converted to mutable
        delegate.add("c")
        assertEquals(3, delegate.dataSize())
    }

    @Test
    fun `multiple mutations work correctly`() {
        delegate.setData(listOf("a", "b", "c"))
        delegate.add("d")
        delegate.removeAt(0)
        delegate[0] = "B"
        delegate.swap(0, 1)

        assertEquals(3, delegate.dataSize())
        assertEquals("c", delegate.getItem(0))
        assertEquals("B", delegate.getItem(1))
        assertEquals("d", delegate.getItem(2))
    }
}
