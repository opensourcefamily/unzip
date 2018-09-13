package com.github.unzip;

import com.github.junrar.Archive;
import com.github.junrar.rarfile.FileHeader;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.tools.zip.ZipEntry;
import org.apache.tools.zip.ZipFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;

/**
 * ${解压zip或rar文件工具类}
 *
 * @author leslie
 * @create 2018-09-13 16:59
 */
@Slf4j
public class UnZip {
    private static final String ZIP = ".zip";
    private static final String RAR = ".rar";

    /**
     * 解压文件，自定义解压后文件名
     * 这里用到了synchronized ，也就是防止出现并发问题
     *
     * @param fileName        需要解压的压缩文件路径
     * @param outputDirectory 需要解压到的目录
     * @param dirName         解压后文件夹需要重命名的名称
     * @return 返回true表示成功
     */
    public static synchronized boolean unFile(String fileName, String outputDirectory, String dirName) {

        //默认失败
        boolean flag = false;
        try {
            String pName = "";
            if (fileName.toLowerCase().endsWith(ZIP)) {
                //zip压缩文件
                pName = unZipFile(fileName, outputDirectory);
            } else if (fileName.toLowerCase().endsWith(RAR)) {
                //rar压缩文件
                pName = unRarFile(fileName, outputDirectory);
            } else {//不支持的压缩文件
                log.error("压缩文件格式非zip或rar");
            }
            if (pName != null && !"".equals(pName)) {
                if (dirName != null) {
                    //============修改文件夹名称开始==================
                    //旧文件夹
                    File from = new File(outputDirectory + File.separator + pName);
                    //新文件夹
                    File to = new File(outputDirectory + File.separator + dirName);
                    //判断是否存在,存在先删除
                    if (to.exists()) {
                        //删除,这里调用下面的删除方法
                        deleteFile(to);
                    }
                    //修改成功
                    if (from.renameTo(to)) {
                        //设置成功标志
                        flag = true;
                    }
                    //============修改文件夹名称结束==================
                }
            }
        } catch (Exception e) {//异常
            log.error(e.toString());
        }
        return flag;
    }

    /**
     * 解压文件,默认使用原压缩文件名
     *
     * @param fileName        需要解压的压缩文件路径
     * @param outputDirectory 需要解压到的目录
     * @return 返回true表示成功
     */
    public static boolean unFile(String fileName, String outputDirectory) {
        return unFile(fileName, outputDirectory, null);
    }

    public static void main(String[] args) {
        unFile("d:/登录注册系列.rar", "d:/");
    }

    /**
     * 解压zip文件方法
     *
     * @param zipFilename     要解压的zip包文件
     * @param outputDirectory 解压后存放的目录
     * @return 返回解压后目录名称
     * @throws Exception
     */
    private static String unZipFile(String zipFilename, String outputDirectory) throws Exception {
        //保存解压后的文件夹名称
        String strName = "";
        File zf = new File(zipFilename);
        if ((!zf.exists()) && (zf.length() <= 0)) {
            throw new Exception("要解压的文件不存在!");
        }
        //创建指定解压后存放的目录对象
        File outFile = new File(outputDirectory);
        //判断文件是否存在,不存在则先创建
        if (!outFile.exists()) {
            outFile.mkdirs();//不存在则创建
        }

        //创建对象
        ZipFile zipFile = new ZipFile(zipFilename);
        Enumeration en = zipFile.getEntries();

        //判断是否有下一个
        while (en.hasMoreElements()) {
            ZipEntry zipEntry = (ZipEntry) en.nextElement();
            //表示是文件夹
            if (zipEntry.isDirectory()) {
                //得到文件夹名称
                String dirName = zipEntry.getName();
                //去掉后面的/ (如： demo/ss/ ,去掉后面的/)
                dirName = dirName.substring(0, dirName.length() - 1);
                //创建文件夹对象
                File f = new File(outFile.getPath() + File.separator + dirName);
                f.mkdirs();//创建文件夹

                //判断是否是最上层目录(没有目录分割符表示最上层,如：demo)
                if (dirName.indexOf("/") <= 0 && dirName.indexOf("\\") <= 0) {
                    //保存解压后的文件夹名称(解压后文件夹目录名称)
                    strName = dirName;
                }
            } else {//表示是文件
                //创建文件对象
                File f = new File(outFile.getPath() + File.separator + zipEntry.getName());
                //========判断文件不存在的话，就创建该文件所在文件夹的目录 ==开始=======
                if (!f.exists()) {
                    String[] arrFolderName = zipEntry.getName().split("/");
                    String strRealFolder = "";

                    for (int i = 0; i < (arrFolderName.length - 1); i++) {
                        strRealFolder += arrFolderName[i] + File.separator;
                    }
                    //创建文件夹目录对象
                    File tempDir = new File(outFile.getPath() + File.separator + strRealFolder);
                    tempDir.mkdirs();//此处使用.mkdirs()方法,而不能用.mkdir()
                }
                //========判断文件不存在的话，就创建该文件所在文件夹的目录 ==结束=======
                f.createNewFile();
                InputStream in = zipFile.getInputStream(zipEntry);
                FileOutputStream out = new FileOutputStream(f);
                try {
                    int c;
                    byte[] by = new byte[1024];
                    while ((c = in.read(by)) != -1) {
                        out.write(by, 0, c);
                    }
                } catch (IOException e) {
                    log.error(e.toString());
                } finally {
                    out.close();
                    in.close();
                }
            }
        }
        return strName;
    }

    /**
     * 解压rar文件的方法
     *
     * @param rarFilename     需要解压的rar文件路径
     * @param outputDirectory 解压后存放的目录
     * @return 返回解压后目录名称
     * @throws Exception
     */
    private static String unRarFile(String rarFilename, String outputDirectory) throws Exception {
        //保存解压后的文件夹名称
        String strName = "";
        File rf = new File(rarFilename);
        if ((!rf.exists()) && (rf.length() <= 0)) {
            throw new Exception("要解压的文件不存在!");
        }
        //创建指定解压后存放的目录对象
        File dstDiretory = new File(outputDirectory);
        //判断文件是否存在,不存在则先创建
        if (!dstDiretory.exists()) {
            dstDiretory.mkdirs();//创建目录
        }
        Archive a = null;
        try {
            a = new Archive(new File(rarFilename), null);
            if (a != null) {
                FileHeader fh = a.nextFileHeader();
                while (fh != null) {
                    //防止文件名中文乱码问题的处理
                    String dirName = StringUtils.isBlank(fh.getFileNameW()) ? fh.getFileNameString() : fh.getFileNameW();
                    //表示是文件夹
                    if (fh.isDirectory()) {
                        String tmpUrl = outputDirectory + File.separator + dirName;

                        //该操作是将\分割符转换成当前系统的分割符,这操作是怕在linux系统中不识别\分割符
                        // windows系统分割符是\ , linux系统分隔符是 /
                        //如路径 C:\demo\tmp\dir 在windows下正常,在linux下就会出错
                        //在linux下C:\demo\tmp\dir转换后为 C:/demo/tmp/dir
                        if ("/".equals(File.separator)) {
                            tmpUrl = tmpUrl.replaceAll("\\\\", File.separator);
                        }
                        //创建文件对象
                        File fol = new File(tmpUrl);
                        fol.mkdirs();//创建文件夹

                        //判断是否是最上层目录(没有目录分割符表示最上层,如：demo)
                        if (dirName.indexOf("/") <= 0 && dirName.indexOf("\\") <= 0) {
                            //保存解压后的文件夹名称(解压后文件夹目录名称)
                            strName = dirName;
                        }
                    } else {//表示是文件
                        String tmpUrl = outputDirectory + File.separator + dirName.trim();
                        //该操作是将\分割符转换成当前系统的分割符,这操作是怕在linux系统中不识别\分割符
                        // windows系统分割符是\ , linux系统分隔符是 /
                        //如路径 C:\demo\tmp\dir 在windows下正常,在linux下就会出错
                        //在linux下C:\demo\tmp\dir转换后为 C:/demo/tmp/dir
                        if ("/".equals(File.separator)) {
                            tmpUrl = tmpUrl.replaceAll("\\\\", File.separator);
                        }
                        //创建文件对象
                        File out = new File(tmpUrl);
                        FileOutputStream os = null;
                        try {
                            //判断文件是否存在
                            if (!out.exists()) {
                                //判断文件所在的目录是否存在,不存在则先创建
                                if (!out.getParentFile().exists()) {
                                    out.getParentFile().mkdirs();//创建目录
                                }
                                out.createNewFile();//创建文件
                            }
                            os = new FileOutputStream(out);
                            a.extractFile(fh, os);
                        } catch (Exception ex) {
                            log.error(ex.toString());
                        } finally {
                            os.close();
                        }
                    }
                    fh = a.nextFileHeader();
                }
            }
        } catch (Exception e) {
            log.error(e.toString());
        } finally {
            a.close();
        }
        return strName;
    }

    /**
     * 删除文件方法
     *
     * @param file 表示要删除的文件对象
     */
    private static void deleteFile(File file) {
        //首先判断文件是否存在
        if (file.exists()) {
            //判断是文件
            if (file.isFile()) {
                file.delete();//删除文件
            } else if (file.isDirectory()) {
                //若是目录,获取该目录下的所有文件及目录
                File[] files = file.listFiles();
                //遍历
                for (int i = 0; i < files.length; i++) {
                    //自己调用自己,迭代删除
                    deleteFile(files[i]);
                }
            }
            file.delete();//最后删除剩余的空文件夹
        }
    }
}
