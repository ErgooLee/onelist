package io.github.ergoo.onelist

import android.view.View
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [34])
class ClickListenerTest {

    data class Item(val id: Int, val name: String)

    @Test
    fun `ClickListenerWrapper delegates onClick to SimpleClickListener`() {
        var capturedData: Item? = null
        var capturedView: View? = null

        val simple = object : SimpleClickListener<Item> {
            override fun onClick(data: Item, view: View) {
                capturedData = data
                capturedView = view
            }
        }

        val wrapper = ClickListenerWrapper<Item, String>(simple)
        val item = Item(1, "test")
        val view = View(RuntimeEnvironment.getApplication())

        wrapper.onClick(item, view, "holder")

        assertEquals(item, capturedData)
        assertSame(view, capturedView)
    }

    @Test
    fun `LongClickListenerWrapper delegates onLongClick to SimpleLongClickListener`() {
        var capturedData: Item? = null
        var returnValue = true

        val simple = object : SimpleLongClickListener<Item> {
            override fun onLongClick(data: Item, view: View): Boolean {
                capturedData = data
                return returnValue
            }
        }

        val wrapper = LongClickListenerWrapper<Item, String>(simple)
        val item = Item(2, "long")
        val view = View(RuntimeEnvironment.getApplication())

        assertTrue(wrapper.onLongClick(item, view, "holder"))
        assertEquals(item, capturedData)

        returnValue = false
        assertFalse(wrapper.onLongClick(item, view, "holder"))
    }

    @Test
    fun `ClickListener interface has correct signature`() {
        val listener = object : ClickListener<Item, String> {
            override fun onClick(data: Item, view: View, holder: String) {}
        }
        assertNotNull(listener)
    }

    @Test
    fun `LongClickListener interface has correct signature`() {
        val listener = object : LongClickListener<Item, String> {
            override fun onLongClick(data: Item, view: View, holder: String): Boolean = false
        }
        assertNotNull(listener)
    }
}
