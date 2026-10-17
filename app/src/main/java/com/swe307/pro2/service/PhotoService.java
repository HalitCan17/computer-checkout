package com.swe307.pro2.service;

import java.io.IOException;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.swe307.pro2.dto.EmpDto;

@Service
public class PhotoService {

    private static final Logger log = LoggerFactory.getLogger(PhotoService.class);
    private static final long MAX_BYTES = 2L * 1024 * 1024;

    private final EmpService _emps;
    private final HdfsService _hdfs;

    public PhotoService(EmpService emps, HdfsService hdfs) {
        _emps = emps;
        _hdfs = hdfs;
    }

    public Optional<EmpDto> upload(Integer empno, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Dosya bos");
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new IllegalArgumentException("Dosya okunamadi");
        }
        if (bytes.length > MAX_BYTES) {
            throw new IllegalArgumentException("Dosya 2 MB'tan buyuk olamaz");
        }

        String ext = switch (String.valueOf(file.getContentType())) {
            case "image/jpeg" -> "jpg";
            case "image/png" -> "png";
            default -> throw new IllegalArgumentException("Sadece JPG veya PNG yuklenebilir");
        };

        if (!_emps.exists(empno)) {
            return Optional.empty();
        }

        String fileName = empno + "-" + System.currentTimeMillis() + "." + ext;
        _hdfs.upload(fileName, bytes);
        log.info("PHOTO: {} personeli icin {} HDFS'e yazildi", empno, fileName);

        return _emps.updateImage(empno, fileName);
    }

}