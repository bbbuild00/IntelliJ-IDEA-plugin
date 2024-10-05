package tongji.lwm;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiManager;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.project.ProjectUtil;
import org.jetbrains.annotations.NotNull;
import kotlin.coroutines.Continuation;
import kotlin.Unit;

import java.io.File;
import java.util.logging.Logger;

public class VersionControlPlugin implements ProjectActivity {

    private static final Logger LOGGER = Logger.getLogger(VersionControlPlugin.class.getName());

    @Override
    public Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
        // 获取项目的根目录
        VirtualFile baseDir = ProjectUtil.guessProjectDir(project);
        if (baseDir != null) {
            String projectBasePath = baseDir.getPath();
            File snapshotDir = new File(projectBasePath, "snapshots");
            if (!snapshotDir.exists() && snapshotDir.mkdirs()) {
                LOGGER.info("Snapshots directory created at startup: " + snapshotDir.getPath());
            }
        } else {
            LOGGER.severe("Project base directory is null, cannot create snapshots directory.");
        }

        // 注册PSI监听器，监听代码结构变化，并传入一个Disposable，确保资源释放
        PsiManager.getInstance(project).addPsiTreeChangeListener(new VersionControlListener(project), project);
        LOGGER.info("PSI Tree Change Listener registered for project: " + project.getName());
        return Unit.INSTANCE;
    }
}
