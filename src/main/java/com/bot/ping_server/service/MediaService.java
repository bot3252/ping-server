package com.bot.ping_server.service;

import com.bot.ping_server.PingServerApplication;
import com.bot.ping_server.utils.ImageUtil;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

@Service
public class MediaService {
    static String path = PingServerApplication.getConfig().getPathData()+"\\user_files";
    public static void saveFile(MultipartFile fileToSave, String uuidFile) throws IOException {
        String extension = FilenameUtils.getExtension(fileToSave.getOriginalFilename());

        if (fileToSave == null) {
            throw new NullPointerException("fileToSave is null");
        }

//        + uuidFile+"."+extension

        var targetFile = new File(path +"\\"+ uuidFile+"."+extension);
        if (!Objects.equals(targetFile.getParent(), path)) {
            throw new SecurityException("Unsupported filename!");
        }
        Files.copy(fileToSave.getInputStream(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    public static File getDownloadFile(String fileName) throws Exception {
        if (fileName == null) {
            throw new NullPointerException("fileName is null");
        }
        var fileToDownload = new File(path + "\\" + fileName);
        if (!Objects.equals(fileToDownload.getParent(), path)) {
            throw new SecurityException("Unsupported filename!");
        }
        if (!fileToDownload.exists()) {
            throw new FileNotFoundException("No file named: " + fileName);
        }
        return fileToDownload;
    }
}
