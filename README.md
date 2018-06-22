# NoseDetection
The above repository allows usage of both Camera1 and Camera2 APIs with Google Mobile Vision for face Detection. This project features a live ROI of the nose region from the front camera captured using the mapped facial landmarks. It also saves on detection of a new face and displays the last nose detected on idle state. The application also includes an auto-refresh feature which can be triggered to avoid stagnation during continuos usage.

## Features
The following is a list of features that can be used to configure the application
* bBoxScaleFactor - The following scale factor can be used to change the ROI of the capture. This scale factor scales both the width and height of the bounding box keeping the initial coordinate of the bounding box the same. This initial point is calibrated using the detected face center landmark, nose width and height, and preview width and height. 

* handler.postdelayed - The following handler object triggers the activity to recreate, thereby refreshing all it's instances. The current value is set to 15 minutes and can be edited by changing the value of minutes in handler.postDelayed (runnableCode,10000\*6\***15**)'

* useCamera2 - Flag set to use Camera2 API, API level of >21 is needed else the application will crash. If needed to run for a lower API level do set value to 'false'.

* person_store_count - Currently set to 1, populates a BitmapArrayList with images to store and display during idle state. To reduce duplicates and false negatives brought in by vision api only the first detected nose was saved per face id. 
