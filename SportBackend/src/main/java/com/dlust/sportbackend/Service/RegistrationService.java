package com.dlust.sportbackend.Service;

import com.dlust.sportbackend.entity.RegistrationVO;
import java.util.List;

public interface RegistrationService {

    List<RegistrationVO> getBySportsMeetingId(Long sportsMeetingId);

    void update(Long id, Integer status);

    void delete(Long id);
}
