package tongji.ggyl.eventlistening;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.*;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.ProjectUtil;
import tongji.ggyl.versioncontrol.VersionControl;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Logger;

public class VersionControlListener extends com.intellij.psi.PsiTreeChangeAdapter implements VirtualFileListener {

    private final VersionControl versionControl;
    private final Project project;
    private static final Logger LOGGER = Logger.getLogger(VersionControlListener.class.getName());
    private int lastModifiedLine = -1;  // 保存最后一次修改的行号
    private boolean isSnapshotSaved = false;  // 防止重复生成快照
    private long lastSnapshotTime = 0;  // 记录上一次保存快照的时间戳
    private static final long SNAPSHOT_INTERVAL_MS = 1000; // 1秒的时间间隔，防止重复快照

    public VersionControlListener(Project project, VersionControl versionControl) {
        this.project = project;
        this.versionControl = versionControl;
        // 注册 VFS 监听器，监听文件系统的变化
        VirtualFileManager.getInstance().addVirtualFileListener(this);
    }

    // 用于保存传入的 PsiFile 的快照
    private void saveSnapshot(PsiFile psiFile) {
        if (psiFile == null) {
            LOGGER.warning("PsiFile 为 null，跳过快照。");
            return;
        }

        long currentTime = System.currentTimeMillis();
        // 检查是否距离上次保存时间过短（防止短时间内生成多个快照）
        if (currentTime - lastSnapshotTime < SNAPSHOT_INTERVAL_MS) {
            LOGGER.info("短时间内不重复生成快照");
            return;
        }

        VirtualFile file = psiFile.getVirtualFile();
        if (file == null) {
            LOGGER.warning("PsiFile 的 VirtualFile 为 null: " + psiFile.getName());
            return;
        }

        Document document = PsiDocumentManager.getInstance(project).getDocument(psiFile);
        if (document == null) {
            LOGGER.warning("文件的 Document 为 null：" + file.getName());
            return;
        }

        // 从文档中获取文本内容，准备保存快照
        byte[] content = document.getText().getBytes();

        // 获取项目的根目录
        VirtualFile baseDir = ProjectUtil.guessProjectDir(project);
        if (baseDir == null) {
            LOGGER.severe("项目根目录为空，无法保存快照。");
            return;
        }

        // 创建 snapshots 目录
        File snapshotDir = new File(baseDir.getPath(), "snapshots");
        if (!snapshotDir.exists() && !snapshotDir.mkdirs()) {
            LOGGER.severe("创建快照目录失败。");
            return;
        }

        // 生成快照文件路径
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        long thistimestamp = System.currentTimeMillis();

        String name = file.getName() + "-" + thistimestamp;
        Snapshot snapshot = new Snapshot(name, file.getPath(), content, thistimestamp);

        versionControl.saveVersionSnapshot(snapshot);
        isSnapshotSaved = true;  // 标记快照已保存
        lastSnapshotTime = currentTime;  // 更新最后一次保存时间
    }

    // 检查行号变化，并在变化时生成快照
    private void checkLineChangeAndSave(PsiFile psiFile) {
        if (psiFile == null) {
            return;
        }

        Document document = PsiDocumentManager.getInstance(project).getDocument(psiFile);
        if (document != null) {
            // 获取当前修改的行号
            int currentLine = document.getLineNumber(document.getTextLength());
            if (currentLine != lastModifiedLine && !isSnapshotSaved) {
                saveSnapshot(psiFile);  // 行号发生变化，保存快照
                lastModifiedLine = currentLine;  // 更新最后修改的行号
            }
        }
    }

    // 重置快照标志，用于新的一轮监听
    private void resetSnapshotFlag() {
        isSnapshotSaved = false;
    }

    // 重写子节点替换事件（用于检测文件重命名）
    @Override
    public void childReplaced(com.intellij.psi.PsiTreeChangeEvent event) {
        PsiFile oldFile = event.getOldChild() instanceof PsiFile ? (PsiFile) event.getOldChild() : null;
        PsiFile newFile = event.getNewChild() instanceof PsiFile ? (PsiFile) event.getNewChild() : null;

        if (oldFile != null && newFile != null) {
            String oldPath = oldFile.getVirtualFile().getPath();
            String newPath = newFile.getVirtualFile().getPath();
            // 检测文件名是否不同
            if (!oldPath.equals(newPath)) {
                handleFileRename(oldPath, newPath);  // 调用自定义方法处理重命名
            }
        }

        resetSnapshotFlag();  // 重置快照标志
    }

    // 处理文件重命名的方法
    private void handleFileRename(String oldPath, String newPath) {
        // 自定义重命名处理逻辑
        LOGGER.info("文件重命名：" + oldPath + " -> " + newPath);
        // 生成包含旧路径和新路径的快照
        saveSnapshotForRenamedFile(oldPath, newPath);
    }

    // 重写子节点添加事件（添加文件时保存快照）
    @Override
    public void childAdded(com.intellij.psi.PsiTreeChangeEvent event) {
        if (!isSnapshotSaved) {
            LOGGER.info("子节点添加事件触发: " + (event.getFile() != null ? event.getFile().getName() : "unknown"));
            saveSnapshot(event.getFile());
        }
        resetSnapshotFlag();  // 重置快照标志
    }

    // 重写子节点移除事件（删除文件时保存快照）
    @Override
    public void childRemoved(com.intellij.psi.PsiTreeChangeEvent event) {
        if (!isSnapshotSaved) {
            LOGGER.info("子节点移除事件触发: " + (event.getFile() != null ? event.getFile().getName() : "unknown"));
            saveSnapshot(event.getFile());
        }
        resetSnapshotFlag();  // 重置快照标志
    }

    // 重写子节点更改事件（行号变化时保存快照）

    public void childChanged(com.intellij.psi.PsiTreeChangeEvent event) {
        LOGGER.info("子节点更改事件触发: " + (event.getFile() != null ? event.getFile().getName() : "unknown"));
        checkLineChangeAndSave(event.getFile());  // 检查行号变化
        resetSnapshotFlag();  // 重置快照标志
    }

    // === 处理虚拟文件系统的监听事件 ===

    // 文件重命名事件
    @Override
    public void beforePropertyChange(VirtualFilePropertyEvent event) {
        if (VirtualFile.PROP_NAME.equals(event.getPropertyName())) {
            LOGGER.info("文件重命名事件触发：" + event.getOldValue() + " -> " + event.getNewValue());
            String oldPath = event.getFile().getPath();
            String newPath = event.getFile().getParent().getPath() + "/" + event.getNewValue();
            versionControl.changeSnapshotsPathForFile(oldPath, newPath);
            // 更新重命名文件的快照
            //saveSnapshotForRenamedFile(oldPath, newPath);  // 处理文件重命名
        }
    }

    // 文件删除事件
    @Override
    public void beforeFileDeletion(VirtualFileEvent event) {
        System.out.println("lwmlwmlwmlwm");
        LOGGER.info("文件删除事件触发：" + event.getFile().getPath());
        //saveSnapshotForVirtualFile(event.getFile(), "删除");
        versionControl.deleteSnapshotsForFile(event.getFile().getPath());
    }

    // 保存包含旧路径和新路径的重命名快照
    private void saveSnapshotForRenamedFile(String oldPath, String newPath) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastSnapshotTime < SNAPSHOT_INTERVAL_MS) {
            LOGGER.info("短时间内不重复生成快照");
            return;
        }

        // 生成快照内容，包含重命名前后的路径
        String snapshotContent = "文件重命名:\n旧路径: " + oldPath + "\n新路径: " + newPath;

        // 获取项目的根目录
        VirtualFile baseDir = ProjectUtil.guessProjectDir(project);
        if (baseDir == null) {
            LOGGER.severe("项目根目录为空，无法保存快照。");
            return;
        }

        // 创建 snapshots 目录
        File snapshotDir = new File(baseDir.getPath(), "snapshots");
        if (!snapshotDir.exists() && !snapshotDir.mkdirs()) {
            LOGGER.severe("创建快照目录失败。");
            return;
        }

        // 生成快照文件路径
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        String name = "RenamedFile-" + timestamp;
        Snapshot snapshot = new Snapshot(name, oldPath, snapshotContent.getBytes(), currentTime);

        versionControl.saveVersionSnapshot(snapshot);
        lastSnapshotTime = currentTime;  // 更新最后一次保存时间
    }

    // 保存快照的虚拟文件方法
    private void saveSnapshotForVirtualFile(VirtualFile file, String operation) {
        if (file == null) {
            LOGGER.warning("VirtualFile 为 null，跳过快照。");
            return;
        }

        long currentTime = System.currentTimeMillis();
        // 检查是否距离上次保存时间过短（防止短时间内生成多个快照）
        if (currentTime - lastSnapshotTime < SNAPSHOT_INTERVAL_MS) {
            LOGGER.info("短时间内不重复生成快照");
            return;
        }

        // 对于删除操作，不读取文件内容，只保存文件路径
        String snapshotContent;
        if ("删除".equals(operation)) {
            System.out.println("gyygyygyygyy");
            System.out.println(file.getPath());
            System.out.println("gyygyygyygyy");
            versionControl.deleteSnapshotsForFile(file.getPath());
            System.out.println("ywqywqywqywq");
            snapshotContent = "文件已被删除：" + file.getPath();
        } else {
            // 对于其他操作，仍然读取文件内容
            byte[] content;
            try {
                content = file.contentsToByteArray();
            } catch (java.io.IOException e) {
                LOGGER.severe("无法读取文件内容：" + file.getPath());
                return;
            }
            snapshotContent = "文件操作: " + operation + "\n文件路径: " + file.getPath() + "\n文件内容:\n" + new String(content);
        }

        // 获取项目的根目录
        VirtualFile baseDir = ProjectUtil.guessProjectDir(project);
        if (baseDir == null) {
            LOGGER.severe("项目根目录为空，无法保存快照。");
            return;
        }

        // 创建 snapshots 目录
        File snapshotDir = new File(baseDir.getPath(), "snapshots");
        if (!snapshotDir.exists() && !snapshotDir.mkdirs()) {
            LOGGER.severe("创建快照目录失败。");
            return;
        }

        // 生成快照文件路径
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        long thistimestamp = System.currentTimeMillis();
        String name = file.getName() + "-" + thistimestamp;

        Snapshot snapshot = new Snapshot(name, file.getPath(), snapshotContent.getBytes(), thistimestamp);

        versionControl.saveVersionSnapshot(snapshot);
        lastSnapshotTime = currentTime;  // 更新最后一次保存时间
    }

    // 移除 VFS 监听器
    public void removeListener() {
        VirtualFileManager.getInstance().removeVirtualFileListener(this);
    }
}
