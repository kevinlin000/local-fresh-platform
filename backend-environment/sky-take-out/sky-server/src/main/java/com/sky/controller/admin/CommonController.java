package com.sky.controller.admin;

import com.sky.constant.MessageConstant;
import com.sky.result.Result;
import com.sky.utils.AwsS3Util;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.bridge.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

/**
 * 通用介面
 */
@RestController
@RequestMapping("/admin/common")
@Tag(name = "通用介面")
@Slf4j
public class CommonController {

    @Autowired
    private AwsS3Util awsS3Util;

    /**
     * 檔案上傳
     * @param file
     * @return
     */
    @PostMapping("/upload")
    @Operation(summary = "檔案上傳")
    public Result<String> upload(MultipartFile file) {
        log.info("檔案上傳：{}", file);
        try {
            //原始文件名
            String originalFilename = file.getOriginalFilename();
            //擷取原始文件名的後綴 dfdf.png
            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            //構建新文件名稱
            String objectName = UUID.randomUUID().toString() + extension;
            //文件的請求路徑
            String filePath = awsS3Util.upload(file.getBytes(), objectName);
            return Result.success(filePath);

        } catch (IOException e) {
            log.error("檔案上傳失敗：{}",e);
        }


        return Result.error(MessageConstant.UPLOAD_FAILED);
    }
}
