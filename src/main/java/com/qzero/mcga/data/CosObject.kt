package com.qzero.mcga.data

import jakarta.persistence.Embeddable

@Embeddable
data class CosObject(
    var cosKey: String,
    var fileSize: Long,
) {
    constructor(): this("", 0L)
}
