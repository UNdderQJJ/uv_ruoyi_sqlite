package com.ruoyi.business.service.DataPool.type.Http;

import com.ruoyi.business.domain.DataPool.DataPool;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.business.domain.config.HttpSourceConfig;
import com.ruoyi.business.domain.config.ParsingRuleConfig;
import com.ruoyi.business.domain.config.TriggerConfig;
import com.ruoyi.business.service.DataPool.DataPoolConfigFactory;
import com.ruoyi.business.service.DataPool.IDataPoolService;
import com.ruoyi.business.service.common.DataIngestionService;
import com.ruoyi.business.service.common.ParsingRuleEngineService;
import org.springframework.context.ApplicationEventPublisher;
import com.ruoyi.business.events.ConnectionStateChangedEvent;
import com.ruoyi.business.enums.ConnectionState;
import com.ruoyi.business.enums.PoolStatus;
import okhttp3.*;
import okhttp3.FormBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * HTTP 提供者
 * 负责发送HTTP请求、接收响应、解析数据并入库
 */
public class HttpProvider {
    
    private static final Logger log = LoggerFactory.getLogger(HttpProvider.class);
    
    private final Long poolId;
    private final IDataPoolService dataPoolService;
    private final DataPoolConfigFactory configFactory;
    private final DataIngestionService dataIngestionService;
    private final ParsingRuleEngineService parsingRuleEngineService;
    private final ApplicationEventPublisher eventPublisher;
    
    private volatile HttpSourceConfig sourceConfig;
    private volatile TriggerConfig triggerConfig;
    private volatile ParsingRuleConfig parsingRuleConfig;
    
    // HTTP客户端
    private final OkHttpClient httpClient;
    
    // 防止重复请求
    private final AtomicBoolean requestInProgress = new AtomicBoolean(false);
    
    public HttpProvider(Long poolId,
                        IDataPoolService dataPoolService,
                        DataPoolConfigFactory configFactory,
                        DataIngestionService dataIngestionService,
                        ParsingRuleEngineService parsingRuleEngineService,
                        ApplicationEventPublisher eventPublisher) {
        this.poolId = poolId;
        this.dataPoolService = dataPoolService;
        this.configFactory = configFactory;
        this.dataIngestionService = dataIngestionService;
        this.parsingRuleEngineService = parsingRuleEngineService;
        this.eventPublisher = eventPublisher;
        
        // 初始化HTTP客户端
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        
        reloadConfigs();
    }
    
    /**
     * 重新加载数据池配置（源、触发、解析）
     */
    public synchronized void reloadConfigs() {
        DataPool pool = dataPoolService.selectDataPoolById(poolId);
        if (pool == null) {
            log.warn("[HttpProvider] 数据池不存在: {}", poolId);
            return;
        }
        try {
            this.sourceConfig = (HttpSourceConfig) configFactory.parseSourceConfig("HTTP", pool.getSourceConfigJson());
        } catch (Exception e) {
            log.error("解析HTTP配置失败: {}", e.getMessage(), e);
        }
        try {
            this.triggerConfig = configFactory.parseTriggerConfig(pool.getTriggerConfigJson());
        } catch (Exception e) {
            log.error("解析触发配置失败: {}", e.getMessage(), e);
        }
        try {
            this.parsingRuleConfig = configFactory.parseParsingRuleConfig(pool.getParsingRuleJson());
        } catch (Exception e) {
            log.error("解析规则配置失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 发送HTTP请求（由调度器触发）
     */
    public void requestData() {
        // 检查数据池状态
        DataPool latest = dataPoolService.selectDataPoolById(poolId);
        if (latest == null || !PoolStatus.RUNNING.getCode().equals(latest.getStatus())) {
            log.debug("[HttpProvider] 数据池非运行状态，跳过请求: poolId={}", poolId);
            return;
        }
        
        // 验证配置
        if (sourceConfig == null || sourceConfig.getUrl() == null || sourceConfig.getUrl().trim().isEmpty()) {
            log.error("[HttpProvider] HTTP配置不完整，无法发送请求: poolId={}", poolId);
            return;
        }
        
        // 防止重复请求
        if (!requestInProgress.compareAndSet(false, true)) {
            log.debug("[HttpProvider] 请求正在进行中，跳过本次触发: poolId={}", poolId);
            return;
        }
        
        try {
            // 更新连接状态
//            updateConnectionState(ConnectionState.CONNECTING);

            // 构建HTTP请求
            Request request = buildHttpRequest();
            if (request == null) {
                log.error("[HttpProvider] 构建HTTP请求失败: poolId={}", poolId);
                return;
            }

            log.info("[HttpProvider] 发送HTTP请求: {} {}, poolId={}",
                    sourceConfig.getMethod(), sourceConfig.getUrl(), poolId);
            
            // 异步发送请求
            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    log.error("[HttpProvider] HTTP请求失败: poolId={}, error={}", poolId, e.getMessage());
                    dataPoolService.updateDataPoolStatus(poolId,PoolStatus.ERROR.getCode());
                    updateConnectionState(ConnectionState.ERROR);
                    requestInProgress.set(false);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try {
                        if (response.isSuccessful()) {
                            String responseBody = response.body().string();
                            log.debug("[HttpProvider] 收到HTTP响应: poolId={}, status={}, body={}",
                                    poolId, response.code(), responseBody);

                            // 处理响应数据
                            boolean success = processResponse(responseBody);
                            // 仅在成功处理时，置为 CONNECTED
                            if (success) {
                                updateConnectionState(ConnectionState.CONNECTED);
                            }
                        } else {
                            log.error("[HttpProvider] HTTP请求失败: poolId={}, status={}",
                                    poolId, response.code());
                            dataPoolService.updateDataPoolStatus(poolId,PoolStatus.ERROR.getCode());
                            updateConnectionState(ConnectionState.ERROR);
                        }
                    } catch (Exception e) {
                        log.error("[HttpProvider] 处理HTTP响应失败: poolId={}", poolId, e);
                        dataPoolService.updateDataPoolStatus(poolId,PoolStatus.ERROR.getCode());
                        updateConnectionState(ConnectionState.ERROR);
                    } finally {
                        response.close();
                        requestInProgress.set(false);
                    }
                }
            });

        } catch (Exception e) {
            log.error("[HttpProvider] 发送HTTP请求异常: poolId={}", poolId, e);
            dataPoolService.updateDataPoolStatus(poolId,PoolStatus.ERROR.getCode());
            updateConnectionState(ConnectionState.ERROR);
            requestInProgress.set(false);
        }
    }

    private void updateConnectionState(ConnectionState state) {
        try {
            dataPoolService.updateConnectionState(poolId, state.getCode());
            // 发布事件，交由调度器监听处理
            if (eventPublisher != null) {
                eventPublisher.publishEvent(new ConnectionStateChangedEvent(poolId, state));
            }
        } catch (Exception ignore) {
        }
    }
    
    /**
     * 构建HTTP请求
     */
    private Request buildHttpRequest() {
        if (sourceConfig == null) {
            return null;
        }
        
        // 处理URL：路径模板替换 + GET 查询参数构建
        String rawUrl = sourceConfig.getUrl();
        List<HttpSourceConfig.HttpBody> bodyList = sourceConfig.getBody();
        java.util.Set<String> usedForPath = new java.util.HashSet<>();

        // 路径模板占位符 {var} 替换（GET/POST 等方法均生效）
        if (rawUrl != null && bodyList != null && !bodyList.isEmpty() && rawUrl.contains("{")) {
            StringBuilder replaced = new StringBuilder();
            int i = 0;
            while (i < rawUrl.length()) {
                char c = rawUrl.charAt(i);
                if (c == '{') {
                    int end = rawUrl.indexOf('}', i + 1);
                    if (end > i) {
                        String key = rawUrl.substring(i + 1, end);
                        String value = null;
                        for (HttpSourceConfig.HttpBody kv : bodyList) {
                            if (key.equals(kv.getKey())) {
                                value = kv.getValue();
                                usedForPath.add(key);
                                break;
                            }
                        }
                        if (value == null) {
                            log.warn("[HttpProvider] 路径模板变量缺失: {}，URL: {}，poolId={}", key, rawUrl, poolId);
                            value = ""; // 避免NPE，留空
                        }
                        replaced.append(value);
                        i = end + 1;
                        continue;
                    }
                }
                replaced.append(c);
                i++;
            }
            rawUrl = replaced.toString();
        }

        // 替换后URL校验与日志
        if (rawUrl != null && rawUrl.contains("{")) {
            log.error("[HttpProvider] URL 路径变量未完全替换，rawUrl={}，poolId={}", rawUrl, poolId);
            return null;
        }

        log.debug("[HttpProvider] 最终请求URL: {}，poolId={}", rawUrl, poolId);

        // 设置请求方法
        String method = sourceConfig.getMethod() != null ? sourceConfig.getMethod().toUpperCase() : "GET";

        Request.Builder builder;
        if ("GET".equals(method) && bodyList != null && !bodyList.isEmpty()) {
            // GET 将 body 视为查询参数（未用于路径变量的键）
            HttpUrl base = HttpUrl.parse(rawUrl);
            if (base != null) {
                HttpUrl.Builder httpUrlBuilder = base.newBuilder();
                for (HttpSourceConfig.HttpBody kv : bodyList) {
                    if (!usedForPath.contains(kv.getKey())) {
                        httpUrlBuilder.addQueryParameter(kv.getKey(), kv.getValue());
                    }
                }
                builder = new Request.Builder().url(httpUrlBuilder.build());
            } else {
                // URL 不合法则直接使用原字符串
                builder = new Request.Builder().url(rawUrl);
            }
        } else {
            builder = new Request.Builder().url(rawUrl);
        }
        
        // 设置请求头
        if (sourceConfig.getHeaders() != null) {
            for (HttpSourceConfig.HttpHeader header : sourceConfig.getHeaders()) {
                builder.addHeader(header.getKey(), header.getValue());
            }
        }
        
        // 设置请求体（支持 List<HttpBody>）
        if ("POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method)) {
            RequestBody requestBody = null;
            // 读取 Content-Type，默认 application/json
            String contentType = "application/json; charset=utf-8";
            if (sourceConfig.getHeaders() != null) {
                for (HttpSourceConfig.HttpHeader header : sourceConfig.getHeaders()) {
                    if ("Content-Type".equalsIgnoreCase(header.getKey())) {
                        contentType = header.getValue();
                        break;
                    }
                }
            }

            if (sourceConfig.getBody() != null && !sourceConfig.getBody().isEmpty()) {
                if (contentType.toLowerCase().startsWith("application/json")) {
                    // 构建 JSON 对象
                    JSONObject json = new JSONObject();
                    for (HttpSourceConfig.HttpBody kv : sourceConfig.getBody()) {
                        json.put(kv.getKey(), kv.getValue());
                    }
                    requestBody = RequestBody.create(JSON.toJSONString(json), MediaType.parse(contentType));
                } else if (contentType.toLowerCase().startsWith("application/x-www-form-urlencoded")) {
                    // 表单编码
                    FormBody.Builder formBuilder = new FormBody.Builder();
                    for (HttpSourceConfig.HttpBody kv : sourceConfig.getBody()) {
                        formBuilder.add(kv.getKey(), kv.getValue());
                    }
                    requestBody = formBuilder.build();
                } else {
                    // 其他类型，按纯文本拼接 key=value&key2=value2
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < sourceConfig.getBody().size(); i++) {
                        HttpSourceConfig.HttpBody kv = sourceConfig.getBody().get(i);
                        sb.append(kv.getKey()).append("=").append(kv.getValue());
                        if (i < sourceConfig.getBody().size() - 1) sb.append("&");
                    }
                    requestBody = RequestBody.create(sb.toString(), MediaType.parse(contentType));
                }
            } else {
                // 空请求体
                requestBody = RequestBody.create("", null);
            }
            builder.method(method, requestBody);
        } else {
            builder.method(method, null);
        }
        
        return builder.build();
    }
    
    /**
     * 处理HTTP响应
     * @return 是否处理成功（成功则可将连接状态置为 CONNECTED）
     */
    private boolean processResponse(String responseBody) {
        if (parsingRuleConfig == null) {
            log.error("[HttpProvider] 解析规则配置为空，无法处理响应: poolId={}", poolId);
            return false;
        }
        
        try {
            // 先检查响应内容，识别错误响应
            if (responseBody != null && responseBody.contains("\"code\"")) {
                try {
                    // 尝试解析JSON响应，检查是否有错误码
                    JSONObject jsonResponse = JSON.parseObject(responseBody);
                    Integer code = jsonResponse.getInteger("code");
                    String msg = jsonResponse.getString("msg");
                    
                    if (code != null && code != 200) {
                        // 发现错误响应，记录详细日志
                        String errorType = getErrorType(code);
                        log.warn("[HttpProvider] HTTP请求返回错误响应: poolId={}, code={}, type={}, message={}, responseBody={}", 
                                poolId, code, errorType, msg, responseBody);
                        
                        // 更新数据池连接状态为错误状态
                        dataPoolService.updateDataPoolStatus(poolId, PoolStatus.ERROR.getCode());
                        updateConnectionState(ConnectionState.ERROR);
                        return false; // 不进行数据解析
                    }
                } catch (Exception jsonParseException) {
                    // JSON解析失败，继续正常流程
                    log.debug("[HttpProvider] 响应不是标准JSON格式，继续正常处理: poolId={}", poolId);
                }
            }

            
            // 正常响应，进行数据解析
            log.debug("[HttpProvider] 开始解析HTTP响应数据: poolId={}, responseLength={}", poolId, 
                    responseBody != null ? responseBody.length() : 0);
            
            List<String> items = parsingRuleEngineService.extractItems(responseBody, parsingRuleConfig);
            if (items == null || items.isEmpty()) {
                log.debug("[HttpProvider] 解析结果为空: poolId={}", poolId);
                return true; // 空结果视为成功但无数据
            }
            
            // 数据入库
            dataIngestionService.ingestItems(poolId, items);
            
            log.info("[HttpProvider] 数据处理完成: poolId={}, 解析出 {} 条记录", poolId, items.size());
            return true;
        } catch (Exception e) {
            log.error("[HttpProvider] 处理响应数据失败: poolId={}", poolId, e);
            // 解析失败时也更新连接状态为错误
             dataPoolService.updateDataPoolStatus(poolId, PoolStatus.ERROR.getCode());
            updateConnectionState(ConnectionState.ERROR);
            return false;
        }
    }
    
    /**
     * 检查是否正在请求中
     */
    public boolean isRequestInProgress() {
        return requestInProgress.get();
    }
    
    /**
     * 启动前的同步连通性校验（立即返回结果，不依赖调度器）
     */
    public boolean testConnection() {
        try {
            if (sourceConfig == null || sourceConfig.getUrl() == null || sourceConfig.getUrl().trim().isEmpty()) {
                log.error("[HttpProvider] testConnection 配置不完整: poolId={}", poolId);
                return false;
            }
            Request request = buildHttpRequest();
            if (request == null) {
                log.error("[HttpProvider] testConnection 构建请求失败: poolId={}", poolId);
                return false;
            }
            try (Response response = httpClient.newCall(request).execute()) {
                String body = response.body() != null ? response.body().string() : "";
                log.debug("[HttpProvider] testConnection 响应: poolId={}, status={}, body={}", poolId, response.code(), body);
                if (!response.isSuccessful()) {
                    return false;
                }
                // 复用错误识别逻辑：若返回体中包含业务 code 且不为 200，则视为失败
                if (body != null && body.contains("\"code\"")) {
                    try {
                        JSONObject json = JSON.parseObject(body);
                        Integer code = json.getInteger("code");
                        if (code != null && code != 200) {
                            return false;
                        }
                    } catch (Exception ignore) {
                    }
                }
                return true;
            }
        } catch (Exception ex) {
            log.warn("[HttpProvider] testConnection 异常: poolId={}, err={}", poolId, ex.getMessage());
            return false;
        }
    }
    
    /**
     * 关闭HTTP客户端
     */
    public void close() {
        if (httpClient != null) {
            httpClient.dispatcher().executorService().shutdown();
            httpClient.connectionPool().evictAll();
        }
        dataPoolService.updateConnectionState(poolId, ConnectionState.DISCONNECTED.getCode());
    }
    
    /**
     * 获取数据池ID
     */
    public Long getPoolId() {
        return poolId;
    }
    
    /**
     * 根据HTTP状态码获取错误类型描述
     */
    private String getErrorType(Integer code) {
        if (code == null) return "未知错误";
        
        switch (code) {
            case 400: return "请求参数错误";
            case 401: return "认证失败";
            case 403: return "权限不足";
            case 404: return "资源不存在";
            case 500: return "服务器内部错误";
            case 502: return "网关错误";
            case 503: return "服务不可用";
            case 504: return "网关超时";
            default: return "其他错误";
        }
    }
}
