package tongji.ggyl.versioncontrol;


import tongji.ggyl.eventlistening.Snapshot;

import java.util.List;

/**
 * VersionControl 是版本控制的接口，定义了保存代码快照的方法。
 */
public interface VersionControl {

    /**
     * @param snapshot Snapshot 对象，包含文件的快照信息
     */

    void saveVersionSnapshot(Snapshot snapshot);

    // 查询某个文件的历史快照
    List<Snapshot> getSnapshotsForFile(String path);
    List<Snapshot> getAllSnapshots();
    void deleteSnapshotsForFile(String path);
    void changeSnapshotsPathForFile(String oldPath,String newPath);
}
