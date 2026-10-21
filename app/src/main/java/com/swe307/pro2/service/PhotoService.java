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
    private final StudentService _studentService;

    public PhotoService(HdfsService hdfs, StudentRepository students, StudentService studentService) {
        _hdfs = hdfs;
        _students = students;
        _studentService = studentService;
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

        _hdfs.upload(newName, file.getBytes());                       
        student.setPhoto(newName);
        _studentService.save(student);                               

        deleteFile(oldName);                                          
        return Optional.of(newName);
    }

    public boolean remove(Long studentId) {
        Student student = _students.findById(studentId).orElse(null);
        if (student == null) {
            return false;
        }
        String oldName = student.getPhoto();
        if (oldName != null) {
            student.setPhoto(null);
            _studentService.save(student);                            
            deleteFile(oldName);                                    
        }
        return true;
    }

    
    public void deleteFile(String fileName) {
        if (fileName != null && fileName.startsWith("student-")) {
            _hdfs.delete(fileName);
        }
    }

    private String extensionOf(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
    }
}