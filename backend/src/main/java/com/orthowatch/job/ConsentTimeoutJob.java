package com.orthowatch.job;

import com.orthowatch.model.Alert;
import com.orthowatch.model.Episode;
import com.orthowatch.repository.AlertRepository;
import com.orthowatch.repository.EpisodeRepository;
import java.util.UUID;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ConsentTimeoutJob implements Job {

  private static final Logger logger = LoggerFactory.getLogger(ConsentTimeoutJob.class);

  @Autowired private EpisodeRepository episodeRepository;

  @Autowired private AlertRepository alertRepository;

  @Override
  public void execute(JobExecutionContext context) throws JobExecutionException {
    String episodeIdStr = context.getJobDetail().getJobDataMap().getString("episodeId");
    UUID episodeId = UUID.fromString(episodeIdStr);

    logger.info("Consent timeout job executing for episodeId={}", episodeId);

    episodeRepository
        .findById(episodeId)
        .ifPresent(
            episode -> {
              if ("PENDING".equals(episode.getConsentStatus())) {
                createConsentTimeoutAlert(episode);
                logger.warn(
                    "Consent timeout: patient did not respond within 24h. episodeId={}",
                    episodeId);
              } else {
                logger.info(
                    "Consent already resolved (status={}). No alert needed. episodeId={}",
                    episode.getConsentStatus(),
                    episodeId);
              }
            });
  }

  private void createConsentTimeoutAlert(Episode episode) {
    Alert alert =
        Alert.builder()
            .episode(episode)
            .alertType("CONSENT_TIMEOUT")
            .severity("MEDIUM")
            .assignedTo(episode.getPrimarySurgeon())
            .status("PENDING")
            .build();
    alertRepository.save(alert);
    logger.info(
        "Created CONSENT_TIMEOUT alert for episodeId={}, assigned to surgeonId={}",
        episode.getId(),
        episode.getPrimarySurgeon().getId());
  }
}
