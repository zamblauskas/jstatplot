
About
===========
Visualize `jstat -gc` results.

Usage
===========
``` shell
Usage: jstatplot [options] <file>...

  <file>...
        One or more of jstat result files.
  -u <value> | --unit <value>
        Size unit to display in graphs. Available values are [MB, GB].
  -w <value> | --width <value>
        Graph width
  -h <value> | --height <value>
        Graph height
  -y <value> | --range-y <value>
        Fix Y axis range upper value. By default max value in data will be used.
  -s <value> | --skip <value>
        Number of lines to skip before actual jstat result begins. You should use this option if you write any additional information at the beginning of jstat result file (e.g. start time, configuration parameters).
```

Runtime dependencies
===========
[GnuPlot](http://www.gnuplot.info/) must be available on your system.

Examples
===========
Produce jstat result file:
``` shell
/usr/bin/jstat -gc -t <pid> 1000 > jstat-result.txt
```
Create graphs:
``` shell
jstatplot jstat-result.txt
```
Produced graphs:

![Capacity](http://i.imgur.com/PgtJRsd.png)

![Utilization](http://i.imgur.com/aZUhlK5.png)

![GC events](http://i.imgur.com/YECYLgA.png)

![GC time](http://i.imgur.com/a6Phgv5.png)

Build it
===========
Using [SBT](http://www.scala-sbt.org/).

To create a local executable at `jstatplot/target/universal/stage`:
``` shell
sbt stage
```

To create a distributable `tgz` package at `jstatplot/target/universal`:
``` shell
sbt universal:package-zip-tarball
```
