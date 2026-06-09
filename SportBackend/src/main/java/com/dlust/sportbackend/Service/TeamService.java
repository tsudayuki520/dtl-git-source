package com.dlust.sportbackend.Service;

import com.dlust.sportbackend.entity.Team;
import java.util.List;

public interface TeamService {
    List<Team> getBySportsMeetingId(Long sportsMeetingId);
    List<Team> getByGroupTypeId(Long groupTypeId);
    Team getById(Long id);
    void add(Team team);
    void update(Team team);
    void delete(Long id);
}
