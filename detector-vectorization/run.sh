#!/bin/bash
mvn -e exec:java -Dexec.mainClass="cz.cuni.mff.peckam.ais.detection.VectorizationDetector" -Dexec.classpathScope=runtime -Dexec.args="../../data/387X/FRM_AIS_RDR_3874.LBL 0"
