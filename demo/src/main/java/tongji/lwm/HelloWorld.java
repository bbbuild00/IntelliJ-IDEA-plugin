package tongji.lwm;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.vfs.VirtualFile;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public class HelloWorld extends AnAction {
    private final List<String> versionHistory = new ArrayList<>();

    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getProject();
        if (project != null) {
            Document document = FileDocumentManager.getInstance().getDocument(getCurrentFile(project));
            if (document != null) {
                // 显示当前项目名称
                Messages.showMessageDialog("项目名称为" + project.getName(), "这是标题", Messages.getInformationIcon());

                // 添加文档监听器以跟踪更改
                document.addDocumentListener(new DocumentListener() {
                    @Override
                    public void documentChanged(com.intellij.openapi.editor.event.DocumentEvent event) {
                        trackVersion(document);
                    }
                });

                // 显示历史版本
                showVersionHistory();
            }
        }
    }

    private VirtualFile getCurrentFile(Project project) {
        return FileEditorManager.getInstance(project).getSelectedTextEditor() != null ?
                FileDocumentManager.getInstance().getFile(FileEditorManager.getInstance(project).getSelectedTextEditor().getDocument()) :
                null;
    }

    private void trackVersion(Document document) {
        String currentVersion = document.getText();
        versionHistory.add(currentVersion);
        // 这里可以添加逻辑来保存版本到文件或数据库
    }

    private void showVersionHistory() {
        if (versionHistory.isEmpty()) {
            Messages.showMessageDialog("没有历史版本可显示。", "版本历史", Messages.getInformationIcon());
            return;
        }

        StringBuilder history = new StringBuilder("历史版本:\n");
        for (int i = 0; i < versionHistory.size(); i++) {
            history.append("版本 ").append(i + 1).append(":\n").append(versionHistory.get(i)).append("\n\n");
        }

        JTextArea textArea = new JTextArea(history.toString());
        textArea.setEditable(false);
        JOptionPane.showMessageDialog(null, new JScrollPane(textArea), "版本历史", JOptionPane.INFORMATION_MESSAGE);
    }

    public List<String> getVersionHistory() {
        return versionHistory;
    }
}


