package com.runwangguo.fileuploader;

import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

public class ConfigReader {
	private static final String CONFIG_FILE = "config.yml";
    private static Map<String, Object> config;
    
    static {
        loadConfig();
    }

    public static void loadConfig() {
        try (InputStream inputStream = new FileInputStream(CONFIG_FILE)) {
            Yaml yaml = new Yaml();
            config = yaml.load(inputStream);
        } catch (IOException e) {
        }
    }

    public static Object getValue(String propertyName) {
        return config.get(propertyName);
    }
    
    public static String getStringValue(String propertyName) {
        Object value = config.get(propertyName);
        if (value instanceof List) {
            List<String> valueList = (List<String>) value;
            if (!valueList.isEmpty()) {
                return valueList.get(0);
            }
        } else if (value instanceof String) {
            return (String) value;
        }
        return null;
    }
    
    public static int getIntegerValue(String propertyName) {
        Object value = config.get(propertyName);
        if (value instanceof Integer) {
            return (Integer) value;
        } else if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                // 处理异常
                e.printStackTrace();
            }
        }
        return 0; // 如果无法获取数值，则返回默认值
    }
   
    public static String[] getStringArray(String propertyName) {
        List<String> list = (List<String>) config.get(propertyName);
        if (list != null) {
            return list.toArray(new String[0]);
        }
        return new String[0];
    }
}
