package com.swe307.pro2.service;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.swe307.pro2.model.Student;
import com.swe307.pro2.repository.StudentRepository;

@Service
public class PhotoService {

    private static final Set<String> ALLOWED = Set.of("jpg", "jpeg", "png");

    private final HdfsService _hdfs;
    private final StudentRepository _students;
    private final ReservationService _reservations;

    public PhotoService(HdfsService hdfs, StudentRepository students, ReservationService reservations) {
        _hdfs = hdfs;
        _students = students;
        _reservations = reservations;
    }

    // Ogrenci yoksa Optional.empty(); basariliysa yeni dosya adini doner
    public Optional<String> upload(Long studentId, MultipartFile file) throws IOException {
        Student student = _students.findById(studentId).orElse(null);
        if (student == null) {
            return Optional.empty();
        }
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Dosya secilmedi");
        }
        String ext = extensionOf(file.getOriginalFilename());
        if (!ALLOWED.contains(ext)) {
            throw new IllegalArgumentException("Sadece jpg, jpeg veya png yuklenebilir");
        }

        String oldName = student.getPhoto();
        // her yuklemede yeni ad: tarayici eski resmi onbellekten gostermesin
        String newName = "student-" + studentId + "-" + System.currentTimeMillis() + "." + ext;

        _hdfs.upload(newName, file.getBytes());                       // 1) resim HDFS'e
        student.setPhoto(newName);
        _students.save(student);                                      // 2) dosya adi MySQL'e
        _reservations.refreshStudentPhoto(studentId, newName);        // 3) Redis'teki satirlar guncellenir

        if (oldName != null && oldName.startsWith("student-")) {      // sadece kendi yukledigimiz eski dosyayi sil
            _hdfs.delete(oldName);
        }
        return Optional.of(newName);
    }

    private String extensionOf(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
    }
}