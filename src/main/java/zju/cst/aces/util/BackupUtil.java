package zju.cst.aces.util;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

public class BackupUtil {
    // 备份目标文件夹(target)到指定位置
    public static void backupTargetFolder(String sourceFolderPath, String backupFolderPath) throws IOException {
        File sourceFolder = new File(sourceFolderPath);
        File backupFolder = new File(backupFolderPath);

        // 如果备份文件夹不存在，创建它
        if (!backupFolder.exists()) {
            backupFolder.mkdirs();
        }

        // 备份目标文件夹到备份文件夹
        FileUtils.copyDirectory(sourceFolder, backupFolder);
    }

    // 恢复备份的目标文件夹(backup)到指定位置
    public static void restoreTargetFolder(String backupFolderPath, String targetFolderPath) throws IOException {
        File backupFolder = new File(backupFolderPath);
        File targetFolder = new File(targetFolderPath);
        //如果没有backupFolder，说明这是第一次运行，那就创建备份文件夹

        // 清空目标文件夹
        if (targetFolder.exists()) {
            FileUtils.cleanDirectory(targetFolder);
        }

        // 恢复备份的文件夹到目标文件夹(复制）
        FileUtils.copyDirectory(backupFolder, targetFolder);
    }

    // 删除备份文件夹
    public static void deleteBackupFolder(String backupFolderPath) throws IOException {
        File backupFolder = new File(backupFolderPath);

        // 判断备份文件夹是否存在
        if (backupFolder.exists()) {
            FileUtils.deleteDirectory(backupFolder);
        }
    }
}
