package moe.caa.multilogin.core.main;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import moe.caa.multilogin.core.impl.IPlugin;
import moe.caa.multilogin.core.logger.LoggerLevel;
import moe.caa.multilogin.core.logger.MultiLogger;
import moe.caa.multilogin.core.util.YamlReader;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.zip.GZIPOutputStream;

/**
 * 高度定制的bStats
 */
public class MetricsLite {

    //插件
    private final IPlugin plugin;
    //bStats是否启用
    private boolean enabled;
    //服务器uuid
    private String serverUUID;

    public MetricsLite(IPlugin plugin) throws IOException {
        if (plugin == null) {
            throw new IllegalArgumentException("Plugin cannot be null!");
        }
        this.plugin = plugin;

        loadConfig();
        if (enabled) {
            MultiLogger.getLogger().log(LoggerLevel.DEBUG, "bStats enabled", null);
            startSubmitting();
        }
    }

    private void loadConfig() throws IOException {
        File bStatsFolder = new File(plugin.getDataFolder().getParentFile(), "bStats");
        bStatsFolder.mkdirs();
        File configFile = new File(bStatsFolder, "config.yml");
        UUID uuid;
        if (!configFile.exists()) {
            uuid = UUID.randomUUID();
            writeFile(configFile,
                    "#bStats collects some data for plugin authors like how many servers are using their plugins.",
                    "#To honor their work, you should not disable it.",
                    "#This has nearly no effect on the server performance!",
                    "#Check out https://bStats.org/ to learn more :)",
                    "enabled: true",
                    "serverUuid: \"" + uuid + "\"",
                    "logFailedRequests: false",
                    "logSentData: false",
                    "logResponseStatusText: false");
        }
        // Load configuration
        YamlReader config = YamlReader.fromInputStream(new FileInputStream(configFile));
        enabled = config.get("enabled", Boolean.class, true);
        serverUUID = config.get("serverUuid", String.class);
    }

    //    配置文件写入
    private void writeFile(File file, String... lines) throws IOException {
        try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file))) {
            for (String line : lines) {
                bufferedWriter.write(line);
                bufferedWriter.newLine();
            }
        }
    }

    private void startSubmitting() {
        //周期什么的不要动 会被封禁
        long initialDelay = 1000 * 60 * 4;
        long secondDelay = 1000 * 60 * 10;
        plugin.getRunServer().getScheduler().runTaskAsync(this::submitData, initialDelay);
        plugin.getRunServer().getScheduler().runTaskAsyncTimer(this::submitData, initialDelay + secondDelay, 1000 * 60 * 30);
    }


    //    获取插件数据
    public JsonObject getPluginData() {
        JsonObject data = new JsonObject();

        String pluginVersion = plugin.getPluginVersion();

        data.addProperty("pluginName", "MultiLoginR"); // Append the name of the plugin
        data.addProperty("id", 12130); // Append the id of the plugin
        data.addProperty("pluginVersion", pluginVersion); // Append the version of the plugin
        data.add("customCharts", new JsonArray());

        return data;
    }

    //    获取服务器信息
    private JsonObject getServerData() {
        // Minecraft 数据
        int playerAmount = plugin.getRunServer().getPlayerManager().getOnlinePlayers().size();
        String serverVersion = plugin.getRunServer().getVersion();
        String serverCoreName = plugin.getRunServer().getName();

        // OS/Java 数据
        String javaVersion = System.getProperty("java.version");
        String osName = System.getProperty("os.name");
        String osArch = System.getProperty("os.arch");
        String osVersion = System.getProperty("os.version");
        int coreCount = Runtime.getRuntime().availableProcessors();

        JsonObject data = new JsonObject();

        data.addProperty("serverUUID", serverUUID);

        data.addProperty("playerAmount", playerAmount);
        data.addProperty("onlineMode", 1);
        data.addProperty("bukkitVersion", serverVersion);
        data.addProperty("bukkitName", serverCoreName);

        data.addProperty("javaVersion", javaVersion);
        data.addProperty("osName", osName);
        data.addProperty("osArch", osArch);
        data.addProperty("osVersion", osVersion);
        data.addProperty("coreCount", coreCount);

        return data;
    }

    //    提交数据
    private void submitData() {
        try {
            // 发送数据
            senDataWithRetry();
            MultiLogger.getLogger().log(LoggerLevel.DEBUG, "bStats submit success");
        } catch (Exception e) {
            //出错记录
            MultiLogger.getLogger().log(LoggerLevel.DEBUG, "bStats submit error", e);
        }
    }

    //    五次重试
    private void senDataWithRetry() throws Exception {
        Exception thr = null;
        for (int i = 0; i < 5; i++) {
            try {
                if (sendData()) return;
            } catch (Exception e) {
                thr = e;
            }
        }
        throw thr == null ? new IOException("unknown") : thr;
    }

    //发信
    private boolean sendData() throws Exception {
        final JsonObject data = getServerData();

        JsonArray pluginData = new JsonArray();
        pluginData.add(getPluginData());

        data.add("plugins", pluginData);
        HttpsURLConnection connection = (HttpsURLConnection) new URL("https://bStats.org/submitData/bukkit").openConnection();

        // 压缩数据发送
        byte[] compressedData = compress(data.toString());

        // 添加浏览器头信息
        connection.setRequestMethod("POST");
        connection.addRequestProperty("Accept", "application/json");
        connection.addRequestProperty("Connection", "close");
        connection.addRequestProperty("Content-Encoding", "gzip");
        connection.addRequestProperty("Content-Length", String.valueOf(compressedData.length));
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("User-Agent", "MC-Server/1");//1是bStats版本

        // 发送数据
        connection.setDoOutput(true);
        connection.setDoInput(true);
        try (DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream())) {
            outputStream.write(compressedData);
        }
//        这里似乎必须这样
        Thread.sleep(1000);
        try (DataInputStream inputStream = new DataInputStream(connection.getInputStream())) {
            byte[] bytes = new byte[inputStream.available()];
            inputStream.read(bytes);
            MultiLogger.getLogger().log(LoggerLevel.DEBUG, "receive:" + new String(bytes));
        }
        return true;
    }

    //gzip压缩 数据传输需要
    private byte[] compress(final String str) throws IOException {
        if (str == null) {
            return null;
        }
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(outputStream)) {
            gzip.write(str.getBytes(StandardCharsets.UTF_8));
        }
        return outputStream.toByteArray();
    }

}