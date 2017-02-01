# About MinecraftRegionManager
A Minecraft Bukkit plugin for manipulating region files and easing builds backup/restore during map resets

Installation
============
Just copy the resulting jar file in your server plugins subdirectory and relaunch your server.

Caveats
=======
Developed with Java 1.8 (recompile if your server is running under a lower version) and tested only on Minecraft 1.7.10 with a KCauldron 1.52 server.

Versions and changelog
======================
	1.00	2017-01-25	Added the /resetmap command for resetting dimensions to marked items
	                  	Added a /verifyregion command for checking region files integrity
	                  	Modified the /showlocation command to indicate if a region/chunk is marked
	                  	Modified the /markarea command to say something after first corner marking
	0.91	2016-11-09	Added a config.yml file for admin modifiable configuration
				Added a /unmarkeverything command for testing players
				Added a /listreports command for admins
	0.9	2016-11-08	Initial public release

License
=======
This open source software is distributed under a BSD license (see the "License" file for details).

Author
======
Hubert Tournier

February 1st, 2017
