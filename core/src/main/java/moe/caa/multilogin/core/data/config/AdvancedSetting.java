package moe.caa.multilogin.core.data.config;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import moe.caa.multilogin.core.logger.LoggerLevel;
import moe.caa.multilogin.core.logger.MultiLogger;
import moe.caa.multilogin.core.util.ValueUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Properties;

/**
 * 高级配置阅读程序
 */
@Getter
@ToString
@NoArgsConstructor
public class AdvancedSetting {
    private transient final Properties properties = new Properties();
    private final String database_user_data_table_field_online_uuid = "online_uuid";
    private final String database_user_data_table_field_current_name = "current_name";
    private final String database_user_data_table_field_redirect_uuid = "redirect_uuid";
    private final String database_user_data_table_field_yggdrasil_service = "yggdrasil_service";
    private final String database_user_data_table_field_whitelist = "whitelist";
    private final String database_cache_whitelist_table_field_sign = "sign";
    private final String database_skin_restorer_table_field_online_uuid = "online_uuid";
    private final String database_skin_restorer_table_field_current_skin_url = "current_skin_url";
    private final String database_skin_restorer_table_field_restorer_data = "restorer_data";
    private String database_user_data_table_name = "{prefix}{_}user_data";
    private String database_cache_whitelist_table_name = "{prefix}{_}cache_whitelist";
    private String database_skin_restorer_table_name = "{prefix}{_}skin_restorer";
    private String database_backend_url_h2 = "jdbc:h2:{file_path};TRACE_LEVEL_FILE=0;TRACE_LEVEL_SYSTEM_OUT=0";
    private String database_backend_url_mysql = "jdbc:mysql://{ip}:{port}/{database}?autoReconnect=true&useUnicode=true&amp&characterEncoding=UTF-8&useSSL=false";
    private boolean logger_debug = false;


    public void load(File file) throws IllegalAccessException, IOException {
        MultiLogger.getLogger().log(LoggerLevel.INFO, String.format("加载文件: %s", file.getName()));
        FileInputStream stream = new FileInputStream(file);
        properties.clear();
        properties.load(stream);
        for (Field field : this.getClass().getDeclaredFields()) {
            if (Modifier.isTransient(field.getModifiers())) continue;
            if (Modifier.isFinal(field.getModifiers())) continue;
            String path = field.getName();
            if (field.getType() == String.class) {
                field.set(this, properties.getProperty(path, (String) field.get(this)));
            } else if (field.getType() == boolean.class) {
                String value = properties.getProperty(path);
                if (ValueUtil.isBoolValue(value)) {
                    field.set(this, Boolean.parseBoolean(value));
                }
            }
        }
    }
}
