package com.vintagevinyl.repository;

import com.vintagevinyl.model.Record;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface RecordRepository extends JpaRepository<Record, Long> {
    // 根据标题查找唱片（不区分大小写）
    List<Record> findByTitleContainingIgnoreCase(String title);

    // 根据艺术家查找唱片（不区分大小写）
    List<Record> findByArtistContainingIgnoreCase(String artist);

    // 根据发行年份范围查找唱片
    List<Record> findByReleaseYearBetween(Integer startYear, Integer endYear);

    // 根据流派查找唱片
    List<Record> findByGenreIgnoreCase(String genre);

    // 根据价格范围查找唱片
    List<Record> findByPriceBetween(Double minPrice, Double maxPrice);

    // 找出特定艺术家的所有唱片，并按发行年份排序
    List<Record> findByArtistOrderByReleaseYearDesc(String artist);
}