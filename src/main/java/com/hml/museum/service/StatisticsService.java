package com.hml.museum.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class StatisticsService {
    private final JdbcTemplate jdbc;

    public Map<String, Object> overview() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("total", one("select count(*) from artwork where deleted=0"));
        m.put("inStorage", one("select count(*) from artwork where deleted=0 and status_id=1"));
        m.put("onDisplay", one("select count(*) from artwork where deleted=0 and status_id=2"));
        m.put("lost", one("select count(*) from artwork where deleted=0 and status_id=3"));
        m.put("gifted", one("select count(*) from artwork where deleted=0 and status_id=4"));
        m.put("publication", one("select count(*) from artwork where deleted=0 and status_id=5"));
        m.put("auction", one("select count(*) from artwork where deleted=0 and status_id=6"));
        return m;
    }

    public List<Map<String, Object>> byCategory() {
        return jdbc.queryForList("select c.id,c.name,c.level,count(distinct r.artwork_id) artwork_count from artwork_category c left join artwork_category_relation r on r.category_id=c.id left join artwork a on a.id=r.artwork_id and a.deleted=0 where c.status=1 group by c.id,c.name,c.level,c.sort_order order by c.sort_order,c.id");
    }

    private Long one(String sql) {
        return jdbc.queryForObject(sql, Long.class);
    }
}
