# OpeningHoursFragment


This is a re-usable UI element for editing opening hour values that covers the complete specification, instead of a broken, write only, subset.

![Screenshot](documentation/images/Screenshot_basic.png)


## Using the opening hours editor

The OpenStreetMap opening hours specification is fairly complex and does not readily lend itself to a simple and intuitive user interface.

However most of the time you will likely only be using a small part of the definition. The editor takes this in to account by trying to hide the more obscure features in menus and most of the time reducing the "on the road" use to small customizations of pre-defined templates.

If you have defined a default template (do this via the "Manage templates" menu item) it will be loaded automatically when the editor is started with an empty value. The "Load template" function you can load any saved template and with the "Save template" menu you can save the current value as a template.

Naturally you can build an opening hours value from scratch, but we would recommend using one of the existing templates as a starting point.

If an existing opening hours value is loaded, an attempt is made to auto-correct it to conform to the opening hours specification. If that is not possible the rough location where the error occurred will be highlighted in the display of the raw OH value and you can try and correct it manually. Roughly a quarter of the OH values in the OpenStreetMap database have problems, but less than 10% can't be corrected, see [OpeningHoursParser](https://github.com/simonpoole/OpeningHoursParser) for more information on what deviations from the specification are tolerated.
