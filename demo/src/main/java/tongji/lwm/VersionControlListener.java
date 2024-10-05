package tongji.lwm;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.project.ProjectUtil;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Logger;

public class VersionControlListener extends com.intellij.psi.PsiTreeChangeAdapter {

    private final Project project;
    private static final Logger LOGGER = Logger.getLogger(VersionControlListener.class.getName());

    public VersionControlListener(Project project) {
        this.project = project;
    }

    private void saveSnapshot(PsiFile psiFile) {
        if (psiFile == null) {
            LOGGER.warning("PsiFile is null, skipping snapshot.");
            return;
        }

        VirtualFile file = psiFile.getVirtualFile();
        if (file == null) {
            LOGGER.warning("VirtualFile is null for PsiFile: " + psiFile.getName());
            return;  // 如果没有关联的虚拟文件，直接返回
        }

        Document document = PsiDocumentManager.getInstance(project).getDocument(psiFile);
        if (document == null) {
            LOGGER.warning("Document is null for file: " + file.getName());
            return;
        }

        String content = document.getText();

        // 获取项目的根目录
        VirtualFile baseDir = ProjectUtil.guessProjectDir(project);
        if (baseDir == null) {
            LOGGER.severe("Project base directory is null, cannot save snapshots.");
            return;
        }

        String projectBasePath = baseDir.getPath();

        // 创建 snapshots 目录
        File snapshotDir = new File(projectBasePath, "snapshots");
        if (!snapshotDir.exists()) {
            if (snapshotDir.mkdirs()) {
                LOGGER.info("Snapshots directory created: " + snapshotDir.getPath());
            } else {
                LOGGER.severe("Failed to create snapshots directory.");
                return; // 无法创建目录则退出
            }
        }

        // 生成快照文件路径
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        File snapshotFile = new File(snapshotDir, timestamp + "_" + file.getName());

        // 保存快照内容
        try (FileWriter writer = new FileWriter(snapshotFile)) {
            writer.write(content);
            LOGGER.info("Snapshot saved: " + snapshotFile.getPath());
        } catch (IOException e) {
            LOGGER.severe("Error saving snapshot: " + e.getMessage());
        }
    }

    @Override
    public void childReplaced(com.intellij.psi.PsiTreeChangeEvent event) {
        LOGGER.info("childReplaced event triggered for file: " + (event.getFile() != null ? event.getFile().getName() : "unknown"));
        saveSnapshot(event.getFile());
    }

    @Override
    public void childAdded(com.intellij.psi.PsiTreeChangeEvent event) {
        LOGGER.info("childAdded event triggered for file: " + (event.getFile() != null ? event.getFile().getName() : "unknown"));
        saveSnapshot(event.getFile());
    }

    @Override
    public void childRemoved(com.intellij.psi.PsiTreeChangeEvent event) {
        LOGGER.info("childRemoved event triggered for file: " + (event.getFile() != null ? event.getFile().getName() : "unknown"));
        saveSnapshot(event.getFile());
    }
}
