# Postman Collection: Pegelhub Core

This Postman collection contains API requests for the Pegelhub core.

## Usage Instructions

1. Install [Postman](https://www.postman.com/downloads/), if you haven't already.
2. Import `pegelhub.postman_collection.json`.
3. Run the Pegelhub core.
4. Set the required variables in the collection or environment to match your specific setup.
   - `baseAddress`: The IP address of your Pegelhub core instance.
   - `port`: The port of the Pegelhub core application.
   - `apiPath`: The API path of your core application. This only needs to be modified if you changed the API path in the Pegelhub core application.
   - `accessToken`: A Keycloak access token for the request you want to execute.

## Collection Structure

### Metadata

- **Supplier**
  - **Get All**: Sends a GET request to retrieve all suppliers.
  - **Get All With Measurement**: Sends a GET request to retrieve all suppliers with their respective last measurement.
  - **Get by Id**: Sends a GET request to retrieve a supplier by UUID.
  - **Create Supplier**: Sends a POST request to create a new supplier.
  - **Delete Supplier**: Sends a DELETE request to delete a supplier.

### Data

- **Measurement**
  - **Get Measurement in Range**: Sends a GET request to retrieve all measurements within a range.
  - **Measurement by Supplier**: Sends a GET request to retrieve all measurements by a supplier within a range.

## Executing Requests

You can execute the requests individually or run them as part of a sequence.

The requests in this collection use `Authorization: Bearer {{accessToken}}`. Request a token from Keycloak with client credentials, then paste the access token into the `accessToken` collection or environment variable. Use an operator token for metadata writes and a connector token for connector-owned data writes.

Some requests require values for database objects that already exist in your local database:

- **Get by Id**: Requires the UID of a supplier.
- **Delete Supplier**: Requires the UID of a supplier.
- **Get Measurement in Range**: Requires a measurement range.
- **Measurement by Supplier**: Requires a supplier and measurement range.

For more information on each request's details and parameters, refer to the request documentation within the collection.
