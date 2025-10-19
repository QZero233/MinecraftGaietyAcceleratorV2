package com.qzero.mcga.exception;

import com.qzero.mcga.ResponseCodeList;
import com.qzero.mcga.utils.UUIDUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cglib.proxy.UndeclaredThrowableException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class ExceptionHandleController {

    private final Logger log= LoggerFactory.getLogger(getClass());

    @ExceptionHandler
    public Map<String,Object> handleException(HttpServletRequest request,
                                              Throwable e){
        return processException(e);
    }

    private Map<String,Object> processException(Throwable e){
        if(e instanceof UndeclaredThrowableException){
            Throwable undeclared=((UndeclaredThrowableException) e).getUndeclaredThrowable();
            if(undeclared!=null && !(undeclared instanceof UndeclaredThrowableException))
                return processException(undeclared);
        }

        Map<String,Object> resultMap=new HashMap<>();

        if(e instanceof ResponsiveException){
            resultMap.put("responseCode", ResponseCodeList.KNOWN_ERROR);
            resultMap.put("errorCode",((ResponsiveException) e).getErrorCode());
            resultMap.put("errorMessage",((ResponsiveException) e).getErrorMessage());
        }else if(e instanceof HttpMessageNotReadableException){
            //Missing parameter
            resultMap.put("responseCode", ResponseCodeList.KNOWN_ERROR);
            resultMap.put("errorCode", -1);
            resultMap.put("errorMessage","Missing parameter, please check your input");
        }else{
            if(!(e instanceof IOException)){
                String unknownErrorId= UUIDUtils.getRandomUUID();
                //Unknown error
                resultMap.put("responseCode", ResponseCodeList.UNKNOWN_ERROR);
                resultMap.put("errorCode", -1);
                resultMap.put("errorMessage","Unknown error occurs, please contact admin, error log ID : "+unknownErrorId);
                log.error("Unknown error #{}", unknownErrorId, e);
            }
        }

        return resultMap;
    }

}
