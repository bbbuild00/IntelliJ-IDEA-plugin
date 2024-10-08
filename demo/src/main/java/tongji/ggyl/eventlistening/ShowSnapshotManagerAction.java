package tongji.ggyl.eventlistening;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import tongji.ggyl.ui.SnapshotManagerUI;
import tongji.ggyl.versioncontrol.VersionControlImpl;

public class ShowSnapshotManagerAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent event) {
        //System.out.println("lps4");
        var project = event.getProject();
        if (project == null) return;
        // 确保在事件调度线程中执行
        ApplicationManager.getApplication().invokeLater(() -> {

            // 获取 VersionControlPlugin 实例
            VersionControlImpl versionControl = VersionControlPlugin.getVersionControl();
            // 调用 SnapshotManagerUI 来显示快照管理界面
            new SnapshotManagerUI(versionControl);
            //System.out.println("lps3");

    });
    }
}
