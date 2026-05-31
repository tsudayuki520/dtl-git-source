package com.dlust.sportbackend.Service.Impl;

import com.dlust.sportbackend.Service.BannerService;
import com.dlust.sportbackend.entity.Banner;
import com.dlust.sportbackend.Mapper.BannerMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class BannerServiceImpl implements BannerService {

    @Autowired
    private BannerMapper bannerMapper;

    @Override
    public List<Banner> getActiveBanners() {
        return bannerMapper.selectActiveBanners();
    }

    @Override
    public void addBanner(String imageUrl, String title, Integer sortOrder) {
        Banner banner = new Banner();
        banner.setImageUrl(imageUrl);
        banner.setTitle(title);
        banner.setSortOrder(sortOrder);
        banner.setStatus(1);
        bannerMapper.insertBanner(banner);
    }
}
