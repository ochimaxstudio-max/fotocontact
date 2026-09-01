package com.fotocontact.app.overlay

object MessageBuffer {

    class Item(
        val key: String,
        val name: String,
        val line: String,
        val photoPath: String?,
        var time: Long
    )

    private val items = mutableListOf<Item>()

    @Synchronized
    fun addOrUpdate(item: Item) {
        val idx = items.indexOfFirst { it.key == item.key }
        if (idx >= 0) items.removeAt(idx)
        items.add(0, item)
        while (items.size > 8) items.removeAt(items.size - 1)
    }

    @Synchronized
    fun snapshot(): List<Item> = items.toList()

    @Synchronized
    fun isEmpty(): Boolean = items.isEmpty()

    @Synchronized
    fun clear() = items.clear()

    @Synchronized
    fun removeByKey(key: String) {
        items.removeAll { it.key == key }
    }
}
