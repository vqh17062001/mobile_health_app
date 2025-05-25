# MongoDB Atlas Database Setup

This module provides functionality to create a MongoDB Atlas database structure from a JSON schema file.

## Overview

The `CreateDB` class reads a JSON file that describes the database structure (collections, indexes, validators) and creates the corresponding structure in MongoDB Atlas.

## Prerequisites

1. MongoDB Atlas account with a cluster created
2. Connection string to your MongoDB Atlas cluster
3. Required dependencies in your app's build.gradle.kts file:
   ```kotlin
   // MongoDB dependencies
   implementation("org.mongodb:mongodb-driver-kotlin-sync:4.11.1")
   implementation("org.mongodb:mongodb-driver-kotlin-coroutine:4.11.1")
   implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
   ```

## JSON Schema Format

The JSON schema file should follow this format:

```json
{
  "collections": [
    {
      "name": "collection_name",
      "schema": {
        "field1": "Type",
        "field2": "Type"
      },
      "indexes": [
        { "fields": { "field1": 1 }, "unique": true },
        { "fields": { "field2": 1 }, "unique": false }
      ],
      "validator": {
        "$jsonSchema": {
          "bsonType": "object",
          "required": ["field1", "field2"],
          "properties": {
            "field1": { "bsonType": "string" },
            "field2": { "bsonType": "int" }
          }
        }
      },
      "options": {
        "timeseries": {
          "timeField": "timestamp",
          "metaField": "metadata",
          "granularity": "seconds"
        }
      }
    }
  ]
}
```

The "schema" field is optional and is used for documentation purposes only. The actual schema is enforced by the validator.

## Usage

### 1. Configuration

Update the connection string in the `CreateDB` class:

```kotlin
private val connectionString = "YOUR_MONGODB_ATLAS_CONNECTION_STRING"
private val databaseName = "mobile_health_app"
```

### 2. Using the CreateDB class

```kotlin
// Create an instance of CreateDB
val createDB = CreateDB()

// In a coroutine or suspend function
viewModelScope.launch {
    try {
        // Create the database structure from a JSON file
        createDB.createDatabaseFromJson(context, "/path/to/schema.json")
        
        // Optionally populate with sample data
        createDB.populateWithSampleData()
    } catch (e: Exception) {
        Log.e("TAG", "Error: ${e.message}")
    } finally {
        // Close the MongoDB connection when done
        createDB.closeConnection()
    }
}
```

### 3. Using the DatabaseSetupActivity

The `DatabaseSetupActivity` provides a simple UI to trigger the database creation process. It expects the schema file to be in the app's assets folder with the name "DBMobila.txt".

## Special Collection Types

### Timeseries Collections

For collections like "sensor_readings" that need to be created as a timeseries collection, specify the options in the JSON schema:

```json
"options": {
  "timeseries": {
    "timeField": "timestamp",
    "metaField": "metadata",
    "granularity": "seconds"
  }
}
```

## Security Considerations

- Store the MongoDB Atlas connection string securely, not hardcoded in your app.
- Consider using environment variables or a secure key storage mechanism.
- Implement proper authentication and authorization in your MongoDB Atlas cluster.
- Use encrypted fields for sensitive data as specified in the schema.

## Error Handling

The `CreateDB` class includes comprehensive error handling and logging. Check your logcat for messages with the tag "CreateDB".
