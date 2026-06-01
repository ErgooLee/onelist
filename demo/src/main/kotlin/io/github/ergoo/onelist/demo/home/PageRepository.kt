package io.github.ergoo.onelist.demo.home

import io.github.ergoo.onelist.demo.home.floor.FloorTitle
import io.github.ergoo.onelist.demo.home.horizontal.HorizontalVideo
import io.github.ergoo.onelist.demo.home.vertical.VerticalVideo
import kotlinx.coroutines.delay
import java.io.IOException
import kotlin.jvm.Throws
import kotlin.random.Random

class PageRepository {

    private val random = Random(System.currentTimeMillis())

    private var refreshTimes = 0

    // --- Realistic English Content Assets ---
    private val videoTitles = listOf(
        "Mastering Kotlin Coroutines: From Zero to Hero",
        "2024 Flagship Smartphone Review: Is it Worth It?",
        "VLOG: A Peaceful Weekend in the Swiss Alps",
        "Why Senior Developers are Switching to AI Engineering",
        "10 Hidden Android Features You Didn't Know Existed",
        "Street Food Tour: Discovering Secret Gems in Tokyo",
        "Late Night Kitchen: Easy 15-Minute Beef Bowl Recipe",
        "The Future of Mobile Development in 2025",
        "How I Built a Startup with No-Code Tools",
        "Everything New in Jetpack Compose This Year"
    )

    private val authorNames = listOf("TechPulse", "PixelPerfect", "FoodieExplorer", "CodeWithMe", "TravelJournal", "DailyDev")

    @Throws(IOException::class)
    suspend fun refresh(): PageResult {
        delay(800)

        if (refreshTimes++ % 2 == 0) {
            throw IOException("Network error ${System.currentTimeMillis()}")
        }

        return PageResult(
            items = generateItems(0), hasMore = true
        )
    }

    @Throws(IOException::class)
    suspend fun loadMore(nextId: Int): PageResult {
        delay(500)
        if (random.nextDouble() < 0.33) {
            throw IOException("Network error ${System.currentTimeMillis()}")
        }
        return PageResult(
            items = generateItems(nextId), hasMore = nextId < 100
        )
    }

    private fun generateItems(nextId: Int): List<HomeChannelItem> {

        val page = nextId / 20

        val floorTitle = FloorTitle(
            id = nextId.toLong(),
            title = if (page == 0) "Recommended for You" else "Editor's Choice - Page ${page + 1}",
        )

        val horizontalVideos = generateHorizontalVideo(page * 20 + 1, page * 20 + 10)

        val verticalVideo = generateVerticalVideo(page * 20 + 11, page * 20 + 19)

        val items = mutableListOf<HomeChannelItem>()

        items.add(floorTitle)
        items.addAll(horizontalVideos)
        items.addAll(verticalVideo)

        return items
    }

    private fun generateVerticalVideo(
        startId: Int, endId: Int
    ): List<VerticalVideo> {
        return (startId until endId + 1).map { id ->
            val author = authorNames[id % authorNames.size]
            VerticalVideo(
                id = id.toLong(),
                title = videoTitles[id % videoTitles.size],
                subTitle = "$author • ${id % 99 + 1}K likes",
                floorIndex = id - startId,
                liked = id % 3 == 0,
            )
        }
    }

    private fun generateHorizontalVideo(
        startId: Int, endId: Int
    ): List<HorizontalVideo> {
        return (startId until endId + 1).map { id ->
            val author = authorNames[id % authorNames.size]
            HorizontalVideo(
                id = id.toLong(),
                title = videoTitles[id % videoTitles.size],
                subTitle = "$author • ${id % 59 + 1} mins ago",
                floorIndex = id - startId,
                liked = id % 2 == 0,
            )
        }
    }

    data class PageResult(
        val items: List<HomeChannelItem>,
        val hasMore: Boolean,
    )
}