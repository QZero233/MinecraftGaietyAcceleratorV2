package com.qzero.mcga.service

import com.qcloud.cos.COSClient
import com.qcloud.cos.ClientConfig
import com.qcloud.cos.Headers
import com.qcloud.cos.auth.BasicCOSCredentials
import com.qcloud.cos.auth.COSCredentials
import com.qcloud.cos.http.HttpMethodName
import com.qcloud.cos.model.GeneratePresignedUrlRequest
import com.qcloud.cos.model.PutObjectRequest
import com.qcloud.cos.region.Region
import com.qzero.mcga.config.ModHubCosConfig
import com.qzero.mcga.utils.UUIDUtils
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.File
import java.util.Date

@Service
class CosService(
    private val cosConfig: ModHubCosConfig
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    private val client = createClient()

    private fun createClient(): COSClient {
        val cred: COSCredentials = BasicCOSCredentials(
            cosConfig.secretId,
            cosConfig.secretKey
        )

        val region = Region(cosConfig.region)
        val clientConfig = ClientConfig(region)
        val client = COSClient(cred, clientConfig)

        return client
    }

    private fun getObjectAccessUrl(key: String): String {
        val req = GeneratePresignedUrlRequest(cosConfig.bucketName, key, HttpMethodName.GET)

        req.expiration = Date(System.currentTimeMillis() + 3 * 60 * 1000) // 3分钟过期
        req.putCustomRequestHeader(Headers.HOST,
            client.getClientConfig().endpointBuilder.buildGeneralApiEndpoint(cosConfig.bucketName));
        val url = client.generatePresignedUrl(req, true).toExternalForm()
        return url
    }

    fun uploadFileSync(key: String, file: File): Boolean {
        val putObjectRequest = PutObjectRequest(cosConfig.bucketName, key, file)
        try {
            logger.debug("Begin to upload ${file.absolutePath} to ${cosConfig.bucketName} at $key")
            val putObjectResult = client.putObject(putObjectRequest)
            logger.debug("Upload ${file.absolutePath} finished with result ${putObjectResult.requestId}")
            return true
        } catch (e: Exception) {
            logger.error("Failed to upload ${file.absolutePath}", e)
            return false
        }
    }
}