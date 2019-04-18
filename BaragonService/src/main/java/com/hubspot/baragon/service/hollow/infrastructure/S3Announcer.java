package com.hubspot.baragon.service.hol.infrastructure;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.netflix.hollow.api.producer.HollowProducer.Announcer;

public class S3Announcer implements Announcer {

  public static final String ANNOUNCEMENT_OBJECTNAME = "announced.version";

  private final AmazonS3 s3;
  private final String bucketName;
  private final String blobNamespace;

  public S3Announcer(AWSCredentials credentials, String bucketName, String blobNamespace) {
    this.s3 = getAmazonS3(credentials);
    this.bucketName = bucketName;
    this.blobNamespace = blobNamespace;
  }

  private AmazonS3 getAmazonS3(AWSCredentials credentials) {
    return AmazonS3ClientBuilder.standard().withCredentials(new AWSStaticCredentialsProvider(credentials)).build();
  }

  @Override
  public void announce(long stateVersion) {
    s3.putObject(bucketName, blobNamespace + "/" + ANNOUNCEMENT_OBJECTNAME, String.valueOf(stateVersion));
  }

}
