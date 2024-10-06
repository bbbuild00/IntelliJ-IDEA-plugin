package tongji.lwm;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.vfs.VirtualFile;

import java.io.*;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class VersionControlImpl implements VersionControl {
    private final Project project;
    private final String projectBasePath;
    private static final Logger logger = Logger.getLogger(VersionControlPlugin.class.getName()); // 用于记录日志信息
    private final ExecutorService executorService = Executors.newCachedThreadPool(); // 创建线程池

    public VersionControlImpl(Project project) {
        this.project = project;
        VirtualFile baseDir = ProjectUtil.guessProjectDir(project);
        this.projectBasePath = baseDir.getPath()+"/snapshots";
        System.out.println("project:"+this.projectBasePath);
    }

    @Override
    public void saveVersionSnapshot(Snapshot snapshot) {
        executorService.submit(() -> {
            System.out.println("66666666");

            // 定义快照文件名
            String fileName = snapshot.getName();
            File snapshotFile = new File(projectBasePath, fileName);

            // 保存整个快照对象
            try (ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream(snapshotFile))) {
                System.out.println(snapshotFile.getPath());
                outputStream.writeObject(snapshot); // 写入整个快照对象
                System.out.println("77777777");
                logger.info("为文件保存的快照 :" + snapshot.getFilePath() + " at " + snapshot.getTimestamp());
            } catch (IOException e) {
                logger.severe("保存快照失败: " + snapshot.getFilePath());
            }
        });
    }

    @Override
    public List<Snapshot> getSnapshotsForFile(String path) {

        List<Snapshot> snapshots = new ArrayList<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(projectBasePath))) {
            for (Path entry : stream) {
                if (Files.isRegularFile(entry)) { // 检查是否为常规文件
                    System.out.println(entry.getFileName()); // 输出文件名
                    // 反序列化 Snapshot 对象
                    try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(entry.toFile()))) {
                        Snapshot snapshot = (Snapshot) ois.readObject(); // 反序列化
                        if (snapshot != null && snapshot.getFilePath().equals(path)) {
                            System.out.println(snapshot.getFilePath());
                            System.out.println("fanxuliehua win");
                            snapshots.add(snapshot); // 将反序列化的对象添加到列表中
                            System.out.println("add win");
                        }
                    } catch (ClassNotFoundException e) {
                        System.out.println("catch1");
                        System.err.println("Class not found: " + e.getMessage());
                    } catch (IOException e) {
                        System.out.println("catch2");
                        System.err.println("Error reading file: " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }


        return snapshots;
    }


    // 关闭线程池（在适当的地方调用，比如在项目关闭时）
    public void shutdown() {
        executorService.shutdown();
    }
}
