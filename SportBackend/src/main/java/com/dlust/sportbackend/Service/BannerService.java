package com.dlust.sportbackend.Service;

import com.dlust.sportbackend.entity.Banner;
import java.util.List;

public interface BannerService {
    List<Banner> getActiveBanners();
    void addBanner(String imageUrl, String title, Integer sortOrder);
}
