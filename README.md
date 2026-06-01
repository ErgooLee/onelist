# OneList

A lightweight Android RecyclerView Adapter framework that simplifies common scenarios like multi-type lists, diff-based updates, pagination, and pull-to-refresh.

## Features

- 🧩 **Multi-Type Lists** — Use the `ListBinder` mechanism to map each data type to its own Binder, effortlessly building complex heterogeneous lists
- 🔄 **DiffUtil Support** — Built-in `DifferAdapter` / `DifferMergeAdapter` powered by `AsyncListDiffer` for automatic background diff computation and efficient updates
- 📄 **Paging 3 Integration** — `OneListPagingAdapter` seamlessly integrates with Jetpack Paging 3, avoiding unnecessary prefetch triggers
- ⬇️ **Load More** — `LoadMoreAdapter` works with `ConcatAdapter` for bottom/top pagination with configurable preload threshold and scroll direction detection
- 🔃 **Pull-to-Refresh** — `RefreshAdapter` + `RefreshView` provide a complete pull-to-refresh UI and lifecycle management
- 👆 **Click Handling** — Unified item-level and child-view click/long-click management, bound/unbound on view attach/detach to prevent memory leaks
- 📐 **Grid & StaggeredGrid** — `OneListGridLayoutManager` + `Spannable` interface for custom span sizes and full-span items
- 📎 **ViewBinding** — `BindingViewHolder` directly holds a ViewBinding instance
- 🎯 **Single-Item Adapter** — `OneItemAdapter` for conditionally displaying headers, footers, empty states, or loading indicators

## Requirements

- **minSdk** 28
- **compileSdk** 36
- AndroidX

## Core Classes

| Class | Description |
|---|---|
| `OneListAdapter` | Base class for all adapters, providing click events, span support, etc. |
| `MutableListAdapter` | Mutable data list adapter with add / remove / swap operations |
| `DifferAdapter` | Single-type adapter based on AsyncListDiffer |
| `MergeAdapter` | Multi-type adapter that delegates creation and binding to `ListBinder` instances |
| `DifferMergeAdapter` | Multi-type + DiffUtil adapter |
| `ListBinder` | Delegate for view creation, binding, and event handling of a single type in multi-type lists |
| `OneItemAdapter` | 0-or-1 item adapter for state-driven headers/footers |
| `OneListPagingAdapter` | Paging 3 integration adapter |
| `LoadMoreAdapter` | Load-more adapter, used with ConcatAdapter |
| `RefreshAdapter` / `RefreshView` | Pull-to-refresh adapter and UI interface |
| `BindingViewHolder` | ViewHolder that holds a ViewBinding instance |
| `OneListGridLayoutManager` | GridLayoutManager with Spannable interface support |

## Quick Start

### Single-Type List (DiffUtil)

```kotlin
class MyAdapter : DifferAdapter<MyItem, BindingViewHolder<ItemBinding>>(
    object : DiffUtil.ItemCallback<MyItem>() {
        override fun areItemsTheSame(old: MyItem, new: MyItem) = old.id == new.id
        override fun areContentsTheSame(old: MyItem, new: MyItem) = old == new
    }
) {
    override fun onCreateViewHolder(context: Context, parent: ViewGroup, viewType: Int) =
        BindingViewHolder(ItemBinding.inflate(LayoutInflater.from(context), parent, false))

    override fun onBindViewHolder(holder: BindingViewHolder<ItemBinding>, position: Int, item: MyItem) {
        holder.binding.title.text = item.title
    }
}

// Usage
adapter.submitList(listOf(...))
adapter.itemClickListener = object : ClickListener<MyItem, BindingViewHolder<ItemBinding>> {
    override fun onClick(data: MyItem, view: View, holder: BindingViewHolder<ItemBinding>) { }
}
```

### Multi-Type List

```kotlin
class MyMergeAdapter : DifferMergeAdapter() {
    init {
        addListBinder(HeaderBinder())
        addListBinder(ContentBinder())
    }
}

class HeaderBinder : ListBinder<HeaderData, BindingViewHolder<HeaderBinding>>() {
    override fun getViewType() = R.layout.item_header
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        BindingViewHolder(HeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    override fun convert(holder: BindingViewHolder<HeaderBinding>, data: HeaderData) {
        holder.binding.title.text = data.title
    }
}
```

### Load More

```kotlin
val contentAdapter = MyAdapter()  // implements MainContentAdapter
val loadMoreAdapter = MyLoadMoreAdapter()
loadMoreAdapter.preloadSize = 5

recyclerView.adapter = ConcatAdapter(contentAdapter, loadMoreAdapter)
```

## Project Structure

```
onelist/          # Core library module
demo/             # Sample application
```

## License

See the [LICENSE](LICENSE) file.
