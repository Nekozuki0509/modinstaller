![GitHub Downloads (all assets, all releases)](https://img.shields.io/github/downloads/Nekozuki0509/modinstaller/total)
[![Discord](https://img.shields.io/discord/1241236305741090836?logo=discord&color=5765f2)](https://discord.gg/352Cdy8MjV)
[![Static Badge](https://img.shields.io/badge/litlink-Nekozuki0509-9594f9)](https://lit.link/nekozuki0509)
# ModInstaller
## description
It's a fabric mod installer that allows you to easily download fabric mods from modrinth.
## TODO
1. Download this jar file to any location
1. Run once
1. edit config file which is name of config.json.
1. Run it again and the mod installation will begin!
> [!CAUTION]
> - **You can also run it by double-clicking the jar file, but it is recommended to run it on the console since you do not know which id did not work and which mod's specified version could not be found.**
## config file
```
{
  "_vcomment_" : "Version of mod to download",
  "version" : "1.21",
  "_bcomment_" : "Directory containing the jar of the mod to be downloaded",
  "baseModsDir" : "C:/Users/User/AppData/Roaming/.minecraft/mods",
  "_icomment_" : "modrinth project ID of the mod to be downloaded that is not in “baseModsDir“",
  "ids" : []
}
```
