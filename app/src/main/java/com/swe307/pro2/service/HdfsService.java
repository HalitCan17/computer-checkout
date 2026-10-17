package com.swe307.pro2.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import tools.jackson.databind.JsonNode;

import org.springframework.beans.factory.annotation.Value;

@Service
public class HdfsService {

    private static final Logger log = LoggerFactory.getLogger(HdfsService.class);

    private final RestClient _client;
    private final String _imageDir;

    public HdfsService(@Value("${hdfs.webhdfs-url}") String webhdfsUrl,
            @Value("${hdfs.image-dir}") String imageDir) {

        _client = RestClient.builder().baseUrl(webhdfsUrl).build();
        _imageDir = imageDir;
    }

    public void upload(String fileName, byte[] content) {
        String path = _imageDir + "/" + fileName;

        JsonNode step1 = _client.put()
                .uri("/webhdfs/v1" + path + "?op=CREATE&overwrite=true&noredirect=true&user.name=root")
                .retrieve()
                .body(JsonNode.class);
        String location = step1.path("Location").asString();

        _client.put()
        .uri(java.net.URI.create(location))
        .body(content)
        .retrieve()
        .toBodilessEntity();

        log.info("HDFS: {} yuklendi ({} bayt)", path, content.length);

    }

}
