package com.hubspot.baragon.migrations;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.baragon.data.BaragonZkMetaDatastore;
import com.hubspot.baragon.utils.JavaUtils;

@Singleton
public class ZkDataMigrationRunner {

  private static final Logger LOG = LoggerFactory.getLogger(ZkDataMigrationRunner.class);

  private final BaragonZkMetaDatastore metadataManager;
  private final Set<ZkDataMigration> migrations;

  @Inject
  public ZkDataMigrationRunner(BaragonZkMetaDatastore metadataManager, Set<ZkDataMigration> migrations) {
    this.metadataManager = metadataManager;
    this.migrations = migrations;
  }

  public int checkMigrations() {
    final long start = System.currentTimeMillis();
    final Optional<String> currentVersion = metadataManager.getZkDataVersion();
    final int intVersionNumber = Integer.parseInt(currentVersion.or("0"));

    LOG.info("Current ZK data version is {}, known migrations: {}", intVersionNumber, migrations);

    int numMigrationsApplied = 0;
    int lastAppliedMigration = intVersionNumber;

    for (ZkDataMigration migration : migrations) {
      if (migration.getMigrationNumber() > intVersionNumber) {
        numMigrationsApplied++;

        final long migrationStart = System.currentTimeMillis();

        LOG.info("Applying {}", migration);

        migration.applyMigration();

        LOG.info("Applied {} in {}", migration, JavaUtils.duration(migrationStart));

        lastAppliedMigration = migration.getMigrationNumber();
      }
    }

    if (lastAppliedMigration > intVersionNumber) {
      LOG.info("Setting new version to {}", lastAppliedMigration);
      metadataManager.setZkDataVersion(Integer.toString(lastAppliedMigration));
    }

    LOG.info("Applied {} migrations in {}", numMigrationsApplied, JavaUtils.duration(start));

    return numMigrationsApplied;
  }

}
