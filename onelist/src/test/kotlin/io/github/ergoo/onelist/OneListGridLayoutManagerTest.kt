package io.github.ergoo.onelist

import android.content.Context
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [34])
class OneListGridLayoutManagerTest {

    private val context: Context get() = RuntimeEnvironment.getApplication()

    // --- FullSpanSizeLookup direct tests ---

    @Test
    fun `FullSpanSizeLookup returns 1 when adapter is null`() {
        val lookup = OneListGridLayoutManager.FullSpanSizeLookup()
        lookup.spanCount = 4
        lookup.adapter = null

        assertEquals(1, lookup.getSpanSize(0))
    }

    @Test
    fun `FullSpanSizeLookup returns spanCount for FullSpan adapter`() {
        val lookup = OneListGridLayoutManager.FullSpanSizeLookup()
        lookup.spanCount = 4

        val fullSpanAdapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>(), FullSpan {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
                object : RecyclerView.ViewHolder(FrameLayout(parent.context)) {}
            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {}
            override fun getItemCount() = 3
        }
        lookup.adapter = fullSpanAdapter

        assertEquals(4, lookup.getSpanSize(0))
        assertEquals(4, lookup.getSpanSize(1))
    }

    @Test
    fun `FullSpanSizeLookup returns spanCount for Spannable with fullSpan viewType`() {
        val lookup = OneListGridLayoutManager.FullSpanSizeLookup()
        lookup.spanCount = 4

        val spannableAdapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>(), Spannable {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
                object : RecyclerView.ViewHolder(FrameLayout(parent.context)) {}
            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {}
            override fun getItemCount() = 2
            override fun getItemViewType(position: Int) = if (position == 0) 1 else 2
            override fun isFullSpanByViewType(itemType: Int) = itemType == 1
            override fun isFullSpanItemByPosition(position: Int) = isFullSpanByViewType(getItemViewType(position))
            override fun spanCount(position: Int) = 2
        }
        lookup.adapter = spannableAdapter

        // Position 0: viewType=1, isFullSpan=true → spanCount (4)
        assertEquals(4, lookup.getSpanSize(0))
        // Position 1: viewType=2, isFullSpan=false → spanCount(1) = 2
        assertEquals(2, lookup.getSpanSize(1))
    }

    @Test
    fun `FullSpanSizeLookup falls back to originalSpanSizeLookup for plain adapter`() {
        val lookup = OneListGridLayoutManager.FullSpanSizeLookup()
        lookup.spanCount = 4

        val plainAdapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
                object : RecyclerView.ViewHolder(FrameLayout(parent.context)) {}
            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {}
            override fun getItemCount() = 5
        }
        lookup.adapter = plainAdapter

        // No originalSpanSizeLookup set → default 1
        assertEquals(1, lookup.getSpanSize(0))

        // Set a custom fallback
        lookup.originalSpanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int) = 3
        }
        assertEquals(3, lookup.getSpanSize(0))
    }

    // --- OneListGridLayoutManager integration ---

    @Test
    fun `constructor creates valid layout manager`() {
        val lm = OneListGridLayoutManager(context, 3)
        assertEquals(3, lm.spanCount)
    }

    @Test
    fun `setSpanCount updates internal lookup`() {
        val lm = OneListGridLayoutManager(context, 2)
        lm.spanCount = 5
        assertEquals(5, lm.oneListSpanSizeLookup.spanCount)
    }

    @Test
    fun `setSpanSizeLookup stores as fallback`() {
        val lm = OneListGridLayoutManager(context, 4)
        val customLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int) = 2
        }

        lm.spanSizeLookup = customLookup
        assertSame(customLookup, lm.oneListSpanSizeLookup.originalSpanSizeLookup)
    }

    @Test
    fun `FullSpan adapter gets full span in RecyclerView`() {
        val lm = OneListGridLayoutManager(context, 4)
        val rv = RecyclerView(context)
        rv.layoutManager = lm

        val fullSpanAdapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>(), FullSpan {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
                object : RecyclerView.ViewHolder(FrameLayout(parent.context)) {}
            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {}
            override fun getItemCount() = 1
        }

        rv.adapter = fullSpanAdapter
        assertEquals(4, lm.oneListSpanSizeLookup.getSpanSize(0))
    }

    @Test
    fun `ConcatAdapter with mixed FullSpan and Spannable adapters`() {
        val lm = OneListGridLayoutManager(context, 6)
        val rv = RecyclerView(context)
        rv.layoutManager = lm

        // FullSpan header adapter (1 item)
        val headerAdapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>(), FullSpan {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
                object : RecyclerView.ViewHolder(FrameLayout(parent.context)) {}
            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {}
            override fun getItemCount() = 1
        }

        // Spannable content adapter (3 items, span=2 each)
        val contentAdapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>(), Spannable {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
                object : RecyclerView.ViewHolder(FrameLayout(parent.context)) {}
            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {}
            override fun getItemCount() = 3
            override fun isFullSpanByViewType(itemType: Int) = false
            override fun isFullSpanItemByPosition(position: Int) = false
            override fun spanCount(position: Int) = 2
        }

        val concatAdapter = ConcatAdapter(headerAdapter, contentAdapter)
        rv.adapter = concatAdapter

        // Position 0 = header (FullSpan) → 6
        assertEquals(6, lm.oneListSpanSizeLookup.getSpanSize(0))
        // Position 1 = content item 0 → 2
        assertEquals(2, lm.oneListSpanSizeLookup.getSpanSize(1))
        // Position 2 = content item 1 → 2
        assertEquals(2, lm.oneListSpanSizeLookup.getSpanSize(2))
    }
}

