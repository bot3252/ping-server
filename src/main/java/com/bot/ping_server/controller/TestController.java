package com.bot.ping_server.controller;

import com.bot.ping_server.entity.FirebaseToken;
import com.bot.ping_server.entity.NotificationMessage;
import com.bot.ping_server.service.FirebaseService;
import com.bot.ping_server.service.MediaService;
import com.bot.ping_server.service.UserService;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.UUID;
//import org.springframework.web.socket.TextMessage;

@Controller
public class TestController {

    @Lazy
    @Autowired
    public final FirebaseService firebaseMessagingService;

    public TestController(FirebaseService firebaseMessagingService) {
        this.firebaseMessagingService = firebaseMessagingService;
    }

    @GetMapping(value = "/test")
    @ResponseBody
    public String test(/*@RequestParam(value = "token") String token, @RequestParam(value = "uuid") String uuid , @RequestParam(value = "id_device") String id_device*/) throws SQLException {
//        firebaseMessagingService.setToken(uuid,token,id_devise);
//        ArrayList<String> arrayList=new ArrayList<>();
//        arrayList.add(token);
//        NotificationMessage notificationMessage = new NotificationMessage(arrayList, "test_name", "body", "");
//        firebaseMessagingService.sendNotificationByToken(notificationMessage);
//        firebaseMessagingService.setToken(uuid,token, id_device);
        return "done";
    }

    @GetMapping(value = "/test_video")
    @ResponseBody
    public ResponseEntity<StreamingResponseBody> playMediaV01(
            @RequestHeader(value = "Range", required = false)
            String rangeHeader) {
        try {
            StreamingResponseBody responseStream;
            String filePathString = "D:\\ping_data\\user_videos\\test_video.mp4";
            Path filePath = Paths.get(filePathString);
            Long fileSize = Files.size(filePath);
            byte[] buffer = new byte[1024];
            final HttpHeaders responseHeaders = new HttpHeaders();

            if (rangeHeader == null) {
                responseHeaders.add("Content-Type", "video/mp4");
                responseHeaders.add("Content-Length", fileSize.toString());
                responseStream = os -> {
                    RandomAccessFile file = new RandomAccessFile(filePathString, "r");
                    try (file) {
                        long pos = 0;
                        file.seek(pos);
                        while (pos < fileSize - 1) {
                            file.read(buffer);
                            os.write(buffer);
                            pos += buffer.length;
                        }
                        os.flush();
                    } catch (Exception e) {
                    }
                };

                return new ResponseEntity<StreamingResponseBody>
                        (responseStream, responseHeaders, HttpStatus.OK);
            }

            String[] ranges = rangeHeader.split("-");
            Long rangeStart = Long.parseLong(ranges[0].substring(6));
            Long rangeEnd;
            if (ranges.length > 1) {
                rangeEnd = Long.parseLong(ranges[1]);
            } else {
                rangeEnd = fileSize - 1;
            }

            if (fileSize < rangeEnd) {
                rangeEnd = fileSize - 1;
            }

            String contentLength = String.valueOf((rangeEnd - rangeStart) + 1);
            responseHeaders.add("Content-Type", "video/mp4");
            responseHeaders.add("Content-Length", contentLength);
            responseHeaders.add("Accept-Ranges", "bytes");
            responseHeaders.add("Content-Range", "bytes" + " " +
                    rangeStart + "-" + rangeEnd + "/" + fileSize);
            final Long _rangeEnd = rangeEnd;
            responseStream = os -> {
                RandomAccessFile file = new RandomAccessFile(filePathString, "r");
                try (file) {
                    long pos = rangeStart;
                    file.seek(pos);
                    while (pos < _rangeEnd) {
                        file.read(buffer);
                        os.write(buffer);
                        pos += buffer.length;
                    }
                    os.flush();
                } catch (Exception e) {
                }
            };

            return new ResponseEntity<StreamingResponseBody>
                    (responseStream, responseHeaders, HttpStatus.PARTIAL_CONTENT);
        } catch (FileNotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (IOException e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
