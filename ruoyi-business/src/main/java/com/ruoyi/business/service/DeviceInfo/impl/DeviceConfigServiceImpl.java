package com.ruoyi.business.service.DeviceInfo.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.business.domain.DeviceInfo.params.DeviceParams;
import com.ruoyi.business.enums.DeviceConfigKey;
import com.ruoyi.business.service.DeviceInfo.DeviceConfigService;
import com.ruoyi.common.exception.ServiceException;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.Map;

@Service
public class DeviceConfigServiceImpl implements DeviceConfigService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Map<DeviceConfigKey, Object> parseAndValidate(String parametersJson) {
        try {
            EnumMap<DeviceConfigKey, Object> result = new EnumMap<>(DeviceConfigKey.class);
            if (parametersJson == null || parametersJson.trim().isEmpty()) {
                return result;
            }

            Map<String, Object> raw = objectMapper.readValue(parametersJson, new TypeReference<Map<String, Object>>(){});
            for (Map.Entry<String, Object> e : raw.entrySet()) {
                DeviceConfigKey key = DeviceConfigKey.fromKey(e.getKey());
                if (key == null) {
                    throw new ServiceException("未知的设备参数键: " + e.getKey());
                }
                Object value = e.getValue();
                // 将 Object 再序列化再反序列化为强类型模型
                String json = objectMapper.writeValueAsString(value);
                switch (key) {
                    case SET_SYSTIME:
                        result.put(key, objectMapper.readValue(json, DeviceParams.SetSysTimeParam.class));
                        break;
                    case CHANGEOBJ_SIZE:
                        result.put(key, objectMapper.readValue(json, DeviceParams.ChangeObjSizeParam.class));
                        break;
                    case CHANGEOBJ_POWER:
                        result.put(key, objectMapper.readValue(json, DeviceParams.ChangeObjPowerParam.class));
                        break;
                    case CHANGEOBJ_MARKNUM:
                        result.put(key, objectMapper.readValue(json, DeviceParams.ChangeObjMarknumParam.class));
                        break;
                    case DIT_SNUM:
                        result.put(key, objectMapper.readValue(json, DeviceParams.DitSnumParam.class));
                        break;
                    case RESET_SERNUM:
                        result.put(key, objectMapper.readValue(json, DeviceParams.ResetSernumParam.class));
                        break;
                    case SETFIXEDDATA:
                        result.put(key, objectMapper.readValue(json, DeviceParams.SetFixedDataParam.class));
                        break;
                    case SETLIMITCOUNT:
                        result.put(key, objectMapper.readValue(json, DeviceParams.SetLimitCountParam.class));
                        break;
                    case CHANGEOBJ_ENMARK:
                        result.put(key, objectMapper.readValue(json, DeviceParams.ChangeObjEnmarkParam.class));
                        break;
                    case CHANGEOBJ_POS:
                        result.put(key, objectMapper.readValue(json, DeviceParams.ChangeObjPosParam.class));
                        break;
                    case CHANGE_TEXTSIZE:
                        result.put(key, objectMapper.readValue(json, DeviceParams.ChangeTextSizeParam.class));
                        break;
                    case CHANGEREPLEN:
                        result.put(key, objectMapper.readValue(json, DeviceParams.ChangeReplenParam.class));
                        break;
                    case SETRPYMODE:
                        result.put(key, objectMapper.readValue(json, DeviceParams.SetRpyModeParam.class));
                        break;
                    case EDIT_TEXTDATA:
                        result.put(key, objectMapper.readValue(json, DeviceParams.EditTextDataParam.class));
                        break;
                    default:
                        throw new ServiceException("未处理的键: " + key);
                }
            }
            return result;
        } catch (ServiceException se) {
            throw se;
        } catch (Exception ex) {
            throw new ServiceException("设备参数解析失败: " + ex.getMessage());
        }
    }
}

 