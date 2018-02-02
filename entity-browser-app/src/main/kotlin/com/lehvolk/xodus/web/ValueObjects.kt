package com.lehvolk.xodus.web

import java.util.*

interface Named {
    var name: String
}

data class PropertyType(
        var readonly: Boolean = false,
        var clazz: String,
        var displayName: String
)

data class EntityProperty(
        override var name: String,
        var type: PropertyType,
        var value: String? = null
) : Named

data class EntityLink(
        override var name: String,
        var typeId: Int,
        var type: String,
        var label: String,
        var entityId: Long = 0
) : Named

data class LinkPager(
        override var name: String,
        var skip: Int = 0,
        var top: Int = 100,
        var totalSize: Long = 0L,
        var entities: List<EntityLink> = emptyList()
) : Named

data class EntityBlob(
        override var name: String,
        var blobSize: Long = 0) : Named

data class EntityView(
        var id: String,
        var type: String,
        var label: String,
        var typeId: String,
        var properties: List<EntityProperty> = emptyList(),
        var links: List<LinkPager> = emptyList(),
        var blobs: List<EntityBlob> = emptyList()
)

data class EntityType(var id: String?, var name: String)

data class SearchPager(val items: List<EntityView>, val totalCount: Long)

open class ChangeSummaryAction<T>(
        override var name: String,
        var newValue: T? = null
) : Named

open class PropertiesChangeSummaryAction(name: String, newValue: EntityProperty) : ChangeSummaryAction<EntityProperty>(name, newValue)
open class LinkChangeSummaryAction(
        name: String,
        newValue: EntityLink,
        var oldValue: EntityLink? = null,
        var totallyRemoved: Boolean = false
) : ChangeSummaryAction<EntityLink>(name, newValue)

open class BlobChangeSummaryAction(name: String, newValue: EntityBlob) : ChangeSummaryAction<EntityBlob>(name, newValue)

data class ChangeSummary(
        var properties: List<PropertiesChangeSummaryAction> = listOf(),
        var links: List<LinkChangeSummaryAction> = listOf(),
        var blobs: List<BlobChangeSummaryAction> = listOf()
)

data class DBSummary(
        var location: String,
        var key: String,
        var isOpened: Boolean = false,
        var uuid: String = UUID.randomUUID().toString()
)