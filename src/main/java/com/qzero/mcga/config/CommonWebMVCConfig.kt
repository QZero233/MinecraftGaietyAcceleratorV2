package com.qzero.mcga.config

import com.qzero.mcga.interceptor.AuthInterceptor
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
open class CommonWebMVCConfig(
    private val authInterceptor: AuthInterceptor
): WebMvcConfigurer {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun addInterceptors(registry: InterceptorRegistry) {
        super.addInterceptors(registry)
        registry.addInterceptor(authInterceptor)
    }

}