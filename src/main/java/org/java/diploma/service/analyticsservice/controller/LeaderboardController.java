package org.java.diploma.service.analyticsservice.controller;

import org.java.diploma.service.analyticsservice.dto.LeaderboardRowResponse;
import org.java.diploma.service.analyticsservice.dto.PlayerStatsResponse;
import org.java.diploma.service.analyticsservice.service.LeaderboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/analytics")
public class LeaderboardController {

    private final LeaderboardService leaderboardService;

    public LeaderboardController(LeaderboardService leaderboardService) {
        this.leaderboardService = leaderboardService;
    }

    @GetMapping("/leaderboard")
    public List<LeaderboardRowResponse> leaderboard(@RequestParam(required = false) Integer limit) {
        return leaderboardService.getLeaderboard(limit);
    }

    @GetMapping("/players/{userId}/stats")
    public PlayerStatsResponse playerStats(@PathVariable long userId) {
        return leaderboardService.getPlayerStats(userId);
    }
}
