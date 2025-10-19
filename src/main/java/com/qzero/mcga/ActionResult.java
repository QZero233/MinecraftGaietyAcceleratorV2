package com.qzero.mcga;


import java.util.HashMap;
import java.util.Map;

public class ActionResult {

    private int responseCode;
    private int errorCode;
    private String errorMessage;
    private Map<String,Object> data;

    //For succeeded situations

    /**
     * 空的ActionResult构造函数是未定义事件，仅用于json解析框架，统一按照UNKNOWN_ERROR处理
     * 请使用带boolean参数的构造函数
     */
    @Deprecated
    public ActionResult() {
        responseCode = ResponseCodeList.UNKNOWN_ERROR;
    }

    public ActionResult(boolean success) {
        if (success) {
            responseCode= ResponseCodeList.OK;
        } else {
            responseCode = ResponseCodeList.UNKNOWN_ERROR;
        }
    }

    public ActionResult(Map<String, Object> data){
        this.responseCode= ResponseCodeList.OK;
        this.data=data;
    }

    public ActionResult(String name,Object data){
        Map<String,Object> dataMap=new HashMap<>();
        dataMap.put(name,data);

        this.data=dataMap;
        this.responseCode= ResponseCodeList.OK;
    }

    //For failed situations
    public ActionResult(int errorCode, String errorMessage, Map<String, Object> data) {
        this.responseCode= ResponseCodeList.KNOWN_ERROR;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.data = data;
    }

    public int getResponseCode() {
        return responseCode;
    }

    public void setResponseCode(int responseCode) {
        this.responseCode = responseCode;
    }

    public Map<String, Object> getData() {
        if (data == null) {
            return new HashMap<>();
        }
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public ActionResult addData(String key,Object value){
        if(data==null)
            data=new HashMap<>();
        data.put(key,value);

        return this;
    }

    @Override
    public String toString() {
        return "ActionResult{" +
                "responseCode=" + responseCode +
                ", errorCode=" + errorCode +
                ", errorMessage='" + errorMessage + '\'' +
                ", data=" + data +
                '}';
    }
}
