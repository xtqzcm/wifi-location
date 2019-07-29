# wifi-localization
source code of HKUST UROP1000 project


## method
1. compute distance and locate<br/>
   --------------------------------
   similar principle to GPS localization<br/>
   use wifi RSSI to compute the distance<br/>
   with 3 different access points then able to compute and locate the receiver<br/>
   
2. use fingerprint<br/>
   --------------------
   get the current wifi scan result with access point's MAC and RSSI<br/>
   compare with a set of pre-collected data<br/>
   determine the similarity and use probability to locate<br/>
