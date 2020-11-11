# Ping Identity Sample CDR AU Consent Application

This sample application provides the ability for a data holder to build their own consent application in accordance to the CX Standards and CX Guidelines (more information [here](https://consumerdatastandards.gov.au/consumer-data-standards/consumer-experience/)).

Account information is not typically stored in the identity system and the consent application allows the data holder full control to define their methods to retrieve and present customer accounts for selection during consent. The sample consent application also provides a way for data holders to manage/store consent externally.

## Sample Application Overview

The sample consent application is a Springboot application which leverages the [Agentless Integration Kit](https://support.pingidentity.com/s/marketplace-integration/a7i1W0000004ICWQA2/agentless-integration-kit) to interact with PingFederate. This Integration Kit allows PingFederate to integrate with an external application during the authentication process, such as a consent application.

The sample consent application abstracts the Agentless IK requirements, so the developers aren't required build that functionality. The sample application only requires the data holder to implement the following items:
1. Configure the application ([here](#configure-the-application)).
2. Optionally update the branding and UI ([here](#branding)).
3. Implement an Account Retrieval strategy ([here](#define-an-account-retrieval-strategy)).
4. Optionally implement an External Consent Storage strategy ([here](#define-a-consent-storage-strategy-optional)).

## Pre-requisites

The sample consent application comes in pre-compiled source code. To build the project, you'll need:
- JDK 11
    - JAVA_HOME and PATH environment settings.
      - JAVA_HOME=/path/to/java
      - PATH=$PATH:$JAVA_HOME/bin
    - Example: https://adoptopenjdk.net/
- Maven
    - MAVEN_HOME and PATH environment settings.
      - MAVEN_HOME=/path/to/maven
      - PATH=$PATH:$MAVEN_HOME/bin
    - https://maven.apache.org/

## Configure the application

The application configuration is stored in the [application.properties](src/main/resources/application.properties) properties file.

### Server and HTTP Settings

| Property Name | Description | Default Value
| --- | --- | ---
| server.tomcat.remote_ip_header | HTTP Header indicating the remote IP. | X-Forwarded-For
| server.tomcat.protocol_header | HTTP Header indicating the remote protocol. | X-Forwarded-Proto
| server.port | The port which Springboot will operate on. | 7879
| server.baseurl | The frontend base URL which the consent application is run on. | http://localhost:7879

### Session Management Settings

Being a Springboot application, session management is based on [Spring Session](https://spring.io/projects/spring-session).

| Property Name | Description | Default Value
| --- | --- | ---
| spring.session.store-type | Session state storage option for the application. | none

The following is an example of how to configure database storage for a clustered deployment.
```
#Example of how to configure clustering with Spring Security
#
#spring.datasource.driver-class-name=com.mysql.jdbc.Driver
#spring.datasource.url=jdbc:mysql://localhost/springSession
#spring.datasource.username=root
#spring.datasource.password=root
#
#spring.session.store-type=jdbc
#spring.session.jdbc.initialize-schema=always
#spring.session.timeout.seconds=900
#
#spring.h2.console.enabled=true
#spring.session.timeout.seconds=900
```

### PingFederate settings

| Property Name | Description | Default Value
| --- | --- | ---
| pf.baseurl | The frontend PingFederate base URL which the consent application will redirect back to upon completion. | https://sso.data-holder.local
| pf.baseurl.backend | The backend base URL which the consent application will interact with PingFederate on. | https://localhost:9031
| pf.ignoresslerrors | Ignore SSL errors for backend calls to PingFederate. Not recommended in production. | false
| pf.oauth.client.mgt.username | Username required to authenticate into PingFederate's client management service. The consent application uses this to receive extra metadata about the requesting client. A "Password Credential Validator" must be configured in the [OAuth AS settings](https://docs.pingidentity.com/bundle/pingfederate-101/page/ird1564002990806.html) under "OAuth Administrative Web Services Settings". | Administrator
| pf.oauth.client.mgt.password | Password required to authenticate into PingFederate's client management service. The consent application uses this to receive extra metadata about the requesting client. A "Password Credential Validator" must be configured in the [OAuth AS settings](https://docs.pingidentity.com/bundle/pingfederate-101/page/ird1564002990806.html) under "OAuth Administrative Web Services Settings". | 2FederateM0re

### Agentless Adapter Settings

| Property Name | Description | Default Value
| --- | --- | ---
| pf.agentless.instanceid | The instanceid value of the Agentless Adapter configured in PingFederate. | consentAgentlessAdapter
| pf.agentless.username | The username required to authenticate into the Agentless Adapter configured in PingFederate. Note that MTLS can be used instead of basic credentials. | consentappuser
| pf.agentless.password | The password required to authenticate into the Agentless Adapter configured in PingFederate. Note that MTLS can be used instead of basic credentials. | consentappuser

Using the following property values, the sample consent application can instead authenticate into the Agentless Adapter using MTLS.

| Property Name | Description | Default Value
| --- | --- | ---
| pf.agentless.mtls.keystore.location | The file location of the keystore which is used to authenticate into the Agentless Adapter configured in PingFederate. | /path/to/network.p12
| pf.agentless.mtls.keystore.password | The password of the keystore which is used to authenticate into the Agentless Adapter configured in PingFederate. | /path/to/network.p12
| pf.agentless.mtls.trustedca.location | The file location of the truststore which is used to authenticate into the Agentless Adapter configured in PingFederate. | /path/to/trustedcacerts.p12

## Branding

The data holder can manage its branding by updating the provided [html template](src/main/resources/templates). This template leverages branding provided by PingFederate, so in many cases this would suffice.

## Define an account retrieval strategy

The data holder is required to implement abstract class [AbstractAccountRetriever](src/main/java/com/pingidentity/ps/cdr/consentapp/sdk/account/AbstractAccountRetriever.java) as a way to integrate into the data holder's account repository.

### Sample implementations

Ping Identity has provided the following implementation classes for your reference (or usage):
- [SampleAccountRetriever](src/main/java/com/pingidentity/ps/cdr/consentapp/impl/sample/account/SampleAccountRetriever.java):
    - A sample implementation which calls [Deepthought's administrative API's](https://github.com/bizaio/deepthought) to retrieve account information.
- [ChainedAttributeAccountRetriever](src/main/java/com/pingidentity/ps/cdr/consentapp/impl/standard/account/ChainedAttributeAccountRetriever.java):
    - If accounts can be received in PingFederate's authentication flow, then this implementation will present accounts provided in PingFederate's chained attributes.

### Configuration

The account retrieval implementation is configured in [application.properties](src/main/resources/application.properties) with the following settings:
- account.retriever.class.impl
    - Fully qualified class name of the account retrieval implementation.
    - Example: com.pingidentity.ps.cdr.consentapp.impl.sample.account.SampleAccountRetriever
- account.retriever.configuration
    - Configuration specific to the implementation. It can be a JSON object (as the sample provides), however this is not enforced.
    - Example:
    ```
    { "baseUrl": "http://localhost:8088/dio-au", "client-id": "deepthought-admin-service", "client-secret": "2FederateM0re", "scopes": "DEEPTHOUGHT:ADMIN:GRANT:WRITE DEEPTHOUGHT:ADMIN:CUSTOMER:READ DEEPTHOUGHT:ADMIN:CUSTOMER:WRITE DEEPTHOUGHT:ADMIN:BANK_ACCOUNT:READ DEEPTHOUGHT:ADMIN:BANK_ACCOUNT:WRITE", "brand-id": "abaaef28-e212-4532-8223-c875e80692ac", "ignore-ssl": true }
    ```

## Define a consent storage strategy (Optional)

The data holder may wish to store or perform an action after the consent screen has been approved. This is optional as the solution provided by Ping Identity captures consent upon code exchange and is stored as a persisted grant.

The data holder can implement interface [IExternalConsentStorage](src/main/java/com/pingidentity/ps/cdr/consentapp/sdk/extconsent/IExternalConsentStorage.java) as a way to integrate into an external consent store after the user clicks approve.

### Sample implementations

Ping Identity has provided the following implementation classes for your reference (or usage):
- [SampleConsentStorageImpl](src/main/java/com/pingidentity/ps/cdr/consentapp/impl/sample/extconsent/SampleConsentStorageImpl.java):
    - A sample implementation which calls [Deepthought's administrative API's](https://github.com/bizaio/deepthought) to create a consent record.
- [NoExternalConsentStorageImpl](src/main/java/com/pingidentity/ps/cdr/consentapp/impl/standard/extconsent/NoExternalConsentStorageImpl.java):
    - This implementation can be used if external consent storage is not required.

### Configuration

The account retrieval implementation is configured in [application.properties](src/main/resources/application.properties) with the following settings:
- external.consent.class.impl
    - Fully qualified class name of the external consent storage implementation.
    - Example: com.pingidentity.ps.cdr.consentapp.impl.sample.extconsent.SampleConsentStorageImpl
- external.consent.configuration
    - Configuration specific to the implementation. It can be a JSON object (as the sample provides), however this is not enforced.
    - Example:
    ```
    { "baseUrl": "http://localhost:8088/dio-au", "client-id": "deepthought-admin-service", "client-secret": "2FederateM0re", "scopes": "DEEPTHOUGHT:ADMIN:GRANT:WRITE DEEPTHOUGHT:ADMIN:CUSTOMER:READ DEEPTHOUGHT:ADMIN:CUSTOMER:WRITE DEEPTHOUGHT:ADMIN:BANK_ACCOUNT:READ DEEPTHOUGHT:ADMIN:BANK_ACCOUNT:WRITE", "brand-id": "abaaef28-e212-4532-8223-c875e80692ac", "ignore-ssl": true }

## Run the consent application

### Install the maven project

Under the project folder (where the pom.xml resides), run the following command:

- mvn install

### Run the consent application

Under the project folder (where the pom.xml resides), run the following command:

- java -jar target/cdr-au-sample-consentapp-0.0.1-SNAPSHOT.jar