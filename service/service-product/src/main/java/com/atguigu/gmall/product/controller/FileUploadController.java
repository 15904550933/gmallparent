package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import org.apache.commons.io.FilenameUtils;
import org.csource.fastdfs.ClientGlobal;
import org.csource.fastdfs.StorageClient1;
import org.csource.fastdfs.TrackerClient;
import org.csource.fastdfs.TrackerServer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 上传文件类  fastdfs-client-java jar包提供的依赖
 * @author smy
 * @create 2020-06-10 15:54
 */
@RestController
@RequestMapping("admin/product/")
public class FileUploadController {

    //引入图片服务器地址  需要在application.yml文件配置
    @Value("${fileServer.url}")
    private String fileUrl;

    /**
     * 当图片上传完之后，回显图片，本质是拼接图片路径字符串
     * @param file
     * @return
     * @throws Exception
     */
    @RequestMapping("fileUpload")
    public Result<String> fileUpload(MultipartFile file) throws Exception{
        //读取到tracker.confwe文件  项目路径不能有中文或者符号
        String configFile = this.getClass().getResource("/tracker.conf").getFile();
        String path = null;

        if (configFile != null){
            // 初始化
            ClientGlobal.init(configFile);
            // 创建trackerClient
            TrackerClient trackerClient = new TrackerClient();
            // 获取trackerService
            TrackerServer trackerServer = trackerClient.getConnection();
            // 创建storageClient1
            StorageClient1 storageClient1 = new StorageClient1(trackerServer, null);
            //获取文件后缀
            String extName = FilenameUtils.getExtension(file.getOriginalFilename());
            //开始上传文件，并获取上传的图片路径   file.getBytes()：获取文件的字节数组
            path = storageClient1.upload_appender_file1(file.getBytes(), extName , null);
            System.out.println("上传文件的完整路径："+fileUrl + path);
        }
        //拼接完整的图片地址返回（包含服务器地址）
        return Result.ok(fileUrl+path);
    }
}
