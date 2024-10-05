package tongji.lwm;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.project.ProjectUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Logger;

// 用于监听 PSI 树的变化
public class VersionControlListener extends com.intellij.psi.PsiTreeChangeAdapter {

    private final VersionControl versionControl;
    private final Project project;  // 存储当前项目的引用
    private static final Logger LOGGER = Logger.getLogger(VersionControlListener.class.getName());  // 记录日志信息

    public VersionControlListener(Project project,VersionControl versionControl) {
        this.project = project;
        this.versionControl = versionControl;
    }

    // 用于保存传入的 PsiFile 的快照
    private void saveSnapshot(PsiFile psiFile) {
        System.out.println("22222");
        // 如果 psiFile 为 null，记录警告并返回，跳过快照保存
        if (psiFile == null) {
            LOGGER.warning("PsiFile 为 null，跳过快照。");
            return;
        }

        VirtualFile file = psiFile.getVirtualFile();
        if (file == null) {
            LOGGER.warning("PsiFile 的 VirtualFile 为 null: " + psiFile.getName());
            return;  // 如果没有关联的虚拟文件，直接返回
        }

        Document document = PsiDocumentManager.getInstance(project).getDocument(psiFile);
        if (document == null) {
            LOGGER.warning("文件的 Document 为 null：" + file.getName());
            return;
        }

        // 从文档中获取文本内容，准备保存快照。
        byte[] content = document.getText().getBytes();

        // 获取项目的根目录
        VirtualFile baseDir = ProjectUtil.guessProjectDir(project);
        if (baseDir == null) {
            LOGGER.severe("项目根目录为空，无法保存快照。");
            return;
        }

        // 从 VirtualFile 获取项目的路径，存储为字符串
        String projectBasePath = baseDir.getPath();

        // 创建 snapshots 目录
        File snapshotDir = new File(projectBasePath, "snapshots");
        // 如果目录不存在且创建成功，记录信息。如果创建失败，记录严重错误并返回。
        if (!snapshotDir.exists()) {
            if (snapshotDir.mkdirs()) {
                LOGGER.info("快照目录已创建： " + snapshotDir.getPath());
            } else {
                LOGGER.severe("创建快照目录失败。");
                return; // 无法创建目录则退出
            }
        }

        // 生成快照文件路径:使用当前时间戳和文件名生成快照文件的完整路径
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        long thistimestamp=System.currentTimeMillis();
        String name = file.getName()+"-"+thistimestamp;
        Snapshot snapshot = new Snapshot(name, file.getPath(), content, thistimestamp);
        System.out.println("3333");
        versionControl.saveVersionSnapshot(snapshot);
        System.out.println("4444");


    }

    // 重写子节点替换事件
    @Override
    public void childReplaced(com.intellij.psi.PsiTreeChangeEvent event) {
        System.out.println("子节点被替换事件触发： ");
        LOGGER.info("子节点被替换事件触发： " + (event.getFile() != null ? event.getFile().getName() : "unknown"));
        saveSnapshot(event.getFile());
    }

    // 重写子节点添加事件
    @Override
    public void childAdded(com.intellij.psi.PsiTreeChangeEvent event) {
        LOGGER.info("子节点添加事件触发: " + (event.getFile() != null ? event.getFile().getName() : "unknown"));
        saveSnapshot(event.getFile());
    }

    // 重写子节点移除事件
    @Override
    public void childRemoved(com.intellij.psi.PsiTreeChangeEvent event) {
        LOGGER.info("子节点移除事件触发: " + (event.getFile() != null ? event.getFile().getName() : "unknown"));
        saveSnapshot(event.getFile());
    }
}

