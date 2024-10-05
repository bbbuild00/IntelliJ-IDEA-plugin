package tongji.lwm;

import com.intellij.openapi.application.ApplicationManager;
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

//实现 ProjectActivity 接口，表示这是一个在项目活动生命周期中执行的插件
public class VersionControlPlugin implements ProjectActivity {
    private static VersionControlImpl versionControl;
    private static final Logger logger = Logger.getLogger(VersionControlPlugin.class.getName()); //用于记录日志信息

    //实现 execute 方法。在项目活动时被调用，接收当前项目和 Kotlin 的 Continuation 对象。
    @Override
    public Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
        // 获取项目的根目录
        VirtualFile baseDir = ProjectUtil.guessProjectDir(project);
        // 查获取的根目录是否为 null
        if (baseDir != null) {
            // 获取项目的路径
            String projectBasePath = baseDir.getPath();
            // 在项目根目录下创建一个名为 snapshots 的目录的 File 对象
            File snapshotDir = new File(projectBasePath, "snapshots");
            if (!snapshotDir.exists() && snapshotDir.mkdirs()) {   //目录不存在且创建目录成功
                logger.info("启动时创建了快照目录: " + snapshotDir.getPath());
            }
        }
        else {
            logger.severe("项目根目录为空，无法创建快照目录");
        }
        versionControl = new VersionControlImpl(project);

        // 注册PSI监听器，监听代码结构变化，并传入一个Disposable，确保资源释放
        PsiManager.getInstance(project).addPsiTreeChangeListener(new VersionControlListener(project,versionControl), project);
        logger.info("为项目注册了 PSI 树变化监听器：" + project.getName());


        return Unit.INSTANCE;
    }
    public static VersionControlImpl getVersionControl() {
        return versionControl;
    }

}
