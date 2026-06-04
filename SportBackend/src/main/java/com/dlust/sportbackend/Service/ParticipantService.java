package com.dlust.sportbackend.Service;

import com.dlust.sportbackend.entity.Participant;
import java.util.List;

public interface ParticipantService {

    List<Participant> getBySportsMeetingId(Long sportsMeetingId);

    Participant getById(Long id);

    void add(Participant participant);

    void update(Participant participant);

    void delete(Long id);
}
