package com.avira.bdo.chc.update;

import com.avira.bdo.chc.ArgsException;
import com.avira.bdo.chc.exp.CouchbaseAction;
import com.avira.bdo.chc.exp.CouchbaseOutputFormat;
import com.avira.bdo.chc.exp.ExportArgs;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class BenchmarkUpdater extends Configured implements Tool {

  private static final Logger LOGGER = LoggerFactory.getLogger(BenchmarkUpdater.class);

  public void start(String[] args) throws ArgsException {
    int exitCode = 0;
    try {
      exitCode = ToolRunner.run(this, args);
    } catch (ArgsException e) {
      throw e;
    } catch (Exception e) {
      LOGGER.error(ExceptionUtils.getStackTrace(e));
    }

    System.exit(exitCode);
  }

  @Override
  public int run(String[] args) throws Exception {
    Configuration conf = getConf();
    ExportArgs exportArgs;
    exportArgs = new ExportArgs(conf, args);

    Job job;
    boolean exitStatus = true;
    try {
      job = configureJob(conf, exportArgs.getInput());
      exitStatus = job.waitForCompletion(true);
    } catch (Exception e) {
      LOGGER.error(ExceptionUtils.getStackTrace(e));
    }

    return exitStatus ? 0 : 2;
  }

  public Job configureJob(Configuration conf, String input) throws IOException {
    conf.setInt("mapreduce.map.failures.maxpercent", 5);
    conf.setInt("mapred.max.map.failures.percent", 5);
    conf.setInt("mapred.max.tracker.failures", 20);

    Job job = new Job(conf);
    job.setJarByClass(BenchmarkUpdater.class);

    // Input
    FileInputFormat.setInputPaths(job, input);

    // Mapper
    job.setMapperClass(BenchmarkUpdateMapper.class);
    job.setMapOutputKeyClass(String.class);
    job.setMapOutputValueClass(CouchbaseAction.class);

    // Reducer
    job.setNumReduceTasks(0);

    // Output
    job.setOutputFormatClass(CouchbaseOutputFormat.class);
    job.setMapOutputKeyClass(String.class);
    job.setMapOutputValueClass(CouchbaseAction.class);

    return job;
  }
}
