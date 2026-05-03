package org.java.diploma.service.analyticsservice.repository;

import org.java.diploma.service.analyticsservice.entity.PlayerStatistics;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PlayerStatisticsRepository extends JpaRepository<PlayerStatistics, Integer> {

    Optional<PlayerStatistics> findByUserId(long userId);
}
