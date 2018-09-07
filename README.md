# ExportSMS
Android app to export text messages to JSON file

## Requirements
- Android API level +19 (KitKat)
- `READ_SMS` and `WRITE_EXTERNAL_STORAGE` permissions
- Note: Only tested on Android 6.0 (Marshmallow)

## Usage
Open the app and press the export button. The app will create the following files in your download folder: `SMS-Export-YYYYMMDD.json` (SMS text messages), `MMS-Export-YYYYMMDD.json` (multimedia message metadata), and for each image/video `MMS-Part-<id>.<file extension>`. If you press the button and it looks like nothing happened, don't worry - exporting messages can take a while.

## View most contacted phone numbers
`jq '.[] | .address' SMS-Export-20180824.json | sed 's/[^0-9]//g' | sort | uniq -c | sort -n`
