package com.bot.ping_server.controller;

import com.bot.ping_server.PingServerApplication;
import com.bot.ping_server.utils.Log;
import com.bot.ping_server.utils.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Objects;
import java.util.Scanner;
@Controller
@RequestMapping("/updates")
public class UpdateController {

    HashMap<String, String> aboutUpdate;
    File apkClient;

    String path = PingServerApplication.getConfig().getPathData();
    public UpdateController() throws IOException, JSONException {
        apkClient = new File(path+"//client//ping.apk");
        aboutUpdate=new HashMap<>();
        getLastInfoAboutUpdate();
    }


    public void getLastInfoAboutUpdate() throws IOException, JSONException, JSONException {
        String string = new String(Files.readAllBytes(Paths.get(path+"//client//update_info.txt")), StandardCharsets.UTF_8);
        JSONObject jsonObject = new JSONObject(string);

        String version_client = jsonObject.getString("version");
        String description = jsonObject.getString("description");

        aboutUpdate.put("version", version_client);
        aboutUpdate.put("description", description);
    }


    @ResponseBody
    @GetMapping(value = "/checkVersionClient")
    public boolean checkVersionClient(@RequestParam(value = "version") String version) {
        Log log = new Log("checkVersionClient");
        Logger.logEvent(log, "Starting....");
        Logger.logEvent(log, "version: "+version);
        return !Objects.equals(version, aboutUpdate.get("version"));
    }

    @ResponseBody
    @GetMapping(value = "/aboutUpdateClient")
    public HashMap<String, String> aboutUpdateClient(){
        return aboutUpdate;
    }

    @RequestMapping(path = "/download", method = RequestMethod.GET)
    public ResponseEntity<Resource> download() throws IOException {

        HttpHeaders headers = new HttpHeaders();
        headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
        headers.add("Pragma", "no-cache");
        headers.add("Expires", "0");

        Path path = Paths.get(apkClient.getAbsolutePath());
        ByteArrayResource resource = new ByteArrayResource(Files.readAllBytes(path));

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=ping.apk")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

}
