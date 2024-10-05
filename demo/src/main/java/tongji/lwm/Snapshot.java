package tongji.lwm;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Snapshot 类用于保存代码文件的快照信息，包括文件路径、内容和时间戳。
 */
public class Snapshot implements Serializable {
    private String name;
    private final String filePath;    // 文件路径
    private final byte[] content;      // 文件内容
    private final long timestamp;      // 快照创建时间戳
    //private static int count=0;
    //private int ID;                    // ID

    public Snapshot(String name,String filePath, byte[] content, long timestamp) {
        this.name = name;
        this.filePath = filePath;
        this.content = content;
        this.timestamp = timestamp;
        //this.ID=count;
        //count++;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFilePath() {
        System.out.println(filePath);
        return filePath;
    }

    public byte[] getContent() {
        return content;
    }

    public String getTimestamp() {
        // 创建 DateTimeFormatter，格式为 年/月/日 时:分:秒
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");

        // 将时间戳转换为 LocalDateTime
        LocalDateTime dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());

        // 格式化并返回字符串
        return dateTime.format(formatter);
    }


}