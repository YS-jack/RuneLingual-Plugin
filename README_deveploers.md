this readme is for developers of this plugin or the Runelite reviewers, to give general idea of this plugin.

1. features of this plugin
   this plugin aims to translate everything on the screen. it also supports chat input for languages that aren't latin characters. refer to the normal readme for some images.
   it translates using transcripts created manually on the [RuneLingual transcript](https://github.com/YS-jack/Runelingual-Transcripts) repository, or via DeepL's API translation, which is selectable in the config. 
3. brief explanation of what happens
   the plugin checks through every texts in the menu entry and every visible widgets. if the user selected to use deepl, it will check if it has already translated the text in the past if it has it will display the same translation, else translate the text via API.
   if the user selected to translate via manual transcript, which is stored in sql format, it will search the database with a query and show the returned text.
5. flow after installation
6. limitation
   it cannot translate texts that are not widgets nor menu entries, such as texts on the world map.
7. features to be added
   *create a setting interface that works like the plugin configuration, but that can display texts in the selected language
   *add bank tags to every items so they can be searched in the selected language. (may not be possible)
   *make it work with other useful plugins like the menu entry swapper, ground marker. may need to make a similar version of them in this plugin, or make a PR to those plugins.
