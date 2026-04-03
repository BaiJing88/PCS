package com.pcs.central.controller;

import com.pcs.central.database.RatingRepository;
import com.pcs.central.model.entity.RatingEntity;
import com.pcs.central.service.RatingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/ratings")
public class RatingController {
    
    private final RatingService ratingService;
    private final RatingRepository ratingRepository;
    
    public RatingController(RatingService ratingService, RatingRepository ratingRepository) {
        this.ratingService = ratingService;
        this.ratingRepository = ratingRepository;
    }
    
    /**
     * 获取指定玩家的评分历史
     * @param playerName 玩家名称
     * @param limit 返回的最大记录数（可选，默认50）
     * @return 评分历史列表
     */
    @GetMapping("/history/{playerName}")
    public ResponseEntity<Map<String, Object>> getPlayerRatingHistory(
            @PathVariable String playerName,
            @RequestParam(defaultValue = "50") int limit) {
        
        List<RatingEntity> ratings = ratingRepository.findByTargetNameOrderByRatedAtDesc(playerName);
        
        if (ratings == null || ratings.isEmpty()) {
            return ResponseEntity.ok(Map.of(
                "success", true,
                "playerName", playerName,
                "ratings", new ArrayList<>(),
                "totalCount", 0,
                "averageScore", 0.0,
                "averageWeightedScore", 0.0
            ));
        }
        
        // 限制返回数量
        List<RatingEntity> limitedRatings = ratings.stream()
            .limit(limit)
            .collect(Collectors.toList());
        
        // 计算统计信息
        double avgScore = ratings.stream()
            .mapToDouble(RatingEntity::getScore)
            .average()
            .orElse(0.0);
            
        double avgWeightedScore = ratings.stream()
            .mapToDouble(RatingEntity::getWeightedScore)
            .average()
            .orElse(0.0);
        
        // 格式化响应
        List<Map<String, Object>> ratingList = limitedRatings.stream()
            .map(this::formatRatingResponse)
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "playerName", playerName,
            "ratings", ratingList,
            "totalCount", ratings.size(),
            "averageScore", Math.round(avgScore * 100.0) / 100.0,
            "averageWeightedScore", Math.round(avgWeightedScore * 100.0) / 100.0
        ));
    }
    
    /**
     * 获取评分者给过的评分记录
     * @param raterName 评分者名称
     * @param limit 返回的最大记录数（可选，默认50）
     * @return 评分记录列表
     */
    @GetMapping("/given/{raterName}")
    public ResponseEntity<Map<String, Object>> getRatingsGivenByPlayer(
            @PathVariable String raterName,
            @RequestParam(defaultValue = "50") int limit) {
        
        List<RatingEntity> ratings = ratingRepository.findByRaterNameOrderByRatedAtDesc(raterName);
        
        if (ratings == null || ratings.isEmpty()) {
            return ResponseEntity.ok(Map.of(
                "success", true,
                "raterName", raterName,
                "ratings", new ArrayList<>(),
                "totalCount", 0
            ));
        }
        
        List<RatingEntity> limitedRatings = ratings.stream()
            .limit(limit)
            .collect(Collectors.toList());
        
        List<Map<String, Object>> ratingList = limitedRatings.stream()
            .map(this::formatRatingResponse)
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "raterName", raterName,
            "ratings", ratingList,
            "totalCount", ratings.size()
        ));
    }
    
    /**
     * 获取服务器全局评分统计
     * @return 评分统计信息
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getRatingStats() {
        
        // 获取今日评分数量
        LocalDateTime startOfToday = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        int todayRatings = ratingRepository.countTodayRatings(startOfToday);
        
        // 获取总评分数量
        long totalRatings = ratingRepository.count();
        
        // 获取评分者数量
        int raterCount = ratingRepository.countDistinctRaters();
        
        // 获取被评分玩家数量
        int targetCount = ratingRepository.countDistinctTargets();
        
        // 获取平均分
        Double averageScore = ratingRepository.getAverageScore();
        Double averageWeightedScore = ratingRepository.getAverageWeightedScore();
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "todayRatings", todayRatings,
            "totalRatings", totalRatings,
            "raterCount", raterCount,
            "targetCount", targetCount,
            "averageScore", averageScore != null ? Math.round(averageScore * 100.0) / 100.0 : 0.0,
            "averageWeightedScore", averageWeightedScore != null ? Math.round(averageWeightedScore * 100.0) / 100.0 : 0.0
        ));
    }
    
    /**
     * 获取今日评分统计（用于admin面板）
     */
    @GetMapping("/today")
    public ResponseEntity<Map<String, Object>> getTodayRatings() {
        LocalDateTime startOfToday = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        
        List<RatingEntity> todayRatings = ratingRepository.findTodayRatings(startOfToday);
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "count", todayRatings != null ? todayRatings.size() : 0,
            "ratings", todayRatings != null ? 
                todayRatings.stream().limit(50).map(this::formatRatingResponse).collect(Collectors.toList()) :
                new ArrayList<>()
        ));
    }
    
    /**
     * 格式化评分记录为响应格式
     */
    private Map<String, Object> formatRatingResponse(RatingEntity rating) {
        Map<String, Object> map = new HashMap<>();
        map.put("ratingId", rating.getRatingId());
        map.put("raterName", rating.getRaterName());
        map.put("raterUuid", rating.getRaterUuid());
        map.put("targetName", rating.getTargetName());
        map.put("targetUuid", rating.getTargetUuid());
        map.put("score", rating.getScore());
        map.put("weightedScore", Math.round(rating.getWeightedScore() * 100.0) / 100.0);
        map.put("weight", Math.round(rating.getWeight() * 100.0) / 100.0);
        map.put("comment", rating.getComment() != null && !rating.getComment().isEmpty() ? rating.getComment() : "");
        map.put("serverId", rating.getServerId());
        map.put("ratedAt", rating.getRatedAt() != null ? 
            rating.getRatedAt().format(DateTimeFormatter.ISO_DATE_TIME) : "");
        return map;
    }
}
