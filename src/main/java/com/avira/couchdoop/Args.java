/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.avira.couchdoop;

import org.apache.commons.cli.*;
import org.apache.hadoop.conf.Configuration;

/**
 * Contract for JavaBeans which hold information read from the properties of a Hadoop
 * {@link org.apache.hadoop.conf.Configuration} file. This configuration in turn can be passed directly or may be read
 * from command line with Apache commons-cli library.
 */
public abstract class Args {

  protected Configuration hadoopConfiguration;

  public static class ArgDef {
    char shortName;
    String propertyName;

    public ArgDef(char shortName, String propertyName) {
      this.shortName = shortName;
      this.propertyName = propertyName;
    }

    public char getShortName() {
      return shortName;
    }

    public String getLongName() {
      return propertyName.toLowerCase().replace('.', '-');
    }

    public String getPropertyName() {
      return propertyName;
    }
  }

  public Args() {
    this.hadoopConfiguration = new Configuration();
  }

  public Args(Configuration hadoopConfiguration) throws ArgsException {
    this.hadoopConfiguration = hadoopConfiguration;

    loadHadoopConfiguration();
  }

  /**
   * Create an instance from the command line arguments. Pass null to hadoopConfiguration is only checking the
   * arguments is required. Otherwise the Configuration is updated with the data parsed from the arguments.
   *
   * @deprecated Use one of the other constructors and then call
   * @param hadoopConfiguration Hadoop configuration instance to be used and updated with the arguments data
   * @param cliArgs command line arguments from the main class
   */
  @Deprecated
  public Args(Configuration hadoopConfiguration, String[] cliArgs) throws ArgsException {
    this.hadoopConfiguration = hadoopConfiguration;

    loadCliArgs(cliArgs);
  }

  public void loadCliArgs(String[] cliArgs) throws ArgsException {
    CommandLineParser parser = new PosixParser();
    CommandLine cl = null;
    try {
      cl = parser.parse(getCliOptions(), cliArgs);
    } catch (ParseException e) {
      System.err.println(e.getMessage());
      HelpFormatter formatter = new HelpFormatter();
      formatter.setOptionComparator(null);
      formatter.printHelp("...", getCliOptions());

      throw new ArgsException(e);
    }

    if (hadoopConfiguration != null && cl != null) {
      loadCliArgsIntoHadoopConfiguration(cl);
      loadHadoopConfiguration();
    }
  }

  /**
   * This method defines the command line interface options for this Args implementation.
   */
  protected abstract Options getCliOptions();

  /**
   * Populates JavaBean instance fields with data from Hadoop configuration member.
   */
  protected abstract void loadHadoopConfiguration() throws ArgsException;

  /**
   * Queries a {@link org.apache.commons.cli.CommandLine} instance and populates the Hadoop configuration member.
   *
   * @param cl an object which contains user's parsed arguments
   * @throws ParseException
   */
  protected abstract void loadCliArgsIntoHadoopConfiguration(CommandLine cl) throws ArgsException;

  public void setHadoopConfiguration(Configuration hadoopConfiguration) {
    this.hadoopConfiguration = hadoopConfiguration;
  }

  protected void addOption(Options options, ArgDef arg, boolean hasArg, boolean isRequired, String description) {
    Option option = new Option(arg.getShortName() + "", arg.getLongName(), hasArg, description);
    option.setRequired(isRequired);
    options.addOption(option);
  }

  protected void setPropertyFromCliArg(CommandLine cl, ArgDef arg) {
    String argValue = cl.getOptionValue(arg.getShortName());

    if (argValue != null) {
      hadoopConfiguration.set(arg.getPropertyName(), argValue);
    }
  }

  public Configuration getHadoopConfiguration() {
    return hadoopConfiguration;
  }
}
