package com.hubspot.baragon.service.hol;

import com.netflix.hollow.api.consumer.HollowConsumer.AnnouncementWatcher;
import com.netflix.hollow.api.consumer.HollowConsumer.BlobRetriever;
import com.netflix.hollow.api.consumer.fs.HollowFilesystemAnnouncementWatcher;
import com.netflix.hollow.api.consumer.fs.HollowFilesystemBlobRetriever;
import com.netflix.hollow.api.producer.HollowProducer;
import com.netflix.hollow.api.producer.HollowProducer.Announcer;
import com.netflix.hollow.api.producer.HollowProducer.Publisher;
import com.netflix.hollow.api.producer.fs.HollowFilesystemAnnouncer;
import com.netflix.hollow.api.producer.fs.HollowFilesystemPublisher;

import com.hubspot.baragon.service.hol.datamodel.BaragonHollowState;
import com.hubspot.baragon.service.hol.datamodel.StateDataRetriver;

import java.io.File;
import java.util.Optional;

public class Producer {

  public static final String SCRATCH_DIR = System.getProperty("java.io.tmpdir");
  private static final long MIN_TIME_BETWEEN_CYCLES = 10000;

  public static void main(String args[]) {
    File publishDir = new File(SCRATCH_DIR, "publish-dir");
    publishDir.mkdir();

    System.out.println("I AM THE PRODUCER.  I WILL PUBLISH TO " + publishDir.getAbsolutePath());

    Publisher publisher = new HollowFilesystemPublisher(publishDir);
    Announcer announcer = new HollowFilesystemAnnouncer(publishDir);

    BlobRetriever blobRetriever = new HollowFilesystemBlobRetriever(publishDir);
    AnnouncementWatcher announcementWatcher = new HollowFilesystemAnnouncementWatcher(publishDir);

    HollowProducer producer = HollowProducer.withPublisher(publisher)
        .withAnnouncer(announcer)
        .build();

    producer.initializeDataModel(BaragonHollowState.class);

    restoreIfAvailable(producer, blobRetriever, announcementWatcher);

    cycleForever(producer);

  }

  public static void restoreIfAvailable(HollowProducer producer,
                                        BlobRetriever retriever,
                                        AnnouncementWatcher unpinnableAnnouncementWatcher) {

    System.out.println("ATTEMPTING TO RESTORE PRIOR STATE...");
    long latestVersion = unpinnableAnnouncementWatcher.getLatestVersion();
    if (latestVersion != AnnouncementWatcher.NO_ANNOUNCEMENT_AVAILABLE) {
      producer.restore(latestVersion, retriever);
      System.out.println("RESTORED " + latestVersion);
    } else {
      System.out.println("RESTORE NOT AVAILABLE");
    }
  }


  public static void cycleForever(HollowProducer producer) {
    final StateDataRetriver sourceDataRetriever = new StateDataRetriver(); //TODO: fix the state retriver

    long lastCycleTime = Long.MIN_VALUE;
    while (true) {
      waitForMinCycleTime(lastCycleTime);
      lastCycleTime = System.currentTimeMillis();
      producer.runCycle(writeState -> {
        Optional<BaragonHollowState> baragonHollowState = sourceDataRetriever.getBaragonHollowState();
        writeState.add(baragonHollowState);
      });
    }
  }

  private static void waitForMinCycleTime(long lastCycleTime) {
    long targetNextCycleTime = lastCycleTime + MIN_TIME_BETWEEN_CYCLES;

    while (System.currentTimeMillis() < targetNextCycleTime) {
      try {
        Thread.sleep(targetNextCycleTime - System.currentTimeMillis());
      } catch (InterruptedException ignore) { }
    }
  }
}
