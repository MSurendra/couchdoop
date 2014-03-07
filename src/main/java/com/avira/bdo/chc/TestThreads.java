package com.avira.bdo.chc;

import com.couchbase.client.CouchbaseClient;
import net.spy.memcached.internal.BulkFuture;
import net.spy.memcached.internal.BulkGetCompletionListener;
import net.spy.memcached.internal.BulkGetFuture;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.Map;

/**
 * Created by calinburloiu on 3/6/14.
 */
public class TestThreads {

  public static void main(String[] args) throws Exception {
    System.out.println("main starts.");

    Thread thread = new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          System.out.println("Thread starts.");
          Thread.sleep(2500);
          System.out.println("Thread ends.");
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    });

    thread.start();
//    thread.join();

    Thread.sleep(500);
    System.out.println("got terminate request");
    thread.interrupt();
    System.out.println("still here");
    Thread.sleep(5000);
    System.out.println("wait ended");
  }

  /**
   * Sample with asynchronous multi-get with callback.
   */
  public void sample() throws Exception {
    CouchbaseClient couchbaseClient = new CouchbaseClient(null, null, null);
    BulkFuture<Map<String, Object>> f = couchbaseClient.asyncGetBulk(Arrays.asList(new String[]{"q", "w"}));
    f.addListener(new BulkGetCompletionListener() {
      @Override
      public void onComplete(BulkGetFuture<?> future) throws Exception {
        Map<String, ?> results = future.get();
        String user1 = results.get("user1").toString();
        System.out.println(user1);
      }
    });
  }
}
