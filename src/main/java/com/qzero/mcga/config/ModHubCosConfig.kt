package com.qzero.mcga.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
data class ModHubCosConfig(
    @param:Value("\${modhub.cos.secret-id}")
    val secretId: String,
    @param:Value("\${modhub.cos.secret-key}")
    val secretKey: String,
    @param:Value("\${modhub.cos.region}")
    val region: String,
    @param:Value("\${modhub.cos.bucket-name}")
    val bucketName: String,
)
