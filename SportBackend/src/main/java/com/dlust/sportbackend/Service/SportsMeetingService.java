package com.dlust.sportbackend.Service;

import com.dlust.sportbackend.entity.SportsMeeting;
import java.util.List;

public interface SportsMeetingService {
    List<SportsMeeting> getAll();
    List<SportsMeeting> search(String keyword);
    SportsMeeting getById(Long id);
    void add(SportsMeeting sportsMeeting);
    void update(SportsMeeting sportsMeeting);
    void delete(Long id);
}
