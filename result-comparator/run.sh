#!/bin/bash
mvn -e exec:java -Dexec.mainClass="cz.cuni.mff.peckam.ais.ResultComparator" -Dexec.classpathScope=runtime -Dexec.args="../../data/ _SUM_FITTING _SUM_PERIODOGRAM 3863 4000"
