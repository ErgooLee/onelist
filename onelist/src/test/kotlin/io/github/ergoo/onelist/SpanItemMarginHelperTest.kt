package io.github.ergoo.onelist

import org.junit.Assert.*
import org.junit.Test

class SpanItemMarginHelperTest {

    @Test
    fun `countPerLine calculated correctly`() {
        val helper = SpanItemMarginHelper(
            marginStart = 16,
            marginEnd = 16,
            itemSpace = 8,
            parentWidth = 1080,
            managerSpanCount = 4,
            materialWidthHeightRatio = 1.0f,
            spanCount = 2
        )
        assertEquals(2, helper.countPerLine)
    }

    @Test
    fun `single column layout has correct width`() {
        val helper = SpanItemMarginHelper(
            marginStart = 20,
            marginEnd = 20,
            itemSpace = 0,
            parentWidth = 1080,
            managerSpanCount = 1,
            materialWidthHeightRatio = 1.5f
        )
        assertEquals(1, helper.countPerLine)
        // width = (1080 - 20 - 20 - 0) / 1 = 1040
        assertEquals(1040, helper.materialWidth)
        // height = 1040 * 1.5 = 1560
        assertEquals(1560, helper.materialHeight)
    }

    @Test
    fun `two column layout has correct width`() {
        val helper = SpanItemMarginHelper(
            marginStart = 16,
            marginEnd = 16,
            itemSpace = 8,
            parentWidth = 1080,
            managerSpanCount = 2,
            materialWidthHeightRatio = 1.0f
        )
        assertEquals(2, helper.countPerLine)
        // allSpace = 16 + 16 + 8 = 40
        // width = (1080 - 40) / 2 = 520
        assertEquals(520, helper.materialWidth)
        assertEquals(520, helper.materialHeight)
    }

    @Test
    fun `three column layout margin sum is consistent`() {
        val helper = SpanItemMarginHelper(
            marginStart = 12,
            marginEnd = 12,
            itemSpace = 6,
            parentWidth = 900,
            managerSpanCount = 3,
            materialWidthHeightRatio = 1.0f
        )
        assertEquals(3, helper.countPerLine)

        // Verify margins exist for all columns
        for (i in 0 until helper.countPerLine) {
            val (left, right) = helper.getMargin(i)
            assertTrue("left margin should be >= 0", left >= 0)
            assertTrue("right margin should be >= 0", right >= 0)
        }
    }

    @Test
    fun `first column starts with marginStart`() {
        val helper = SpanItemMarginHelper(
            marginStart = 20,
            marginEnd = 20,
            itemSpace = 10,
            parentWidth = 1080,
            managerSpanCount = 2,
            materialWidthHeightRatio = 1.0f
        )
        val (left, _) = helper.getMargin(0)
        assertEquals(20, left)
    }

    @Test
    fun `last column ends with marginEnd`() {
        val helper = SpanItemMarginHelper(
            marginStart = 20,
            marginEnd = 20,
            itemSpace = 10,
            parentWidth = 1080,
            managerSpanCount = 2,
            materialWidthHeightRatio = 1.0f
        )
        val (_, right) = helper.getMargin(1)
        assertEquals(20, right)
    }

    @Test
    fun `rtl layout mirrors margins`() {
        val ltrHelper = SpanItemMarginHelper(
            marginStart = 10,
            marginEnd = 30,
            itemSpace = 8,
            parentWidth = 1080,
            managerSpanCount = 2,
            materialWidthHeightRatio = 1.0f,
            isRtl = false
        )
        val rtlHelper = SpanItemMarginHelper(
            marginStart = 10,
            marginEnd = 30,
            itemSpace = 8,
            parentWidth = 1080,
            managerSpanCount = 2,
            materialWidthHeightRatio = 1.0f,
            isRtl = true
        )

        // RTL column 0 should have mirrored margins compared to LTR column 1
        val (ltrLastLeft, ltrLastRight) = ltrHelper.getMargin(1)
        val (rtlFirstLeft, rtlFirstRight) = rtlHelper.getMargin(0)
        assertEquals(ltrLastRight, rtlFirstLeft)
        assertEquals(ltrLastLeft, rtlFirstRight)
    }

    @Test(expected = IndexOutOfBoundsException::class)
    fun `getMargin throws for invalid index`() {
        val helper = SpanItemMarginHelper(
            marginStart = 16,
            marginEnd = 16,
            itemSpace = 8,
            parentWidth = 1080,
            managerSpanCount = 2,
            materialWidthHeightRatio = 1.0f
        )
        helper.getMargin(2) // only 0 and 1 are valid
    }

    @Test(expected = IndexOutOfBoundsException::class)
    fun `getMargin throws for negative index`() {
        val helper = SpanItemMarginHelper(
            marginStart = 16,
            marginEnd = 16,
            itemSpace = 8,
            parentWidth = 1080,
            managerSpanCount = 2,
            materialWidthHeightRatio = 1.0f
        )
        helper.getMargin(-1)
    }

    @Test
    fun `spanCount greater than 1 reduces countPerLine`() {
        val helper = SpanItemMarginHelper(
            marginStart = 0,
            marginEnd = 0,
            itemSpace = 0,
            parentWidth = 1200,
            managerSpanCount = 6,
            materialWidthHeightRatio = 1.0f,
            spanCount = 3
        )
        assertEquals(2, helper.countPerLine)
        // width = 1200 / 2 = 600
        assertEquals(600, helper.materialWidth)
    }

    @Test
    fun `zero margins and spacing gives full width per item`() {
        val helper = SpanItemMarginHelper(
            marginStart = 0,
            marginEnd = 0,
            itemSpace = 0,
            parentWidth = 1000,
            managerSpanCount = 1,
            materialWidthHeightRatio = 1.0f
        )
        assertEquals(1000, helper.materialWidth)
        assertEquals(1000, helper.materialHeight)
    }

    @Test
    fun `data class equality works`() {
        val h1 = SpanItemMarginHelper(10, 10, 5, 1080, 2, 1.0f)
        val h2 = SpanItemMarginHelper(10, 10, 5, 1080, 2, 1.0f)
        assertEquals(h1, h2)
    }
}

