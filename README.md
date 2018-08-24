# ExportSMS
Android app to export text messages to JSON file

## Requirements
- Android API level +19 (KitKat)
- `READ_SMS` and `WRITE_EXTERNAL_STORAGE` permissions
- Note: Only tested on Android 6.0 (Marshmallow)

## Usage
Open the app and press the export button. This will create a file called SMS-Export-YYYYMMDD.json in your downloads folder with all SMS messages. Note: currently multimedia messages are not exported.

## View most contacted phone numbers
`jq '.[] | .address' SMS-Export-20180824.json | sed 's/[^0-9]//g' | sort | uniq -c | sort -n`
