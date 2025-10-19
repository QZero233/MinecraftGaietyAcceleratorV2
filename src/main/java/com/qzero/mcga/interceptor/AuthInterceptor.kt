package com.qzero.mcga.interceptor

import com.qzero.mcga.config.AuthConfig
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor

@Component
class AuthInterceptor(
    private val authConfig: AuthConfig
): HandlerInterceptor {

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        val auth = request.getHeader("Authorization") ?: request.getParameter("Authorization")
        if (auth == null) {
            response.outputStream.write("Get off!".toByteArray())
            response.outputStream.flush()
            return false
        } else {
            if (auth != authConfig.token) {
                response.outputStream.write("Get off!".toByteArray())
                response.outputStream.flush()
                return false
            }
        }
        return super.preHandle(request, response, handler)
    }

}