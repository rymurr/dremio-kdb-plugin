# Dremio kdb+ connector

[![Build Status](https://travis-ci.org/rymurr/dremio-kdb-plugin.svg?branch=master)](https://travis-ci.org/UBS-IB/dremio-kdb-plugin)

## Intro

This is a community maintained connector to enable [Dremio](https://dremio.com) to talk to a [kdb+](https://kx.com) 
timeseries database. The connector translates `ANSII SQL` into `q-sql` on the fly and submits to the `kdb+` rpc mechansim.
This connector is not an official Dremio product and support and improvements is the responsibility of the community

## Status

This is *very* beta software and is only able to handle a subset of `q-sql`. The Dremio engine will return an
error when it can't properly parse a `SQL` statement so there should be minimal amount of crashes. However it has not
extensively been tested.

## Features

The engine can currently do the following:

* all data types, including symbols and arrays
* basic `project`, `aggregate` and `filter`
* a subset of functions for date manipulation and mathematical functions
* basic handling of partitioned on disk data and `where` clause ordering
* fast and efficient conversion from the kdb ipc format to arrow buffers

It does not support:
* some aggregates (most notably avg and stddev)
* imperfect handling of the differences inherit in symbols and char arrays
* some complex and nested SQL
* joins are not pushed down
* no support for higher level q functionality and timeseries operations

The best way to use this plugin is either to use it to do simple table access on kdb tables, 
for example incremental raw reflections on tables or to query kdb+ where all the heavy lifting is 
done inside of q and a result table is made available.

## Building and Installation

1. In root directory with the pom.xml file run mvn clean install
1. Take the resulting .jar file in the target folder and put it in the \dremio\jars folder in Dremio
1. Restart Dremio

## Contributing

The project is actively looking for maintainers and contributors. Please feel free to dive in and 
improve this project.


