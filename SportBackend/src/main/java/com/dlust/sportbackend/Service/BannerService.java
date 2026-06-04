package com.dlust.sportbackend.Service;

import com.dlust.sportbackend.entity.Banner;
import java.util.List;

public interface BannerService {
    List<Banner> getActiveBanners();
    List<Banner> getAll();
    Banner getById(Long id);
    void addBanner(String imageUrl, String title, Integer sortOrder);
    void update(Banner banner);
    void delete(Long id);
}
