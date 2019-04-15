# Table of contents
 - [API Server](#api)
 - [Documentation](#documentation)
 - [Generate a new server stub](#generate)
 - [Running the server](#run1)
 - [Modifications needed](#modifications)
 - [Run the server](#run2)
 - [Example](#example)
 - [Updates to Swagger specification](#updates)
 - [Workflow for updates](#workflow)


Prototype implementation used RabitMQ for Pegasus-Mobius interaction. As part of this feature, REST API is implemented. Please find the details for API below:

# <a name="api"></a>API Server
Swagger enables the generation of clients and servers in a variety of common programming languages via the swagger codegen project.

Clients are generated to be fully formed and functional from the generated files including documentation
Servers are generated as stubbed code, and require the logical operations to be added by the user
The server within this repository is based on Spring Boot and Java API for RESTful Web Services.

## <a name="documentation"></a>Documentation
[API Documentation](https://app.swaggerhub.com/apis-docs/kthare10/mobius/1.0.0#/)

## <a name="generate"></a>Generate a new server stub

In a browser, go to [Swagger definition](https://app.swaggerhub.com/apis/kthare10/mobius/1.0.0)

From the generate code icon (downward facing arrow), select Download API > JSON Resolved

A file named swagger-client-generated.zip should be downloaded. This file will contain swagger.json. Extract the json file from the swagger-client-generated.zip and run the following command to generate the Srpingboot Swagger server.

Use myOptions.json config file to specify name of the packages. Refer [Swagger-Codegen](https://github.com/swagger-api/swagger-codegen/wiki/Server-stub-generator-HOWTO#java-springboot) for more details.

```
$ cat myOptions.json
{
  "basePackage": "org.renci.mobius",
  "modelPackage": "org.renci.mobius.model",
  "apiPackage": "org.renci.mobius.api",
  "configPackage":"org.renci.mobius.config"
}
$ swagger-codegen generate -i swagger.json -l spring -c myOptions.json -o mobius
```
## <a name="run1"></a>Running the server
The server stub is runnable upon generation with following modifications.
### <a name="modifications"></a>Modifications needed:
- Change java version to 1.8 in pom.xml
- For APIs which have body in post(network, storage and compute). Modify <Name>Api.java <Name>ApiController.java and <Name>ApiControllerIntegrationTest.java to add a , between parameters body and workflowID

## <a name="run2"></a>Run the server
```
cd /PATH_TO/mobius/
java -jar target/mobius-spring-1.0.0.jar
```
Validate that the server is running at: http://localhost:8080/swagger.json

The stubbed server will not have any logic encoded into it, however should return the response magic for calls made to valid endpoints.

## <a name="example"></a>Example:
```
$ curl -X POST -i "localhost:8080/mobius/workflow" -H "accept: application/json"
HTTP/1.1 501
Content-Length: 0
Date: Wed, 12 Dec 2018 16:43:36 GMT
Connection: close
```
Since by default, there is no implementation for any of the APIs, stub server returns 501 - HTTP Status Not Implemented.

## <a name="updates"></a>Updates to Swagger specification
Since swaggerhub only generates server stub code, it becomes the task of the developer(s) to differentiate foundational code changes that occur when the underlying specification is updated.

There is no good way to predict a-priori which elements will need to be modified, and the experience of the developer(s) integrating the updated code will be relied upon to do the updates effectively.

### <a name="workflow"></a>Workflow for updates:
- Update the specification in swaggerhub and save the results
- Generate new Spring Boot server stub code into a separate directory
- Diff the elements of the new stub code as they correspond to their counterparts in the repository
- Manually implement the diffs where needed
- Add new code to enable the new features of the updated specification
