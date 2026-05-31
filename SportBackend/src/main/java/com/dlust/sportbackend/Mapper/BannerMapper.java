package com.dlust.sportbackend.Mapper;

import com.dlust.sportbackend.entity.Banner;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface BannerMapper {
    List<Banner> selectActiveBanners();

    void insertBanner(Banner banner);
}
