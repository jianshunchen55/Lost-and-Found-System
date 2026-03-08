package com.campus.lostfound.config;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.campus.lostfound.entity.MapPoint;
import com.campus.lostfound.mapper.MapPointMapper;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
public class CommonMapPointsInitializer implements CommandLineRunner {
    private final MapPointMapper mapper;
    public CommonMapPointsInitializer(MapPointMapper mapper) { this.mapper = mapper; }
    @Override
    public void run(String... args) {
        // Disabled auto-initialization to prevent deleted points from reappearing
        /*
        try {
            List<MapPoint> existing = mapper.selectList(new QueryWrapper<>());
            Set<String> names = existing.stream().map(MapPoint::getName).collect(java.util.stream.Collectors.toSet());
            addIfMissing(names, "图书馆", "主校区图书馆");
            addIfMissing(names, "第一食堂", "一食堂");
            addIfMissing(names, "第二食堂", "二食堂");
            addIfMissing(names, "体育馆", "综合体育馆");
            addIfMissing(names, "教学楼A", "A座教学楼");
            addIfMissing(names, "教学楼B", "B座教学楼");
            addIfMissing(names, "实验楼", "理工实验楼");
            addIfMissing(names, "校医院", "校医院门诊部");
            addIfMissing(names, "南门", "校园南门");
            addIfMissing(names, "北门", "校园北门");
            addIfMissing(names, "宿舍区一", "学生宿舍一区");
            addIfMissing(names, "宿舍区二", "学生宿舍二区");
            addIfMissing(names, "校园超市", "校内生活超市");
        } catch (Exception ignored) {
            // ignore seeding failures to avoid startup crash
        }
        */
    }
    private void addIfMissing(Set<String> names, String name, String desc) {
        if (!names.contains(name)) {
            MapPoint p = new MapPoint();
            p.setName(name);
            p.setDescription(desc);
            p.setLatitude(0.0);
            p.setLongitude(0.0);
            mapper.insert(p);
        }
    }
}
