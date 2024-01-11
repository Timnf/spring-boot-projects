package com.lou.springboot.controller;

import com.alibaba.fastjson.JSON;
import org.apache.commons.codec.Charsets;
import org.apache.commons.codec.binary.Hex;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.jws.soap.SOAPBinding;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;


@Controller
@RequestMapping("/track")
public class ActivateController {

    @Value("${huawei.action-upload-url}")
    private String actionUploadUrl;

    @Value("${huawei.secret-key}")
    private String secretKey;

    @Value("${kuaishou.adid}")
    private String adid;


    @GetMapping("/activate")
    public ResponseEntity<?> handleActivateRequest(
            @RequestParam String callback,
//            @RequestParam String actionType
            @RequestParam int actionType,
            HttpServletRequest request
    ) {

        // 获取请求时间
        Date requestTime = new Date(System.currentTimeMillis());

        // 格式化请求时间 -- 转化时间代确认？
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String formattedRequestTime = dateFormat.format(requestTime);

        // 打印请求时间到控制台
        logger.info("请求时间：{}", formattedRequestTime);

//        String actionType = extractActionTypeFromCallback(callback);

        //打印请求内容
//        System.out.println("Request URL: " + request.getRequestURL());
//        System.out.println("Request Method: " + request.getMethod());

        logger.info("Request URL: {}", request.getRequestURL());
        logger.info("Request Method: {}", request.getMethod());

        // 打印请求参数
        Map<String, String[]> parameterMap = request.getParameterMap();
        parameterMap.forEach((paramName, paramValues) ->
                logger.info("Parameter: {} = {}", paramName, String.join(", ", paramValues))
        );

        //打印请求参数
//        Map<String, String[]> parameterMap = request.getParameterMap();
//        for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
//            String paramName = entry.getKey();
//            String[] paramValues = entry.getValue();
//            System.out.println("Parameter: " + paramName + "= " + String.join(", ", paramValues));
//        }

        UserActionType userActionType = UserActionType.getByCode(actionType);
        if (userActionType == UserActionType.INVALID) {
            //构建响应体
            ApiResponse apiResponse = new ApiResponse(-1, "error actionType", new ApiData(userActionType.getDescription()));
            //返回 ResponseEntity
            return new ResponseEntity<>(apiResponse, HttpStatus.PRECONDITION_FAILED);
        }

        //处理业务逻辑
        //返回是否需要和上传的返回做处理，
        UpLoad(adid, userActionType.getDescription(), callback, formattedRequestTime);
        System.out.println("actionType is:" + actionType);

        //构建响应体
        ApiResponse apiResponse = new ApiResponse(0, "", new ApiData(userActionType.getDescription()));

        //返回 ResponseEntity
        return new ResponseEntity<>(apiResponse, HttpStatus.OK);
    }

    public enum UserActionType {
        //华为对应的conversion_type
        NEW(1, "activate"),
        NEXT_DAY_RETENTION(2, "retain"),
        LOSS_RECOVERY(4, "流失回流"),

        INVALID(-1, "无效类型");

        private final int code;
        private final String description;

        UserActionType(int code, String description) {
            this.code = code;
            this.description = description;
        }

        public int getCode() {
            return code;
        }

        public String getDescription() {
            return description;
        }

        public static UserActionType getByCode(int code) {
            for (UserActionType actionType : values()) {
                if (actionType.code == code) {
                    return actionType;
                }
            }

            return INVALID;
//            throw new IllegalArgumentException("Invalid UserActionType code: " + code);
        }
    }


    private String extractActionTypeFromCallback(String callback) {
        //实现根据callback 解析 actionType  的逻辑
        if (callback != null && callback.contains("actionType=")) {
            int startIndex = callback.indexOf("actionType=") + "actionType=".length();
            int endIndex = callback.indexOf("&", startIndex);
            if (endIndex == -1) {
                return callback.substring(startIndex);
            } else {
                return callback.substring(startIndex, endIndex);
            }
        }

        return "error actionType";
    }

    // 内部类， 用于构建规范的响应体
    private static class ApiResponse {
        private int ret;
        private String msg;
        private ApiData data;

        public ApiResponse(int ret, String msg, ApiData data) {
            this.ret = ret;
            this.msg = msg;
            this.data = data;
        }

        public int getRet() {
            return ret;
        }

        public String getMsg() {
            return msg;
        }

        public ApiData getData() {
            return data;
        }
    }

    //内部类，用于构建规范的data 部分
    private static class ApiData {
        private String actionType;

        public ApiData(String actionType) {
            this.actionType = actionType;
        }

        public String getActionType() {
            return actionType;
        }
    }

    /////////////////////////////////////////huaweiclient////////////////////////////////////////

//    private static final String SECRET_KEY = "Wpjr1xxxxxxxxxxxxxxx==";
//    private static final String ACTION_UPLOAD_URL = "https://ppscrowd-drcn.op.cloud.huawei.com/action-lib-track/hiad/v2/actionupload";

    public String UpLoad(String adid, String actionType, String callback, String conversion_time) {
        final HttpPost httpPost = new HttpPost(actionUploadUrl);
        // final String postBodyJson = buildPostBody(adid, actionType, callback);
        final String postBodyJson = buildPostBody(callback, conversion_time, actionType);
        logger.info("postBodyJson: {}", postBodyJson);

        final StringEntity entity = new StringEntity(postBodyJson, ContentType.APPLICATION_JSON);
        httpPost.setEntity(entity);
        // 根据密钥和请求体JSON字符串构造鉴权签名
        final String authSign = buildAuthorizationHeader(postBodyJson, secretKey);
        httpPost.addHeader("Authorization", authSign);
        // 发起POST请求
        try (CloseableHttpClient httpclient = HttpClients.createDefault();
             CloseableHttpResponse response = httpclient.execute(httpPost)) {
            String responseStr = EntityUtils.toString(response.getEntity());
            logger.info("responseStr: {}", responseStr);
            System.out.println(responseStr);
        } catch (IOException e) {
            logger.error("post request error!");
            e.printStackTrace();
        }

        return "test ok";
    }


    /**
     * 构造post请求体
     * callback,campaign_id,conversion_type,conversion_time,timestamp,request_id为必传字段
     *
     * @return 请求体
     */
    private static String buildPostBody(String callback, String conversion_time, String conversion_type) {
        final Map<String, String> body = new HashMap<>(16);
        body.put("callback",callback);
//        body.put("content_id", "45040150");
//        body.put("campaign_id", "45040505");
//        body.put("oaid", "7b777eeb-e9e6-12ab-bfde-e2789fb6b29");
//        body.put("tracking_enabled", "1");
//        body.put("ip", "192.168.1.1");
        body.put("conversion_type", conversion_type);
        body.put("conversion_time", conversion_time);
        body.put("timestamp", String.valueOf(System.currentTimeMillis()));
//        body.put("conversion_count", "1");
//        body.put("conversion_price", "1.00");
        return JSON.toJSONString(body);
    }

    /**
     * 计算请求头中的Authorization
     *
     * @param body 请求体json
     * @param key  密钥
     * @return Authorization 鉴权头
     */
    public static String buildAuthorizationHeader(String body, String key) {
        // 广告主请求头header中的Authorization
        final String authorizationFormat = "Digest validTime=\"{0}\", response=\"{1}\"";
        String authorization = null;
        try {
            byte[] keyBytes = key.getBytes(Charsets.UTF_8);
            byte[] bodyBytes = body.getBytes(Charsets.UTF_8);
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKey secretKey = new SecretKeySpec(keyBytes, "HmacSHA256");
            mac.init(secretKey);
            byte[] signatureBytes = mac.doFinal(bodyBytes);
            final String timestamp = String.valueOf(System.currentTimeMillis());
            final String signature = (signatureBytes == null) ? null : Hex.encodeHexString(signatureBytes);
            authorization = MessageFormat.format(authorizationFormat, timestamp, signature);
        } catch (Exception e) {
            System.err.println("build Authorization Header failed！");
            e.printStackTrace();
        }
        logger.info("generate Authorization Header: {}", authorization);
        System.out.println("generate Authorization Header: " + authorization);
        return authorization;
    }

    private static final Logger logger = LoggerFactory.getLogger(ActivateController.class);
}
