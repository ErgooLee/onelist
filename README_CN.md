# OneList

一个轻量级的 Android RecyclerView Adapter 框架，简化多类型列表、Diff 更新、分页加载、下拉刷新等常见场景的开发。

## 特性

- 🧩 **多类型列表** — 通过 `ListBinder` 机制，每种数据类型对应一个 Binder，轻松构建复杂的多类型列表
- 🔄 **DiffUtil 支持** — 内置 `DifferAdapter` / `DifferMergeAdapter`，基于 `AsyncListDiffer` 自动在后台线程计算差异并高效更新
- 📄 **Paging 3 集成** — `OneListPagingAdapter` 无缝对接 Jetpack Paging 3，避免不必要的预加载触发
- ⬇️ **加载更多** — `LoadMoreAdapter` 配合 `ConcatAdapter` 实现底部/顶部分页加载，支持预加载阈值和滚动方向检测
- 🔃 **下拉刷新** — `RefreshAdapter` + `RefreshView` 提供完整的下拉刷新 UI 和生命周期管理
- 👆 **点击事件** — 统一的 Item 级和子 View 级点击/长按事件管理，在 View attach/detach 时绑定/解绑，避免内存泄漏
- 📐 **Grid & StaggeredGrid** — `OneListGridLayoutManager` + `Spannable` 接口支持自定义 span 和全宽 item
- 📎 **ViewBinding** — 提供 `BindingViewHolder`，直接持有 ViewBinding 实例
- 🎯 **单 Item 适配器** — `OneItemAdapter` 用于 Header、Footer、空状态、Loading 等条件显示的单条目场景

## 环境要求

- **minSdk** 28
- **compileSdk** 36
- AndroidX

## 核心类

| 类 | 说明 |
|---|---|
| `OneListAdapter` | 所有适配器的基类，提供点击事件、Span 支持等通用能力 |
| `MutableListAdapter` | 可变数据列表适配器，支持 add / remove / swap 等操作 |
| `DifferAdapter` | 基于 AsyncListDiffer 的单类型适配器 |
| `MergeAdapter` | 多类型适配器，通过 `ListBinder` 委托不同类型的创建和绑定 |
| `DifferMergeAdapter` | 多类型 + DiffUtil 适配器 |
| `ListBinder` | 多类型列表中单个类型的视图创建、绑定和事件处理委托 |
| `OneItemAdapter` | 0 或 1 个条目的适配器，适用于状态驱动的 Header/Footer |
| `OneListPagingAdapter` | Paging 3 集成适配器 |
| `LoadMoreAdapter` | 加载更多适配器，配合 ConcatAdapter 使用 |
| `RefreshAdapter` / `RefreshView` | 下拉刷新适配器和 UI 接口 |
| `BindingViewHolder` | 持有 ViewBinding 的 ViewHolder |
| `OneListGridLayoutManager` | 支持 Spannable 接口的 GridLayoutManager |

## 快速开始

### 单类型列表（DiffUtil）

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

// 使用
adapter.submitList(listOf(...))
adapter.itemClickListener = object : ClickListener<MyItem, BindingViewHolder<ItemBinding>> {
    override fun onClick(data: MyItem, view: View, holder: BindingViewHolder<ItemBinding>) { }
}
```

### 多类型列表

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

### 加载更多

```kotlin
val contentAdapter = MyAdapter()  // 实现 MainContentAdapter 接口
val loadMoreAdapter = MyLoadMoreAdapter()
loadMoreAdapter.preloadSize = 5

recyclerView.adapter = ConcatAdapter(contentAdapter, loadMoreAdapter)
```

## 项目结构

```
onelist/          # 核心库模块
demo/             # 示例应用
```

## License

见 [LICENSE](LICENSE) 文件。

