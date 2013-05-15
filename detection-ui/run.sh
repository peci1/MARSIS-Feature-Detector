#!/bin/bash
mvn -e exec:java -Dexec.mainClass="cz.cuni.mff.peckam.ais.gui.DetectionFrame" -Dexec.classpathScope=runtime
