package com.dlust.sportbackend.Service;

import org.springframework.web.multipart.MultipartFile;
import java.util.Map;

public interface ImportService {
    Map<String, Object> importParticipants(MultipartFile file, Long sportsMeetingId);
}
