package com.ruoyi.business.service.DeviceInfo.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.business.domain.DeviceFileConfig.DeviceFileConfig;
import com.ruoyi.business.domain.DeviceInfo.DeviceInfo;
import com.ruoyi.business.domain.DeviceInfo.params.DeviceParams;
import com.ruoyi.business.enums.DeviceConfigKey;
import com.ruoyi.business.enums.DeviceStatus;
import com.ruoyi.business.service.DeviceFileConfig.IDeviceFileConfigService;
import com.ruoyi.business.service.DeviceInfo.DeviceCommandService;
import com.ruoyi.business.service.DeviceInfo.IDeviceInfoService;
import com.ruoyi.business.utils.StxEtxProtocolUtil;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class DeviceCommandServiceImpl implements DeviceCommandService {

    private static final Logger log = LoggerFactory.getLogger(DeviceCommandServiceImpl.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Autowired
    private IDeviceInfoService deviceInfoService;
    
    @Autowired
    private IDeviceFileConfigService deviceFileConfigService;
    
    private static final int CONNECT_TIMEOUT = 3000; // 3秒连接超时
    private static final int READ_TIMEOUT = 2000;    // 2秒读取超时

    @Override
    public void applyParameters(Long deviceId, Map<DeviceConfigKey, Object> params) {
        if (deviceId == null) {
            throw new ServiceException("设备ID不能为空");
        }
        if (params == null || params.isEmpty()) {
            log.info("[DeviceCommand] deviceId={} no params to apply", deviceId);
            return;
        }
        params.forEach((k, v) -> applySingle(deviceId, k, v));
    }

    @Override
    public void sendPrintData(Long deviceId, Object setaParam) {
        applySingle(deviceId, DeviceConfigKey.SETA, setaParam);
    }

     /**
     * 将模版与实际数据合成为 SETA 指令参数结构。
     * 统一返回 Map 结构，便于直接传给 DeviceCommandServiceImpl 的 SETA 构建逻辑。
     *
     * @param text 文本内容（用于 v1）
     * @param width 宽度
     * @param height 高度
     * @param x X 坐标
     * @param y Y 坐标
     * @param r 旋转角度
     * @param ng 负向标志
     */
    @Override
    public Map<String, Object> buildSetaParam(String text, Integer width, Integer height, Integer x, Integer y, Integer r, Integer ng) {
        Map<String, Object> map = new HashMap<>();
        if (StringUtils.isNotEmpty(text)) {
            map.put("text", text);
        }
        if (width != null && height != null) {
            map.put("size", width + "|" + height);
        }
        if (x != null && y != null) {
            StringBuilder pos = new StringBuilder();
            pos.append(Objects.toString(x, "")).append("|").append(Objects.toString(y, ""));
            pos.append("|").append(Objects.toString(r, "0"));
            pos.append("|").append(Objects.toString(ng, "0"));
            map.put("pos", pos.toString());
        }
        return map;
    }

    @Override
    public void start(Long deviceId) {
        applySingle(deviceId, DeviceConfigKey.START, null);
    }

    @Override
    public void stop(Long deviceId) {
        applySingle(deviceId, DeviceConfigKey.STOP, null);
    }

    @Override
    public void triggerMark(Long deviceId) {
        applySingle(deviceId, DeviceConfigKey.TRIMARK, null);
    }

    @Override
    public String getSystemStatus(Long deviceId, boolean includeErrWarn) {
        String arg = includeErrWarn ? "errsta" : "";
        return sendAndRead(deviceId, buildCommand(deviceId, DeviceConfigKey.SYS_STA, arg, null));
    }

    @Override
    public Integer getBufferCount(Long deviceId) {
        String res = sendAndRead(deviceId, buildCommand(deviceId, DeviceConfigKey.GETA, null, null));
        if (res == null) return null;
        // 期望格式: geta:数量 → 解析冒号后数字
        int idx = res.indexOf(":");
        if (idx >= 0 && idx + 1 < res.length()) {
            try { return Integer.parseInt(res.substring(idx + 1).trim()); } catch (Exception ignore) {}
        }
        return null;
    }

    @Override
    public Integer getSerialCurrent(String serialName, boolean editMode, Long deviceId) {
        Map<String, Object> param = java.util.Collections.unmodifiableMap(new java.util.HashMap<String, Object>() {{
            put("name", serialName);
            put("edit", editMode ? "1" : "0");
        }});
        String res = sendAndRead(deviceId, buildCommand(deviceId, DeviceConfigKey.SNUM_INDEX, param, null));
        if (res == null) return null;
        int idx = res.indexOf(":");
        if (idx >= 0 && idx + 1 < res.length()) {
            try { return Integer.parseInt(res.substring(idx + 1).trim()); } catch (Exception ignore) {}
        }
        return null;
    }

    @Override
    public String getObjectText(String objectName, boolean editMode, Long deviceId) {
        Map<String, Object> param = java.util.Collections.unmodifiableMap(new java.util.HashMap<String, Object>() {{
            put("name", Objects.toString(objectName, ""));
            put("edit", editMode ? "1" : "0");
        }});
        return sendAndRead(deviceId, buildCommand(deviceId, DeviceConfigKey.GET_TEXTDATA, param, objectName));
    }

    @Override
    public Integer getProcessedCount(Long deviceId) {
        String res = sendAndRead(deviceId, buildCommand(deviceId, DeviceConfigKey.GETCOUNT, null, null));
        if (res == null) return null;
        int idx = res.indexOf(":");
        if (idx >= 0 && idx + 1 < res.length()) {
            try { return Integer.parseInt(res.substring(idx + 1).trim()); } catch (Exception ignore) {}
        }
        return null;
    }

    @Override
    public String getCurrentText(Long deviceId) {
        return sendAndRead(deviceId, buildCommand(deviceId, DeviceConfigKey.GET_CURRTEXT, null, null));
    }

    @Override
    public boolean loadFile(Long deviceId, String filePath) {
        String cmd = buildCommand(deviceId, DeviceConfigKey.LOAD, filePath, null);
        return sendAndExpectSuccess(deviceId, cmd, "load");
    }

    @Override
    public String getFileList(Long deviceId) {
        return sendAndRead(deviceId, buildCommand(deviceId, DeviceConfigKey.GET_FILELIST, null, null));
    }

    @Override
    public boolean clearBuffer(Long deviceId) {
        String cmd = buildCommand(deviceId, DeviceConfigKey.CLEARBUF, null, null);
        return sendAndExpectSuccess(deviceId, cmd, "clearbuf");
    }

    @Override
    public boolean powerOffPi(Long deviceId) {
        String cmd = buildCommand(deviceId, DeviceConfigKey.PI_CLOSEUV, null, null);
        return sendAndExpectSuccess(deviceId, cmd, "pi_closeuv");
    }

    @Override
    public void applySingle(Long deviceId, DeviceConfigKey key, Object param) {
        try {
            // 获取设备信息
            DeviceInfo device = deviceInfoService.selectDeviceInfoById(deviceId);
            // 获取默认文件配置
            List<DeviceFileConfig> deviceFileConfigs = deviceFileConfigService.selectDefaultDeviceFileConfigListByDeviceId(deviceId);
            if (device == null) {
                throw new ServiceException("设备不存在: " + deviceId);
            }
            
            // 检查设备是否在线
            if (!DeviceStatus.ONLINE_IDLE.getCode().equals(device.getStatus()) && 
                !DeviceStatus.ONLINE_PRINTING.getCode().equals(device.getStatus())) {
                log.warn("[DeviceCommand] deviceId={} 设备不在线，状态: {}", deviceId, device.getStatus());
                return;
            }
            // 构建命令并发送
            String command = buildCommand(deviceId, key, param, deviceFileConfigs.get(0).getVariableName());
            if (command != null) {
                boolean success = sendCommand(device.getIpAddress(), device.getPort(), command);
                if (success) {
                    log.info("[DeviceCommand] deviceId={} key={} param={} -> 下发成功", deviceId, key, param);
                } else {
                    log.error("[DeviceCommand] deviceId={} key={} param={} -> 下发失败", deviceId, key, param);
                    throw new ServiceException("设备参数下发失败");
                }
            } else {
                log.warn("[DeviceCommand] deviceId={} key={} 不支持的命令类型", deviceId, key);
            }
            
        } catch (Exception e) {
            log.error("[DeviceCommand] deviceId={} key={} 下发异常", deviceId, key, e);
            throw new ServiceException(e.getMessage());
        }
    }
    
    /**
     * 根据配置键和参数构建命令字符串
     */
    private String buildCommand(Long deviceId, DeviceConfigKey key, Object param, String objectName) {
        try {
            switch (key) {
                case SETA:
                    // 使用强类型参数构建：seta:data#...+v1=...+size#w|h+pos#x|y|r|ng
                    DeviceParams.SetaParam setaParam = objectMapper.convertValue(param, DeviceParams.SetaParam.class);
                    StringBuilder sb = new StringBuilder();
                    sb.append("seta:");
                    if (setaParam != null && setaParam.getText() != null && !setaParam.getText().isEmpty()) {
                        sb.append("data#").append(setaParam.getText());
                    }
                    if (setaParam != null && setaParam.getWidth() != null && setaParam.getHeight() != null) {
                        if (sb.charAt(sb.length()-1) != ':') sb.append('+');
                        sb.append("size#").append(setaParam.getWidth()).append('|').append(setaParam.getHeight());
                    }
                    if (setaParam != null && setaParam.getX() != null && setaParam.getY() != null) {
                        if (sb.charAt(sb.length()-1) != ':') sb.append('+');
                        int rr = setaParam.getR() == null ? 0 : setaParam.getR();
                        int ng = setaParam.getNg() == null ? 0 : setaParam.getNg();
                        sb.append("pos#").append(setaParam.getX()).append('|').append(setaParam.getY()).append('|').append(rr).append('|').append(ng);
                    }
                    return sb.toString();
                
                case CHANGEOBJ_POWER:
                    DeviceParams.ChangeObjPowerParam powerParam = objectMapper.convertValue(param, DeviceParams.ChangeObjPowerParam.class);
                    return String.format("changeobj_power:%s,%d", objectName, powerParam.getPowerPercent());
                
                case CHANGEOBJ_SIZE:
                    DeviceParams.ChangeObjSizeParam sizeParam = objectMapper.convertValue(param, DeviceParams.ChangeObjSizeParam.class);
                    return String.format("changeobj_size:%s,%d,%d", objectName, sizeParam.getWidth(), sizeParam.getHeight());
                
                case CHANGEOBJ_MARKNUM:
                    DeviceParams.ChangeObjMarknumParam markNumParam = objectMapper.convertValue(param, DeviceParams.ChangeObjMarknumParam.class);
                    return String.format("changeobj_marknum:%s,%d", objectName, markNumParam.getMarkNum());
                
                case DIT_SNUM:
                    DeviceParams.DitSnumParam ditSnumParam = objectMapper.convertValue(param, DeviceParams.DitSnumParam.class);
                    return String.format("dit_snum:%s,%d", objectName, ditSnumParam.getCurrentValue());
                
                case RESET_SERNUM:
                    // 重置流水号到初始值，仅需对象名
                    return String.format("reset_sernum:%s", objectName);
                
                case SETFIXEDDATA:
                    DeviceParams.SetFixedDataParam fixedParam = objectMapper.convertValue(param, DeviceParams.SetFixedDataParam.class);
                    return String.format("setfixeddata:%s,%s", objectName, fixedParam.getValue());
                
                case SETLIMITCOUNT:
                    DeviceParams.SetLimitCountParam limitParam = objectMapper.convertValue(param, DeviceParams.SetLimitCountParam.class);
                    return String.format("setlimitcount:%d,%d", limitParam.getMaxCount(), limitParam.getCurrentCount());
                
                case CHANGEOBJ_ENMARK:
                    DeviceParams.ChangeObjEnmarkParam enmarkParam = objectMapper.convertValue(param, DeviceParams.ChangeObjEnmarkParam.class);
                    return String.format("changeobj_enmark:%s,%s", objectName, enmarkParam.getEnabled() ? "1" : "0");
                
                case CHANGEOBJ_POS:
                    DeviceParams.ChangeObjPosParam posParam = objectMapper.convertValue(param, DeviceParams.ChangeObjPosParam.class);
                    return String.format("changeobj_pos:%s,%d,%d,%s", objectName, posParam.getX(), posParam.getY(), posParam.getAlign());
                
                case CHANGE_TEXTSIZE:
                    DeviceParams.ChangeTextSizeParam textSizeParam = objectMapper.convertValue(param, DeviceParams.ChangeTextSizeParam.class);
                    return String.format("change_textsize:%s,%d,%d,%d", objectName, textSizeParam.getHeight(), textSizeParam.getWidth(), textSizeParam.getCharSpacing());
                
                case CHANGEREPLEN:
                    DeviceParams.ChangeReplenParam replenParam = objectMapper.convertValue(param, DeviceParams.ChangeReplenParam.class);
                    return String.format("changereplen:%d", replenParam.getRepeatLength());
                
                case SETRPYMODE:
                    DeviceParams.SetRpyModeParam rpyParam = objectMapper.convertValue(param, DeviceParams.SetRpyModeParam.class);
                    return String.format("setrpymode:%s", rpyParam.getMode());
                
                case START:
                    return "start:";
                case STOP:
                    return "stop:";
                case TRIMARK:
                    return "trimark";
                case SYS_STA:
                    // 强类型：includeErrWarn=true → sys_sta:errsta
                    DeviceParams.SysStaParam sysStaParam = objectMapper.convertValue(param, DeviceParams.SysStaParam.class);
                    boolean include = sysStaParam != null && Boolean.TRUE.equals(sysStaParam.getIncludeErrWarn());
                    return include ? "sys_sta:errsta" : "sys_sta:";
                case GETA:
                    return "geta:";
                case SNUM_INDEX:
                    // 强类型：snum_index:序列名,是否编辑(0/1)
                    DeviceParams.SnumIndexQueryParam si = objectMapper.convertValue(param, DeviceParams.SnumIndexQueryParam.class);
                    if (si == null) return null;
                    return String.format("snum_index:%s,%s", si.getSerialName(), si.getEdit());
                case GET_TEXTDATA:
                    // 强类型：get_textdata:对象名,是否编辑(0/1)
                    DeviceParams.GetTextDataQueryParam gt = objectMapper.convertValue(param, DeviceParams.GetTextDataQueryParam.class);
                    if (gt == null) return null;
                    return String.format("get_textdata:%s,%s", gt.getObjectName(), gt.getEdit());
                case GETCOUNT:
                    return "getcount:";
                case GET_CURRTEXT:
                    return "get_currtext";
                case LOAD:
                    // 强类型：load:filepath
                    DeviceParams.LoadFileParam lf = objectMapper.convertValue(param, DeviceParams.LoadFileParam.class);
                    if (lf == null) return null;
                    return "load:" + lf.getFilePath();
                case GET_FILELIST:
                    return "get_filelist";
                case CLEARBUF:
                    return "clearbuf";
                case PI_CLOSEUV:
                    return "pi_closeuv";
                
                case EDIT_TEXTDATA:
                    DeviceParams.EditTextDataParam editParam = objectMapper.convertValue(param, DeviceParams.EditTextDataParam.class);
                    return String.format("edit_textdata:%s,%s", objectName, editParam.getContent());
                
                case SET_SYSTIME:
                    DeviceParams.SetSysTimeParam sysTimeParam = objectMapper.convertValue(param, DeviceParams.SetSysTimeParam.class);
                    return String.format("set_systime:%s", sysTimeParam.getDatetime());
                
                default:
                    log.warn("未支持的设备配置键: {}", key);
                    return null;
            }
        } catch (Exception e) {
            log.error("构建命令失败: key={}, param={}", key, param, e);
            return null;
        }
    }
    
    /**
     * 从设备文件配置中获取对象名称
     * 查询该设备的默认文件配置，使用variableName作为对象名称
     */
    private String getObjectNameFromDeviceConfig(Long deviceId) {
            // 查询设备的默认文件配置
            List<DeviceFileConfig> defaultConfigs = deviceFileConfigService.selectDefaultDeviceFileConfigListByDeviceId(deviceId);
            
            if (defaultConfigs != null && !defaultConfigs.isEmpty()) {
                // 如果有默认配置，使用第一个默认配置的variableName
                String variableName = defaultConfigs.get(0).getVariableName();
                if (variableName != null && !variableName.trim().isEmpty()) {
                    log.debug("设备 {} 使用默认配置的variableName: {})", deviceId, variableName);
                    
                    // 获取设备当前变量名进行比对
                    String currentFileName = getCurrentFileName(deviceId);
                    if (currentFileName != null) {
                        // 比对当前变量名与默认配置的variableName
                        if (!currentFileName.equals(variableName)) {
                            log.warn("设备 {} 当前变量名({})与默认配置的variableName({})不匹配，可能存在配置错误", 
                                    deviceId, currentFileName, variableName);
                            // 可以选择抛出异常或继续执行
                             throw new ServiceException("设备当前文件"+currentFileName+"与默认配置 "+defaultConfigs.get(0).getVariableName()+"不匹配");
                        } else {
                            log.info("设备 {} 当前变量名({})与默认配置的variableName({})匹配", 
                                    deviceId, currentFileName, variableName);
                        }
                    } else {
                        log.warn("设备 {} 无法获取当前变量名，跳过比对", deviceId);
                    }
                    
                    return variableName;
                } else {
                    log.warn("设备 {} 默认配置的variableName为空，需要设置有效的variableName", deviceId);
                    throw new ServiceException("设备默认配置的variableName为空");
                }
            } else {
                // 如果没有默认配置，抛出警告
                log.warn("设备 {} 没有设置默认文件配置，需要设置默认配置以确保正确的对象名称映射", deviceId);
                throw new ServiceException("设备没有设置默认文件配置");
            }
    }
    
    /**
     * 通过TCP发送命令到设备
     */
    @Override
    public boolean sendCommand(String ip, Integer port, String command) {
        Socket socket = null;
        try {
            // 建立连接
            socket = new Socket();
            socket.connect(new java.net.InetSocketAddress(ip, port), CONNECT_TIMEOUT);
            socket.setSoTimeout(READ_TIMEOUT);
            
            // 构建协议命令
            byte[] commandBytes = StxEtxProtocolUtil.buildCommand(command);
            log.debug("发送命令到 {}:{} - {}", ip, port, StxEtxProtocolUtil.toHexString(commandBytes));
            
            // 发送命令
            socket.getOutputStream().write(commandBytes);
            socket.getOutputStream().flush();
            
            // 读取响应
            byte[] buffer = new byte[1024];
            int bytesRead = socket.getInputStream().read(buffer);
            
            if (bytesRead > 0) {
                byte[] response = new byte[bytesRead];
                System.arraycopy(buffer, 0, response, 0, bytesRead);
                
                log.debug("收到响应 - {}", StxEtxProtocolUtil.toHexString(response));
                
                // 检查响应格式是否正确
                if (StxEtxProtocolUtil.isValidResponse(response)) {
                    String data = StxEtxProtocolUtil.parseResponse(response);
                    log.debug("解析响应数据: {}", data);
                     //截取':'后面取一位
                    data = data.substring(data.indexOf(":")+1, data.indexOf(":") + 2);
                    // 检查返回状态：1表示成功，0表示失败
                    if (data != null && data.trim().equals("1")) {
                        return true;
                    } else {
                        log.warn("设备返回失败状态: {}", data);
                        return false;
                    }
                } else {
                    log.warn("设备响应格式不正确");
                    return false;
                }
            } else {
                log.warn("设备无响应");
                return false;
            }
            
        } catch (SocketTimeoutException e) {
            log.warn("设备 {}:{} 响应超时", ip, port);
            return false;
        } catch (IOException e) {
            log.warn("设备 {}:{} 通信失败: {}", ip, port, e.getMessage());
            return false;
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    log.debug("关闭socket异常: {}", e.getMessage());
                }
            }
        }
    }

    /**
     * 发送命令并读取响应数据文本（不强制要求为1/0）。
     */
    private String sendAndRead(Long deviceId, String command) {
        try {
            DeviceInfo device = deviceInfoService.selectDeviceInfoById(deviceId);
            if (device == null) {
                log.error("设备不存在: {}", deviceId);
                return null;
            }
            Socket socket = null;
            try {
                socket = new Socket();
                socket.connect(new java.net.InetSocketAddress(device.getIpAddress(), device.getPort()), CONNECT_TIMEOUT);
                socket.setSoTimeout(READ_TIMEOUT);
                byte[] commandBytes = StxEtxProtocolUtil.buildCommand(command);
                socket.getOutputStream().write(commandBytes);
                socket.getOutputStream().flush();
                byte[] buffer = new byte[2048];
                int bytesRead = socket.getInputStream().read(buffer);
                if (bytesRead <= 0) return null;
                byte[] response = new byte[bytesRead];
                System.arraycopy(buffer, 0, response, 0, bytesRead);
                if (!StxEtxProtocolUtil.isValidResponse(response)) return null;
                String data = StxEtxProtocolUtil.parseResponse(response);
                return data;
            } finally {
                if (socket != null) try { socket.close(); } catch (IOException ignore) {}
            }
        } catch (Exception e) {
            log.warn("sendAndRead 异常 deviceId={} cmd={}", deviceId, command, e);
            return null;
        }
    }

    /** 发送命令并校验 like prefix:1 成功 */
    private boolean sendAndExpectSuccess(Long deviceId, String command, String prefix) {
        String res = sendAndRead(deviceId, command);
        if (res == null) return false;
        // 期望格式: prefix:1
        return res.trim().equals(prefix + ":1");
    }
    
    /**
     * 获取设备当前变量名
     *
     * @param deviceId 设备ID
     * @return 当前变量名，如果获取失败返回null
     */
    public String getCurrentFileName(Long deviceId) {
        try {
            // 获取设备信息
            DeviceInfo device = deviceInfoService.selectDeviceInfoById(deviceId);
            if (device == null) {
                log.error("设备不存在: {}", deviceId);
                return null;
            }
            
            // 检查设备是否在线
            if (!DeviceStatus.ONLINE_IDLE.getCode().equals(device.getStatus()) && 
                !DeviceStatus.ONLINE_PRINTING.getCode().equals(device.getStatus())) {
                log.warn("设备 {} 不在线，状态: {}", deviceId, device.getStatus());
                return null;
            }
            
            Socket socket = null;
            try {
                // 建立连接
                socket = new Socket();
                socket.connect(new java.net.InetSocketAddress(device.getIpAddress(), device.getPort()), CONNECT_TIMEOUT);
                socket.setSoTimeout(READ_TIMEOUT);
                
                // 发送get_currfile命令
                String command = "get_currfile";
                byte[] commandBytes = StxEtxProtocolUtil.buildCommand(command);
                log.debug("发送get_currfile命令到设备 {} - {}", deviceId, StxEtxProtocolUtil.toHexString(commandBytes));
                
                // 发送命令
                socket.getOutputStream().write(commandBytes);
                socket.getOutputStream().flush();
                
                // 读取响应
                byte[] buffer = new byte[1024];
                int bytesRead = socket.getInputStream().read(buffer);
                
                if (bytesRead > 0) {
                    byte[] response = new byte[bytesRead];
                    System.arraycopy(buffer, 0, response, 0, bytesRead);
                    
                    log.debug("收到get_currfile响应 - {}", StxEtxProtocolUtil.toHexString(response));
                    
                    // 检查响应格式是否正确
                    if (StxEtxProtocolUtil.isValidResponse(response)) {
                        String fileName = StxEtxProtocolUtil.parseResponse(response);
                        if(ObjectUtils.isEmpty(fileName)){
                            log.warn("设备 {} get_currfile返回文件名为空", deviceId);
                        }
                        //截取':'后面的内容
                        fileName = fileName.substring(fileName.indexOf(":") + 1);
                        log.debug("解析get_currfile响应数据: {}", fileName);
                        
                        if (!fileName.trim().isEmpty()) {
                            log.info("设备 {} 当前文件: {}", deviceId, fileName);
                            //裁剪掉文件名后面的后缀.ncfm
                            fileName = fileName.substring(0, fileName.lastIndexOf("."));
                            return fileName.trim();
                        } else {
                            log.warn("设备 {} 返回的文件名为空", deviceId);
                            return null;
                        }
                    } else {
                        log.warn("设备 {} get_currfile响应格式不正确", deviceId);
                        return null;
                    }
                } else {
                    log.warn("设备 {} get_currfile无响应", deviceId);
                    return null;
                }
                
            } catch (SocketTimeoutException e) {
                log.warn("设备 {} get_currfile响应超时", deviceId);
                return null;
            } catch (IOException e) {
                log.warn("设备 {} get_currfile通信失败: {}", deviceId, e.getMessage());
                return null;
            } finally {
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        log.debug("关闭socket异常: {}", e.getMessage());
                    }
                }
            }
            
        } catch (Exception e) {
            log.error("获取设备 {} 当前文件名异常", deviceId, e);
            return null;
        }
    }



}


